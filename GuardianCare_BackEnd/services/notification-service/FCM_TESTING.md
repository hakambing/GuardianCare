# Firebase Cloud Messaging (FCM) Testing Guide

This guide provides instructions for testing and troubleshooting Firebase Cloud Messaging (FCM) notifications in the GuardianCare system.

## Prerequisites

1. The notification service is running
2. The MongoDB database is accessible
3. Firebase project is set up with proper credentials in `.env`
4. Android app with Firebase initialized

## Getting the FCM Token

Before testing, you need to get the FCM token from your Android app:

1. Run the Android app on a device or emulator
2. Look for logs with "FCM Token:" in Android Studio's Logcat
3. Copy the token (it's a long string)

## Testing Methods

### 1. Using the Test Endpoint

The simplest way to test FCM is using the test endpoint:

```bash
curl -X POST http://localhost:3000/api/test/send-notification \
  -H "Content-Type: application/json" \
  -d '{
    "token": "YOUR_FCM_TOKEN_HERE",
    "title": "Test Notification",
    "body": "This is a test notification"
  }'
```

Replace `YOUR_FCM_TOKEN_HERE` with the token from your Android app.

### 2. Using the Test Fall Detection Endpoint

To test the complete fall detection flow:

```bash
curl -X POST http://localhost:3000/api/test/fall-detection \
  -H "Content-Type: application/json" \
  -d '{
    "elderlyId": "ELDERLY_USER_ID",
    "useTestToken": true,
    "testToken": "YOUR_FCM_TOKEN_HERE"
  }'
```

Replace:
- `ELDERLY_USER_ID` with an elderly user ID from your system
- `YOUR_FCM_TOKEN_HERE` with the token from your Android app

### 3. Using the FCM_TEST_TOKEN Environment Variable

You can update the `.env` file with your FCM token:

```
FCM_TEST_TOKEN=your-actual-fcm-token-here
```

Then restart the notification service. This token will be used as a fallback when the system can't find proper device registrations.

### 4. Triggering a Real Fall Detection

To test with the M5Stick:

1. Make sure the M5Stick is connected to WiFi and MQTT
2. Trigger a fall detection on the M5Stick
3. The notification service should receive the MQTT message and send a notification

## Troubleshooting

### Check the Logs

The notification service now has detailed logging. Look for:

- `[FALL DETECTION]` - Logs related to fall detection processing
- `[ELDERLY INFO]` - Logs related to retrieving elderly and caretaker info
- `[SEND NOTIFICATIONS]` - Logs related to sending notifications
- `[FCM]` - Logs related to Firebase Cloud Messaging

### Common Issues

1. **No device tokens found**
   - Check if the caretaker has registered devices
   - Verify the caretaker-elderly relationship is set up correctly

2. **FCM sending failures**
   - Check if the token is valid and not expired
   - Verify Firebase credentials in `.env`

3. **MQTT message not received**
   - Check MQTT broker connection
   - Verify M5Stick is publishing to the correct topic

## Device Registration

For FCM to work properly, devices need to be registered:

```bash
curl -X POST http://localhost:3000/api/devices/register \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "CARETAKER_USER_ID",
    "deviceToken": "FCM_TOKEN_FROM_APP",
    "deviceType": "android"
  }'
```

The Android app should do this automatically after login.

## Monitoring Registered Devices

You can check registered devices in MongoDB:

```javascript
db.devices.find({ userId: "CARETAKER_USER_ID" })
```

This will show all registered devices for a specific caretaker.
