const axios = require('axios');

class AuthServiceClient {
    constructor() {
        this.baseURL = process.env.AUTH_SERVICE_URL || 'http://localhost:3000';
        
        this.client = axios.create({
            baseURL: this.baseURL
        });
    }

    /**
     * Get user by ID
     */
    async getUserById(userId) {
        try {
            const response = await this.client.get(`/api/users/${userId}`);
            return response.data;
        } catch (error) {
            if (error.response?.status === 404) {
                throw new Error('User not found');
            }
            throw new Error(`Error fetching user: ${error.message}`);
        }
    }

    /**
     * Get users by type (elderly or caretaker)
     */
    async getUsersByType(userType) {
        try {
            const response = await this.client.get(`/api/users/type/${userType}`);
            return response.data;
        } catch (error) {
            throw new Error(`Error fetching users: ${error.message}`);
        }
    }

    /**
     * Get multiple users by their IDs
     */
    async getUsersByIds(userIds) {
        try {
            const response = await this.client.post('/api/users/batch', { userIds });
            return response.data;
        } catch (error) {
            throw new Error(`Error fetching users: ${error.message}`);
        }
    }

    /**
     * Get all elderly users for a caretaker
     */
    async getElderlyForCaretaker(caretakerId) {
        try {
            const elderly = await this.getUsersByType('elderly');
            return elderly.filter(user => user.caretaker_id?.toString() === caretakerId);
        } catch (error) {
            throw new Error(`Error fetching elderly users: ${error.message}`);
        }
    }

    /**
     * Update user details
     */
    async updateUser(userId, updateData) {
        try {
            const response = await this.client.put(`/api/users/${userId}`, updateData);
            return response.data;
        } catch (error) {
            throw new Error(`Error updating user: ${error.response?.data?.message || error.message}`);
        }
    }

    /**
     * Get latest check-in data for an elderly user
     */
    async getLatestCheckIn(elderlyId) {
        try {
            console.log(`Fetching latest check-in for elderly: ${elderlyId}`);
            
            // Ensure elderlyId is a clean string
            const elderlyIdStr = elderlyId.toString().trim();
            
            console.log(`Cleaned elderly ID: ${elderlyIdStr}`);
            
            // First, check if the elderly ID is valid
            try {
                await this.getUserById(elderlyIdStr);
            } catch (error) {
                console.error(`Invalid elderly ID: ${elderlyIdStr}`);
                throw new Error(`Invalid elderly ID: ${elderlyIdStr}`);
            }
            
            // If no check-in data exists, return a default object
            try {
                // Log the request for debugging
                console.log(`Making request to /api/checkins/${elderlyIdStr}/latest`);
                
                const response = await this.client.get(`/api/checkins/${elderlyIdStr}/latest`);
                console.log(`Check-in data found for elderly: ${elderlyIdStr}`);
                console.log(`Response data: ${JSON.stringify(response.data)}`);
                return response.data;
            } catch (error) {
                console.error(`Error fetching check-in data: ${error.message}`);
                console.error(`Error details: ${JSON.stringify(error.response?.data || {})}`);
                
                // Try to get all check-ins to see if any exist for this elderly
                try {
                    console.log(`Attempting to get all check-ins for debugging`);
                    const allCheckInsResponse = await this.client.get(`/api/checkins/all`);
                    console.log(`All check-ins: ${JSON.stringify(allCheckInsResponse.data)}`);
                } catch (debugError) {
                    console.error(`Failed to get all check-ins for debugging: ${debugError.message}`);
                }
                
                // Return a default check-in object if no data is found
                if (error.response?.status === 404 || error.response?.status === 400) {
                    console.log(`No check-in data found for elderly: ${elderlyIdStr}, returning default`);
                    return {
                        _id: `default-${elderlyIdStr}`,
                        elderly_id: elderlyIdStr,
                        summary: "No check-in data available for this elderly person.",
                        priority: 0,
                        mood: 2,
                        status: "No data",
                        transcript: "",
                        created_at: new Date().toISOString(),
                        updated_at: new Date().toISOString()
                    };
                }
                
                throw new Error(`Error fetching check-in data: ${error.message}`);
            }
        } catch (error) {
            console.error(`getLatestCheckIn error: ${error.message}`);
            throw error;
        }
    }

    /**
     * Get all check-in data for an elderly user
     */
    async getAllElderlyCheckIns(elderlyId) {
        try {
            console.log(`Fetching all check-ins for elderly: ${elderlyId}`);
            
            // Ensure elderlyId is a clean string
            const elderlyIdStr = elderlyId.toString().trim();
            
            console.log(`Cleaned elderly ID: ${elderlyIdStr}`);
            
            try {
                // Log the request for debugging
                console.log(`Making request to /api/checkins/${elderlyIdStr}`);
                
                const response = await this.client.get(`/api/checkins/${elderlyIdStr}`);
                
                // Log the response for debugging
                console.log(`Received check-in data: ${JSON.stringify(response.data)}`);
                
                return response.data;
            } catch (error) {
                console.error(`Error fetching check-in data: ${error.message}`);
                console.error(`Error details: ${JSON.stringify(error.response?.data || {})}`);
                
                // Try to get all check-ins to see if any exist for this elderly
                try {
                    console.log(`Attempting to get all check-ins for debugging`);
                    const allCheckInsResponse = await this.client.get(`/api/checkins/all`);
                    console.log(`All check-ins: ${JSON.stringify(allCheckInsResponse.data)}`);
                } catch (debugError) {
                    console.error(`Failed to get all check-ins for debugging: ${debugError.message}`);
                }
                
                // Return an empty array if no data is found
                if (error.response?.status === 404 || error.response?.status === 400) {
                    console.log(`No check-in data found for elderly: ${elderlyIdStr}, returning empty array`);
                    return [];
                }
                
                throw new Error(`Error fetching check-in data: ${error.message}`);
            }
        } catch (error) {
            console.error(`getAllElderlyCheckIns error: ${error.message}`);
            throw error;
        }
    }

    /**
     * Get check-in data by date range
     */
    async getElderlyCheckInsByDateRange(elderlyId, startDate, endDate) {
        try {
            console.log(`Fetching check-ins by date range for elderly: ${elderlyId}`);
            
            // Ensure elderlyId is a string
            const elderlyIdStr = elderlyId.toString();
            
            try {
                // Log the request for debugging
                console.log(`Making request to /api/checkins/${elderlyIdStr}/range with params: ${startDate} to ${endDate}`);
                
                const response = await this.client.get(`/api/checkins/${elderlyIdStr}/range`, {
                    params: { startDate, endDate }
                });
                
                // Log the response for debugging
                console.log(`Received check-in data by date range: ${JSON.stringify(response.data)}`);
                
                return response.data;
            } catch (error) {
                console.error(`Error fetching check-in data by date range: ${error.message}`);
                
                // Return an empty array if no data is found
                if (error.response?.status === 404 || error.response?.status === 400) {
                    console.log(`No check-in data found for elderly: ${elderlyIdStr} in date range, returning empty array`);
                    return [];
                }
                
                throw new Error(`Error fetching check-in data: ${error.message}`);
            }
        } catch (error) {
            console.error(`getElderlyCheckInsByDateRange error: ${error.message}`);
            throw error;
        }
    }
}

module.exports = new AuthServiceClient();
