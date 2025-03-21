#ifndef WIFI_CONN_H
#define WIFI_CONN_H

#include <WiFi.h>
#include <HTTPClient.h>
#include <Preferences.h>
#include "WiFiCredentials.h"

#define SERVER_HEALTH_URL "http://172.20.10.14:8000/health"
#define AUDIO_STREAM_URL "http://172.20.10.14:8000/api/check-in/m5stick/audio/stream"
#define AUDIO_STOP_URL "http://172.20.10.14:8000/api/check-in/m5stick/audio/stop"
#define EMERGENCY_URL "http://172.20.10.14:8000/api/check-in/m5stick/emergency"
#define FALL_URL "http://172.20.10.14:8000/api/check-in/m5stick/fall"
#define WIFI_TIMEOUT 5000

#define SERVER_CONNECTED 3
#define SERVER_UNREACHABLE 2
#define WIFI_CONNECTED 1
#define DISCONNECTED 0
#define CONNECTING -1
#define NO_WIFI_CREDENTIALS -2
#define UNABLE_TO_CONNECT -3

class WifiConn {
private:
    Preferences* m_preferences;

public:
    int state;
    WiFiClient* m_wifiClient;
    HTTPClient* m_httpClient;

    WifiConn();
    bool setupWiFiConnection(Preferences *preferences);
    void sendData(const char *url, uint8_t* bytes, size_t count);
    void connectToServer();
};

#endif
