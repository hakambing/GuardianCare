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
    m_mqtt_conn = new MQTTConn();
    m_microphone = new Microphone();
    m_fall_detection = new FallDetection();

    lastState = APP_NORMAL_ACTIVITY;
    continousRefreshStateStartTime = millis();
    continuousRefreshStateInterval = 0;
    lastRefreshStateTime = millis();
    batteryLevel = 100;
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

void Application::begin()
{
    m_preferences->begin("guardiancare", false);

    // // TEST DELETE
    // Serial.println("[TEST] Clearing preferences\n");
    // m_preferences->putString("token", TOKEN);

    M5.Lcd.fillScreen(BLACK);
    M5.Lcd.setTextSize(2);
    M5.Lcd.setCursor(10, 50, 2);
    M5.Lcd.printf("Initialising....");
    m_ble_conn->setupBLEConnection(m_preferences);

    m_wifi_conn->setupWiFiConnection(m_preferences);
    m_wifi_conn->connectToServer();

    if (m_wifi_conn->state == SERVER_CONNECTED)
    {
        m_mqtt_conn->setupMQTTConnection(m_preferences, m_wifi_conn);
    }
    
    TaskHandle_t processing_task_handle;
    xTaskCreatePinnedToCore(process_audio_task, "Processing Task", 4096, this, 2, &processing_task_handle, 0);
    m_microphone->setupMicrophone(processing_task_handle);

    m_fall_detection->setupFallDetection();

    lastState = APP_NORMAL_ACTIVITY;
    continousRefreshStateStartTime = millis();
    continuousRefreshStateInterval = 0;
    lastRefreshStateTime = millis();
    updateState(lastState);

    batteryLevel = int((M5.Axp.GetBatVoltage() / MAX_BATTERY_VOLTAGE) * 100);
    if (m_ble_conn->isPaired)
    {
        m_ble_conn->sendBLETrackerUpdate(batteryLevel, m_wifi_conn->state >= WIFI_CONNECTED);
    }
}

void Application::loop()
{
    if (m_wifi_conn->state == NO_WIFI_CREDENTIALS && wifiCredentialsIsSet())
    {
        return updateState(APP_CONNECT_WIFI);
    }

    if (m_wifi_conn->state == SERVER_CONNECTED && m_mqtt_conn->state == MQTT_NOT_CONN)
    {
        Serial.println("Trying to MQTT cause got wifi");
        return updateState(APP_CONNECT_MQTT);
    }

    M5.update();
    if (M5.BtnA.wasPressed())
    {
        if (lastState == APP_FALL_DETECTED)
        {
            continuousRefreshStateInterval = 0;
            return updateState(APP_NORMAL_ACTIVITY);
        }
        else if (!m_microphone->isRecording)
        {
            if (!wifiCredentialsIsSet())
            {
                return updateState(APP_CONNECT_NO_WIFI_CREDENTIALS);
            }

            // // TEST DELETE
            // if (m_wifi_conn->state == NO_WIFI_CREDENTIALS)
            // {
            //     m_preferences->putString("ssid", SSID);
            //     m_preferences->putString("password", PASSWORD);
            //     m_preferences->putString("token", TOKEN);
            //     return;
            // }

            if (m_wifi_conn->state == DISCONNECTED || m_wifi_conn->state == UNABLE_TO_CONNECT || m_wifi_conn->state == SERVER_UNREACHABLE)
            {
                return updateState(APP_CONNECT_WIFI);
            }

            return updateState(APP_STARTING_CHECK_IN);
        }
        else
        {
            return updateState(APP_DONE_CHECK_IN);
        }
    }

    if (M5.BtnB.wasPressed())
    {
        if (!m_ble_conn->isPaired)
        {
            return updateState(APP_CONNECT_BT);
        }
    }

    if (m_fall_detection->detectFall())
    {
        return updateState(APP_INIT_FALL_DETECTED);
    }
    delay(FALL_SAMPLE_RATE);

    if (continuousRefreshStateInterval > 0 || millis() - lastRefreshStateTime >= REFRESH_STATE_INTERVAL)
    {
        return updateState(lastState);
    }

    if (!m_microphone->isRecording && m_mqtt_conn->state == MQTT_CONN_SUCCESS)
    {
        m_mqtt_conn->loop();
    }
}

