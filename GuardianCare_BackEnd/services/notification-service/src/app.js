const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const mqttClient = require('./services/mqtt-client');
const routes = require('./routes');
const logger = require('./utils').logger;

const app = express();
const port = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());

// Connect to MongoDB
mongoose.connect(process.env.MONGODB_URI)
  .then(() => logger.info('Connected to MongoDB'))
  .catch(err => logger.error('MongoDB connection error:', err));

// Routes
app.use('/api', routes);

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

// Error handling middleware
app.use((err, req, res, next) => {
  logger.error('Unhandled error:', err);
  res.status(500).json({ error: 'Internal server error' });
});

// Start server
app.listen(port, () => {
  logger.info(`Notification service listening on port ${port}`);
});

// Handle process termination
process.on('SIGTERM', () => {
  logger.info('SIGTERM received. Shutting down gracefully...');
  mqttClient.close();
  process.exit(0);
});

process.on('SIGINT', () => {
  logger.info('SIGINT received. Shutting down gracefully...');
  mqttClient.close();
  process.exit(0);
});
