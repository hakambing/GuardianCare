const express = require('express');
const router = express.Router();
const notificationService = require('./services/notification');
const deviceService = require('./services/device');
const logger = require('./utils').logger;

// Health check endpoint
router.get('/health', (req, res) => {
  res.status(200).json({
    status: 'success',
    message: 'Notification service is running'
  });
});

// Get notifications for a user
router.get('/notifications', async (req, res) => {
  try {
    const { userId } = req.query;
    const options = {
      limit: parseInt(req.query.limit) || 20,
      skip: parseInt(req.query.skip) || 0,
      unreadOnly: req.query.unreadOnly === 'true'
    };

    if (!userId) {
      return res.status(400).json({
        status: 'error',
        message: 'userId is required'
      });
    }

    const notifications = await notificationService.getNotificationsForUser(userId, options);
    res.status(200).json({
      status: 'success',
      data: notifications
    });
  } catch (error) {
    logger.error('Error getting notifications:', error);
    res.status(500).json({
      status: 'error',
      message: 'Failed to get notifications'
    });
  }
});

// Mark notification as read
router.put('/notifications/:id/read', async (req, res) => {
  try {
    const { id } = req.params;
    const { userId } = req.body;

    if (!userId) {
      return res.status(400).json({
        status: 'error',
        message: 'userId is required'
      });
    }

    const notification = await notificationService.markAsRead(id, userId);
    res.status(200).json({
      status: 'success',
      data: notification
    });
  } catch (error) {
    logger.error('Error marking notification as read:', error);
    res.status(500).json({
      status: 'error',
      message: 'Failed to mark notification as read'
    });
  }
});

// M5StickC Plus endpoints
router.post('/m5stick/fall-detection', async (req, res) => {
  try {
    const { deviceId, location, sensorData, contactNumber } = req.body;

    if (!deviceId) {
      return res.status(400).json({
        status: 'error',
        message: 'deviceId is required'
      });
    }

    await notificationService.handleFallDetection(deviceId, { 
      location, 
      sensorData,
      contactNumber,
      timestamp: new Date().toISOString()
    });
    
    res.status(200).json({
      status: 'success',
      message: 'Fall detection processed successfully'
    });
  } catch (error) {
    logger.error('Error processing fall detection:', error);
    res.status(500).json({
      status: 'error',
      message: 'Failed to process fall detection'
    });
  }
});

router.post('/m5stick/wellbeing-check', async (req, res) => {
  try {
    const { deviceId, audioBase64 } = req.body;

    if (!deviceId || !audioBase64) {
      return res.status(400).json({
        status: 'error',
        message: 'deviceId and audioBase64 are required'
      });
    }

    const result = await notificationService.processWellbeingCheck(deviceId, audioBase64);
    res.status(200).json({
      status: 'success',
      data: result
    });
  } catch (error) {
    logger.error('Error processing wellbeing check:', error);
    res.status(500).json({
      status: 'error',
      message: 'Failed to process wellbeing check'
    });
  }
});

// Device registration
router.post('/devices/register', async (req, res) => {
  try {
    const { userId, deviceToken, deviceType } = req.body;

    if (!userId || !deviceToken || !deviceType) {
      return res.status(400).json({
        status: 'error',
        message: 'userId, deviceToken, and deviceType are required'
      });
    }

    // Check for JWT token in Authorization header
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      logger.warn('[DEVICE] Missing JWT token in Authorization header');
      // Continue without JWT validation for backward compatibility
    } else {
      logger.info('[DEVICE] JWT token provided in Authorization header');
      // In a production environment, you would validate the JWT token here
    }

    const device = await deviceService.registerDevice(userId, deviceToken, deviceType);
    
    res.status(200).json({
      status: 'success',
      message: 'Device registered successfully',
      data: device
    });
  } catch (error) {
    logger.error('Error registering device:', error);
    res.status(500).json({
      status: 'error',
      message: 'Failed to register device'
    });
  }
});

// Device unregistration
router.post('/devices/unregister', async (req, res) => {
  try {
    const { userId, deviceToken } = req.body;

    if (!userId || !deviceToken) {
      return res.status(400).json({
        status: 'error',
        message: 'userId and deviceToken are required'
      });
    }

    // Check for JWT token in Authorization header
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      logger.warn('[DEVICE] Missing JWT token in Authorization header for unregister');
      // Continue without JWT validation for backward compatibility
    } else {
      logger.info('[DEVICE] JWT token provided in Authorization header for unregister');
      // In a production environment, you would validate the JWT token here
    }

    await deviceService.removeDevice(userId, deviceToken);
    
    res.status(200).json({
      status: 'success',
      message: 'Device unregistered successfully'
    });
  } catch (error) {
    logger.error('Error unregistering device:', error);
    res.status(500).json({
      status: 'error',
      message: 'Failed to unregister device'
    });
  }
});

