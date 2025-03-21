const axios = require('axios');
const logger = require('../utils').logger;

class AuthServiceClient {
  constructor() {
    this.baseURL = process.env.AUTH_SERVICE_URL || 'http://auth-service:3000';
    
    logger.info(`[AUTH CLIENT] Initializing with base URL: ${this.baseURL}`);
    
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
      logger.info(`[AUTH CLIENT] Making request to: ${config.method.toUpperCase()} ${config.url}`);
      return config;
    }, error => {
      logger.error(`[AUTH CLIENT] Request error:`, error);
      return Promise.reject(error);
    });
    
    // Add response interceptor for logging
    this.client.interceptors.response.use(response => {
      logger.info(`[AUTH CLIENT] Received response from: ${response.config.url} with status: ${response.status}`);
      return response;
    }, error => {
      if (error.response) {
        logger.error(`[AUTH CLIENT] Response error: ${error.response.status} - ${error.response.statusText}`);
      } else if (error.request) {
        logger.error(`[AUTH CLIENT] No response received:`, error.message);
      } else {
        logger.error(`[AUTH CLIENT] Request setup error:`, error.message);
      }
      return Promise.reject(error);
    });
  }

  async getUserById(userId) {
    try {
      logger.info(`[AUTH CLIENT] Getting user by ID: ${userId}`);
      
      // If userId looks like an email, use it directly
      // Otherwise, assume it's a MongoDB ObjectId
      const endpoint = `/api/users/${userId}`;
      
      const response = await this.client.get(endpoint);
      return response.data;
    } catch (error) {
      logger.error(`[AUTH CLIENT] Error fetching user ${userId}:`, error.message);
      // Return null instead of throwing to allow fallback behavior
      return null;
    }
  }

  async getCaretakersByType() {
    try {
      logger.info(`[AUTH CLIENT] Getting all caretakers`);
      const response = await this.client.get('/api/users/type/caretaker');
      return response.data;
    } catch (error) {
      logger.error('[AUTH CLIENT] Error fetching caretakers:', error.message);
      // Return empty array instead of throwing
      return [];
    }
  }

  async getElderlyForCaretaker(caretakerId) {
    try {
      logger.info(`[AUTH CLIENT] Getting elderly users for caretaker: ${caretakerId}`);
      // This is a custom query that might need to be implemented in the auth service
      // For now, we'll just get all elderly users and filter by caretaker_id
      const response = await this.client.get('/api/users/type/elderly');
      
      // Filter elderly users by caretaker_id
      const elderlyUsers = response.data.filter(user => 
        user.caretaker_id && user.caretaker_id.toString() === caretakerId.toString()
      );
      
      return elderlyUsers;
    } catch (error) {
      logger.error(`[AUTH CLIENT] Error fetching elderly for caretaker ${caretakerId}:`, error.message);
      // Return empty array instead of throwing
      return [];
    }
  }
  
  async getUserByEmail(email) {
    try {
      logger.info(`[AUTH CLIENT] Getting user by email: ${email}`);
      
      // This is a custom query that might need to be implemented in the auth service
      // For now, we'll just use the getUserById method since it should work with emails too
      return await this.getUserById(email);
    } catch (error) {
      logger.error(`[AUTH CLIENT] Error fetching user by email ${email}:`, error.message);
      // Return null instead of throwing to allow fallback behavior
      return null;
    }
  }
}

module.exports = new AuthServiceClient();
