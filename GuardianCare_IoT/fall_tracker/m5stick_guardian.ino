#include <M5StickCPlus.h>
#include <WiFi.h>
#include <PubSubClient.h>
#include <Preferences.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

// BLE UUIDs
#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

// MQTT broker settings
const char* mqtt_server = "test.mosquitto.org";
const int mqtt_port = 1883;

// Global objects
Preferences preferences;
WiFiClient espClient;
PubSubClient mqtt(espClient);
BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
bool deviceConnected = false;
bool oldDeviceConnected = false;
unsigned long resetStart = 0;  // For reset button timing

// Configuration variables
String ssid = "";
String password = "";
String deviceId = "";  // User's email
String topic_prefix = "guardiancare/device/";
String fall_topic = "";

// Fall detection parameters
const float FALL_THRESHOLD = 2.0;  // g-force threshold
const int FALL_DURATION = 500;     // milliseconds
bool fallDetected = false;
unsigned long lastFallCheck = 0;
bool fallConfirmed = false;  // Tracks if a fall has been confirmed and is awaiting reset

// Thresholds from fall_detection.cpp
const float IMPACT_TRIGGER_THRESHOLD = 2.0;
const float FREEFALL_TRIGGER_THRESHOLD = 0.5;
const float STILL_MIN_G = 0.7;
const float STILL_MAX_G = 1.3;
const float STILL_SD_THRESHOLD = 0.3;
const float ACTIVITY_THRESHOLD = 1.5;

// Circular buffer for acceleration values
const int BUFFER_SIZE = 10;
float accelBuffer[BUFFER_SIZE];
int pos = 0; // Current position in circular buffer
bool freefallDetected = false;

const unsigned long WAIT_TIME = 2000;
unsigned long freefallStartTime = 0;

// Function declarations
void connectToWiFi();
void setupMQTT();
void reconnectMQTT();
void displayStatus();
void checkForFall();
void sendFallAlert(float impact, float accX, float accY, float accZ);
void setupBLE();
void resetConfiguration();

// BLE callbacks
class ServerCallbacks: public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    deviceConnected = true;
    M5.Lcd.fillScreen(BLACK);
    M5.Lcd.setCursor(0, 10);
    M5.Lcd.println("BLE Connected");
  }

  void onDisconnect(BLEServer* pServer) {
    deviceConnected = false;
    M5.Lcd.fillScreen(BLACK);
    M5.Lcd.setCursor(0, 10);
    M5.Lcd.println("BLE Disconnected");
  }
};

class CharacteristicCallbacks: public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pCharacteristic) {
    std::string value = pCharacteristic->getValue();
    if (value.length() > 0) {
      String data = String(value.c_str());
      // Expected format: "WIFI:ssid,password,email"
      if (data.startsWith("WIFI:")) {
        data = data.substring(5);  // Remove "WIFI:"
        int firstComma = data.indexOf(',');
        int secondComma = data.indexOf(',', firstComma + 1);
        
        if (firstComma > 0 && secondComma > 0) {
          String newSSID = data.substring(0, firstComma);
          String newPassword = data.substring(firstComma + 1, secondComma);
          String newEmail = data.substring(secondComma + 1);
          
          // Save to preferences
          preferences.putString("ssid", newSSID);
          preferences.putString("password", newPassword);
          preferences.putString("deviceId", newEmail);
          
          // Update variables
          ssid = newSSID;
          password = newPassword;
          deviceId = newEmail;
          fall_topic = topic_prefix + deviceId + "/fall";
          
          // Try to connect to WiFi
          connectToWiFi();
        }
      }
    }
  }
};

void setupBLE() {
  BLEDevice::init("GuardianCare");
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new ServerCallbacks());
  
  BLEService *pService = pServer->createService(SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(
    CHARACTERISTIC_UUID,
    BLECharacteristic::PROPERTY_READ |
    BLECharacteristic::PROPERTY_WRITE |
    BLECharacteristic::PROPERTY_NOTIFY
  );
  
  pCharacteristic->setCallbacks(new CharacteristicCallbacks());
  pCharacteristic->addDescriptor(new BLE2902());
  pService->start();
  
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);
  pAdvertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();
}

void connectToWiFi() {
  M5.Lcd.fillScreen(BLACK);
  M5.Lcd.setCursor(0, 10);
  M5.Lcd.println("Connecting to WiFi...");
  
  WiFi.begin(ssid.c_str(), password.c_str());
  
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 20) {
    delay(500);
    M5.Lcd.print(".");
    attempts++;
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    M5.Lcd.fillScreen(BLACK);
    M5.Lcd.setCursor(0, 10);
    M5.Lcd.println("WiFi Connected!");
    M5.Lcd.setCursor(0, 30);
    M5.Lcd.println(WiFi.localIP().toString());
    setupMQTT();
  } else {
    M5.Lcd.fillScreen(BLACK);
    M5.Lcd.setCursor(0, 10);
    M5.Lcd.println("WiFi Failed");
    M5.Lcd.println("Check credentials");
  }
}

