const jwt = require('jsonwebtoken');

/**
 * Middleware to authenticate JWT tokens
 * Adds user data to req.user if token is valid
 */
module.exports = (req, res, next) => {
    try {
        // Get token from header
        const authHeader = req.header('Authorization');
        if (!authHeader) {
            return res.status(401).json({ message: 'No token, authorization denied' });
        }

        // Verify token
        const token = authHeader.replace('Bearer ', '');
        const decoded = jwt.verify(token, process.env.JWT_SECRET);

        // Add user data to request
        req.user = decoded;
        next();
    } catch (error) {
        console.error('Auth middleware error:', error);
        res.status(401).json({ message: 'Token is not valid' });
    }
};
