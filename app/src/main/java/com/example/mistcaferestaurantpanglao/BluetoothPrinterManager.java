package com.example.mistcaferestaurantpanglao;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manages Bluetooth thermal printer connections
 * Handles device discovery, pairing, and connection management
 */
public class BluetoothPrinterManager {

    private static final String TAG = "BluetoothPrinterMgr";

    // Standard UUID for SPP (Serial Port Profile) used by most thermal printers
    private static final UUID PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice connectedDevice;
    private OutputStream outputStream;
    private boolean isConnected = false;
    private ConnectionCallback connectionCallback;

    /**
     * Interface for connection status callbacks
     */
    public interface ConnectionCallback {
        void onConnected(BluetoothDevice device);
        void onDisconnected();
        void onConnectionFailed(String error);
    }

    /**
     * Interface for device discovery callbacks
     */
    public interface DeviceDiscoveryCallback {
        void onDevicesFound(List<BluetoothDevice> devices);
        void onDiscoveryFailed(String error);
    }

    public BluetoothPrinterManager(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Check if Bluetooth is supported on this device
     */
    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }

    /**
     * Check if Bluetooth is currently enabled
     */
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * Get the default Bluetooth adapter
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    /**
     * Check if necessary Bluetooth permissions are granted
     */
    public boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 and above
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Below Android 12
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Get list of paired Bluetooth devices
     */
    public List<BluetoothDevice> getPairedDevices() {
        List<BluetoothDevice> devices = new ArrayList<>();

        if (!isBluetoothSupported() || !isBluetoothEnabled()) {
            Log.w(TAG, "Bluetooth not supported or not enabled");
            return devices;
        }

        if (!hasBluetoothPermissions()) {
            Log.w(TAG, "Bluetooth permissions not granted");
            return devices;
        }

        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices != null && !pairedDevices.isEmpty()) {
                devices.addAll(pairedDevices);
                Log.d(TAG, "Found " + devices.size() + " paired devices");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when getting paired devices", e);
        }

