const elderlyService = require('../services/elderlyService');

/**
 * Assign elderly to caretaker
 * @route POST /api/elderly/assign
 */
exports.assignCaretaker = async (req, res) => {
    try {
        console.log("Received assign request:", JSON.stringify(req.body));
        const { elderlyId, caretakerId, elderlyEmail } = req.body;
        
        // Require caretakerId 
        if (!caretakerId) {
            return res.status(400).json({ 
                success: false,
                message: 'caretakerId is required' 
            });
        }
        
        let result;
        
        // Check if we're doing email-based or id-based assignment
        if (elderlyEmail) {
            console.log(`Assigning elderly with email ${elderlyEmail} to caretaker ${caretakerId}`);
            result = await elderlyService.assignElderlyByEmail(caretakerId, elderlyEmail);
        } else if (elderlyId) {
            console.log(`Assigning elderly with ID ${elderlyId} to caretaker ${caretakerId}`);
            result = await elderlyService.assignCaretaker(elderlyId, caretakerId);
        } else {
            // If neither elderlyId nor elderlyEmail is provided, return an error
            return res.status(400).json({
                success: false,
                message: 'Either elderlyId or elderlyEmail is required'
            });
        }
        
        res.json(result);
    } catch (error) {
        console.error('Assign caretaker error:', error);
        res.status(400).json({ 
            success: false,
            message: error.message 
        });
    }
};

/**
 * Remove caretaker assignment
 * @route POST /api/elderly/remove-caretaker
 */
exports.removeCaretaker = async (req, res) => {
    try {
        console.log("Received unassign request:", JSON.stringify(req.body));
        const { elderlyId } = req.body;
        
        if (!elderlyId) {
            return res.status(400).json({ 
                success: false,
                message: 'elderlyId is required' 
            });
        }

        const result = await elderlyService.removeCaretaker(elderlyId);
        res.json(result);
    } catch (error) {
        console.error('Remove caretaker error:', error);
        res.status(400).json({ 
            success: false,
            message: error.message 
        });
    }
};

/**
 * Get all elderly assigned to a caretaker
 * @route GET /api/elderly/caretaker/:caretakerId
 */
exports.getCaretakerElderly = async (req, res) => {
    try {
        const elderly = await elderlyService.getCaretakerElderly(req.params.caretakerId);
        res.json(elderly);
    } catch (error) {
        console.error('Get caretaker elderly error:', error);
        res.status(400).json({ message: error.message });
    }
};

/**
 * Get elderly's caretaker details
 * @route GET /api/elderly/:elderlyId/caretaker
 */
exports.getElderlyCaretaker = async (req, res) => {
    try {
        const caretaker = await elderlyService.getElderlyCaretaker(req.params.elderlyId);
        res.json(caretaker);
    } catch (error) {
        console.error('Get elderly caretaker error:', error);
        res.status(400).json({ message: error.message });
    }
};


/**
 * Get elderly's latest check-in details
 * @route GET /api/elderly/check-in/:elderlyId/latest
 */
exports.getLatestCheckin = async (req, res) => {
    try {
        const checkin = await elderlyService.getLatestCheckin(req.params.elderlyId);
        res.json(checkin);
    } catch (error) {
        console.error('Get latest check-in data error:', error);
        res.status(400).json({ message: error.message });
    }
};

/**
 * Get all check-ins for an elderly
 * @route GET /api/elderly/check-in/:elderlyId
 */
exports.getAllElderlyCheckIns = async (req, res) => {
    try {
        const checkins = await elderlyService.getAllElderlyCheckIns(req.params.elderlyId);
        res.json(checkins);
    } catch (error) {
        console.error('Get all check-in data error:', error);
        res.status(400).json({ message: error.message });
    }
};

/**
 * Get check-ins by date range
 * @route GET /api/elderly/check-in/:elderlyId/range
 */
exports.getElderlyCheckInsByDateRange = async (req, res) => {
    try {
        const { startDate, endDate } = req.query;
        
        if (!startDate || !endDate) {
            return res.status(400).json({ message: 'startDate and endDate are required query parameters' });
        }
        
        const checkins = await elderlyService.getElderlyCheckInsByDateRange(
            req.params.elderlyId,
            startDate,
            endDate
        );
        
        res.json(checkins);
    } catch (error) {
        console.error('Get check-in data by date range error:', error);
        res.status(400).json({ message: error.message });
    }
};
