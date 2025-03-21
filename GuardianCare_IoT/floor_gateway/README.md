# GuardianCare Floor Gateway

This folder contains the floor gateway component of the GuardianCare system, which is designed to convert signals from the Fall Tracker to LoRa.

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
   cd GuardianCare_IoT/floor_gateway
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
- x2 LoRa Gateways
- Charging cable

## Usage
1. Open the project in VS Code:
   ```sh
   code ./GuardianCare_IoT/floor_gateway
   ```
2. Install the required dependencies:
   - Open PlatformIO in VS Code.
   - Navigate to `PlatformIO Home > Open Project` and select the `floor_gateway` folder.
3. ???

## Configuration
Modify `src/config.h` to update settings such as:
- ???

## Troubleshooting
- If the device is not detected, ensure drivers are installed for the M5StickC Plus.
- Use `platformio run --target clean` to clear the build cache.
