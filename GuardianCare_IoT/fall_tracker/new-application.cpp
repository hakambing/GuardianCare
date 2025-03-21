#include <Application.h>

void process_audio_task(void *param)
{
    Application *application = (Application *)param;
    const TickType_t xMaxBlockTime = pdMS_TO_TICKS(100);
    bool streamingAudio = false;
    while (true)
    {
        // wait for some samples to process
        uint32_t ulNotificationValue = ulTaskNotifyTake(pdTRUE, xMaxBlockTime);
        if (ulNotificationValue > 0)
        {
            streamingAudio = true;
            application->stream_microphone_audio();
        }
        else
        {
            if (streamingAudio && !application->m_microphone->isRecording)
            {
                application->end_stream_microphone_audio();
                streamingAudio = false;
            }
        }
    }
}

Application::Application()
{
    m_preferences = new Preferences();
    m_ble_conn = new BLEConn();
    m_wifi_conn = new WifiConn();
    m_wifi_client = new WiFiClient();
    m_mqtt_conn = new MQTTConn();
    m_microphone = new Microphone();
}

void Application::begin()
{
    m_preferences->begin("guardiancare", false);

    // // TEST DELETE
    // m_preferences->putString("ssid", "");
    // m_preferences->putString("password", "");
    // m_preferences->putString("token", "");

    M5.Lcd.fillScreen(BLACK);
    M5.Lcd.setCursor(10, 10, 2);
    M5.Lcd.print("Setting up Bluetooth...");
    m_ble_conn->setupBLEConnection(m_preferences);

    M5.Lcd.fillScreen(BLACK);
    M5.Lcd.setCursor(10, 10, 2);
    M5.Lcd.print("Setting up WiFi...");
    m_wifi_conn->setupWiFiConnection(m_preferences);
    
    // Setup MQTT if WiFi is connected
    if (m_wifi_conn->state == CONNECTED) {
        M5.Lcd.fillScreen(BLACK);
        M5.Lcd.setCursor(10, 10, 2);
        M5.Lcd.print("Setting up MQTT...");
        m_mqtt_conn->setupMQTTConnection(m_preferences, m_wifi_client);
    }

    M5.Lcd.fillScreen(BLACK);
    M5.Lcd.setCursor(10, 10, 2);
    M5.Lcd.print("Setting up microphone...");
    TaskHandle_t processing_task_handle;
    xTaskCreatePinnedToCore(process_audio_task, "Processing Task", 4096, this, 2, &processing_task_handle, 0);
    m_microphone->setupMicrophone(processing_task_handle);

    setupFallDetection();
    
    // Show the initial status screen
    M5.Lcd.fillScreen(BLACK);
    displayStatus();
}

