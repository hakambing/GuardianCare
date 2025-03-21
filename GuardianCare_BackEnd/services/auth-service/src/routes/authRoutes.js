const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');
const auth = require('../middleware/auth');

/**
 * @route   POST /api/auth/register
 * @desc    Register a new user
 * @access  Public
 */
router.post('/register', authController.register);

/**
 * @route   POST /api/auth/login
 * @desc    Login user
 * @access  Public
 */
router.post('/login', authController.login);

/**
 * @route   GET /api/auth/profile
 * @desc    Get user profile
 * @access  Private
 */
router.get('/profile', auth, authController.getProfile);

/**
 * @route   PUT /api/auth/profile
 * @desc    Update user profile
 * @access  Private
 */
router.put('/profile', auth, authController.updateProfile);

/**
 * @route   GET /.well-known/jwks.json
 * @desc    Get JWKS for JWT verification
 * @access  Public
 */
router.get('/.well-known/jwks.json', authController.getJWKS);

module.exports = router;
