package com.example.bluetoothcontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button onButton;
    Button offButton;

    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    private OutputStream mmOutputStream;
    private InputStream mmInputStream;
    private BluetoothAdapter mBluetoothAdapter;
    public Set<BluetoothDevice> pairedDevices;
    private Thread workerThread;

    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        onButton = findViewById(R.id.onButton);
        offButton = findViewById(R.id.offButton);

        try {
            findBT();
            openBT();
        } catch (IOException e) {
            e.printStackTrace();
        }

        onButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ledOn();
            }
        });

        offButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ledOff();
            }
        });
    }

    private void ledOn() {
        try {
            sendData("1");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ledOff() {
        try {
            sendData("0");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void openBT() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard //SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();
        beginListenForData();
        Toast.makeText(MainActivity.this, "Bluetooth Opened", Toast.LENGTH_SHORT).show();
    }

    public void findBT() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "No bluetooth adapter available", Toast.LENGTH_SHORT).show();
        }

        if(!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        pairedDevices = mBluetoothAdapter.getBondedDevices();
        mmDevice = mBluetoothAdapter.getRemoteDevice("98:D3:31:FB:41:07"); //Put your HC-05/HC-06's Bluetooth Address Here
        if (pairedDevices.contains(mmDevice))
        {
            Toast.makeText(MainActivity.this,"Bluetooth Device Found, address: " + mmDevice.getAddress() ,Toast.LENGTH_LONG).show();
            Log.d("ArduinoBT", "BT is paired");
        }
        Toast.makeText(MainActivity.this, "Bluetooth Device Found", Toast.LENGTH_SHORT).show();
    }

    void beginListenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(() -> {
            while(!Thread.currentThread().isInterrupted() && !stopWorker) {
                try {
                    int bytesAvailable = mmInputStream.available();
                    if(bytesAvailable > 0) {
                        byte[] packetBytes = new byte[bytesAvailable];
                        mmInputStream.read(packetBytes);
                        for(int i=0;i<bytesAvailable;i++) {
                            byte b = packetBytes[i];
                            if(b == delimiter) {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, StandardCharsets.US_ASCII);
                                readBufferPosition = 0;

                                handler.post(() -> {
                                    Toast.makeText(MainActivity.this, data, Toast.LENGTH_SHORT).show();

                                });
                            }
                            else {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                }
                catch (IOException ex) {
                    stopWorker = true;
                }
            }
        });

        workerThread.start();
    }

    void sendData(String i) throws IOException {
        //mmOutputStream.write(msg.getBytes());
        byte[] buffer = i.getBytes();
        try {
            mmOutputStream = mmSocket.getOutputStream();
            mmOutputStream.write(buffer);
            Log.d("message", i + " sent");
            Toast.makeText(MainActivity.this, "Data sent", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    void closeBt2() throws IOException {
        mmInputStream.close();
        mmOutputStream.close();
        mmSocket.close();
    }

    void closeBT() throws IOException {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        Toast.makeText(MainActivity.this, "Bluetooth Closed", Toast.LENGTH_SHORT).show();
    }
}
