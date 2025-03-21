#ifndef MQTT_CONN_H
#define MQTT_CONN_H

#include <M5StickCPlus.h>
#include <WiFi.h>
#include <PubSubClient.h>
#include <Preferences.h>
#include <wifi_conn.h>

// MQTT connection states
#define MQTT_CONN_SUCCESS 1
#define MQTT_NOT_CONN 0
#define MQTT_CONNECTING -1
#define MQTT_FAILED -2
#define MQTT_UNREACHABLE_SERVER -3

class MQTTConn {
private:
    Preferences* m_preferences;
    WifiConn* m_wifiConn;
    
    const char* m_mqtt_server = "test.mosquitto.org";
    const int m_mqtt_port = 1883;
    
    String m_topic_prefix = "guardiancare/device/";
    String m_fall_topic = "";

public:
    PubSubClient* m_mqttClient;
    int state;

    MQTTConn();
    void setupMQTTConnection(Preferences* preferences, WifiConn* wifiConn);
    bool reconnect();
    void loop();
    void sendFallAlert(String deviceId, float impact, float accX, float accY, float accZ);
    String getFallTopic(String deviceId);
};

#endif
