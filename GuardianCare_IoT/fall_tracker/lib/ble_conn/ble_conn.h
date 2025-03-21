#ifndef BLE_H
#define BLE_H

#include <M5StickCPlus.h>
#include <Preferences.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define DELIMITER ':'
#define SEPERATOR ','
#define BLE_UPDATE_TRACKER_STATE "ble_update"
#define BLE_FALL "ble_ono"
#define BLE_FALSE_ALARM "ble_fal"
#define BLE_RECOVER "ble_rcv"
#define BLE_INITIAL_CONFIG "WIFI"
#define ESP_BROADCAST_FALL "bct_ono"

#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

typedef struct __attribute__((packed)) {
    char data[64];
} BLEMessage;

class BLEConn {
private:
    Preferences* m_preferences;
    BLEServer* m_activeServer;
    BLECharacteristic* m_mainCharacteristic;
    BLEMessage m_bleMessage;

    void sendBLEMessage(const char* message);
    void sendBLEStatusUpdate(const char* messageType, const String& userId);

public:
    bool isPaired;

    BLEConn();
    void setupBLEConnection(Preferences *preferences);
    void sendBLETrackerUpdate(int batterLevel, bool wiFiStatus);
    void sendBLEFallAlert(String userId);
    void sendBLEFalseAlarm(String userId);
    void sendBLERecovered(String userId);

    friend class ServerCallbacks;
    friend class MainCharacteristicCallbacks;
};

#endif
