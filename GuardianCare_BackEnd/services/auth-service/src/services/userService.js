const User = require('../models/User');

class UserService {
    /**
     * Get user by ID or email
     * Excludes sensitive information for service-to-service communication
     */
    async getUserById(userIdOrEmail) {
        let user;
        
        // Check if the input looks like an email
        if (userIdOrEmail.includes('@')) {
            user = await User.findOne({ email: userIdOrEmail })
                .select('-password_hash')
                .lean();
        } else {
            // Try to find by MongoDB ObjectId
            try {
                user = await User.findById(userIdOrEmail)
                    .select('-password_hash')
                    .lean();
            } catch (error) {
                // If the ID format is invalid, try as a username or other identifier
                user = null;
            }
        }
            
        if (!user) {
            throw new Error('User not found');
        }
        
        return user;
    }

    /**
     * Get users by type (elderly or caretaker)
     */
    async getUsersByType(userType) {
        if (!['elderly', 'caretaker'].includes(userType)) {
            throw new Error('Invalid user type');
        }

        return await User.find({ user_type: userType })
            .select('-password_hash')
            .lean();
    }

    /**
     * Get multiple users by their IDs
     */
    async getUsersByIds(userIds) {
        if (!Array.isArray(userIds)) {
            throw new Error('userIds must be an array');
        }

        return await User.find({ _id: { $in: userIds } })
            .select('-password_hash')
            .lean();
    }

    /**
     * Update user by ID
     * Only allows updating non-sensitive fields
     */
    async updateUser(userId, updateData) {
        // Prevent updating sensitive fields
        const { password_hash, user_type, email, ...allowedUpdates } = updateData;

        const user = await User.findByIdAndUpdate(
            userId,
            { 
                ...allowedUpdates,
                updated_at: new Date()
            },
            { new: true }
        ).select('-password_hash');

        if (!user) {
            throw new Error('User not found');
        }

        return user;
    }
}

module.exports = new UserService();
