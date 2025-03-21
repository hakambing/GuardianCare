const express = require('express');
const router = express.Router();
const elderlyController = require('../controllers/elderlyController');
const auth = require('../middleware/auth');

// All routes require authentication
router.use(auth);

/**
 * @route   POST /api/elderly/assign
 * @desc    Assign elderly to caretaker (by ID or email)
 * @access  Private (Caretakers only)
 */
router.post('/assign', elderlyController.assignCaretaker);

/**
 * @route   POST /api/elderly/remove-caretaker
 * @desc    Remove caretaker assignment
 * @access  Private (Caretakers only)
 */
router.post('/remove-caretaker', elderlyController.removeCaretaker);

/**
 * @route   GET /api/elderly/caretaker/:caretakerId
 * @desc    Get all elderly assigned to a caretaker
 * @access  Private (Caretaker access only)
 */
router.get('/caretaker/:caretakerId', elderlyController.getCaretakerElderly);

/**
 * @route   GET /api/elderly/:elderlyId/caretaker
 * @desc    Get elderly's caretaker details
 * @access  Private
 */
router.get('/:elderlyId/caretaker', elderlyController.getElderlyCaretaker);


/**
 * @route   GET /api/elderly/check-in/:elderlyId/latest
 * @desc    Get latest check-in data for an elderly
 * @access  Private
 */
router.get('/check-in/:elderlyId/latest', elderlyController.getLatestCheckin);

/**
 * @route   GET /api/elderly/check-in/:elderlyId
 * @desc    Get all check-in data for an elderly
 * @access  Private
 */
router.get('/check-in/:elderlyId', elderlyController.getAllElderlyCheckIns);

/**
 * @route   GET /api/elderly/check-in/:elderlyId/range
 * @desc    Get check-in data by date range
 * @access  Private
 */
router.get('/check-in/:elderlyId/range', elderlyController.getElderlyCheckInsByDateRange);


module.exports = router;