// Test FCM notification endpoint
router.post('/test/send-notification', async (req, res) => {
  try {
    let { token, title, body, data, userId } = req.body;

    // If token is not provided, try to find it from registered devices
    if (!token) {
      // First try to get userId from request
      if (userId) {
        logger.info(`[TEST] Trying to find device token for user: ${userId}`);
        const devices = await deviceService.getDevicesByUserId(userId);
        
        if (devices && devices.length > 0) {
          token = devices[0].deviceToken;
          logger.info(`[TEST] Found device token for user ${userId}: ${token.substring(0, 10)}...`);
        }
      }
      
      // If still no token, try to get from Authorization header (JWT)
      if (!token && req.headers.authorization) {
        try {
          const authHeader = req.headers.authorization;
          if (authHeader && authHeader.startsWith('Bearer ')) {
            const jwt = authHeader.substring(7);
            const decoded = require('jsonwebtoken').verify(jwt, process.env.JWT_SECRET);
            
            if (decoded && decoded.id) {
              const userDevices = await deviceService.getDevicesByUserId(decoded.id);
              if (userDevices && userDevices.length > 0) {
                token = userDevices[0].deviceToken;
                logger.info(`[TEST] Found device token from JWT for user ${decoded.id}: ${token.substring(0, 10)}...`);
              }
            }
          }
        } catch (err) {
          logger.warn('[TEST] Error decoding JWT:', err.message);
        }
      }
      
      // If still no token, try environment variable
      if (!token) {
        token = process.env.FCM_TEST_TOKEN;
        
        if (token && token !== 'REPLACE_WITH_YOUR_ACTUAL_FCM_TOKEN' && token !== 'your-fcm-device-token') {
          logger.info('[TEST] Using FCM_TEST_TOKEN from environment variables');
        } else {
          // Last resort: try to get the most recently registered device
          const allDevices = await deviceService.getAllDevices();
          if (allDevices && allDevices.length > 0) {
            // Sort by creation date, newest first
            allDevices.sort((a, b) => b.createdAt - a.createdAt);
            token = allDevices[0].deviceToken;
            logger.info(`[TEST] Using most recently registered device token: ${token.substring(0, 10)}...`);
          } else {
            return res.status(400).json({
              status: 'error',
              message: 'FCM token is required. No registered devices found and no FCM_TEST_TOKEN in environment.'
            });
          }
        }
      }
    }

    if (!token) {
      return res.status(400).json({
        status: 'error',
        message: 'FCM token is required in request body, from registered devices, or as FCM_TEST_TOKEN environment variable'
      });
    }

    logger.info(`[TEST] Sending test notification to token: ${token.substring(0, 10)}...`);
    
    const fcmService = require('./services/fcm');
    const result = await fcmService.sendNotification(token, {
      title: title || 'Test Notification',
      body: body || 'This is a test notification from GuardianCare',
      data: data || { type: 'TEST' }
    });
    
    res.status(200).json({
      status: 'success',
      message: 'Test notification sent successfully',
      result
    });
  } catch (error) {
    logger.error('[TEST] Error sending test notification:', error);
    res.status(500).json({
      status: 'error',
      message: `Failed to send test notification: ${error.message}`
    });
  }
});

// Test fall detection endpoint
router.post('/test/fall-detection', async (req, res) => {
  try {
    const { elderlyId, useTestToken } = req.body;

    if (!elderlyId) {
      return res.status(400).json({
        status: 'error',
        message: 'elderlyId is required'
      });
    }

    logger.info(`[TEST] Simulating fall detection for elderly: ${elderlyId}`);
    
    // If useTestToken is true, temporarily override the FCM_TEST_TOKEN
    let originalToken;
    if (useTestToken && req.body.testToken) {
      originalToken = process.env.FCM_TEST_TOKEN;
      process.env.FCM_TEST_TOKEN = req.body.testToken;
      logger.info(`[TEST] Using provided test token: ${req.body.testToken.substring(0, 10)}...`);
    }
    
    // Simulate fall detection
    const result = await notificationService.handleFallDetection(elderlyId, {
      location: req.body.location || "123 Main St, Singapore 123456", // Use string location for better display
      contactNumber: req.body.contactNumber, // Allow passing contact number in test
      sensorData: req.body.sensorData || { accX: -0.5, accY: -0.2, accZ: 0.1 },
      impact: req.body.impact || 'high',
      timestamp: new Date().toISOString()
    });
    
    // Restore original token if needed
    if (useTestToken && req.body.testToken) {
      process.env.FCM_TEST_TOKEN = originalToken;
    }
    
    res.status(200).json({
      status: 'success',
      message: 'Test fall detection processed successfully',
      notificationId: result._id
    });
  } catch (error) {
    logger.error('[TEST] Error processing test fall detection:', error);
    res.status(500).json({
      status: 'error',
      message: `Failed to process test fall detection: ${error.message}`
    });
  }
});

module.exports = router;
