const express = require('express');
const router = express.Router();
const userController = require('../controllers/userController');
const serviceAuth = require('../middleware/serviceAuth');

// Apply service authentication to all routes
router.use(serviceAuth);

/**
 * @route   GET /api/users/:userId
 * @desc    Get user by ID (Internal Service API)
 * @access  Service
 */
router.get('/:userId', userController.getUserById);

/**
 * @route   PUT /api/users/:userId
 * @desc    Update user data
 * @access  Internal Service API
 */
router.put('/:userId', userController.updateUser);

/**
 * @route   GET /api/users/type/:userType
 * @desc    Get users by type (Internal Service API)
 * @access  Service
 */
router.get('/type/:userType', userController.getUsersByType);

/**
 * @route   POST /api/users/batch
 * @desc    Get multiple users by IDs (Internal Service API)
 * @access  Service
 */
router.post('/batch', userController.getUsersByIds);

module.exports = router;
