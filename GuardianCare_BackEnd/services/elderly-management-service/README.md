# Elderly Management Service

The Elderly Management Service handles elderly profiles, caretaker assignments, and elderly-caretaker relationship management for the GuardianCare platform.

## Features

- Elderly profile management
- Caretaker assignment and management
- Elderly-caretaker relationship tracking
- Profile updates and history

## API Endpoints

### Elderly Management

#### Register New Elderly
```http
POST /api/elderly/register
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "name": "Elder Name",
  "age": 75,
  "gender": "female",
  "room_number": "A101",
  "emergency_contact": "1234567890"
}

Response:
{
  "message": "Elderly registered successfully",
  "elderly": {
    "id": "elderly_id",
    "name": "Elder Name",
    "age": 75,
    "gender": "female",
    "room_number": "A101",
    "emergency_contact": "1234567890"
  }
}
```

#### Get Elderly List
```http
GET /api/elderly/list
Authorization: Bearer <jwt_token>

Response:
{
  "elderly": [
    {
      "id": "elderly_id",
      "name": "Elder Name",
      "age": 75,
      "gender": "female",
      "room_number": "A101",
      "emergency_contact": "1234567890",
      "caretaker": {
        "id": "caretaker_id",
        "name": "Caretaker Name"
      }
    }
  ]
}
```

### Caretaker Assignment

#### Assign Caretaker
```http
POST /api/elderly/assign
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "elderlyId": "elderly_object_id",
  "caretakerId": "caretaker_object_id"
}

Response:
{
  "message": "Caretaker assigned successfully",
  "elderly": {
    "id": "elderly_id",
    "name": "Elder Name",
    "caretaker": {
      "id": "caretaker_id",
      "name": "Caretaker Name"
    }
  }
}
```

#### Remove Caretaker
```http
POST /api/elderly/remove-caretaker
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "elderlyId": "elderly_object_id"
}

Response:
{
  "message": "Caretaker removed successfully",
  "elderly": {
    "id": "elderly_id",
    "name": "Elder Name"
  }
}
```

#### Get Elderly's Caretaker
```http
GET /api/elderly/:elderlyId/caretaker
Authorization: Bearer <jwt_token>

Response:
{
  "caretaker": {
    "id": "caretaker_id",
    "name": "Caretaker Name",
    "email": "caretaker@example.com",
    "phone": "1234567890"
  }
}
```

#### Get Caretaker's Elderly List
```http
GET /api/elderly/caretaker/:caretakerId
Authorization: Bearer <jwt_token>

Response:
{
  "elderly": [
    {
      "id": "elderly_id",
      "name": "Elder Name",
      "age": 75,
      "room_number": "A101"
    }
  ]
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
   PORT=3001
   AUTH_SERVICE_URL=http://localhost:3000
   JWT_SECRET=node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
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
- `ELD001`: Elderly not found
- `ELD002`: Invalid caretaker assignment
- `ELD003`: Duplicate room number
- `ELD004`: Invalid elderly data
- `ELD005`: Caretaker not found

## Dependencies

- express: Web framework
- mongoose: MongoDB ODM
- axios: HTTP client for service communication
- joi: Request validation
- winston: Logging

## Contributing

1. Follow the main project's contributing guidelines
2. Ensure all tests pass
3. Add tests for new features
4. Update documentation as needed

## License

[Add your license information here]


## To run app :
sudo docker-compose up --build (be sure to have run docker engine)