void Application::displayNormalState()
{
    M5.Lcd.fillScreen(BLACK);
    M5.Lcd.setTextColor(WHITE, BLACK);
    M5.Lcd.setTextSize(1);

    M5.Lcd.setCursor(155, 10);
    batteryLevel = int((M5.Axp.GetBatVoltage() / MAX_BATTERY_VOLTAGE) * 100);
    M5.Lcd.printf("Battery: %d %", batteryLevel);

    M5.Lcd.setCursor(10, 40);
    M5.Lcd.println("<- Press button to");
    M5.Lcd.setTextSize(2);
    if (m_wifi_conn->state == UNABLE_TO_CONNECT || m_wifi_conn->state == DISCONNECTED)
    {
        M5.Lcd.setCursor(10, 60);
        M5.Lcd.println("Scan for WiFi");
    }
    else if (m_wifi_conn->state == WIFI_CONNECTED || m_wifi_conn->state == SERVER_UNREACHABLE)
    {
        M5.Lcd.setCursor(10, 60);
        M5.Lcd.println("Connect server");
    }
    else
    {
        M5.Lcd.setCursor(10, 60);
        M5.Lcd.println("Check in"); 
    }

    M5.Lcd.setTextSize(1);
    if (m_wifi_conn->state == NO_WIFI_CREDENTIALS || !m_ble_conn->isPaired)
    {
        M5.Lcd.setCursor(10, 105);
        M5.Lcd.println("Open the app to pair with phone"); 
    }

    if (m_ble_conn->isPaired)
    {
        Serial.println("Sending BLE Tracker Update");
        m_ble_conn->sendBLETrackerUpdate(batteryLevel, m_wifi_conn->state >= WIFI_CONNECTED);
    }
}

