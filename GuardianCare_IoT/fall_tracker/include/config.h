#ifndef CONFIG_H
#define CONFIG_H

// MQTT Configuration
#define MQTT_SERVER "test.mosquitto.org"
#define MQTT_PORT 1883
#define MQTT_TOPIC_PREFIX "guardiancare/device/"

// Fall Detection Configuration
#define FALL_IMPACT_THRESHOLD 2.0
#define FALL_FREEFALL_THRESHOLD 0.5
#define FALL_STILL_MIN_G 0.7
#define FALL_STILL_MAX_G 1.3
#define FALL_STILL_SD_THRESHOLD 0.3
#define FALL_ACTIVITY_THRESHOLD 1.5

// Default location coordinates (can be updated with actual GPS if available)
#define DEFAULT_LATITUDE 1.3521
#define DEFAULT_LONGITUDE 103.8198

#endif
