package com.example.guardiancare.utils

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.util.UUID

data class FallTracker(
    val batteryLevel: Int = 0,
    val wifiConnected: Boolean = false
)

class BleManager private constructor(context: Context) {
    private val context: Context = context.applicationContext
    private var bluetoothAdapter: BluetoothAdapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    @RequiresApi(Build.VERSION_CODES.O)
    private val notificationHelper: NotificationHelper = NotificationHelper.getInstance(context)
    private var deviceAddress: String? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var onDataReceived: ((String) -> Unit)? = null
    private var onConnectionStateChange: ((Boolean) -> Unit)? = null

    
    private var debugMode = true

    
    private var isScanning = false
    private val scanTimeoutHandler = Handler(Looper.getMainLooper())
    private val scanTimeoutRunnable = Runnable {
        if (isScanning) {
            stopScanning()
            Toast.makeText(context, "Scan timeout. No GuardianCare device found.", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Scan timeout - no devices found")
        }
    }

    
    private var availableServices = mutableMapOf<UUID, List<UUID>>()

    companion object {
        private const val TAG = "BleManager"
        private const val REQUEST_ENABLE_BT = 1
        private const val SCAN_TIMEOUT_MS = 30000 

        
        private val DEVICE_NAME_PATTERNS = arrayOf(
            "M5Stick",
            "M5-Stick",
            "GuardianCare" 
        )

        
        private val CONFIG_SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        private val CONFIG_CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

        private const val DELIMITER = ':'
        private const val SEPARATOR = ','
        private const val BLE_FALL = "ble_ono"
        private const val BLE_FALSE_ALARM = "ble_fal"
        private const val BLE_RECOVER = "ble_rcv"
        private const val BLE_WIFI_CONFIG = "WIFI:"
        private const val BLE_UPDATE = "ble_update"

        @Volatile
        private var instance: BleManager? = null

        fun getInstance(context: Context): BleManager {
            return instance ?: synchronized(this) {
                instance ?: BleManager(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        try {
            bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing BLE scanner: ${e.message}")
            Toast.makeText(context, "Bluetooth not available or not enabled", Toast.LENGTH_SHORT).show()
        }
    }

    fun setDataCallback(callback: (String) -> Unit) {
        onDataReceived = callback
    }

    fun setConnectionCallback(callback: (Boolean) -> Unit) {
        onConnectionStateChange = callback
    }

    fun startScanning(activity: Activity) {
        if (!checkPermissions(activity)) {
            Log.e(TAG, "Missing permissions for BLE scanning")
            Toast.makeText(context, "Missing permissions for BLE scanning", Toast.LENGTH_SHORT).show()
            return
        }

        
        if (!bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth is not enabled")
            Toast.makeText(context, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        
        if (bluetoothLeScanner == null) {
            try {
                bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
            } catch (e: Exception) {
                Log.e(TAG, "Error getting BLE scanner: ${e.message}")
                Toast.makeText(context, "Bluetooth scanner not available", Toast.LENGTH_SHORT).show()
                return
            }

            if (bluetoothLeScanner == null) {
                Log.e(TAG, "BLE scanner is null")
                Toast.makeText(context, "Bluetooth scanner not available", Toast.LENGTH_SHORT).show()
                return
            }
        }

        
        if (isScanning) {
            Log.d(TAG, "Already scanning, stopping previous scan")
            stopScanning()
        }

        
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Missing BLUETOOTH_SCAN permission")
            return
        }

        try {
            
            Toast.makeText(context, "Scanning for GuardianCare device...", Toast.LENGTH_SHORT).show()

            
            Log.d(TAG, "Starting BLE scan...")

            
            bluetoothLeScanner?.startScan(null, scanSettings, myScanCallback)
            isScanning = true

            
            scanTimeoutHandler.removeCallbacks(scanTimeoutRunnable)
            scanTimeoutHandler.postDelayed(scanTimeoutRunnable, SCAN_TIMEOUT_MS.toLong())

        } catch (e: Exception) {
            isScanning = false
            Log.e(TAG, "Error starting scan: ${e.message}")
            Toast.makeText(context, "Error starting scan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopScanning() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Missing BLUETOOTH_SCAN permission")
            return
        }

        try {
            if (isScanning) {
                
                bluetoothLeScanner?.stopScan(myScanCallback)
                isScanning = false
                scanTimeoutHandler.removeCallbacks(scanTimeoutRunnable)
                Log.d(TAG, "BLE scan stopped")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan: ${e.message}")
        }
    }

    private fun checkPermissions(activity: Activity): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

        val missingPermissions = permissions.filter { permission ->
            ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            Log.e(TAG, "Missing permissions: ${missingPermissions.joinToString()}")
            return false
        }

        return true
    }

    private fun isM5StickDevice(deviceName: String?): Boolean {
        if (deviceName == null) return false

        
        Log.d(TAG, "Checking device name: $deviceName")

        
        val isMatch = DEVICE_NAME_PATTERNS.any { pattern ->
            deviceName.contains(pattern, ignoreCase = true)
        }

        if (isMatch) {
            Log.d(TAG, "Found matching device: $deviceName")
        }

        return isMatch
    }

    
    private val myScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val deviceName = result.device.name
            
            Log.d(TAG, "Found device: ${deviceName ?: "Unknown"}, Address: ${result.device.address}")
            
            if (isM5StickDevice(deviceName)) {
                Log.d(TAG, "Found target device: $deviceName, Address: ${result.device.address}")
                Toast.makeText(context, "Found device: $deviceName", Toast.LENGTH_SHORT).show()

                deviceAddress = result.device.address
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }

                bluetoothGatt = result.device.connectGatt(
                    context,
                    false,
                    gattCallback
                )
                stopScanning()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            scanTimeoutHandler.removeCallbacks(scanTimeoutRunnable)

            val errorMessage = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE not supported"
                SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
                else -> "Unknown error code: $errorCode"
            }

            Log.e(TAG, "Scan failed with error: $errorCode - $errorMessage")
            Toast.makeText(context, "Scan error: $errorCode - $errorMessage", Toast.LENGTH_SHORT).show()

            if (errorCode == SCAN_FAILED_ALREADY_STARTED) {
                try {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_SCAN
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        Log.d(TAG, "Attempting to stop existing scan and restart")
                        
                        bluetoothLeScanner?.stopScan(myScanCallback)
                        
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (context is Activity) {
                                startScanning(context)
                            }
                        }, 1000)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling SCAN_FAILED_ALREADY_STARTED: ${e.message}")
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceName = if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                gatt.device.name ?: "Unknown Device"
            } else {
                "Unknown Device"
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to $deviceName")
                    Toast.makeText(context, "Connected to $deviceName", Toast.LENGTH_SHORT).show()

                    onConnectionStateChange?.invoke(true)
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from $deviceName")
                    Toast.makeText(context, "Disconnected from $deviceName", Toast.LENGTH_SHORT).show()

                    
                    availableServices.clear()

                    onConnectionStateChange?.invoke(false)
                    
                    
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")

                
                availableServices.clear()

                
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    gatt.services.forEach { service ->
                        val characteristics = service.characteristics.map { it.uuid }
                        availableServices[service.uuid] = characteristics

                        Log.d(TAG, "Service: ${service.uuid}")
                        service.characteristics.forEach { characteristic ->
                            Log.d(TAG, "  Characteristic: ${characteristic.uuid}")
                        }
                    }
                }

                
                val service = gatt.getService(CONFIG_SERVICE_UUID)
                if (service != null) {
                    Log.d(TAG, "Found our service: ${CONFIG_SERVICE_UUID}")

                    val characteristic = service.getCharacteristic(CONFIG_CHARACTERISTIC_UUID)
                    if (characteristic != null) {
                        Log.d(TAG, "Found our characteristic: ${CONFIG_CHARACTERISTIC_UUID}")

                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            return
                        }
                        gatt.setCharacteristicNotification(characteristic, true)
                    } else {
                        Log.e(TAG, "Characteristic not found")
                        Toast.makeText(context, "Characteristic not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "Service not found")
                    Toast.makeText(context, "Service not found", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e(TAG, "Service discovery failed with status: $status")
                Toast.makeText(context, "Service discovery failed", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Successfully wrote to characteristic: ${characteristic.uuid}")
                Toast.makeText(context, "WiFi configuration sent successfully", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "Failed to write to characteristic: ${characteristic.uuid}, status: $status")
                Toast.makeText(context, "Failed to send WiFi configuration", Toast.LENGTH_SHORT).show()
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == CONFIG_CHARACTERISTIC_UUID) {
                val data = String(value)
                Log.d(TAG, "Received data: $data")

                val parts = data.split(DELIMITER)

                if (parts.isNotEmpty()) {
                    val action = parts[0]
                    val params = if (parts.size > 1) parts.subList(1, parts.size).joinToString(DELIMITER.toString()) else ""
                    
                    when (action) {
                        BLE_FALL -> {
                            val userId = if (params.isNotEmpty()) params else ""
                            val message = if (userId.isNotEmpty()) "Fall detected for user $userId!"
                            else "Fall detected!"
                            onDataReceived?.invoke(message)
                            notificationHelper.showFallDetectionNotification(userId)
                        }
                        BLE_RECOVER -> {
                            val userId = if (params.isNotEmpty()) params else ""
                            val message = if (userId.isNotEmpty()) "User $userId recovered from fall."
                            else "User recovered from fall."
                            onDataReceived?.invoke(message)
                        }
                        BLE_FALSE_ALARM -> {
                            val userId = if (params.isNotEmpty()) params else ""
                            val message = if (userId.isNotEmpty()) "User $userId sent out a false alarm!"
                            else "User sent out a false alarm!"
                            onDataReceived?.invoke(message)
                        }
                        BLE_UPDATE -> {
                            val paramsList = params.split(SEPARATOR)
                            val batteryLevel = if (paramsList.size > 0) paramsList[0].toIntOrNull() ?: 0 else 0
                            val wifiConnectedValue = if (paramsList.size > 1) paramsList[1].toIntOrNull() ?: 0 else 0
                            val wifiConnected = wifiConnectedValue == 1
                            
                            val fallTracker = FallTracker(batteryLevel, wifiConnected)
                            onDataReceived?.invoke("FallTracker:$batteryLevel:$wifiConnected")
                            Log.d(TAG, "Received device status: Battery=$batteryLevel%, WiFi=${if (wifiConnected) "Connected" else "Disconnected"}")
                        }
                        else -> onDataReceived?.invoke("Connected to GuardianCare device")
                    }
                }
            }
        }
    }

    
    fun sendMessage(message: String): Boolean {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission")
            return false
        }

        if (bluetoothGatt == null) {
            Log.e(TAG, "Not connected to any device")
            return false
        }

        
        if (bluetoothGatt?.services.isNullOrEmpty()) {
            Log.e(TAG, "Bluetooth services not discovered yet. Waiting...")
            bluetoothGatt?.discoverServices()
            return false
        }

        Log.d(TAG, "Attempting to send: $message")
        Log.d(TAG, "Available services: ${bluetoothGatt?.services?.map { it.uuid }}")

        
        val configService = bluetoothGatt?.getService(CONFIG_SERVICE_UUID)
        val characteristic = configService?.getCharacteristic(CONFIG_CHARACTERISTIC_UUID)

        if (configService == null) {
            Log.e(TAG, "Config Service not found!")
            return false
        }

        if (characteristic == null) {
            Log.e(TAG, "Config Characteristic not found!")
            return false
        }

        
        if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) == 0 &&
            (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) {
            Log.e(TAG, "Characteristic is not writable")
            return false
        }

        
        val writeType = if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }
        characteristic.writeType = writeType

        
        val messageBytes = message.toByteArray(Charsets.UTF_8)

        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            characteristic.setValue(messageBytes)
            @Suppress("DEPRECATION")
            val success = bluetoothGatt?.writeCharacteristic(characteristic) ?: false
            Log.d(TAG, "Message sent (legacy): $success")
            return success
        }

        
        val result = bluetoothGatt?.writeCharacteristic(characteristic, messageBytes, writeType)

        val success = result == BluetoothStatusCodes.SUCCESS
        Log.d(TAG, "Write success: $success, Status: $result")

        return success
    }

