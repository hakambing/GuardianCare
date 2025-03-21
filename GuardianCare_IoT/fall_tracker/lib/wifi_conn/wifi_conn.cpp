#include <wifi_conn.h>

WifiConn::WifiConn()
{
    state = DISCONNECTED;
    m_preferences = nullptr;
    m_wifiClient = nullptr;
    m_httpClient = nullptr;
}

bool WifiConn::setupWiFiConnection(Preferences *preferences)
{
    state = CONNECTING;
    m_preferences = preferences;

    String ssid = m_preferences->getString("ssid", SSID);
    String password = m_preferences->getString("password", PASSWORD);
    String token = m_preferences->getString("token", TOKEN);

    if (ssid.length() == 0 || password.length() == 0 || token.length() == 0)
    {
        Serial.printf("No WiFi credentials found\n");
        state = NO_WIFI_CREDENTIALS;
        return false;
    }

    Serial.printf("SSID: %s\n", ssid.c_str());
    Serial.printf("Password: %s\n", password.c_str());
    Serial.printf("Connecting to WiFi");
    WiFi.mode(WIFI_STA);
    WiFi.begin(ssid, password);
    unsigned long startTime = millis();
    while ((millis() - startTime) < WIFI_TIMEOUT)
    {
        Serial.print(".");
        delay(1000);
    }

    Serial.println("");
    if (WiFi.waitForConnectResult() != WL_CONNECTED)
    {
        Serial.println("Failed to connect to WiFi");
        state = UNABLE_TO_CONNECT;
        return false;
    }

    m_wifiClient = new WiFiClient();
    m_httpClient = new HTTPClient();

    state = WIFI_CONNECTED;
    Serial.println("WiFi Connected");

    return true;
}

void WifiConn::sendData(const char *url, uint8_t *bytes, size_t count)
{
    if (state != SERVER_CONNECTED)
    {
        return;
    }

    String token = m_preferences->getString("token", TOKEN);

    size_t bufferSize = strlen("Bearer ") + token.length() + 1;
    char *buffer = new char[bufferSize];

    snprintf(buffer, bufferSize, "%s%s",
             "Bearer ",
             token.c_str());

    m_httpClient->begin(*m_wifiClient, url);
    m_httpClient->addHeader("content-type", "application/octet-stream");
    m_httpClient->addHeader("Authorization", String(buffer));
    m_httpClient->POST(bytes, count);
    m_httpClient->end();

    delete[] buffer;
}

void WifiConn::connectToServer()
{
    if (state <= DISCONNECTED)
    {
        return;
    }

    m_httpClient->begin(*m_wifiClient, SERVER_HEALTH_URL);
    int httpCode = m_httpClient->GET();

    if (httpCode > 0) {
        state = SERVER_CONNECTED;
        Serial.printf("Server contactable! HTTP Code: %d\n", httpCode);
    } else {
        state = SERVER_UNREACHABLE;
        Serial.printf("Failed to connect! Error: %d\n", httpCode);
    }

    m_httpClient->end();
}