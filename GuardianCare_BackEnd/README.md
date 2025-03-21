# GuardianCare Backend

GuardianCare Backend is a comprehensive microservices-based system that powers the elderly care management platform. It provides a robust infrastructure for authentication, elderly management, check-ins, notifications, and AI-powered analysis.

## System Architecture

The backend consists of several microservices, each responsible for specific functionality:

- **API Gateway (Envoy)**: Routes requests to appropriate services and handles authentication
- **Auth Service**: Manages user authentication, registration, and authorization
- **Elderly Management Service**: Handles elderly profiles, caretaker assignments, and relationships
- **Check-in Service**: Manages elderly check-in status, history, and AI analysis of check-ins
- **ASR Service**: Provides automatic speech recognition capabilities for voice check-ins
- **LLM Service**: Offers language model processing for analyzing elderly speech and detecting potential issues
- **Notification Service**: Handles push notifications to caretakers and other stakeholders

Each service has its own documentation in its respective directory:
- [Auth Service Documentation](services/auth-service/README.md)
- [Elderly Management Service Documentation](services/elderly-management-service/README.md)
- [Check-in Service Documentation](services/check-in-service/README.md)
- [ASR Service Documentation](services/asr-service/README.md)
- [LLM Service Documentation](services/llm-service/README.md)
- [Notification Service Documentation](services/notification-service/README.md)

## Prerequisites

- Docker and Docker Compose
- Node.js (for local development)
- MongoDB
- Python 3.8+ (for ASR and Check-in services)
- C++ compiler (for LLM service)

## Environment Setup

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd GuardianCare_BackEnd
   ```

2. Copy the example environment file and configure your settings:
   ```bash
   cd services/k8s
   cp .env.example .env
   ```

3. Configure the environment variables in `.env`. Contact the project owner for the actual values:
   ```
   MONGODB_URI=CONTACT OWNER FOR DETAILS
   JWT_SECRET=CONTACT OWNER FOR DETAILS
   FIREBASE_PROJECT_ID=CONTACT OWNER FOR DETAILS
   FIREBASE_CLIENT_EMAIL=CONTACT OWNER FOR DETAILS
   FIREBASE_PRIVATE_KEY=CONTACT OWNER FOR DETAILS
   ```

## Running the Services

### Using Docker Compose

1. Start all services using Docker Compose:
   ```bash
   cd services/k8s
   docker-compose up --build
   ```

2. The API will be available at `http://localhost:8000`
3. Monitor Envoy's admin interface at `http://localhost:9901`

### Running Individual Services (Development)

Each service can be run individually for development purposes. Refer to the README in each service directory for specific instructions.

## API Documentation

### Authentication Endpoints

1. Register a new user:
   ```http
   POST http://localhost:8000/api/auth/register
   Content-Type: application/json

   {
     "email": "test@example.com",
     "password": "password123",
     "name": "Test User",
     "user_type": "caretaker",
     "phone": "1234567890"
   }
   ```

2. Login to get JWT token:
   ```http
   POST http://localhost:8000/api/auth/login
   Content-Type: application/json

   {
     "email": "test@example.com",
     "password": "password123"
   }
   ```

### User Management

1. Get user profile:
   ```http
   GET http://localhost:8000/api/users/profile
   Authorization: Bearer <your_jwt_token>
   ```

2. Update user profile:
   ```http
   PUT http://localhost:8000/api/users/profile
   Authorization: Bearer <your_jwt_token>
   Content-Type: application/json

   {
     "name": "Updated Name",
     "phone": "9876543210"
   }
   ```

### Elderly Management

1. Register new elderly:
   ```http
   POST http://localhost:8000/api/elderly/register
   Authorization: Bearer <your_jwt_token>
   Content-Type: application/json

   {
     "name": "Elder Name",
     "age": 75,
     "gender": "female",
     "room_number": "A101",
     "emergency_contact": "1234567890"
   }
   ```

2. Get elderly list:
   ```http
   GET http://localhost:8000/api/elderly/list
   Authorization: Bearer <your_jwt_token>
   ```

### Check-in Service

1. Get check-in status:
   ```http
   GET http://localhost:8000/api/check-in/status
   Authorization: Bearer <your_jwt_token>
   ```

2. Record check-in:
   ```http
   POST http://localhost:8000/api/check-in/record
   Authorization: Bearer <your_jwt_token>
   Content-Type: application/json

   {
     "elderly_id": "elderly_object_id",
     "status": "checked_in",
     "location": "dining_room",
     "timestamp": "2024-03-04T12:00:00Z"
   }
   ```

## Postman Collection

A Postman collection is provided for testing all endpoints. To use it:

1. Import `services/k8s/GuardianCare Envoy.postman_collection.json` into Postman
2. Create an environment and set the following variables:
   - `baseUrl`: http://localhost:8000
   - `auth_token`: (Set this after successful login)

## Monitoring and Debugging

1. Service Logs:
   ```bash
   # View all service logs
   docker-compose logs

   # View specific service logs
   docker-compose logs auth-service
   docker-compose logs elderly-service
   ```

2. Envoy Admin Interface:
   - Access http://localhost:9901 for:
     - Runtime statistics
     - Configuration dump
     - Cluster health
     - Route table

## Security Considerations

- All sensitive information (API keys, database credentials, etc.) should be stored in environment variables
- JWT tokens are used for authentication
- HTTPS should be enabled in production environments
- Regular security audits should be performed

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
