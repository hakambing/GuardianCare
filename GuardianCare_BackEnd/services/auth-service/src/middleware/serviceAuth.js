/**
 * Middleware for service-to-service communication
 * Authentication has been disabled as per requirements
 */
module.exports = (req, res, next) => {
    // Add service info to request without authentication
    req.service = {
        authenticated: true,
    };
    
    next();
};
