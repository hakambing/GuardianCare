require('dotenv').config();

const config = {
  port: process.env.PORT || 3000,
  mongodb: {
    uri: process.env.MONGODB_URI || 'mongodb://localhost:27017/guardiancare'
  },
  mqtt: {
    brokerUrl: process.env.MQTT_BROKER_URL || 'mqtt://test.mosquitto.org:1883',
    username: process.env.MQTT_USERNAME || '',
    password: process.env.MQTT_PASSWORD || ''
  },
  firebase: {
    projectId: process.env.FIREBASE_PROJECT_ID,
    clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
    privateKey: process.env.FIREBASE_PRIVATE_KEY ? 
      process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n') : undefined
  },
  jwt: {
    secret: process.env.JWT_SECRET,
    expiresIn: process.env.JWT_EXPIRES_IN || '1d'
  },
  services: {
    authUrl: process.env.AUTH_SERVICE_URL || 'http://auth-service:3000',
    elderlyUrl: process.env.ELDERLY_SERVICE_URL || 'http://elderly-service:3001',
    checkInUrl: process.env.CHECKIN_SERVICE_URL || 'http://check-in-service:6000'
  }
};

// Validate required environment variables
const requiredEnvVars = [
  'MONGODB_URI',
  'FIREBASE_PROJECT_ID',
  'FIREBASE_CLIENT_EMAIL',
  'FIREBASE_PRIVATE_KEY',
  'JWT_SECRET'
];

const missingEnvVars = requiredEnvVars.filter(envVar => !process.env[envVar]);

if (missingEnvVars.length > 0) {
  console.error('Missing required environment variables:', missingEnvVars.join(', '));
  process.exit(1);
}

module.exports = config;
