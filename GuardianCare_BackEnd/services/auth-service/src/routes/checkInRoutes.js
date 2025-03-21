const express = require('express');
const router = express.Router();
const checkInController = require('../controllers/checkInController');
const serviceAuth = require('../middleware/serviceAuth');

// Apply service authentication to all routes
router.use(serviceAuth);

/**
 * @route   GET /api/checkins/all
 * @desc    Get all check-ins in the database (for debugging)
 * @access  Service
 */
router.get('/all', checkInController.getAllCheckIns);

/**
 * @route   GET /api/checkins/:elderlyId/latest
 * @desc    Get latest check-in for an elderly user
 * @access  Service
 */
router.get('/:elderlyId/latest', checkInController.getLatestCheckIn);

/**
 * @route   GET /api/checkins/:elderlyId
 * @desc    Get all check-ins for an elderly user
 * @access  Service
 */
router.get('/:elderlyId', checkInController.getAllElderlyCheckIns);

/**
 * @route   GET /api/checkins/:elderlyId/range
 * @desc    Get check-ins by date range
 * @access  Service
 */
router.get('/:elderlyId/range', checkInController.getElderlyCheckInsByDateRange);

/**
 * @route   GET /api/checkins/caretaker/:caretakerId
 * @desc    Get check-in summary of all elderlies for a caretaker
 * @access  Service
 */
router.get('/caretaker/:caretakerId', checkInController.getCaretakerElderliesSummary);

/**
 * @route   POST /api/checkins
 * @desc    Create a new check-in
 * @access  Service
 */
router.post('/', checkInController.createCheckIn);

module.exports = router;
