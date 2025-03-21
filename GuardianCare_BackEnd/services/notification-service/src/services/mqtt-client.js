const mqtt = require('mqtt');
const jwt = require('jsonwebtoken');
const logger = require('../utils').logger;
const notificationService = require('./notification');
const config = require('../config');
const axios = require('axios');

class MQTTClient {
  constructor() {
    this.client = mqtt.connect(config.mqtt.brokerUrl, {
      clientId: `notification-service-${Math.random().toString(16).substr(2, 8)}`,
      username: config.mqtt.username,
      password: config.mqtt.password
    });

    this.setupEventHandlers();
  }

  setupEventHandlers() {
    this.client.on('connect', () => {
      logger.info('Connected to MQTT broker');
      
      // Subscribe to topics
      this.client.subscribe('guardiancare/device/+/fall', { qos: 1 });
      this.client.subscribe('guardiancare/device/+/status', { qos: 1 });
      
      logger.info('Subscribed to device topics');
    });

    this.client.on('message', async (topic, message) => {
      logger.info(`Received message on topic: ${topic}`);
      
      try {
        const payload = JSON.parse(message.toString());
        await this.handleMessage(topic, payload);
      } catch (error) {
        logger.error('Error handling MQTT message:', error);
      }
    });

    this.client.on('error', (error) => {
      logger.error('MQTT client error:', error);
    });

    this.client.on('close', () => {
      logger.info('MQTT client disconnected');
    });
  }

  async handleMessage(topic, payload) {
    const [prefix, _, deviceId, messageType] = topic.split('/');

    switch (messageType) {
      case 'fall':
        await this.handleFallDetection(deviceId, payload);
        break;
      case 'status':
        await this.handleDeviceStatus(deviceId, payload);
        break;
      default:
        logger.warn(`Unknown message type: ${messageType}`);
    }
  }

  async handleFallDetection(deviceId, payload) {
    try {
      const decoded = jwt.verify(payload.token, config.jwt.secret);
      const elderlyId = decoded.userId;
      await axios.post(config.services.authUrl + `/api/checkins`, {
        elderly_id: elderlyId,
        summary: 'User has fallen down and requires immediate medical assistance.',
        priority: 4,
        mood: -3,
        status: 'Fall Detected',
        transcript: null,
      });
      await notificationService.handleFallDetection(deviceId, {
        location: payload.location,
        sensorData: payload.sensorData,
        impact: payload.impact,
        timestamp: payload.timestamp
      });
      logger.info(`Fall detection processed for device ${deviceId}`);
    } catch (error) {
      logger.error('Error handling fall detection:', error);
    }
  }

  async handleDeviceStatus(deviceId, payload) {
    try {
      await notificationService.updateDeviceStatus(deviceId, {
        batteryLevel: payload.batteryLevel,
        wifiStrength: payload.wifiStrength,
        ipAddress: payload.ipAddress,
        timestamp: payload.timestamp
      });
      logger.info(`Status update processed for device ${deviceId}`);
    } catch (error) {
      logger.error('Error handling device status:', error);
    }
  }

  publish(topic, message) {
    this.client.publish(topic, JSON.stringify(message), { qos: 1 });
  }

  close() {
    this.client.end();
  }
}

module.exports = new MQTTClient();
