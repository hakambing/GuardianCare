# GuardianCare IoT

This repository contains the IoT components of the GuardianCare system, which are designed to monitor and ensure the safety of elderly individuals by tracking their movements, detecting falls, and facilitating voice check-ins using the M5StickC Plus smartwatch and gateway devices.

## Components

The IoT system consists of three main components:

1. **Fall Tracker**: M5StickC Plus-based wearable device for fall detection and voice recording
2. **Floor Gateway**: LoRa gateway device for receiving and forwarding data from fall trackers
3. **Office Gateway**: Central gateway for data aggregation and backend communication

## Features

- **Fall Detection**: Utilizes accelerometer data to detect falls with high accuracy
- **Voice Recording**: Captures voice for AI-powered check-ins
- **BLE Communication**: Connects to Android application for configuration and real-time monitoring
- **LoRa Communication**: Long-range, low-power communication between devices and gateways
- **MQTT Integration**: Publishes data to MQTT broker for backend processing
- **Wi-Fi Connectivity**: Connects to internet for data transmission to backend services

## Hardware Requirements

- **Fall Tracker**:
  - M5StickC Plus (ESP32-based smartwatch)
  - Charging cable
  - Optional: External microphone for better audio quality

- **Floor Gateway**:
  - ESP32 development board
  - LoRa transceiver module
  - Wi-Fi connectivity
  - Power supply

- **Office Gateway**:
  - ESP32 development board
  - LoRa transceiver module
  - Wi-Fi connectivity
  - Ethernet connectivity (optional)
  - Power supply

## Installation and Setup

### Prerequisites

1. Install **Visual Studio Code (VS Code)** if not already installed.
2. Install the **PlatformIO IDE** extension in VS Code:
   - Open VS Code.
   - Navigate to `Extensions` (Ctrl + Shift + X).
   - Search for `PlatformIO IDE` and click `Install`.
   - Follow the instructions that pop up to install `PlatformIO` fully.

### Setting up the Environment

1. Clone this repository:
   ```sh
   git clone https://github.com/hakambing/GuardianCare.git
   cd GuardianCare/GuardianCare_IoT
   ```

2. Open the project in VS Code:
   ```sh
   code .
   ```

### Fall Tracker Setup

1. Navigate to the fall_tracker directory:
   ```sh
   cd fall_tracker
   ```

2. Open the project in PlatformIO:
   - In VS Code with PlatformIO extension installed, click on the PlatformIO icon in the sidebar
   - Select "Open Project" and navigate to the fall_tracker directory
   - Open the project

3. Configure the device:
   - Update the Wi-Fi credentials in `include/config.h` if needed
   - Adjust fall detection sensitivity parameters if necessary
   - Set the MQTT broker details if using a different broker

4. Build and upload the firmware:
   - Connect the M5StickC Plus to your computer
   - Click on the PlatformIO "Upload" button or run the following command:
     ```sh
     pio run -t upload
     ```

5. Monitor the device:
   - Use the PlatformIO Serial Monitor to view debug output:
     ```sh
     pio device monitor
     ```

### Floor Gateway Setup

1. Navigate to the floor_gateway directory:
   ```sh
   cd ../floor_gateway
   ```

2. Follow similar steps as the fall tracker to configure, build, and upload the firmware.

### Office Gateway Setup

1. Navigate to the office_gateway directory:
   ```sh
   cd ../office_gateway
   ```

2. Follow similar steps as the fall tracker to configure, build, and upload the firmware.

## Project Structure

```
GuardianCare_IoT/
│-- fall_tracker/                  # Code for the M5StickC-Plus fall tracker
│   │-- include/                   # Header files
│   │   │-- config.h               # Configuration file
│   │-- lib/                       # External libraries
│   │   │-- ble_conn/              # Bluetooth Low-Energy connection library
│   │   │-- fall_detection/        # Fall detection algorithm library
│   │   │-- microphone/            # Microphone interface library
│   │   │-- mqtt_conn/             # MQTT connection library
│   │   │-- wifi_conn/             # Wi-Fi connection library
│   │-- src/                       # Source code
│   │   │-- main.cpp               # Main program logic
│   │   │-- Application.cpp        # Application implementation
│   │   │-- Application.h          # Application header
│   │-- platformio.ini             # PlatformIO configuration
│-- floor_gateway/                 # Code for the LoRa floor gateway
│   │-- (similar structure)
│-- office_gateway/                # Code for the LoRa office gateway
│   │-- (similar structure)
```

## Configuration

### Fall Tracker Configuration

The fall tracker can be configured by modifying `include/config.h`:

```cpp
// MQTT Configuration
#define MQTT_SERVER "test.mosquitto.org"
#define MQTT_PORT 1883
#define MQTT_TOPIC_PREFIX "guardiancare/device/"

// Fall Detection Configuration
#define FALL_IMPACT_THRESHOLD 2.0
#define FALL_FREEFALL_THRESHOLD 0.5
#define FALL_STILL_MIN_G 0.7
#define FALL_STILL_MAX_G 1.3
#define FALL_STILL_SD_THRESHOLD 0.3
#define FALL_ACTIVITY_THRESHOLD 1.5

// Default location coordinates (can be updated with actual GPS if available)
#define DEFAULT_LATITUDE 1.3521
#define DEFAULT_LONGITUDE 103.8198
```

### Gateway Configuration

The gateway devices can be configured in their respective configuration files. Refer to the README files in each gateway directory for specific configuration options.

## Usage

### Fall Tracker

1. Power on the M5StickC Plus by pressing the power button.
2. The device will attempt to connect to the configured Wi-Fi network.
3. If Wi-Fi is not configured, it will enter BLE configuration mode.
4. Use the GuardianCare mobile app to configure the device via BLE.
5. Once configured, the device will continuously monitor for falls and allow voice check-ins.
6. In case of a fall, the device will automatically send an alert to the backend.

### Mobile App Integration

The fall tracker can be paired with the GuardianCare mobile app for:
- Device configuration
- Real-time fall monitoring
- Voice check-in recording
- Location tracking

## Troubleshooting

- **Device not connecting to Wi-Fi**: Ensure the correct credentials are provided in the configuration.
- **Fall detection not working**: Adjust the sensitivity parameters in `config.h`.
- **BLE connection issues**: Make sure Bluetooth is enabled on your mobile device and you're within range.
- **Upload fails**: Check that the correct board is selected in PlatformIO and the device is properly connected.

## Contributing

1. Fork the repository.
2. Create a new feature branch.
3. Commit your changes.
4. Push to your fork and submit a pull request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- M5Stack for the M5StickC Plus hardware
- ESP32 community for libraries and examples
- LoRa Alliance for the LoRaWAN protocol
