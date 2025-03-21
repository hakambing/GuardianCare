const { Notification, NotificationType } = require('../models/notification');
const fcmService = require('./fcm');
const deviceService = require('./device');
const elderlyServiceClient = require('./elderlyServiceClient');
const authServiceClient = require('./authServiceClient');
const logger = require('../utils').logger;

class NotificationService {
  async handleFallDetection(deviceId, data) {
    try {
      logger.info(`[FALL DETECTION] Processing fall detection for device ${deviceId}`);
      
      // Get elderly info from device ID (this would typically be a database query)
      const elderlyInfo = await this.getElderlyInfoFromDevice(deviceId);
      logger.info(`[FALL DETECTION] Found elderly info: ${JSON.stringify({
        id: elderlyInfo.id,
        name: elderlyInfo.name,
        caretakersCount: elderlyInfo.caretakers.length
      })}`);
      
      if (elderlyInfo.caretakers.length === 0) {
        logger.warn(`[FALL DETECTION] No caretakers found for elderly ${elderlyInfo.id}`);
      } else {
        // Log caretaker device tokens
        elderlyInfo.caretakers.forEach(caretaker => {
          logger.info(`[FALL DETECTION] Caretaker ${caretaker.id} has ${caretaker.deviceTokens?.length || 0} device tokens`);
          if (!caretaker.deviceTokens || caretaker.deviceTokens.length === 0) {
            logger.warn(`[FALL DETECTION] No device tokens for caretaker ${caretaker.id}`);
          }
        });
      }
      
      // Create notification
      const notification = new Notification({
        type: NotificationType.FALL_DETECTION,
        elderlyId: elderlyInfo.id,
        priority: 'HIGH',
        content: {
          title: 'Fall Detected',
          message: `A fall has been detected for ${elderlyInfo.name}. Immediate assistance may be required.`,
          elderlyName: elderlyInfo.name,  // Add elderlyName directly in content
          data: {
            location: data.location,
            sensorData: data.sensorData,
            impact: data.impact,
            timestamp: data.timestamp
          }
        },
        recipients: elderlyInfo.caretakers.map(caretaker => ({
          userId: caretaker.id,
          role: 'CARETAKER',
          deviceTokens: caretaker.deviceTokens,
          notificationSent: false
        }))
      });

      logger.info(`[FALL DETECTION] Created notification: ${notification._id}`);
      await notification.save();
      
      const result = await this.sendNotifications(notification);
      logger.info(`[FALL DETECTION] Notification sending result: ${JSON.stringify(result)}`);

      return notification;
    } catch (error) {
      logger.error('[FALL DETECTION] Error handling fall detection:', error);
      throw error;
    }
  }

  async updateDeviceStatus(deviceId, status) {
    try {
      // Store device status in database
      // This could be used to monitor device health and battery levels
      logger.info(`Device ${deviceId} status updated:`, status);
    } catch (error) {
      logger.error('Error updating device status:', error);
      throw error;
    }
  }

  async sendNotifications(notification) {
    try {
      logger.info(`[SEND NOTIFICATIONS] Sending notification ${notification._id} to recipients`);
      
      // Group recipients by their device tokens
      const recipientsByToken = new Map();
      
      notification.recipients.forEach(recipient => {
        if (!recipient.deviceTokens || recipient.deviceTokens.length === 0) {
          logger.warn(`[SEND NOTIFICATIONS] No device tokens for recipient ${recipient.userId}`);
          return;
        }
        
        recipient.deviceTokens.forEach(token => {
          recipientsByToken.set(token, recipient);
        });
      });
      
      // Send notifications to all tokens
      const tokens = Array.from(recipientsByToken.keys());
      
      if (tokens.length === 0) {
        logger.warn('[SEND NOTIFICATIONS] No device tokens found for any recipients');
        return { success: false, reason: 'no_tokens' };
      }
      
      logger.info(`[SEND NOTIFICATIONS] Sending to ${tokens.length} device tokens`);
      
      try {
        const fcmResult = await fcmService.sendMulticast(tokens, {
          title: notification.content.title,
          body: notification.content.message,
          data: {
            notificationId: notification._id.toString(),
            type: notification.type,
            elderlyId: notification.elderlyId.toString(),
            elderlyName: notification.content.elderlyName || "Unknown",
            ...notification.content.data
          }
        });
        
        logger.info(`[SEND NOTIFICATIONS] FCM result: success=${fcmResult.successCount}, failure=${fcmResult.failureCount}`);
        
        // Mark all recipients as notified
        notification.recipients.forEach(recipient => {
          recipient.notificationSent = true;
        });
        
        await notification.save();
        return { 
          success: fcmResult.successCount > 0,
          successCount: fcmResult.successCount,
          failureCount: fcmResult.failureCount
        };
      } catch (error) {
        logger.error('[SEND NOTIFICATIONS] Failed to send multicast notification:', error);
        // Don't mark recipients as notified if there was an error
        return { success: false, reason: 'fcm_error', error: error.message };
      }
    } catch (error) {
      logger.error('[SEND NOTIFICATIONS] Error sending notifications:', error);
      throw error;
    }
  }

