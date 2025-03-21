const admin = require('firebase-admin');
const logger = require('../utils').logger;

class FCMService {
  constructor() {
    if (!admin.apps.length) {
      admin.initializeApp({
        credential: admin.credential.cert({
          projectId: process.env.FIREBASE_PROJECT_ID,
          clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
          privateKey: process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n')
        })
      });
    }
    logger.info('Firebase Admin SDK initialized');
  }

  async sendToCaretakers(caretakers, notification) {
    try {
      // Collect all device tokens from all caretakers
      const allTokens = caretakers.reduce((tokens, caretaker) => {
        if (caretaker.deviceTokens && caretaker.deviceTokens.length > 0) {
          return [...tokens, ...caretaker.deviceTokens];
        }
        return tokens;
      }, []);
      
      if (allTokens.length === 0) {
        logger.warn('No device tokens found for caretakers');
        return;
      }
      
      // Send to all tokens at once
      return await this.sendMulticast(allTokens, notification);
    } catch (error) {
      logger.error('Error sending notifications to caretakers:', error);
      throw error;
    }
  }

  async sendNotification(token, notification) {
    try {
      // Convert all data values to strings
      const stringifiedData = {};
      if (notification.data) {
        Object.keys(notification.data).forEach(key => {
          if (notification.data[key] !== null && notification.data[key] !== undefined) {
            // Convert objects and arrays to JSON strings
            stringifiedData[key] = typeof notification.data[key] === 'object' 
              ? JSON.stringify(notification.data[key])
              : String(notification.data[key]);
          }
        });
      }

      const message = {
        token,
        notification: {
          title: notification.title,
          body: notification.body
        },
        data: stringifiedData,
        android: {
          priority: 'high',
          notification: {
            sound: 'default',
            priority: 'high',
            channelId: 'guardiancare-notifications'
          }
        },
        apns: {
          payload: {
            aps: {
              sound: 'default',
              badge: 1
            }
          }
        }
      };

      const response = await admin.messaging().send(message);
      logger.info(`Notification sent successfully: ${response}`);
      return response;
    } catch (error) {
      logger.error('Error sending FCM notification:', error);
      throw error;
    }
  }

  async sendMulticast(tokens, notification) {
    try {
      logger.info(`[FCM] Sending multicast to ${tokens.length} tokens`);
      logger.info(`[FCM] Notification: ${JSON.stringify({
        title: notification.title,
        body: notification.body,
        dataKeys: notification.data ? Object.keys(notification.data) : []
      })}`);
      
      // Log the first few characters of each token for debugging
      tokens.forEach((token, index) => {
        const tokenPreview = token.substring(0, 10) + '...';
        logger.info(`[FCM] Token ${index + 1}: ${tokenPreview}`);
      });
      
      // Convert all data values to strings
      const stringifiedData = {};
      if (notification.data) {
        Object.keys(notification.data).forEach(key => {
          if (notification.data[key] !== null && notification.data[key] !== undefined) {
            stringifiedData[key] = typeof notification.data[key] === 'object' 
              ? JSON.stringify(notification.data[key])
              : String(notification.data[key]);
          }
        });
      }

      // Instead of using sendMulticast, we'll send individual messages
      // This avoids the batch API which might be causing the 404 error
      const results = await Promise.allSettled(tokens.map(async (token) => {
        try {
          const message = {
            token,
            notification: {
              title: notification.title,
              body: notification.body
            },
            data: stringifiedData,
            android: {
              priority: 'high',
              notification: {
                sound: 'default',
                priority: 'high',
                channelId: 'guardiancare-notifications'
              }
            },
            apns: {
              payload: {
                aps: {
                  sound: 'default',
                  badge: 1
                }
              }
            }
          };
          
          const result = await admin.messaging().send(message);
          logger.info(`[FCM] Message sent successfully to token ${token.substring(0, 10)}...: ${result}`);
          return { success: true, messageId: result, token };
        } catch (err) {
          logger.error(`[FCM] Error sending to token ${token.substring(0, 10)}...: ${err.message}`);
          if (err.code === 'messaging/registration-token-not-registered') {
            // Remove invalid token from the database
            await require('./device').removeDevice(null, token);
            logger.warn(`[FCM] Removed invalid token: ${token.substring(0, 10)}...`);
          }
          return { success: false, error: err, token };
        }
      }));
      
      // Process results
      const successResults = results.filter(r => r.status === 'fulfilled' && r.value.success);
      const failureResults = results.filter(r => r.status === 'rejected' || (r.status === 'fulfilled' && !r.value.success));
      
      logger.info(`[FCM] Individual messages sent. Success: ${successResults.length}/${tokens.length}`);
      
      // Check for failures and log them
      if (failureResults.length > 0) {
        const failureDetails = failureResults.map(r => {
          const token = r.status === 'fulfilled' ? r.value.token : r.reason?.token || 'unknown';
          const error = r.status === 'fulfilled' ? r.value.error : r.reason;
          return { token, error };
        });
        
        logger.error(`[FCM] Failed to send to ${failureResults.length} tokens. Details:`, 
          failureDetails.map(f => `${f.token.substring(0, 10)}...: ${f.error?.code || 'unknown'} - ${f.error?.message || 'unknown error'}`));
        
        // Check for common errors
        const invalidTokens = failureDetails
          .filter(f => f.error?.code === 'messaging/invalid-registration-token' || 
                       f.error?.code === 'messaging/registration-token-not-registered')
          .map(f => f.token);
        
        if (invalidTokens.length > 0) {
          logger.warn(`[FCM] Found ${invalidTokens.length} invalid tokens that should be removed`);
          // TODO: Implement automatic cleanup of invalid tokens
        }
      }
      
      // Return a response object similar to the sendMulticast response
      return {
        successCount: successResults.length,
        failureCount: failureResults.length,
        responses: results.map(r => {
          if (r.status === 'fulfilled') {
            return r.value.success ? 
              { success: true, messageId: r.value.messageId } : 
              { success: false, error: r.value.error };
          } else {
            return { success: false, error: r.reason };
          }
        })
      };
    } catch (error) {
      logger.error('[FCM] Error sending notifications:', error);
      throw error;
    }
  }
}

module.exports = new FCMService();
