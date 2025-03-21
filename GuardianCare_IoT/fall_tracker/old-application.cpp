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

    M5.Lcd.fillScreen(BLACK);
    M5.Lcd.setCursor(10, 10, 2);
    M5.Lcd.print("Setting up microphone...");
    TaskHandle_t processing_task_handle;
    xTaskCreatePinnedToCore(process_audio_task, "Processing Task", 4096, this, 2, &processing_task_handle, 0);
    m_microphone->setupMicrophone(processing_task_handle);

    setupFallDetection();
    M5.Lcd.fillScreen(BLACK);
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
        M5.Lcd.fillScreen(BLACK);
        return;
    }

    if (!m_microphone->isRecording)
    {
        M5.Lcd.setCursor(10, 10, 2);
        M5.Lcd.printf("Bluetooth: %s", m_ble_conn->isPaired ? "Paired" : "Not Paired");
        M5.Lcd.setCursor(10, 30, 2);
        M5.Lcd.printf("WiFi: %s", m_wifi_conn->state != CONNECTED ? "Not Connected" : "Connected");

        M5.Lcd.setCursor(10, 50, 2);
        if (!wifiCredentialsIsSet())
        {
            M5.Lcd.printf("Enter WiFi credentials on the app.");
        }
        else if (m_wifi_conn->state == UNABLE_TO_CONNECT || m_wifi_conn->state == DISCONNECTED)
        {
            M5.Lcd.printf("Press to try to connect to internet.");
        }
        else
        {
            M5.Lcd.printf("Press to start voice check-in.");
        }

        M5.Lcd.setCursor(10, 70, 2);
        if (!m_ble_conn->isPaired)
        {
            M5.Lcd.printf("Press side button to try pairing with app.");
        }
    }

    if (detectFall())
    {
        M5.Lcd.fillScreen(RED);
        M5.Lcd.setCursor(10, 50, 2);
        M5.Lcd.printf("Fall Detected!");
        m_ble_conn->sendBLEFallAlert("ownerId");
        delay(3000);
        M5.Lcd.fillScreen(BLACK);
        return;
    }
    delay(FALL_SAMPLE_RATE);

    M5.update();
    if (M5.BtnA.wasPressed())
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
                M5.Lcd.setCursor(10, 50, 2);
                M5.Lcd.printf("Connecting to internet...");
                m_wifi_conn->setupWiFiConnection(m_preferences);
                M5.Lcd.fillScreen(BLACK);
                return;
            }

            M5.Lcd.fillScreen(RED);
            M5.Lcd.setCursor(10, 50, 2);
            M5.Lcd.printf("Recording...");
            M5.Lcd.setCursor(10, 70, 2);
            M5.Lcd.printf("Press to stop");
            m_microphone->startRecording();
        }
        else
        {
            M5.Lcd.fillScreen(BLACK);
            M5.Lcd.setCursor(10, 50, 2);
            M5.Lcd.printf("Stopping recording...");
            delay(750);
            m_microphone->stopRecording();
        }
    }

    if (M5.BtnB.wasPressed())
    {
        if (!m_ble_conn->isPaired)
        {
            M5.Lcd.fillScreen(BLACK);
            M5.Lcd.setCursor(10, 50, 2);
            M5.Lcd.printf("Waiting for BLE pairing...");
            m_ble_conn->setupBLEConnection(m_preferences);
            delay(5000);
            M5.Lcd.fillScreen(BLACK);
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
