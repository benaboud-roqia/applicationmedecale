package com.dianerverotect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothManager {
    private static final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private Context context;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mBluetoothSocket;
    private InputStream mInputStream;

    public interface DataListener {
        void onDataReceived(String str);
    }

    public BluetoothManager(Context context) {
        this.context = context;
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mBluetoothAdapter = defaultAdapter;
        if (defaultAdapter == null) {
            Toast.makeText(context, "Bluetooth is not supported on this device", 0).show();
        }
    }

    public void connectToDevice(final String deviceName, final Runnable onSuccess, final Runnable onFail) {
        new Thread(new Runnable() { // from class: com.example.dodi2.-$$Lambda$BluetoothManager$SMg6BST17T-zRtgKEQooZ8JoxTU
            @Override // java.lang.Runnable
            public final void run() {
                BluetoothManager.this.connectToDeviceBluetoothManager(deviceName, onSuccess, onFail);
            }
        }).start();
    }

    public void connectToDeviceBluetoothManager(String deviceName, Runnable onSuccess, Runnable onFail) {
        BluetoothDevice device = findDeviceByName(deviceName);
        if (device != null) {
            try {
                this.mBluetoothSocket = device.createRfcommSocketToServiceRecord(SERIAL_UUID);
                if (this.mBluetoothAdapter.isDiscovering()) {
                    this.mBluetoothAdapter.cancelDiscovery();
                }
                this.mBluetoothSocket.connect();
                InputStream inputStream = this.mBluetoothSocket.getInputStream();
                this.mInputStream = inputStream;
                if (inputStream != null) {
                    ((MainActivity) this.context).runOnUiThread(onSuccess);
                    return;
                } else {
                    ((MainActivity) this.context).runOnUiThread(onFail);
                    return;
                }
            } catch (IOException e) {
                Log.e("BluetoothManager", "Connection failed", e);
                closeSocket();
                ((MainActivity) this.context).runOnUiThread(onFail);
                return;
            }
        }
        ((MainActivity) this.context).runOnUiThread(onFail);
    }

    public void sendMessage(final String message) {
        if (this.mBluetoothSocket != null) {
            new Thread(new Runnable() { // from class: com.example.dodi2.-$$Lambda$BluetoothManager$E48jVOP131st-pQtlEWtp7EuDWk
                @Override // java.lang.Runnable
                public final void run() {
                    BluetoothManager.this.lambda$sendMessage$1$BluetoothManager(message);
                }
            }).start();
        }
    }

    public /* synthetic */ void lambda$sendMessage$1$BluetoothManager(String message) {
        try {
            this.mBluetoothSocket.getOutputStream().write(message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeSocket() {
        try {
            BluetoothSocket bluetoothSocket = this.mBluetoothSocket;
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startListening(final Handler handler, final DataListener dataListener) {
        new Thread(new Runnable() { // from class: com.example.dodi2.-$$Lambda$BluetoothManager$r1BOHaFawgs1vVWKRcOzRSKnilo
            @Override // java.lang.Runnable
            public final void run() {
                BluetoothManager.this.lambda$startListening$3$BluetoothManager(handler, dataListener);
            }
        }).start();
    }

    public /* synthetic */ void lambda$startListening$3$BluetoothManager(Handler handler, final DataListener dataListener) {
        byte[] buffer = new byte[1024];
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (this.mInputStream.available() > 0) {
                    int bytes = this.mInputStream.read(buffer);
                    final String data = new String(buffer, 0, bytes).trim();
                    handler.post(new Runnable() {
                        @Override
                        public final void run() {
                            dataListener.onDataReceived(data);
                        }
                    });
                }
            } catch (IOException e) {
                Log.e("BluetoothManager", "Error reading data", e);
                return;
            }
        }
    }

    private BluetoothDevice findDeviceByName(String deviceName) {
        Set<BluetoothDevice> pairedDevices = this.mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName() != null && device.getName().equals(deviceName)) {
                return device;
            }
        }
        return null;
    }
}
