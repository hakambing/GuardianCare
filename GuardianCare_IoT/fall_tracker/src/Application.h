#pragma once

#include <M5StickCPlus.h>
#include <Preferences.h>
#include <driver/i2s.h>
#include <ble_conn.h>
#include <wifi_conn.h>
#include <mqtt_conn.h>
#include <microphone.h>
#include <fall_detection.h>

#define MAX_BATTERY_VOLTAGE 4.17

// States
#define APP_NORMAL_ACTIVITY 0
#define APP_CONNECT_BT 11
#define APP_CONNECT_WIFI 12
#define APP_CONNECT_MQTT 13
#define APP_CONNECT_NO_WIFI_CREDENTIALS 14
#define APP_STARTING_CHECK_IN 21
#define APP_RECORDING_CHECK_IN 22
#define APP_DONE_CHECK_IN 23
#define APP_CHECKING_NO_ACTIVITY 31
#define APP_INIT_FALL_DETECTED 32
#define APP_FALL_DETECTED 33

#define REFRESH_STATE_INTERVAL 10000
#define FALL_RECOVERY_TIME 5000

class Application
{
private:
    WifiConn* m_wifi_conn;
    BLEConn* m_ble_conn;
    MQTTConn* m_mqtt_conn;
    Microphone* m_microphone;
    Preferences* m_preferences;
    FallDetection* m_fall_detection;

    int lastState;
    unsigned long lastRefreshStateTime;
    unsigned long continousRefreshStateStartTime;
    unsigned long continuousRefreshStateInterval;
    unsigned long lastBeep;

    unsigned int batteryLevel;
    
    void stream_microphone_audio();
    void end_stream_microphone_audio();

public:
    Application();
    void begin();
    void loop();
    bool wifiCredentialsIsSet();
    void updateState(int state);
    void displayNormalState();
    void setContinousRefreshStateTime(unsigned long time);

    friend void process_audio_task(void* param);
};
