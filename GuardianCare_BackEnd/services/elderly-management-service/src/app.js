// Load environment variables if not in production
if (process.env.NODE_ENV !== 'production') {
    require('dotenv').config();
}

const express = require('express');
const cors = require('cors');
const morgan = require('morgan');
const elderlyRoutes = require('./routes/elderlyRoutes');

const app = express();

// ✅ Middleware
app.use(express.json());
app.use(cors());
app.use(morgan('dev')); // Logs API requests

// ✅ Routes
app.use('/api/elderly', elderlyRoutes);

// ✅ Error handling middleware
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ message: 'Something went wrong!' });
});

// ✅ Start Server
const PORT = process.env.PORT || 3001;
app.listen(PORT, '0.0.0.0', () => console.log(`✅ Elderly Management Service running on port ${PORT}`));