        return devices;
    }

    /**
     * Get list of paired thermal printers
     * Filters devices that are likely to be printers based on device name
     */
    public List<BluetoothDevice> getPairedPrinters() {
        List<BluetoothDevice> printers = new ArrayList<>();
        List<BluetoothDevice> pairedDevices = getPairedDevices();

        for (BluetoothDevice device : pairedDevices) {
            try {
                String deviceName = device.getName();
                if (deviceName != null) {
                    String nameLower = deviceName.toLowerCase();
                    // Common printer name patterns including Officom
                    if (nameLower.contains("printer") ||
                            nameLower.contains("pos") ||
                            nameLower.contains("thermal") ||
                            nameLower.contains("receipt") ||
                            nameLower.contains("rpp") ||
                            nameLower.contains("mpt") ||
                            nameLower.contains("officom") ||
                            nameLower.contains("oc-pt") ||
                            nameLower.contains("bluetooth printer")) {
                        printers.add(device);
                        Log.d(TAG, "Found printer: " + deviceName);
                    }
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception when checking device name", e);
            }
        }

        return printers;
    }

    /**
     * Connect to a Bluetooth printer device
     */
    public void connectToPrinter(BluetoothDevice device, ConnectionCallback callback) {
        if (isConnected) {
            Log.w(TAG, "Already connected to a device. Disconnecting first...");
            disconnect();
        }

        if (!hasBluetoothPermissions()) {
            callback.onConnectionFailed("Bluetooth permissions not granted");
            return;
        }

        this.connectionCallback = callback;

        // Connect in a separate thread to avoid blocking UI
        new Thread(() -> {
            try {
                String deviceName = "Unknown Device";
                try {
                    deviceName = device.getName();
                } catch (SecurityException e) {
                    // Ignore if we can't get the name
                }

                Log.d(TAG, "Attempting to connect to: " + deviceName);

                // Cancel discovery to improve connection speed
                try {
                    if (bluetoothAdapter.isDiscovering()) {
                        bluetoothAdapter.cancelDiscovery();
                        Log.d(TAG, "Cancelled Bluetooth discovery");
                    }
                } catch (SecurityException e) {
                    Log.w(TAG, "Could not cancel discovery", e);
                }

                // Create socket
                bluetoothSocket = device.createRfcommSocketToServiceRecord(PRINTER_UUID);

                // Connect to the device
                bluetoothSocket.connect();

                // Get output stream for sending data
                outputStream = bluetoothSocket.getOutputStream();

                connectedDevice = device;
                isConnected = true;

                Log.d(TAG, "✓ Successfully connected to: " + deviceName);
                callback.onConnected(device);

            } catch (SecurityException e) {
                Log.e(TAG, "Security exception during connection", e);
                cleanup();
                callback.onConnectionFailed("Permission denied: " + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "IO exception during connection", e);
                cleanup();

                // Try fallback connection method
                try {
                    Log.d(TAG, "Trying fallback connection method...");
                    bluetoothSocket = (BluetoothSocket) device.getClass()
                            .getMethod("createRfcommSocket", new Class[]{int.class})
                            .invoke(device, 1);
                    bluetoothSocket.connect();
                    outputStream = bluetoothSocket.getOutputStream();
                    connectedDevice = device;
                    isConnected = true;

                    String deviceName = "Unknown Device";
                    try {
                        deviceName = device.getName();
                    } catch (SecurityException se) {
                        // Ignore
                    }

                    Log.d(TAG, "✓ Connected using fallback method to: " + deviceName);
                    callback.onConnected(device);
                } catch (Exception fallbackException) {
                    Log.e(TAG, "Fallback connection also failed", fallbackException);
                    cleanup();
                    callback.onConnectionFailed("Connection failed. Make sure the printer is turned on and not connected to another device.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during connection", e);
                cleanup();
                callback.onConnectionFailed("Unexpected error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Disconnect from the currently connected printer
     */
    public void disconnect() {
        if (!isConnected) {
            Log.d(TAG, "Not connected to any device");
            return;
        }

        Log.d(TAG, "Disconnecting from printer...");
        cleanup();

        if (connectionCallback != null) {
            connectionCallback.onDisconnected();
        }

        Log.d(TAG, "✓ Disconnected successfully");
    }

    /**
     * Clean up resources
     */
    private void cleanup() {
        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing output stream", e);
        }

        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket", e);
        }

        connectedDevice = null;
        isConnected = false;
    }

    /**
     * Check if printer is currently connected
     */
    public boolean isConnected() {
        // Check if we think we're connected and verify the socket is still connected
        if (isConnected && bluetoothSocket != null) {
            try {
                return bluetoothSocket.isConnected();
            } catch (Exception e) {
                Log.e(TAG, "Error checking socket connection", e);
                isConnected = false;
                return false;
            }
        }
        return false;
    }

    /**
     * Get the currently connected device
     */
    public BluetoothDevice getConnectedDevice() {
        return connectedDevice;
    }

    /**
     * Get the output stream for printing
     * CHANGED: Made public so CounterActivity can access it
     */
    public OutputStream getOutputStream() {
        if (!isConnected || outputStream == null) {
            Log.w(TAG, "Output stream requested but printer not connected");
            return null;
        }
        return outputStream;
    }

    /**
     * Test printer connection by sending a simple command
     */
    public boolean testConnection() {
        if (!isConnected || outputStream == null) {
            Log.w(TAG, "Cannot test connection - not connected");
            return false;
        }

        try {
            // Send a simple line feed command to test
            outputStream.write(new byte[]{0x0A});
            outputStream.flush();
            Log.d(TAG, "✓ Connection test successful");
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Connection test failed", e);
            isConnected = false;
            cleanup();
            if (connectionCallback != null) {
                connectionCallback.onDisconnected();
            }
            return false;
        }
    }

    /**
     * Print a test page to verify printer is working
     */
    public void printTestPage() {
        if (!isConnected || outputStream == null) {
            Log.w(TAG, "Cannot print test page - not connected");
            return;
        }

        new Thread(() -> {
            try {
                OutputStream os = outputStream;

                // Initialize printer
                os.write(new byte[]{0x1B, 0x40});

                // Center align
                os.write(new byte[]{0x1B, 0x61, 0x01});

                // Bold and double size
                os.write(new byte[]{0x1B, 0x45, 0x01});
                os.write(new byte[]{0x1D, 0x21, 0x11});
                os.write("TEST PRINT\n".getBytes());
                os.write(new byte[]{0x1D, 0x21, 0x00});
                os.write(new byte[]{0x1B, 0x45, 0x00});

                os.write("--------------------------------\n".getBytes());
                os.write("Printer is working correctly!\n".getBytes());
                os.write("--------------------------------\n\n".getBytes());

                // Feed and cut
                os.write(new byte[]{0x1B, 0x64, 0x03});
                os.write(new byte[]{0x1D, 0x56, 0x00});

                os.flush();

                Log.d(TAG, "✓ Test page printed successfully");

            } catch (IOException e) {
                Log.e(TAG, "Error printing test page", e);
                isConnected = false;
                cleanup();
                if (connectionCallback != null) {
                    connectionCallback.onConnectionFailed("Print test failed");
                }
            }
        }).start();
    }

    /**
     * Get connection status string for display
     */
    public String getConnectionStatus() {
        if (!isBluetoothSupported()) {
            return "Bluetooth not supported";
        }
        if (!isBluetoothEnabled()) {
            return "Bluetooth disabled";
        }
        if (!hasBluetoothPermissions()) {
            return "Missing permissions";
        }
        if (isConnected && connectedDevice != null) {
            try {
                return "Connected: " + connectedDevice.getName();
            } catch (SecurityException e) {
                return "Connected (name unavailable)";
            }
        }
        return "Not connected";
    }

    /**
     * Release all resources when done
     */
    public void release() {
        Log.d(TAG, "Releasing Bluetooth printer manager");
        disconnect();
        connectionCallback = null;
    }
}