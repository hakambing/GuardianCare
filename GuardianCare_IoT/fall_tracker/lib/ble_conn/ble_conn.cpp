#include <ble_conn.h>

String parseData(String data, int index, char delimiter = DELIMITER, int data_start = 0)
{
    int start = data_start;
    int end = data_start;
    int partIndex = data_start;

    while ((end = data.indexOf(delimiter, start)) != -1)
    {
        if (partIndex == index)
        {
            return data.substring(start, end);
        }
        start = end + 1;
        partIndex++;
    }

    if (partIndex == index)
    {
        return data.substring(start);
    }

    Serial.println("No data found");
    return ""; // Return empty string if index is out of bounds
}

class ServerCallbacks : public BLEServerCallbacks
{
private:
    BLEConn *m_bleConn;

public:
    ServerCallbacks(BLEConn *bleConn) : m_bleConn(bleConn) {}

    void onConnect(BLEServer *pServer)
    {
        Serial.println("Bluetooth device connected");
        m_bleConn->isPaired = true;
    }

    void onDisconnect(BLEServer *pServer)
    {
        Serial.println("Bluetooth device disconnected");
        m_bleConn->isPaired = false;
        pServer->startAdvertising();
    }
};

class MainCharacteristicCallbacks : public BLECharacteristicCallbacks
{
private:
    BLEConn *m_bleConn;

public:
    MainCharacteristicCallbacks(BLEConn *bleConn) : m_bleConn(bleConn) {}

    void onWrite(BLECharacteristic *pCharacteristic)
    {
        Serial.println("Received data from BLE");
        std::string value = pCharacteristic->getValue();
        if (value.length() > 0)
        {
            char *buffer = new char[value.length() + 1];
            memcpy(buffer, value.c_str(), value.length());
            buffer[value.length()] = '\0';

            String action = parseData(buffer, 0, DELIMITER);
            String data = String(buffer).substring(action.length() + 1);
            if (action == BLE_INITIAL_CONFIG)
            {
                String newSSID = parseData(data, 0, SEPERATOR);
                String newPassword = parseData(data, 1, SEPERATOR);
                String newEmail = parseData(data, 2, SEPERATOR);
                String newToken = parseData(data, 3, SEPERATOR);
                String newUsername = parseData(data, 4, SEPERATOR);

                Serial.println("New SSID: " + newSSID);
                Serial.println("New Password: " + newPassword);
                Serial.println("New Email: " + newEmail);
                Serial.println("New Username: " + newUsername);
                Serial.println("New Token: " + newToken);

                m_bleConn->m_preferences->putString("ssid", newSSID);
                m_bleConn->m_preferences->putString("password", newPassword);
                m_bleConn->m_preferences->putString("deviceId", newEmail);
                m_bleConn->m_preferences->putString("username", newUsername);
                m_bleConn->m_preferences->putString("token", newToken);
                
                // Extract email from JWT token for MQTT deviceId
                // For now, hardcode the email since we don't have JWT parsing
                String email = "elderly@example.com";
                m_bleConn->m_preferences->putString("deviceId", email);
                
                Serial.println("Stored deviceId for MQTT: " + email);
                
                // Print additional debug info
                Serial.println("MQTT topic will be: guardiancare/device/" + email + "/fall");
            }

            delete[] buffer;
        }
    }
};

BLEConn::BLEConn()
{
    isPaired = false;
    m_preferences = nullptr;
    m_activeServer = nullptr;
    m_mainCharacteristic = nullptr;
}

void BLEConn::setupBLEConnection(Preferences *preferences)
{
    isPaired = false;
    m_preferences = preferences;

    BLEDevice::init("GuardianCare");
    m_activeServer = BLEDevice::createServer();
    m_activeServer->setCallbacks(new ServerCallbacks(this));

    BLEService *pService = m_activeServer->createService(SERVICE_UUID);
    m_mainCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID,
        BLECharacteristic::PROPERTY_READ |
            BLECharacteristic::PROPERTY_WRITE |
            BLECharacteristic::PROPERTY_NOTIFY);

    m_mainCharacteristic->setCallbacks(new MainCharacteristicCallbacks(this));
    m_mainCharacteristic->addDescriptor(new BLE2902());
    pService->start();

    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinPreferred(0x06);
    pAdvertising->setMinPreferred(0x12);
    BLEDevice::startAdvertising();
}

void BLEConn::sendBLEMessage(const char *message)
{
    if (!isPaired)
    {
        return;
    }

    m_mainCharacteristic->setValue((uint8_t *)message, strlen(message));
    m_mainCharacteristic->notify();
}

void BLEConn::sendBLEStatusUpdate(const char *messageType, const String &userId)
{
    size_t bufferSize = strlen(messageType) + 1 + userId.length() + 1;
    char *buffer = new char[bufferSize];

    snprintf(buffer, bufferSize, "%s%c%s",
             messageType,
             DELIMITER,
             userId.c_str());

    sendBLEMessage(buffer);

    delete[] buffer;
}

void BLEConn::sendBLETrackerUpdate(int batteryLevel, bool wiFiStatus)
{
    if (!isPaired)
    {
        return;
    }
    
    size_t bufferSize = strlen(BLE_UPDATE_TRACKER_STATE) + 1 + sizeof(int) + 1 + sizeof(bool);
    char *buffer = new char[bufferSize];

    snprintf(buffer, bufferSize, "%s%c%d%c%d",
            BLE_UPDATE_TRACKER_STATE,
            DELIMITER,
            batteryLevel,
            SEPERATOR,
            wiFiStatus);

    sendBLEMessage(buffer);

    delete[] buffer;
}

void BLEConn::sendBLEFallAlert(String userId)
{
    sendBLEStatusUpdate(BLE_FALL, userId);
}

void BLEConn::sendBLERecovered(String userId)
{
    sendBLEStatusUpdate(BLE_RECOVER, userId);
}
