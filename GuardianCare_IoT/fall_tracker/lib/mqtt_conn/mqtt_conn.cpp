#include "mqtt_conn.h"

MQTTConn::MQTTConn() {
    state = MQTT_NOT_CONN;
    m_preferences = nullptr;
    m_wifiConn = nullptr;
    m_mqttClient = nullptr;
}

void MQTTConn::setupMQTTConnection(Preferences* preferences, WifiConn* wifiConn) {
    state = MQTT_CONNECTING;

    m_preferences = preferences;
    m_wifiConn = wifiConn;
    
    // Get device ID and token from preferences
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
        m_fall_topic = getFallTopic(deviceId);
        Serial.print("Fall topic set to: ");
        Serial.println(m_fall_topic);
    }
    
    m_mqttClient = new PubSubClient(*m_wifiConn->m_wifiClient);
    m_mqttClient->setServer(m_mqtt_server, m_mqtt_port);
    
    Serial.print("MQTT server: ");
    Serial.println(m_mqtt_server);
    Serial.print("MQTT port: ");
    Serial.println(m_mqtt_port);
    
    if (reconnect()) {
        Serial.println("MQTT Connected!");
        
        // Send a test message to verify connection
        String testTopic = m_topic_prefix + "test";
        String testMessage = "{\"test\":\"MQTT connection test\"}";
        Serial.print("Sending test message to topic: ");
        Serial.println(testTopic);
        bool testSuccess = m_mqttClient->publish(testTopic.c_str(), testMessage.c_str());
        if (testSuccess) {
            Serial.println("Test message sent successfully");
            state = MQTT_CONN_SUCCESS;
        } else {
            Serial.println("Failed to send test message");
            state = MQTT_UNREACHABLE_SERVER;
        }
    } else {
        state = MQTT_FAILED;
        Serial.println("MQTT Connection Failed");
    }
}

bool MQTTConn::reconnect() {
    if (!m_mqttClient) {
        return false;
    }
    
    if (m_mqttClient->connected()) {
        return true;
    }
    
    Serial.println("Attempting MQTT connection...");
    
    // Create a random client ID - same as in .ino file
    String clientId = "m5stick_" + String(random(0xffff), HEX);
    
    // Attempt to connect - simple connection like in .ino file
    if (m_mqttClient->connect(clientId.c_str())) {
        Serial.println("MQTT connected");
        return true;
    } else {
        Serial.print("MQTT connection failed, rc=");
        Serial.println(m_mqttClient->state());
        return false;
    }
}

void MQTTConn::loop() {
    if (!m_mqttClient) {
        return;
    }
    
    if (!m_mqttClient->connected()) {
        state = MQTT_NOT_CONN;
        // Try to reconnect
        if (reconnect()) {
            state = MQTT_CONN_SUCCESS;
        }
    }
    
    if (m_mqttClient->connected()) {
        m_mqttClient->loop();
    }
}

String MQTTConn::getFallTopic(String deviceId) {
    return m_topic_prefix + deviceId + "/fall";
}

void MQTTConn::sendFallAlert(String deviceId, float impact, float accX, float accY, float accZ) {
    Serial.println("Attempting to send fall alert via MQTT...");
    
    // Check connection and reconnect if needed - similar to .ino file
    if (!m_mqttClient->connected()) {
        Serial.println("MQTT not connected, attempting to reconnect...");
        reconnect();
    }
    
    // Only proceed if connected - similar to .ino file
    if (m_mqttClient->connected()) {
        // Update fall topic if needed
        if (m_fall_topic.length() == 0 || !m_fall_topic.equals(getFallTopic(deviceId))) {
            m_fall_topic = getFallTopic(deviceId);
        }

        // Add token to debug output
        String token = m_preferences->getString("token", "");
        if (token.length() > 0) {
            Serial.println("JWT Token available (not included in payload)");
        }
        
        Serial.print("Publishing to topic: ");
        Serial.println(m_fall_topic);
        
        // Create JSON payload - exactly like in .ino file
        String jsonStr = "{";
        jsonStr += "\"deviceId\":\"" + deviceId + "\",";
        jsonStr += "\"token\":\"" + token + "\",";
        jsonStr += "\"timestamp\":" + String(millis()) + ",";
        jsonStr += "\"impact\":" + String(impact) + ",";
        jsonStr += "\"location\":{\"lat\":1.3521,\"lng\":103.8198},";  // Example coordinates
        jsonStr += "\"sensorData\":{";
        jsonStr += "\"accX\":" + String(accX) + ",";
        jsonStr += "\"accY\":" + String(accY) + ",";
        jsonStr += "\"accZ\":" + String(accZ);
        jsonStr += "}}";

        
        Serial.print("JSON payload: ");
        Serial.println(jsonStr);
        
        // Publish message - simple like in the .ino file
        bool success = m_mqttClient->publish(m_fall_topic.c_str(), jsonStr.c_str());
        
        if (success) {
            Serial.println("Fall alert sent successfully via MQTT");
        } else {
            Serial.print("Failed to send fall alert. MQTT state: ");
            Serial.println(m_mqttClient->state());
        }
    } else {
        Serial.println("Cannot send MQTT alert: Not connected to MQTT broker");
    }
}
