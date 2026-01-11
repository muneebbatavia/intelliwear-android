package com.example.intelliwear;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;


import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ReadingsActivity extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice selectedDevice;
    private ArrayAdapter<String> deviceListAdapter;
    private ArrayList<BluetoothDevice> allDevices = new ArrayList<>();
    private boolean isBluetoothConnected = false;

    private TextView bluetoothStatus, tempText, pressureText, avgTempTextView, avgPressureTextView;
    private Button connectBtn;
    private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int MAX_READINGS = 10;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_readings);

        bluetoothStatus = findViewById(R.id.bluetooth_status);
        tempText = findViewById(R.id.temp_value);
        pressureText = findViewById(R.id.pressure_value);
        connectBtn = findViewById(R.id.connect_button);
        avgTempTextView = findViewById(R.id.avg_temp);
        avgPressureTextView = findViewById(R.id.avg_pressure);


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        connectBtn.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                    requestPermissions(new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                    }, 100);
                    return;
                }
            }
            showDeviceDialog();
        });

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void showDeviceDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.bluetooth_device_dialog, null);
        ListView deviceListView = dialogView.findViewById(R.id.device_list);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceListView.setAdapter(deviceListAdapter);
        allDevices.clear();
        deviceListAdapter.clear();

        // Add paired devices
        deviceListAdapter.add("📎 Paired Devices");
        Set<BluetoothDevice> paired = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : paired) {
            deviceListAdapter.add("  • " + device.getName());
            allDevices.add(device);
        }

        // Add available devices (scan)
        deviceListAdapter.add("📡 Available Devices");
        bluetoothAdapter.startDiscovery();
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            String item = deviceListAdapter.getItem(position);
            if (item == null || item.contains("📎") || item.contains("📡")) return;

            selectedDevice = allDevices.get(position - getHeaderCount(position));
            if (selectedDevice == null) {
                Toast.makeText(this, "Device selection failed", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("TAG", "Selected device: " + selectedDevice.getName() + " BondState: " + selectedDevice.getBondState());

            bluetoothAdapter.cancelDiscovery();
            connectToDevice(selectedDevice);
            dialog.dismiss();
        });

        dialog.show();
    }

    private int getHeaderCount(int upToIndex) {
        int count = 0;
        for (int i = 0; i < upToIndex; i++) {
            String item = deviceListAdapter.getItem(i);
            if (item != null && (item.contains("📎") || item.contains("📡"))) {
                count++;
            }
        }
        return count;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && device.getName() != null && !allDevices.contains(device)) {
                    deviceListAdapter.add("  • " + device.getName());
                    allDevices.add(device);
                }
            }
        }
    };

    private void connectToDevice(BluetoothDevice device) {
        new Thread(() -> {
            try {
                runOnUiThread(() -> bluetoothStatus.setText("Connecting..."));

                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    runOnUiThread(() -> {
                        bluetoothStatus.setText("Device not paired!");
                        Toast.makeText(this, "Please pair HC-05 in phone Bluetooth settings", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                bluetoothAdapter.cancelDiscovery(); // stop discovery before connection

                try {
                    // Attempt standard connection
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(BT_UUID);
                    bluetoothSocket.connect();
                } catch (IOException standardException) {
                    Log.d("TAG", "Standard method failed, trying fallback", standardException);

                    // Fallback for HC-05
                    try {
                        bluetoothSocket = (BluetoothSocket) device.getClass()
                                .getMethod("createRfcommSocket", int.class)
                                .invoke(device, 1); // channel 1

                        bluetoothSocket.connect();
                    } catch (Exception fallbackException) {
                        Log.e("TAG", "Fallback connection failed", fallbackException);
                        runOnUiThread(() -> {
                            bluetoothStatus.setText("Connection Failed (Fallback)");
                            Toast.makeText(this, "Fallback connection failed. Try re-pairing the device.", Toast.LENGTH_LONG).show();
                        });
                        return;
                    }
                }

                runOnUiThread(() -> {
                    isBluetoothConnected = true;
                    bluetoothStatus.setText("Connected to: " + device.getName());
                    Toast.makeText(this, "Connected successfully!", Toast.LENGTH_SHORT).show();
                });

                readDataFromBluetooth();

            } catch (Exception e) {
                Log.e("TAG", "Unexpected connection error", e);
                runOnUiThread(() -> {
                    bluetoothStatus.setText("Connection Failed");
                    Toast.makeText(this, "Could not connect. Make sure device is paired and powered.", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void readDataFromBluetooth() {
        InputStream inputStream;
        try {
            inputStream = bluetoothSocket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            while (isBluetoothConnected) {
                String line = reader.readLine();
                if (line != null) {
                    Log.d("TAG", line);
                    processReceivedData(line);
                }
            }
        } catch (IOException e) {
            Log.e("TAG", "Error reading from Bluetooth", e);
        }
    }

    private void processReceivedData(String line) {
        String[] parts = line.split("\\|");
        if (parts.length == 3) {
            String temp = parts[0].trim().replace("Temp:", "").replace("C", "");
            String pressure = parts[1].trim().replace("Pressure:", "").replace("hPa", "");

            runOnUiThread(() -> {
                tempText.setText("Temperature: " + temp + " °C");
                pressureText.setText("Pressure: " + pressure + " hPa");
            });

            saveToFirestore(temp, pressure);
        }
    }

    private void saveToFirestore(String temp, String pressure) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String uid = auth.getCurrentUser().getUid();

        CollectionReference readingsRef = db.collection("users").document(uid).collection("readings");

        readingsRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.size() >= MAX_READINGS) {
                        // Delete the oldest entry
                        DocumentSnapshot oldestDoc = queryDocumentSnapshots.getDocuments().get(0);
                        readingsRef.document(oldestDoc.getId()).delete();
                    }

                    // Prepare new data
                    Map<String, Object> data = new HashMap<>();
                    data.put("temperature", temp);
                    data.put("pressure", pressure);
                    data.put("timestamp", new Date());

                    // Add new reading
                    readingsRef.add(data)
                            .addOnSuccessListener(doc -> {
                                Log.d("TAG", "Saved");
                                calculateAverages(uid); // call after saving
                            })
                            .addOnFailureListener(e -> Log.e("TAG", "Error", e));
                });
    }

    private void calculateAverages(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid).collection("readings")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(MAX_READINGS)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    double totalTemp = 0;
                    double totalPressure = 0;
                    int count = 0;

                    for (DocumentSnapshot doc : querySnapshots) {
                        try {
                            double temp = Double.parseDouble(doc.getString("temperature"));
                            double pressure = Double.parseDouble(doc.getString("pressure"));

                            totalTemp += temp;
                            totalPressure += pressure;
                            count++;
                        } catch (Exception e) {
                            Log.e("TAG", "Invalid number format", e);
                        }
                    }

                    if (count > 0) {
                        double avgTemp = totalTemp / count;
                        double avgPressure = totalPressure / count;

                        // Display on UI (replace with your actual TextViews)
                        avgTempTextView.setText("Avg Temp: " + avgTemp + " °C");
                        avgPressureTextView.setText("Avg Pressure: " + avgPressure + " Pa");
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {}
        try {
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException ignored) {}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                showDeviceDialog(); // retry after permission granted
            } else {
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
