/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kyivaigroup.bluetoothsdpsensor;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.kyivaigroup.bluetoothsdpsensor.record.RecordCollection;
import com.kyivaigroup.bluetoothsdpsensor.record.SerialParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothChatService {
    // Debugging
    private static final String TAG = BluetoothChatService.class.getSimpleName();

    // UUID of the BT classic Serial Port Protocol (SPP)
    private static final UUID UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final long SYNC_CLOCK_PERIOD_MS = 10_000;

    // Member fields
    private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private Timer mSyncTimer;
    private int mState;
    private int mNewState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2;  // now connected to a remote device

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param handler A Handler to send messages back to the UI Activity
     */
    public BluetoothChatService(Handler handler) {
        mState = STATE_NONE;
        mNewState = mState;
        mHandler = handler;
    }

    /**
     * Update UI title according to the current state of the chat connection
     */
    private synchronized void updateUserInterfaceTitle() {
        mState = getState();
        mNewState = mState;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Called by the Activity onResume()
     */
    public synchronized void start() {
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // Update UI title
        updateUserInterfaceTitle();

        if (mSyncTimer != null) {
            mSyncTimer.cancel();
        }
        mSyncTimer = new Timer();
        mSyncTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long tick = System.currentTimeMillis();
                String syncMessage = String.format(Locale.getDefault(), "/%s %d\0", Constants.CLOCK_SYNC, tick);
                byte[] send = syncMessage.getBytes();
                write(send, false);
            }
        }, 1000, SYNC_CLOCK_PERIOD_MS);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSyncTimer != null) {
            mSyncTimer.cancel();
            mSyncTimer = null;
        }

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Write to the ConnectedThread in an synchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[], boolean)
     */
    public synchronized void write(byte[] out, boolean notifyUI) {
        if (mState != STATE_CONNECTED) {
            return;
        }
        mConnectedThread.write(out, notifyUI);
    }

    private void connectionError(String msg) {
        // Send a failure message back to the Activity
        sendToastMessage(msg);

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        BluetoothChatService.this.start();
    }

    private void sendToastMessage(String string) {
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, string);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(UUID_SPP);
            } catch (IOException e) {
                e.printStackTrace();
                // Send a failure message back to the Activity
                connectionError("Could not create a socket");
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            setName(ConnectThread.class.getSimpleName());

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception.
                // mmSocket can be Null.
                mmSocket.connect();
            } catch (IOException | NullPointerException e) {
                // Close the socket silently
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                connectionError("Unable to connect device");
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final SerialParser mmSerialParser = new SerialParser();
        private int mBytesReceivedMax = 1000;  // omit printing small values

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                mState = STATE_CONNECTED;
            } catch (IOException e) {
                e.printStackTrace();
                cancel();
                connectionError("IO sockets not created");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[16284];
            int nbytes;

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    nbytes = mmInStream.read(buffer);
                    if (nbytes > mBytesReceivedMax) {
                        mBytesReceivedMax = nbytes;
                        Log.i(TAG, String.format("RX %d bytes", nbytes));
                    }

                    mmSerialParser.receive(buffer, nbytes);
                    if (mmSerialParser.hasRecords()) {
                        // Send the records to the UI Activity
                        RecordCollection collection = mmSerialParser.consumeRecords();
                        mHandler.obtainMessage(Constants.MESSAGE_READ, collection).sendToTarget();
                    }

                    sleep(100);
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionError("Device connection was lost");
                    break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         * @param notifyUI Send the message back to the UI to be displayed in the sent messages
         */
        public void write(byte[] buffer, boolean notifyUI) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                if (notifyUI) {
                    mHandler.obtainMessage(Constants.MESSAGE_WRITE, buffer).sendToTarget();
                }
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void onPause() {
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
        }
    }

    public void onResume() {
        if (mConnectedThread != null) {
            mConnectedThread.start();
        }
    }
}