void Application::updateState(int state)
{
    lastState = state;

    switch (state)
    {
    case APP_NORMAL_ACTIVITY:
    {
        displayNormalState();
    }
    break;

    case APP_CONNECT_BT:
    {
        M5.Lcd.fillScreen(BLACK);
        M5.Lcd.setTextSize(2);
        M5.Lcd.setCursor(10, 30);
        M5.Lcd.printf("Pairing with app");
        M5.Lcd.setCursor(10, 60);
        M5.Lcd.printf("using Bluetooth...");
        m_ble_conn->setupBLEConnection(m_preferences);
        delay(5000);
        updateState(APP_NORMAL_ACTIVITY);
    }
    break;

    case APP_CONNECT_WIFI:
    {
        M5.Lcd.fillScreen(BLACK);
        M5.Lcd.setTextSize(2);
        M5.Lcd.setCursor(10, 30);
        M5.Lcd.printf("Connecting to");
        M5.Lcd.setCursor(10, 60);
        M5.Lcd.printf("WiFi...");
        m_wifi_conn->setupWiFiConnection(m_preferences);
        M5.Lcd.fillScreen(BLACK);
        M5.Lcd.setCursor(10, 30);
        M5.Lcd.printf("Connecting to");
        M5.Lcd.setCursor(10, 60);
        M5.Lcd.printf("server...");
        m_wifi_conn->connectToServer();
        if (m_wifi_conn->state == SERVER_CONNECTED)
        {
            updateState(APP_CONNECT_MQTT);
        }
        else
        {
            updateState(APP_NORMAL_ACTIVITY);
        }
    }
    break;

    case APP_CONNECT_MQTT:
    {
        M5.Lcd.fillScreen(BLACK);
        M5.Lcd.setTextSize(2);
        M5.Lcd.setCursor(10, 30);
        M5.Lcd.printf("Connecting to");
        M5.Lcd.setCursor(10, 60);
        M5.Lcd.printf("server...");
        m_mqtt_conn->setupMQTTConnection(m_preferences, m_wifi_conn);
        updateState(APP_NORMAL_ACTIVITY);
    }
    break;

    case APP_STARTING_CHECK_IN:
    {
        M5.Lcd.fillScreen(BLACK);
        M5.Lcd.setTextSize(2);
        M5.Lcd.setCursor(10, 30);
        M5.Lcd.printf("Connecting to");
        M5.Lcd.setCursor(10, 60);
        M5.Lcd.printf("server...");
        m_wifi_conn->connectToServer();
        if (m_wifi_conn->state == SERVER_CONNECTED)
        {
            updateState(APP_RECORDING_CHECK_IN);
        }
        else
        {
            updateState(APP_NORMAL_ACTIVITY);
        }
    }
    break;

    case APP_RECORDING_CHECK_IN:
    {
        M5.Lcd.fillScreen(RED);
        M5.Lcd.setTextSize(2);
        M5.Lcd.setCursor(10, 30);
        M5.Lcd.printf("Recording");
        M5.Lcd.setCursor(10, 60);
        M5.Lcd.printf("check-in...");
        M5.Lcd.setCursor(10, 95);
        M5.Lcd.setTextSize(1);
        M5.Lcd.printf("Press to stop");
        m_microphone->startRecording();
    }
    break;

    case APP_DONE_CHECK_IN:
    {
        M5.Lcd.fillScreen(BLACK);
        M5.Lcd.setTextSize(2);
        M5.Lcd.setCursor(10, 30);
        M5.Lcd.printf("Stopping");
        M5.Lcd.setCursor(10, 60);
        M5.Lcd.printf("recording...");
        delay(750);
        m_microphone->stopRecording();
        updateState(APP_NORMAL_ACTIVITY);
    }
    break;

    case APP_CONNECT_NO_WIFI_CREDENTIALS:
    {
        M5.Lcd.fillScreen(BLACK);
        M5.Lcd.setTextSize(2);
        M5.Lcd.setCursor(10, 30);
        M5.Lcd.printf("WiFi details");
        M5.Lcd.setCursor(10, 60);
        M5.Lcd.printf("not found!");
        M5.Lcd.setTextSize(1);
        M5.Lcd.setCursor(10, 95);
        M5.Lcd.printf("Enter WiFi credentials on the app.");
        delay(3000);
        updateState(APP_NORMAL_ACTIVITY);
    }
    break;

    case APP_INIT_FALL_DETECTED:
    {
        M5.Lcd.fillScreen(RED);
        M5.Lcd.setTextSize(2);
        M5.Lcd.setCursor(10, 40);
        M5.Lcd.printf("Fall detected!");
        M5.Lcd.setTextSize(1);
        M5.Lcd.setCursor(10, 75);
        M5.Lcd.printf("<- Press to cancel alert");
        setContinousRefreshStateTime(FALL_RECOVERY_TIME);
        updateState(APP_FALL_DETECTED);
    }
    break;

    case APP_FALL_DETECTED:
    {
        if (continuousRefreshStateInterval > 0 && millis() - continousRefreshStateStartTime >= continuousRefreshStateInterval)
        {
            M5.Lcd.fillScreen(RED);
            M5.Lcd.setTextSize(2);
            M5.Lcd.setCursor(10, 40);
            M5.Lcd.printf("Fall confirmed!");
            M5.Lcd.setTextSize(1);
            M5.Lcd.setCursor(10, 75);
            M5.Lcd.printf("Alert sent to your caretaker.");

            continuousRefreshStateInterval = 0;
            String deviceId = m_preferences->getString("deviceId", "");
            if (m_ble_conn->isPaired)
            {
                m_ble_conn->sendBLEFallAlert(deviceId);
                Serial.println("Fall alert sent via Bluetooth");
            }
            if (m_wifi_conn->state == SERVER_CONNECTED)
            {
                m_wifi_conn->sendData(FALL_URL, {0}, 1);
                Serial.println("Fall alert sent via WiFI");
            }
        }
    }
    break;

    default:
    {
        displayNormalState();
        state = APP_NORMAL_ACTIVITY;
    }
    break;
    }

    lastRefreshStateTime = millis();
    return;
}

void Application::setContinousRefreshStateTime(unsigned long time)
{
    continousRefreshStateStartTime = millis();
    continuousRefreshStateInterval = time;
}

void Application::stream_microphone_audio()
{
    int16_t *samples = m_microphone->getCapturedAudioBuffer();
    if (m_wifi_conn->state == SERVER_CONNECTED)
    {
        Serial.println("Streaming audio data");
        m_wifi_conn->sendData(AUDIO_STREAM_URL, (uint8_t *)samples, m_microphone->getBufferSizeInBytes());
    }
}

void Application::end_stream_microphone_audio()
{
    uint8_t nullData[4] = {0, 0, 0, 0};
    if (m_wifi_conn->state == SERVER_CONNECTED)
    {
        Serial.println("Stopping audio stream");
        m_wifi_conn->sendData(AUDIO_STOP_URL, nullData, sizeof(nullData));
    }
}