void setupMQTT() {
  mqtt.setServer(mqtt_server, mqtt_port);
  reconnectMQTT();
}

void reconnectMQTT() {
  if (!mqtt.connected()) {
    M5.Lcd.fillScreen(BLACK);
    M5.Lcd.setCursor(0, 10);
    M5.Lcd.println("Connecting to MQTT...");
    
    String clientId = "m5stick_" + String(random(0xffff), HEX);
    if (mqtt.connect(clientId.c_str())) {
      M5.Lcd.println("MQTT Connected!");
      delay(1000);
      displayStatus();
    } else {
      M5.Lcd.println("MQTT Failed");
      delay(2000);
    }
  }
}

void displayStatus() {
  M5.Lcd.fillScreen(BLACK);
  M5.Lcd.setTextColor(WHITE, BLACK);
  M5.Lcd.setTextSize(2);  // Increase text size for better readability
  M5.Lcd.setCursor(10, 10);
  M5.Lcd.println("GuardianCare");
  M5.Lcd.setCursor(10, 40);
  M5.Lcd.println("Active");
  M5.Lcd.setCursor(10, 70);
  M5.Lcd.println(deviceId);
  M5.Lcd.setCursor(10, 100);
  M5.Lcd.printf("Batt: %.2fV", M5.Axp.GetBatVoltage());
}

float calculateSD(float *values, int size) {
    float sum = 0.0, mean, SD = 0.0;

    for (int i = 0; i < size; i++) {
        sum += values[i];
    }
    mean = sum / size;

    for (int i = 0; i < size; i++) {
        SD += pow(values[i] - mean, 2);
    }
    return sqrt(SD / size);
}

void getLastNSamples(float *output, int N) {
    for (int i = 0; i < N; i++) {
        int idx = (pos - N + i + BUFFER_SIZE) % BUFFER_SIZE;
        output[i] = accelBuffer[idx];
    }
}

bool checkFreefall() {
    float lastOrderedSamples[BUFFER_SIZE / 2];
    getLastNSamples(lastOrderedSamples, 5);

    for (int i = 0; i < BUFFER_SIZE / 2; i++) {
        if (lastOrderedSamples[i] < FREEFALL_TRIGGER_THRESHOLD) {
            Serial.println("Freefall detected within the last few samples!");
            return true;
        }
    }

    Serial.println("No free fall detected.");
    return false;
}

bool checkStillState() {
    float orderedSamples[BUFFER_SIZE];
    getLastNSamples(orderedSamples, BUFFER_SIZE);

    if (orderedSamples[BUFFER_SIZE - 1] <= STILL_MIN_G || orderedSamples[BUFFER_SIZE - 1] >= STILL_MAX_G) {
        Serial.println("Latest sample was not still.");
        return false;
    }

    int p = 0;
    for (int i = BUFFER_SIZE - 1; i >= 0; i--) {
        if (orderedSamples[i] < ACTIVITY_THRESHOLD) {
            p++;
        } else {
            break;
        }
    }

    if (p <= BUFFER_SIZE / 2) {
        Serial.println("Not enough continuous low activity samples.");
        return false;
    }

    float sdValues[BUFFER_SIZE];
    for (int i = 0; i < p; i++) {
        sdValues[i] = orderedSamples[BUFFER_SIZE - 1 - i];
    }
    float sdp = calculateSD(sdValues, p);

    if (sdp < STILL_SD_THRESHOLD) {
        Serial.println("Still state detected, fall confirmed!");
        return true;
    }

    Serial.println("Still state not detected bc SD does not indicate still.");
    return false;
}

void checkForFall() {
    float ax, ay, az;
    M5.IMU.getAccelData(&ax, &ay, &az);
    float accMagnitude = sqrt(ax * ax + ay * ay + az * az);

    // Store in circular buffer
    accelBuffer[pos] = accMagnitude;
    pos = (pos + 1) % BUFFER_SIZE;

    if (!freefallDetected) {
        if (accMagnitude > IMPACT_TRIGGER_THRESHOLD) {
            Serial.println("Impact detected, checking for freefall...");
            if (checkFreefall()) {
                freefallDetected = true;
                freefallStartTime = millis();
            }
        }
    } else {
        // Wait before checking if user is still
        if (millis() - freefallStartTime >= WAIT_TIME) {
            Serial.println("Checking if user is still...");
            freefallDetected = false;
            if (checkStillState()) {
                // Fall confirmed
                M5.Lcd.fillScreen(RED);
                M5.Lcd.setTextColor(WHITE, RED);
                M5.Lcd.setTextSize(3);
                M5.Lcd.setCursor(10, 10);
                M5.Lcd.println("FALL DETECTED!");
                M5.Lcd.setCursor(10, 50);
                M5.Lcd.setTextSize(2);
                M5.Lcd.println("Press Button A");
                M5.Lcd.setCursor(10, 80);
                M5.Lcd.println("to reset");

                sendFallAlert(accMagnitude, ax, ay, az);

                // Start beeping
                M5.Beep.tone(1000);  // Start beeping at 1000 Hz
                delay(200);          // Beep duration
                M5.Beep.mute();      // Stop beeping

                // Set fallConfirmed to true to stop further fall detection
                fallConfirmed = true;
            }
        }
    }
}

