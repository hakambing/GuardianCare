# Auth Service

The Authentication Service handles user authentication, authorization, and user management for the GuardianCare platform.

## Features

- User registration and authentication
- JWT token generation and validation
- User profile management
- Role-based access control (caretaker, admin)

## API Endpoints

### Authentication

#### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "test@example.com",
  "password": "password123",
  "name": "Test User",
  "user_type": "caretaker",
  "phone": "1234567890"
}

Response:
{
  "message": "User registered successfully",
  "userId": "user_id"
}

cURL command:
curl -X POST http://localhost:8000/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{
           "email": "john@mail.com",
           "password": "password",
           "name": "John Vignesh",
           "user_type": "elderly",
           "phone": "91864675",
           "dob": "1901-11-29"
         }'
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "test@example.com",
  "password": "password123"
}

Response:
{
  "token": "jwt_token",
  "user": {
    "id": "user_id",
    "email": "test@example.com",
    "name": "Test User",
    "user_type": "caretaker"
  }
}
```

### User Management

#### Get Profile
```http
GET /api/auth/profile
Authorization: Bearer <jwt_token>

Response:
{
  "id": "user_id",
  "email": "test@example.com",
  "name": "Test User",
  "user_type": "caretaker",
  "phone": "1234567890"
}
```

#### Update Profile
```http
PUT /api/auth/profile
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "name": "Updated Name",
  "phone": "9876543210"
}

Response:
{
  "message": "Profile updated successfully",
  "user": {
    "id": "user_id",
    "name": "Updated Name",
    "phone": "9876543210"
  }
}
```

## Local Development

1. Install dependencies:
   ```bash
   npm install
   ```

2. Set up environment variables:
   ```bash
   cp .env.example .env
   ```

   Configure:
   ```
   NODE_ENV=development
   PORT=3000
   MONGODB_URI=mongodb://localhost:27017/guardiancare
   JWT_SECRET=node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
   JWT_EXPIRES_IN=24h
   ```

3. Start development server:
   ```bash
   npm run dev
   ```

## Error Handling

The service uses standardized error responses:

```javascript
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human readable message",
    "details": {} // Optional additional information
  }
}
```

Common error codes:
- `AUTH001`: Invalid credentials
- `AUTH002`: User not found
- `AUTH003`: Email already exists
- `AUTH004`: Invalid token
- `AUTH005`: Token expired

## Dependencies

- express: Web framework
- mongoose: MongoDB ODM
- jsonwebtoken: JWT implementation
- bcryptjs: Password hashing
- joi: Request validation
- winston: Logging

## Contributing

1. Follow the main project's contributing guidelines
2. Ensure all tests pass
3. Add tests for new features
4. Update documentation as needed

## License

[Add your license information here]
