const authClient = require('./authServiceClient');

class ElderlyService {
    /**
     * Assign elderly to caretaker
     */
    async assignCaretaker(elderlyId, caretakerId) {
        try {
            // Verify both users exist and have correct roles
            const [elderly, caretaker] = await Promise.all([
                authClient.getUserById(elderlyId),
                authClient.getUserById(caretakerId)
            ]);

            if (elderly.user_type !== 'elderly') {
                throw new Error('Specified user is not an elderly');
            }

            if (caretaker.user_type !== 'caretaker') {
                throw new Error('Logged in user is not a caretaker');
            }
            
            // Check if elderly already has a caretaker
            if (elderly.caretaker_id) {
                // Check if already assigned to this caretaker
                if (elderly.caretaker_id === caretakerId) {
                    return {
                        success: true,
                        message: `${elderly.name} is already assigned to you`,
                        elderly: elderly
                    };
                }
                
                // Get current caretaker's name
                try {
                    const currentCaretaker = await authClient.getUserById(elderly.caretaker_id);
                    return {
                        success: false,
                        message: `${elderly.name} is already assigned to ${currentCaretaker.name}. Please ask them to release the assignment first.`
                    };
                } catch (e) {
                    // If we can't get the current caretaker, just use a generic message
                    return {
                        success: false,
                        message: `${elderly.name} is already assigned to another caretaker. Please ask them to release the assignment first.`
                    };
                }
            }

            // Update elderly's caretaker_id through auth service
            await authClient.updateUser(elderlyId, { caretaker_id: caretakerId });

            return {
                success: true,
                message: `${elderly.name} has been successfully assigned to you`,
                elderly: elderly
            };
        } catch (error) {
            throw new Error(`Failed to assign caretaker: ${error.message}`);
        }
    }
    
    /**
     * Assign elderly to caretaker by email
     */
    async assignElderlyByEmail(caretakerId, elderlyEmail) {
        try {
            // Verify caretaker exists
            const caretaker = await authClient.getUserById(caretakerId);
            
            if (caretaker.user_type !== 'caretaker') {
                throw new Error('Logged in user is not a caretaker');
            }
            
            // Find elderly user by email
            const elderlyUsers = await authClient.getUsersByType('elderly');
            const elderly = elderlyUsers.find(user => user.email.toLowerCase() === elderlyEmail.toLowerCase());
            
            if (!elderly) {
                return {
                    success: false,
                    message: 'No elderly user found with that email'
                };
            }
            
            // Check if elderly already has a caretaker
            if (elderly.caretaker_id) {
                const currentCaretaker = await authClient.getUserById(elderly.caretaker_id);
                
                // Check if already assigned to this caretaker
                if (elderly.caretaker_id === caretakerId) {
                    return {
                        success: false,
                        message: `${elderly.name} is already assigned to you`
                    };
                }
                
                return {
                    success: false,
                    message: `${elderly.name} is already assigned to ${currentCaretaker.name}. Please ask them to release the assignment first.`
                };
            }
            
            // Assign elderly to caretaker
            await authClient.updateUser(elderly._id, { caretaker_id: caretakerId });
            
            return {
                success: true,
                message: `${elderly.name} has been successfully assigned to you`,
                elderly: elderly
            };
        } catch (error) {
            throw new Error(`Failed to assign elderly by email: ${error.message}`);
        }
    }

    /**
     * Remove caretaker assignment
     */
    async removeCaretaker(elderlyId) {
        try {
            const elderly = await authClient.getUserById(elderlyId);
            
            if (elderly.user_type !== 'elderly') {
                throw new Error('Specified user is not an elderly');
            }
            
            // Check if the elderly has a caretaker assigned
            if (!elderly.caretaker_id) {
                return {
                    success: false,
                    message: `${elderly.name} doesn't have a caretaker assigned`
                };
            }

            // Remove caretaker assignment
            await authClient.updateUser(elderlyId, { caretaker_id: null });

            return {
                success: true,
                message: `${elderly.name} has been successfully unassigned`
            };
        } catch (error) {
            throw new Error(`Failed to remove caretaker: ${error.message}`);
        }
    }

    /**
     * Get all elderly assigned to a caretaker with their check-in data
     */
    async getCaretakerElderly(caretakerId) {
        try {
            const caretaker = await authClient.getUserById(caretakerId);
            if (caretaker.user_type !== 'caretaker') {
                throw new Error('Specified user is not a caretaker');
            }

            const elderlyUsers = await authClient.getElderlyForCaretaker(caretakerId);
            if (elderlyUsers.length === 0) {
                return [];
            }
            
            return elderlyUsers;
        } catch (error) {
            throw new Error(`Failed to get caretaker's elderly: ${error.message}`);
        }
    }

    /**
     * Get elderly's caretaker details
     */
    async getElderlyCaretaker(elderlyId) {
        try {
            const elderly = await authClient.getUserById(elderlyId);
            
            if (elderly.user_type !== 'elderly') {
                throw new Error('Specified user is not an elderly');
            }

            if (!elderly.caretaker_id) {
                return null;
            }

            return await authClient.getUserById(elderly.caretaker_id);
        } catch (error) {
            throw new Error(`Failed to get elderly's caretaker: ${error.message}`);
        }
    }

    /**
     * Get elderly's latest check-in details
     */
    async getLatestCheckin(elderlyId) {
        try {
            // Verify the user is an elderly
            const elderly = await authClient.getUserById(elderlyId);
            
            if (elderly.user_type !== 'elderly') {
                throw new Error('Specified user is not an elderly');
            }

            // Get the latest check-in data
            const checkinData = await authClient.getLatestCheckIn(elderlyId);
            
            return checkinData;
        } catch (error) {
            throw new Error(`Failed to get elderly's check-in data: ${error.message}`);
        }
    }

    /**
     * Get all check-ins for an elderly
     */
    async getAllElderlyCheckIns(elderlyId) {
        try {
            // Verify the user is an elderly
            const elderly = await authClient.getUserById(elderlyId);
            
            if (elderly.user_type !== 'elderly') {
                throw new Error('Specified user is not an elderly');
            }

            // Get all check-in data
            const checkinData = await authClient.getAllElderlyCheckIns(elderlyId);
            
            return checkinData;
        } catch (error) {
            throw new Error(`Failed to get elderly's check-in data: ${error.message}`);
        }
    }

    /**
     * Get check-ins by date range
     */
    async getElderlyCheckInsByDateRange(elderlyId, startDate, endDate) {
        try {
            // Verify the user is an elderly
            const elderly = await authClient.getUserById(elderlyId);
            
            if (elderly.user_type !== 'elderly') {
                throw new Error('Specified user is not an elderly');
            }

            // Get check-in data by date range
            const checkinData = await authClient.getElderlyCheckInsByDateRange(elderlyId, startDate, endDate);
            
            return checkinData;
        } catch (error) {
            throw new Error(`Failed to get elderly's check-in data: ${error.message}`);
        }
    }
}

module.exports = new ElderlyService();