void Application::loop()
{
    if (m_wifi_conn->state == NO_WIFI_CREDENTIALS && wifiCredentialsIsSet())
    {
        Serial.println("Connecting to internet cause got wifi credentials now...");
        M5.Lcd.fillScreen(BLACK);
        M5.Lcd.setCursor(10, 50, 2);
        M5.Lcd.printf("Connecting to internet...");
        m_wifi_conn->setupWiFiConnection(m_preferences);
        
        // Setup MQTT if WiFi is connected
        if (m_wifi_conn->state == CONNECTED) {
            M5.Lcd.fillScreen(BLACK);
            M5.Lcd.setCursor(10, 10, 2);
            M5.Lcd.print("Setting up MQTT...");
            m_mqtt_conn->setupMQTTConnection(m_preferences, m_wifi_client);
        }
        
        // Return to normal display
        M5.Lcd.fillScreen(BLACK);
        displayStatus();
        return;
    }
    
    // Handle MQTT loop if WiFi is connected
    if (m_wifi_conn->state == CONNECTED) {
        m_mqtt_conn->loop();
    }

    // Static variables for fall detection
    static bool fallConfirmed = false;
    static unsigned long lastBeep = 0;
    static unsigned long lastDisplayUpdate = 0;
    
    // Update buttons first to ensure we can reset fall state
    M5.update();
    
    // Handle button presses for fall reset
    if (M5.BtnA.wasPressed() && fallConfirmed) {
        // Reset fall confirmation and return to normal display
        Serial.println("Button A pressed - resetting fall state");
        fallConfirmed = false;
        M5.Beep.mute();  // Stop beeping
        
        // Completely reset the display and text properties
        M5.Lcd.fillScreen(BLACK);
        M5.Lcd.setTextColor(WHITE, BLACK);  // Reset text color
        M5.Lcd.setTextSize(1);  // Reset to default text size
        
        displayStatus();  // Show normal status screen
        return;
    }
    
    // Continuous beeping while fall is confirmed
    if (fallConfirmed) {
        if (millis() - lastBeep > 1000) {  // Beep every 1 second
            M5.Beep.tone(1000);  // Start beeping at 1000 Hz
            delay(200);          // Beep duration
            M5.Beep.mute();      // Stop beeping
            lastBeep = millis();
        }
    }
    // Only check for falls if not already confirmed
    else if (detectFall())
    {
        Serial.println("Fall detected! Preparing alerts...");
        
        // Completely clear the screen first
        M5.Lcd.fillScreen(BLACK);
        delay(50);  // Short delay to ensure screen is cleared
        
        // Fall confirmed - update display with clear formatting
        M5.Lcd.fillScreen(RED);
        M5.Lcd.setTextColor(WHITE, RED);  // White text on red background
        
        // Display fall message with consistent formatting
        M5.Lcd.setTextSize(2);
        M5.Lcd.setCursor(10, 10);
        M5.Lcd.println("FALL DETECTED!");
        
        M5.Lcd.setTextSize(2);
        M5.Lcd.setCursor(10, 50);
        M5.Lcd.println("Press Button A");
        M5.Lcd.setCursor(10, 80);
        M5.Lcd.println("to reset");
        
        // Get accelerometer data for the alert
        float ax, ay, az;
        M5.IMU.getAccelData(&ax, &ay, &az);
        float accMagnitude = sqrt(ax * ax + ay * ay + az * az);
        
        // Send fall alert via BLE
        m_ble_conn->sendBLEFallAlert("ownerId");
        
        // Send fall alert via MQTT if connected
        if (m_wifi_conn->state == CONNECTED) {
            String deviceId = m_preferences->getString("deviceId", "");
            String username = m_preferences->getString("username", "");
            String token = m_preferences->getString("token", "");
            
            Serial.print("Device ID (Email) from preferences: ");
            Serial.println(deviceId);
            
            if (username.length() > 0) {
                Serial.print("Username: ");
                Serial.println(username);
            }
            
            if (token.length() > 0) {
                Serial.print("JWT Token length: ");
                Serial.println(token.length());
            }
            
            if (deviceId.length() > 0) {
                Serial.println("Sending MQTT fall alert...");
                // Send directly using the same format as in the .ino file
                if (m_mqtt_conn->m_mqttClient && m_mqtt_conn->m_mqttClient->connected()) {
                    String fall_topic = m_mqtt_conn->getFallTopic(deviceId);
                    
                    // Create JSON payload - exactly like in the .ino file
                    String jsonStr = "{";
                    jsonStr += "\"deviceId\":\"" + deviceId + "\",";
                    jsonStr += "\"timestamp\":" + String(millis()) + ",";
                    jsonStr += "\"impact\":" + String(accMagnitude) + ",";
                    jsonStr += "\"location\":{\"lat\":1.3521,\"lng\":103.8198},";  // Example coordinates
                    jsonStr += "\"sensorData\":{";
                    jsonStr += "\"accX\":" + String(ax) + ",";
                    jsonStr += "\"accY\":" + String(ay) + ",";
                    jsonStr += "\"accZ\":" + String(az);
                    jsonStr += "}}";
                    
                    Serial.print("Publishing to topic: ");
                    Serial.println(fall_topic);
                    Serial.print("JSON payload: ");
                    Serial.println(jsonStr);
                    
                    bool success = m_mqtt_conn->m_mqttClient->publish(fall_topic.c_str(), jsonStr.c_str());
                    Serial.print("MQTT publish result: ");
                    Serial.println(success ? "Success" : "Failed");
                } else {
                    Serial.println("MQTT client not connected, using class method");
                    m_mqtt_conn->sendFallAlert(deviceId, accMagnitude, ax, ay, az);
                }
            } else {
                Serial.println("Cannot send MQTT alert: Device ID is empty");
            }
        } else {
            Serial.println("Cannot send MQTT alert: WiFi not connected");
        }
        
        // Start beeping
        M5.Beep.tone(1000);  // Start beeping at 1000 Hz
        delay(200);          // Beep duration
        M5.Beep.mute();      // Stop beeping
        
        // Set fallConfirmed to true to stop further fall detection until reset
        fallConfirmed = true;
        lastBeep = millis();  // Initialize beep timer
    }
    // Normal operation - update display periodically if not in fall mode
    else if (!fallConfirmed && !m_microphone->isRecording && millis() - lastDisplayUpdate > 5000) {
        // Update display every 5 seconds
        displayStatus();
        lastDisplayUpdate = millis();
    }
    
    delay(FALL_SAMPLE_RATE);

    // Handle other button presses only if not in fall confirmed state
    if (!fallConfirmed && M5.BtnA.wasPressed())
    {
        if (!m_microphone->isRecording)
        {
            // if (!wifiCredentialsIsSet())
            // {
            //     M5.Lcd.fillScreen(BLACK);
            //     M5.Lcd.setCursor(10, 50, 2);
            //     M5.Lcd.printf("No WiFi credentials found.");
            //     M5.Lcd.setCursor(10, 70, 2);
            //     M5.Lcd.printf("Enter WiFi credentials on the app.");
            //     delay(3000);
            //     M5.Lcd.fillScreen(BLACK);
            //     return;
            // }

            // // TEST DELETE
            // if (m_wifi_conn->state == NO_WIFI_CREDENTIALS)
            // {
            //     m_preferences->putString("ssid", SSID);
            //     m_preferences->putString("password", PASSWORD);
            //     m_preferences->putString("token", TOKEN);
            //     return;
            // }

            if (m_wifi_conn->state != CONNECTED && m_wifi_conn->state != CONNECTING)
            {
                M5.Lcd.fillScreen(BLACK);
                M5.Lcd.setTextColor(WHITE, BLACK);
                M5.Lcd.setTextSize(1);
                M5.Lcd.setCursor(10, 50);
                M5.Lcd.printf("Connecting to internet...");
                m_wifi_conn->setupWiFiConnection(m_preferences);
                M5.Lcd.fillScreen(BLACK);
                displayStatus();
                return;
            }

            // Start recording
            M5.Lcd.fillScreen(BLACK);
            M5.Lcd.setTextColor(WHITE, BLACK);
            M5.Lcd.setTextSize(1);
            M5.Lcd.setCursor(10, 50);
            M5.Lcd.printf("Recording...");
            M5.Lcd.setCursor(10, 70);
            M5.Lcd.printf("Press to stop");
            m_microphone->startRecording();
        }
        else
        {
            // Stop recording
            M5.Lcd.fillScreen(BLACK);
            M5.Lcd.setTextColor(WHITE, BLACK);
            M5.Lcd.setTextSize(1);
            M5.Lcd.setCursor(10, 50);
            M5.Lcd.printf("Stopping recording...");
            delay(750);
            m_microphone->stopRecording();
            
            // Return to normal display
            M5.Lcd.fillScreen(BLACK);
            displayStatus();
        }
    }

    if (M5.BtnB.wasPressed())
    {
        if (!m_ble_conn->isPaired)
        {
            M5.Lcd.fillScreen(BLACK);
            M5.Lcd.setTextColor(WHITE, BLACK);
            M5.Lcd.setTextSize(1);
            M5.Lcd.setCursor(10, 50);
            M5.Lcd.printf("Waiting for BLE pairing...");
            m_ble_conn->setupBLEConnection(m_preferences);
            delay(5000);
            
            // Return to normal display
            M5.Lcd.fillScreen(BLACK);
            displayStatus();
        }
    }
}

