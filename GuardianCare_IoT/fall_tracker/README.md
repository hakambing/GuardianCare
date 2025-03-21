# GuardianCare Fall Tracker

This folder contains the fall tracker component of the GuardianCare system, which is designed to monitor and ensure the safety of elderly individuals by tracking their movements and detecting falls using the M5StickC Plus smartwatch.

## Installation

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
   git clone https://github.com/SomeRandomGuyOnTheInternet/GuardianCare_IoT.git
   cd GuardianCare_IoT/fall_tracker
   ```

2. Open the project in VS Code:

   ```sh
   code .
   ```

3. Install the required dependencies:
   - Open PlatformIO in VS Code.
   - Navigate to `PlatformIO Home > Open Project` and select your desired directory.

## Hardware Requirements

- M5StickC Plus (ESP32-based smartwatch)
- Android Phone
- Charging cable
- Wi-Fi connection

## Usage

1. Open the project in VS Code:
   ```sh
   code ./GuardianCare_IoT/fall_tracker
   ```
2. Install the required dependencies:
   - Open PlatformIO in VS Code.
   - Navigate to `PlatformIO Home > Open Project` and select the `fall_tracker` folder.
3. Power on the M5StickC Plus and ensure it is charged.
4. Connect the M5StickC Plus to your device.
5. Upload the firmware to the device:
   `PlatformIO Project Tab > M5StickCPlus > Upload and Monitor`
6. Connect the device via BLE to the GuardianCare Android app for real-time monitoring.

## Configuration

Modify `src/config.h` to update settings such as:

- Wi-Fi credentials
- BLE characteristics UUIDs
- Alert thresholds (fall detection sensitivity)

## Troubleshooting

- If the device is not detected, ensure drivers are installed for the M5StickC Plus.
- Use `platformio run --target clean` to clear the build cache.
