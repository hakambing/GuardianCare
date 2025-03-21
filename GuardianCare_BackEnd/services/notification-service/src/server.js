const app = require('./app');
const config = require('./config');
const { logger } = require('./utils');
const mqttClient = require('./services/mqtt-client');

// Start the server
const server = app.listen(config.app.port, () => {
  logger.info(`Notification service listening on port ${config.app.port}`);
  logger.info(`Environment: ${config.app.env}`);
});

// Handle graceful shutdown
const gracefulShutdown = async () => {
  logger.info('Received shutdown signal. Starting graceful shutdown...');

  // Close MQTT client
  mqttClient.close();
  logger.info('MQTT client closed');

  // Close HTTP server
  server.close(() => {
    logger.info('HTTP server closed');
    
    // Close MongoDB connection
    require('mongoose').connection.close(false, () => {
      logger.info('MongoDB connection closed');
      process.exit(0);
    });
  });

  // Force exit if graceful shutdown fails
  setTimeout(() => {
    logger.error('Could not close connections in time, forcefully shutting down');
    process.exit(1);
  }, 10000);
};

// Listen for shutdown signals
process.on('SIGTERM', gracefulShutdown);
process.on('SIGINT', gracefulShutdown);

// Handle uncaught errors
process.on('uncaughtException', (error) => {
  logger.error('Uncaught exception:', error);
  gracefulShutdown();
});

process.on('unhandledRejection', (reason, promise) => {
  logger.error('Unhandled Rejection at:', promise, 'reason:', reason);
  gracefulShutdown();
});