bool Application::wifiCredentialsIsSet()
{
    String ssid = m_preferences->getString("ssid", "");
    String password = m_preferences->getString("password", "");
    String token = m_preferences->getString("token", "");

    if (ssid.length() == 0 || password.length() == 0 || token.length() == 0)
    {
        return false;
    }

    return true;
}

void Application::stream_microphone_audio()
{
    int16_t *samples = m_microphone->getCapturedAudioBuffer();
    if (m_wifi_conn->state == CONNECTED)
    {
        Serial.println("Streaming audio data");
        m_wifi_conn->sendData(AUDIO_STREAM_URL, (uint8_t *)samples, m_microphone->getBufferSizeInBytes());
    }
}

void Application::end_stream_microphone_audio()
{
    uint8_t nullData[4] = {0, 0, 0, 0};
    if (m_wifi_conn->state == CONNECTED)
    {
        Serial.println("Stopping audio stream");
        m_wifi_conn->sendData(AUDIO_STOP_URL, nullData, sizeof(nullData));
    }
}

void Application::displayStatus()
{
    // Completely reset the display
    M5.Lcd.fillScreen(BLACK);
    M5.Lcd.setTextColor(WHITE, BLACK);
    M5.Lcd.setTextSize(2);
    
    // Display status information with consistent formatting
    M5.Lcd.setCursor(10, 10);
    M5.Lcd.println("GuardianCare");
    
    // Display connection status at the top for visibility
    M5.Lcd.setCursor(10, 40);
    M5.Lcd.printf("BT: %s", m_ble_conn->isPaired ? "Paired" : "Not Paired");
    
    M5.Lcd.setCursor(10, 70);
    M5.Lcd.printf("WiFi: %s", m_wifi_conn->state == CONNECTED ? "Connected" : "Not Connected");
    
    // Display device info
    M5.Lcd.setCursor(10, 100);
    M5.Lcd.println("Active");
    
    String deviceId = m_preferences->getString("deviceId", "");
    if (deviceId.length() > 0) {
        M5.Lcd.setCursor(10, 130);
        M5.Lcd.println(deviceId);
    }
    
    M5.Lcd.setCursor(10, 160);
    M5.Lcd.printf("Batt: %.2fV", M5.Axp.GetBatVoltage());
    
    // Display instructions based on state
    if (!m_ble_conn->isPaired) {
        M5.Lcd.setCursor(10, 190);
        M5.Lcd.println("Press B to pair");
    }
}
