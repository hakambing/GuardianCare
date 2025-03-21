# Notification Service

A microservice for handling notifications in the GuardianCare system. This service manages notifications for fall detection, daily well-being checks, and other alerts for both the Android app and M5StickC Plus devices.

## Features

- Real-time fall detection alerts
- Daily well-being check processing with ASR and LLM integration
- Push notifications via Firebase Cloud Messaging (FCM)
- MQTT communication with M5StickC Plus devices
- Notification history and status tracking
- Device registration and management

## Prerequisites

- Node.js >= 18.0.0
- MongoDB
- MQTT Broker (Mosquitto)
- Firebase project with FCM enabled
- ASR Service running
- LLM Service running

## Environment Variables

Copy `.env.example` to `.env` and configure:

```bash
# Application
NODE_ENV=development
PORT=3000
LOG_LEVEL=info

# MongoDB
MONGODB_URI=mongodb://localhost:27017/guardiancare

# MQTT
MQTT_BROKER_URL=mqtt://localhost:1883
MQTT_USERNAME=your_mqtt_username
MQTT_PASSWORD=your_mqtt_password

# Firebase
FIREBASE_PROJECT_ID=your_project_id
FIREBASE_CLIENT_EMAIL=your_client_email@example.com
FIREBASE_PRIVATE_KEY="your-private-key"

# External Services
ASR_SERVICE_URL=http://asr-service:6001
LLM_SERVICE_URL=http://llm-service:6002
```

## Installation

```bash
# Install dependencies
npm install

# Start the service
npm start

# Start in development mode
npm run dev
```

## Docker

```bash
# Build the image
docker build -t notification-service .

# Run the container
docker run -p 3000:3000 notification-service
```

## API Endpoints

### Notifications

- `GET /api/notifications` - Get notifications for a user
  - Query params: `userId`, `limit`, `skip`, `unreadOnly`

- `PUT /api/notifications/:id/read` - Mark notification as read
  - Body: `{ userId: string }`

### M5StickC Plus Integration

- `POST /api/m5stick/fall-detection` - Report fall detection
  - Body: `{ deviceId: string, location?: { lat: number, lng: number }, sensorData?: object }`

- `POST /api/m5stick/wellbeing-check` - Submit daily well-being check
  - Body: `{ deviceId: string, audioBase64: string }`

### Device Management

- `POST /api/devices/register` - Register a device for notifications
  - Body: `{ userId: string, deviceToken: string, deviceType: string }`

## MQTT Topics

- `device/{deviceId}/fall` - Fall detection events
- `device/{deviceId}/location` - Location updates
- `device/{deviceId}/status` - Device status updates
- `device/{deviceId}/wellbeing` - Well-being check data

## Integration with Other Services

- **ASR Service**: Transcribes audio from well-being checks
- **LLM Service**: Analyzes transcribed text for well-being assessment
- **Auth Service**: Validates user authentication
- **Elderly Management Service**: Retrieves elderly profiles and caretaker relationships

## Testing

```bash
# Run tests
npm test
```

## Error Handling

The service uses standardized error responses:

```json
{
  "status": "error",
  "message": "Error description",
  "details": {} // Optional additional information
}
```

## Monitoring

- Health check endpoint: `/health`
- Logging: Winston logger with configurable log levels
- MQTT connection status monitoring
- Firebase connection status monitoring

## Security

- JWT authentication for API endpoints
- MQTT authentication with username/password
- Firebase service account authentication
- Input validation and sanitization
- Error message sanitization

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request
