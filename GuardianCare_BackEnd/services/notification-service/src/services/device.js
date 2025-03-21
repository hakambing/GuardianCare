const Device = require('../models/device');
const logger = require('../utils').logger;

class DeviceService {
  async registerDevice(userId, deviceToken, deviceType) {
    try {
      // Use findOneAndUpdate with upsert to avoid duplicates
      const device = await Device.findOneAndUpdate(
        { userId, deviceToken },
        { userId, deviceToken, deviceType },
        { upsert: true, new: true }
      );
      
      logger.info(`Device registered for user ${userId}`);
      return device;
    } catch (error) {
      logger.error('Error registering device:', error);
      throw error;
    }
  }

  async getDevicesByUserId(userId) {
    try {
      return await Device.find({ userId });
    } catch (error) {
      logger.error(`Error getting devices for user ${userId}:`, error);
      throw error;
    }
  }

  async getDevicesByUserIds(userIds) {
    try {
      return await Device.find({ userId: { $in: userIds } });
    } catch (error) {
      logger.error('Error getting devices for multiple users:', error);
      throw error;
    }
  }

  async getAllDevices() {
    try {
      return await Device.find({});
    } catch (error) {
      logger.error('Error getting all devices:', error);
      throw error;
    }
  }

  async removeDevice(userId, deviceToken) {
    try {
      const result = await Device.deleteOne({ userId, deviceToken });
      logger.info(`Device removed for user ${userId}`);
      return result;
    } catch (error) {
      logger.error(`Error removing device for user ${userId}:`, error);
      throw error;
    }
  }
}

module.exports = new DeviceService();