void sendFallAlert(float impact, float accX, float accY, float accZ) {
  if (!mqtt.connected()) {
    reconnectMQTT();
  }
  
  if (mqtt.connected()) {
    String jsonStr = "{";
    jsonStr += "\"deviceId\":\"" + deviceId + "\",";
    jsonStr += "\"timestamp\":" + String(millis()) + ",";
    jsonStr += "\"impact\":" + String(impact) + ",";
    jsonStr += "\"location\":{\"lat\":1.3521,\"lng\":103.8198},";  // Example coordinates
    jsonStr += "\"sensorData\":{";
    jsonStr += "\"accX\":" + String(accX) + ",";
    jsonStr += "\"accY\":" + String(accY) + ",";
    jsonStr += "\"accZ\":" + String(accZ);
    jsonStr += "}}";
    
    mqtt.publish(fall_topic.c_str(), jsonStr.c_str());
  }
}

void resetConfiguration() {
  preferences.clear();  // Clear all saved preferences
  M5.Lcd.fillScreen(BLACK);
  M5.Lcd.setCursor(0, 10);
  M5.Lcd.println("Configuration reset");
  M5.Lcd.println("Restarting...");
  delay(2000);
  ESP.restart();  // Restart the device
}

void setup() {
  M5.begin();
  M5.IMU.Init();
  M5.Lcd.setRotation(1);  // Set display to portrait mode
  M5.Lcd.fillScreen(BLACK);
  M5.Lcd.setTextSize(1);

  // Initialize preferences
  preferences.begin("guardiancare", false);

  // Load saved configuration
  ssid = preferences.getString("ssid", "");
  password = preferences.getString("password", "");
  deviceId = preferences.getString("deviceId", "");

  if (deviceId.length() > 0) {
    fall_topic = topic_prefix + deviceId + "/fall";
  }

  // If we have saved credentials, try to connect
  if (ssid.length() > 0 && password.length() > 0) {
    connectToWiFi();
  } else {
    // Otherwise, start BLE for configuration
    setupBLE();
    M5.Lcd.fillScreen(BLACK);
    M5.Lcd.setCursor(0, 10);
    M5.Lcd.println("Waiting for BLE");
    M5.Lcd.println("configuration...");
  }
}

void loop() {
    M5.update();

    // Check for reset combination (A+B buttons)
    if (M5.BtnA.isPressed() && M5.BtnB.isPressed()) {
        if (resetStart == 0) {
            resetStart = millis();
        } else if (millis() - resetStart > 5000) {  // 5 seconds hold
            resetConfiguration();
        }
    } else {
        resetStart = 0;
    }

    // Handle BLE connections
    if (!deviceConnected && oldDeviceConnected) {
        delay(500);
        pServer->startAdvertising();
        oldDeviceConnected = deviceConnected;
    }
    if (deviceConnected && !oldDeviceConnected) {
        oldDeviceConnected = deviceConnected;
    }

    // If WiFi is configured, handle MQTT and fall detection
    if (WiFi.status() == WL_CONNECTED) {
        mqtt.loop();

        // Check for falls every 100ms (only if fall is not confirmed)
        if (!fallConfirmed && millis() - lastFallCheck > 100) {
            checkForFall();
            lastFallCheck = millis();
        }

        // Update display every 5 seconds (only if fall is not confirmed)
        static unsigned long lastDisplay = 0;
        if (!fallConfirmed && millis() - lastDisplay > 5000) {
            displayStatus();
            lastDisplay = millis();
        }
    }

    // Handle button presses
    if (M5.BtnA.wasPressed()) {
        if (fallConfirmed) {
            // Reset fall confirmation and return to normal display
            fallConfirmed = false;
            M5.Beep.mute();  // Stop beeping
            displayStatus();
        } else {
            displayStatus();
        }
    }

    // Continuous beeping while fall is confirmed
    if (fallConfirmed) {
        static unsigned long lastBeep = 0;
        if (millis() - lastBeep > 1000) {  // Beep every 1 second
            M5.Beep.tone(1000);  // Start beeping at 1000 Hz
            delay(200);          // Beep duration
            M5.Beep.mute();      // Stop beeping
            lastBeep = millis();
        }
    }

    delay(10);
}
