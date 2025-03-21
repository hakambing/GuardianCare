const authService = require('../services/authService');

/**
 * Register a new user
 * @route POST /api/auth/register
 */
exports.register = async (req, res) => {
    try {
        const result = await authService.registerUser(req.body);
        res.status(201).json({
            message: 'User registered successfully',
            ...result
        });
    } catch (error) {
        console.error('Registration error:', error);
        res.status(error.message === 'User already exists' ? 400 : 500)
           .json({ message: error.message || 'Error registering user' });
    }
};

/**
 * Login user
 * @route POST /api/auth/login
 */
exports.login = async (req, res) => {
    try {
        const { email, password } = req.body;
        const result = await authService.loginUser(email, password);
        res.json({
            message: 'Login successful',
            ...result
        });
    } catch (error) {
        console.error('Login error:', error);
        res.status(error.message === 'Invalid credentials' ? 401 : 500)
           .json({ message: error.message || 'Error logging in' });
    }
};

/**
 * Get current user profile
 * @route GET /api/auth/profile
 */
exports.getProfile = async (req, res) => {
    try {
        const user = await authService.getUserProfile(req.user.userId);
        res.json(user);
    } catch (error) {
        console.error('Get profile error:', error);
        res.status(error.message === 'User not found' ? 404 : 500)
           .json({ message: error.message || 'Error fetching profile' });
    }
};

/**
 * Update current user profile
 * @route PUT /api/auth/profile
 */
exports.updateProfile = async (req, res) => {
    try {
        const updatedUser = await authService.updateUserProfile(req.user.userId, req.body);
        res.json({
            message: 'Profile updated successfully',
            user: updatedUser
        });
    } catch (error) {
        console.error('Update profile error:', error);
        res.status(error.message === 'User not found' ? 404 : 400)
           .json({ message: error.message || 'Error updating profile' });
    }
};

/**
 * Get JWKS for JWT verification
 * @route GET /.well-known/jwks.json
 */
exports.getJWKS = async (req, res) => {
    try {
        const jwks = await authService.getJWKS();
        res.json(jwks);
    } catch (error) {
        console.error('JWKS error:', error);
        res.status(500).json({ message: 'Error getting JWKS' });
    }
};