  async markAsRead(notificationId, userId) {
    try {
      const notification = await Notification.findById(notificationId);
      
      if (!notification) {
        throw new Error('Notification not found');
      }

      const recipient = notification.recipients.find(r => r.userId.toString() === userId);
      if (recipient) {
        recipient.readTimestamp = new Date();
        await notification.save();
      }

      return notification;
    } catch (error) {
      logger.error('Error marking notification as read:', error);
      throw error;
    }
  }

  async getNotificationsForUser(userId, options = {}) {
    try {
      const { limit = 20, skip = 0, unreadOnly = false } = options;
      
      const query = {
        'recipients.userId': userId
      };
      
      if (unreadOnly) {
        query['recipients.readTimestamp'] = { $exists: false };
      }
      
      return await Notification.find(query)
        .sort({ timestamp: -1 })
        .skip(skip)
        .limit(limit);
    } catch (error) {
      logger.error('Error getting notifications:', error);
      throw error;
    }
  }

  async getElderlyInfoFromDevice(deviceId) {
    try {
      logger.info(`[ELDERLY INFO] Getting elderly info for device ${deviceId}`);
      
      // First try to get the user from the auth service
      let elderly = null;
      
      // Try auth service first
      try {
        logger.info(`[ELDERLY INFO] Getting user from auth service: ${deviceId}`);
        elderly = await authServiceClient.getUserById(deviceId);
        
        if (elderly) {
          logger.info(`[ELDERLY INFO] Found elderly user in auth service: ${elderly._id} (${elderly.name})`);
        }
      } catch (authError) {
        logger.error(`[ELDERLY INFO] Auth service lookup failed: ${authError.message}`);
        throw new Error(`Auth service error: ${authError.message}`);
      }
      
      // If we don't have an elderly user, throw an error
      if (!elderly) {
        logger.error(`[ELDERLY INFO] Elderly user not found for device ${deviceId}`);
        throw new Error(`Elderly user not found for device ${deviceId}`);
      }
      
      // Get the caretaker for this elderly
      let caretaker = null;
      const caretakerId = elderly.caretaker_id;
      
      if (caretakerId) {
        // Get caretaker from auth service
        try {
          caretaker = await authServiceClient.getUserById(caretakerId);
          if (caretaker) {
            logger.info(`[ELDERLY INFO] Found caretaker in auth service: ${caretaker._id} (${caretaker.name})`);
          }
        } catch (authError) {
          logger.error(`[ELDERLY INFO] Auth service caretaker lookup failed: ${authError.message}`);
          throw new Error(`Auth service caretaker error: ${authError.message}`);
        }
      }
      
      if (!caretaker) {
        logger.error(`[ELDERLY INFO] No caretaker found for elderly ${elderly._id}`);
        throw new Error(`No caretaker found for elderly ${elderly._id}`);
      }
      
      // Get all registered devices for the caretaker
      let caretakerDevices = [];
      try {
        caretakerDevices = await deviceService.getDevicesByUserId(caretaker._id);
        logger.info(`[ELDERLY INFO] Found ${caretakerDevices.length} devices for caretaker ${caretaker._id}`);
        
        if (caretakerDevices.length === 0) {
          logger.warn(`[ELDERLY INFO] No devices registered for caretaker ${caretaker._id}`);
          // Use FCM test token if no devices are registered
          caretakerDevices = [{ deviceToken: process.env.FCM_TEST_TOKEN }];
          logger.info(`[ELDERLY INFO] Using FCM_TEST_TOKEN as fallback for missing device tokens`);
        }
      } catch (deviceError) {
        logger.error(`[ELDERLY INFO] Error getting devices for caretaker ${caretaker._id}: ${deviceError.message}`);
        throw new Error(`Device service error: ${deviceError.message}`);
      }
      
      const result = {
        id: elderly._id,
        name: elderly.name,
        contactNumber: elderly.phone || elderly.contactNumber || elderly.mobile || null,
        caretakers: [{
          id: caretaker._id,
          name: caretaker.name,
          deviceTokens: caretakerDevices.map(device => device.deviceToken)
        }]
      };
      
      logger.info(`[ELDERLY INFO] Returning elderly info with ${result.caretakers.length} caretakers`);
      return result;
    } catch (error) {
      logger.error(`[ELDERLY INFO] Error getting elderly info for device ${deviceId}:`, error);
      throw error; // Re-throw the error instead of falling back to mock data
    }
  }
}

module.exports = new NotificationService();
