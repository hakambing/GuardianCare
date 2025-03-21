const axios = require('axios');
const logger = require('../utils').logger;

class ElderlyServiceClient {
  constructor() {
    this.baseURL = process.env.ELDERLY_SERVICE_URL || 'http://elderly-management-service:3000';
    
    logger.info(`[ELDERLY CLIENT] Initializing with base URL: ${this.baseURL}`);
    
    // Configure axios with longer timeout and retry logic for external services
    this.client = axios.create({
      baseURL: this.baseURL,
      timeout: 10000, // 10 seconds timeout
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      }
    });
    
    // Add request interceptor for logging
    this.client.interceptors.request.use(config => {
      logger.info(`[ELDERLY CLIENT] Making request to: ${config.method.toUpperCase()} ${config.url}`);
      return config;
    }, error => {
      logger.error(`[ELDERLY CLIENT] Request error:`, error);
      return Promise.reject(error);
    });
    
    // Add response interceptor for logging
    this.client.interceptors.response.use(response => {
      logger.info(`[ELDERLY CLIENT] Received response from: ${response.config.url} with status: ${response.status}`);
      return response;
    }, error => {
      if (error.response) {
        logger.error(`[ELDERLY CLIENT] Response error: ${error.response.status} - ${error.response.statusText}`);
      } else if (error.request) {
        logger.error(`[ELDERLY CLIENT] No response received:`, error.message);
      } else {
        logger.error(`[ELDERLY CLIENT] Request setup error:`, error.message);
      }
      return Promise.reject(error);
    });
  }

  async getElderlyCaretaker(elderlyId) {
    try {
      logger.info(`[ELDERLY CLIENT] Getting caretaker for elderly: ${elderlyId}`);
      const response = await this.client.get(`/api/elderly/${elderlyId}/caretaker`);
      return response.data;
    } catch (error) {
      logger.error(`[ELDERLY CLIENT] Error fetching caretaker for elderly ${elderlyId}:`, error.message);
      // Return null instead of throwing to allow fallback behavior
      return null;
    }
  }

  async getUserById(userId) {
    try {
      logger.info(`[ELDERLY CLIENT] Getting user by ID: ${userId}`);
      const response = await this.client.get(`/api/users/${userId}`);
      return response.data;
    } catch (error) {
      logger.error(`[ELDERLY CLIENT] Error fetching user ${userId}:`, error.message);
      // Return null instead of throwing to allow fallback behavior
      return null;
    }
  }

  async getCaretakersByType() {
    try {
      logger.info(`[ELDERLY CLIENT] Getting all caretakers`);
      const response = await this.client.get('/api/users/type/caretaker');
      return response.data;
    } catch (error) {
      logger.error('[ELDERLY CLIENT] Error fetching caretakers:', error.message);
      // Return empty array instead of throwing
      return [];
    }
  }

  async getElderlyForCaretaker(caretakerId) {
    try {
      logger.info(`[ELDERLY CLIENT] Getting elderly for caretaker: ${caretakerId}`);
      const response = await this.client.get(`/api/elderly/caretaker/${caretakerId}`);
      return response.data;
    } catch (error) {
      logger.error(`[ELDERLY CLIENT] Error fetching elderly for caretaker ${caretakerId}:`, error.message);
      // Return empty array instead of throwing
      return [];
    }
  }
}

module.exports = new ElderlyServiceClient();