    fun disconnect() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission")
            return
        }
        bluetoothGatt?.close()
        bluetoothGatt = null
        availableServices.clear()
        
        
        onConnectionStateChange?.invoke(false)
        
        Log.d(TAG, "Disconnected from device")
    }
    
    /**
     * Removes (unpairs) the currently or previously connected device.
     * This is different from disconnect() as it also removes the device from the bonded devices list.
     * 
     * @return true if the device was successfully removed, false otherwise
     */
    fun removeDevice(): Boolean {
        
        if (isConnected()) {
            disconnect()
        }
        
        
        val deviceAddr = this.deviceAddress ?: return false
        
        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "Missing BLUETOOTH_CONNECT permission")
                return false
            }
            
            
            val device = bluetoothAdapter.getRemoteDevice(deviceAddr)
            
            
            if (device.bondState != BluetoothDevice.BOND_BONDED) {
                Log.d(TAG, "Device is not bonded, no need to unpair")
                
                this.deviceAddress = null
                return true
            }
            
            
            val method = device.javaClass.getMethod("removeBond")
            val result = method.invoke(device) as Boolean
            
            if (result) {
                
                this.deviceAddress = null
                Log.d(TAG, "Device unpaired successfully")
                return true
            } else {
                Log.e(TAG, "Failed to unpair device")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing device: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * Checks if the device is currently connected.
     * 
     * @return true if connected, false otherwise
     */
    fun isConnected(): Boolean {
        return bluetoothGatt != null && bluetoothGatt?.services != null
    }
    
    /**
     * Checks if the device with the current address is bonded (paired).
     * 
     * @return true if the device is bonded, false otherwise
     */
    fun isDeviceBonded(): Boolean {
        if (deviceAddress == null) return false
        
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission")
            return false
        }
        
        try {
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress!!)
            return device.bondState == BluetoothDevice.BOND_BONDED
        } catch (e: Exception) {
            Log.e(TAG, "Error checking bond state: ${e.message}")
            return false
        }
    }
    
    /**
     * Gets the name of the currently connected device.
     * 
     * @return the device name or null if not connected
     */
    fun getConnectedDeviceName(): String? {
        if (bluetoothGatt == null || deviceAddress == null) return null
        
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission")
            return null
        }
        
        try {
            return bluetoothGatt?.device?.name
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device name: ${e.message}")
            return null
        }
    }

    fun isScanning(): Boolean {
        return isScanning
    }
}
