const checkInService = require('../services/checkInService');

/**
 * Get all check-ins in the database (for debugging)
 * @route GET /api/checkins/all
 */
exports.getAllCheckIns = async (req, res) => {
    try {
        const checkIns = await checkInService.getAllCheckIns();
        res.json(checkIns);
    } catch (error) {
        console.error('Get all check-ins in DB error:', error);
        res.status(500).json({ message: error.message });
    }
};

/**
 * Get latest check-in for an elderly user
 * @route GET /api/checkins/:elderlyId/latest
 */
exports.getLatestCheckIn = async (req, res) => {
    try {
        const { elderlyId } = req.params;
        
        if (!elderlyId) {
            return res.status(400).json({ message: 'Elderly ID is required' });
        }

        const checkIn = await checkInService.getLatestCheckIn(elderlyId);
        res.json(checkIn);
    } catch (error) {
        console.error('Get latest check-in error:', error);
        res.status(400).json({ message: error.message });
    }
};

/**
 * Get all check-ins for an elderly user
 * @route GET /api/checkins/:elderlyId
 */
exports.getAllElderlyCheckIns = async (req, res) => {
    try {
        const { elderlyId } = req.params;
        
        if (!elderlyId) {
            return res.status(400).json({ message: 'Elderly ID is required' });
        }

        const checkIns = await checkInService.getAllElderlyCheckIns(elderlyId);
        res.json(checkIns);
    } catch (error) {
        console.error('Get all check-ins error:', error);
        res.status(400).json({ message: error.message });
    }
};

/**
 * Get check-ins by date range
 * @route GET /api/checkins/:elderlyId/range
 */
exports.getElderlyCheckInsByDateRange = async (req, res) => {
    try {
        const { elderlyId } = req.params;
        const { startDate, endDate } = req.query;
        
        if (!elderlyId) {
            return res.status(400).json({ message: 'Elderly ID is required' });
        }
        
        if (!startDate || !endDate) {
            return res.status(400).json({ message: 'Start date and end date are required' });
        }

        const checkIns = await checkInService.getElderlyCheckInsByDateRange(elderlyId, startDate, endDate);
        res.json(checkIns);
    } catch (error) {
        console.error('Get check-ins by date range error:', error);
        res.status(400).json({ message: error.message });
    }
};

/**
 * Get check-in summary of all elderlies for a caretaker
 * @route GET /api/checkins/caretaker/:caretakerId
 */
exports.getCaretakerElderliesSummary = async (req, res) => {
    try {
        const { caretakerId, latestDate } = req.body;
        
        if (!caretakerId) {
            return res.status(400).json({ message: 'A caretaker ID is required' });
        }

        const summary = await checkInService.getCaretakerElderliesSummary(caretakerId, latestDate);
        res.json(summary);
    } catch (error) {
        console.error('Get caretaker check in summary:', error);
        res.status(400).json({ message: error.message });
    }
};

/**
 * Create a new check-in
 * @route POST /api/checkins
 */
exports.createCheckIn = async (req, res) => {
    try {
        const checkInData = req.body;
        
        if (!checkInData.elderly_id) {
            return res.status(400).json({ message: 'Elderly ID is required' });
        }

        const newCheckIn = await checkInService.createCheckIn(checkInData);
        res.status(201).json(newCheckIn);
    } catch (error) {
        console.error('Create check-in error:', error);
        res.status(400).json({ message: error.message });
    }
};
