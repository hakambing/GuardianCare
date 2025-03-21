const jwt = require('jsonwebtoken');

/**
 * Middleware to authenticate JWT tokens and verify user roles
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

        // Check if route requires caretaker role
        const isCaretakerRoute = (
            req.path.includes('/assign') ||
            req.path.includes('/remove-caretaker') ||
            req.path.includes('/caretaker/')
        );

        if (isCaretakerRoute && decoded.user_type !== 'caretaker') {
            return res.status(403).json({ message: 'Access denied. Caretaker role required.' });
        }

        next();
    } catch (error) {
        console.error('Auth middleware error:', error);
        res.status(401).json({ message: 'Token is not valid' });
    }
};
