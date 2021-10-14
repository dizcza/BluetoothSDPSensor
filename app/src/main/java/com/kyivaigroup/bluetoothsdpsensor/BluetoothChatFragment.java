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

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.github.mikephil.charting.data.Entry;
import com.kyivaigroup.bluetoothsdpsensor.record.RecordCollection;
import com.kyivaigroup.bluetoothsdpsensor.record.RecordStatus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothChatFragment extends Fragment {

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private SensorLineChart mLineChart;
    private TextView mTextViewTemperature;
    private TextView mTextViewHumidity;
    private TextView mTextViewPressure;
    private TextView mTextViewStatusQueueSize;
    private TextView mTextViewStatusReadSensor;
    private TextView mTextViewSDCardFreeMB;
    private MenuItem mConnectMenu;
    private SavedChartsFragment mSavedChartsFragment;
    private Button mSaveGraphBtn;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private Handler mHandler;

    private class MessageHandler extends Handler {

        /**
         * Name of the connected device
         */
        private String mConnectedDeviceName = null;

        public MessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            if (null != activity) {
                                setStatus(activity.getString(R.string.title_connected_to, mConnectedDeviceName));
                            }
                            String timestamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS")
                                    .format(new Date());
                            long tick = System.currentTimeMillis();
                            String syncMessage = String.format(Locale.getDefault(),
                                    "/%s\n/%s %d\n%s\0", Constants.INFO, Constants.SYNC_CLOCK, tick, timestamp);
                            BluetoothChatFragment.this.sendMessage(syncMessage);

                            mConnectMenu.setTitle(R.string.disconnect);
                            mConversationArrayAdapter.clear();
                            mLineChart.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            if (mConnectMenu != null) {
                                // null when the app is launching
                                mConnectMenu.setTitle(R.string.connect);
                            }
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add(writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    // construct a string from the valid bytes in the buffer
                    onRecordsReceived((RecordCollection) msg.obj);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    }

    private void saveChart() {
        List<Entry> entries = mLineChart.getChartEntries();
        if (entries.size() == 0) {
            // no entries in the chart
            return;
        }
        File root = android.os.Environment.getExternalStorageDirectory();
        File records = new File(root.getAbsolutePath(), Constants.SDP_RECORDS_FOLDER);
        records.mkdirs();
        String fileName = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss'.txt'").format(new Date());
        File file = new File(records, fileName);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(fos);
            pw.println(mLineChart.getDescription().getText());
            for (Entry entry : entries) {
                pw.println(String.format("%.6f,%.4f", entry.getX(), entry.getY()));
            }
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mHandler = new MessageHandler(Looper.getMainLooper());

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mSavedChartsFragment = new SavedChartsFragment();

        // If the adapter is null, then Bluetooth is not supported
        FragmentActivity activity = getActivity();
        if (mBluetoothAdapter == null && activity != null) {
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mBluetoothAdapter == null) {
            return;
        }
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.main_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mLineChart = view.findViewById(R.id.graph);

        mTextViewHumidity = view.findViewById(R.id.text_humidity);
        mTextViewPressure = view.findViewById(R.id.text_atm_pressure);
        mTextViewTemperature = view.findViewById(R.id.text_temperature);
        mTextViewStatusQueueSize = view.findViewById(R.id.text_status_queue_size);
        mTextViewStatusReadSensor = view.findViewById(R.id.text_status_read_sensor);
        mTextViewSDCardFreeMB = view.findViewById(R.id.text_sdcard_free_mb);

        mConversationView = view.findViewById(R.id.sent_commands_list);
        mConversationView.setEmptyView(view.findViewById(R.id.empty_list_item));

        mSaveGraphBtn = view.findViewById(R.id.save_btn);
        mSaveGraphBtn.setBackgroundColor(Color.LTGRAY);
        mSaveGraphBtn.setEnabled(false);
        final ActivityResultLauncher<String> requestWriteExternal =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        saveChart();
                    } else {
                        Toast.makeText(getActivity(), "Could not save the chart", Toast.LENGTH_SHORT).show();
                    }
                });
        mSaveGraphBtn.setOnClickListener(buttonView -> {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                saveChart();
            } else {
                // The registered ActivityResultCallback gets the result of this request.
                requestWriteExternal.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        } );

        final EditText sendEditText = view.findViewById(R.id.command_tx);
        Button sendButton = view.findViewById(R.id.button_send);
        sendButton.setOnClickListener(buttonView -> {
            // Send a message using content of the edit text widget
            String message = sendEditText.getText().toString();
            message = String.format(Locale.getDefault(), "/%s\0", message);
            sendMessage(message);
            sendEditText.setText("");
        } );
    }

    private void onRecordsReceived(RecordCollection collection) {
        mLineChart.update(collection);
        Context context = getContext();
        if (context != null) {
            mSaveGraphBtn.setBackgroundColor(context.getResources().getColor(R.color.ic_launcher_background));
        }
        mSaveGraphBtn.setEnabled(true);
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (collection.temperature != null) {
            mTextViewTemperature.setText(String.format(Locale.getDefault(), "%.1f", collection.temperature));
        }
        if (collection.pressureHumidity != null) {
            mTextViewPressure.setText(String.format(Locale.getDefault(), "%.0f", collection.pressureHumidity.pressure));
            mTextViewHumidity.setText(String.format(Locale.getDefault(), "%.1f", collection.pressureHumidity.humidity));
        }
        if (collection.status != null) {
            RecordStatus status = collection.status;
            mTextViewStatusQueueSize.setText(activity.getString(R.string.status_queue_size, status.messagesCurr, status.messagesMax));
            mTextViewStatusReadSensor.setText(activity.getString(R.string.status_read_sensor, status.readDurationMax, status.readsFailed));
        }
        if (collection.sdcardFreeMB != 0) {
            mTextViewSDCardFreeMB.setText(activity.getString(R.string.sdcard_free_mb, collection.sdcardFreeMB));
        }
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        // Initialize the array adapter for the conversation thread
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        mConversationArrayAdapter = new ArrayAdapter<>(activity, R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(mHandler);

        final FragmentManager fragmentManager = getParentFragmentManager();
        fragmentManager.addOnBackStackChangedListener(() -> {
            if (mLineChart != null) {
                // when the count is zero, the fragment is back
                if (fragmentManager.getBackStackEntryCount() == 0) {
                    mLineChart.clear();
                    mChatService.onResume();
                } else {
                    mChatService.onPause();
                }
            }
        });
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send, true);
        }
    }

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getSupportActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getSupportActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    FragmentActivity activity = getActivity();
                    if (activity != null) {
                        Toast.makeText(activity, R.string.bt_not_enabled_leaving,
                                Toast.LENGTH_SHORT).show();
                        activity.finish();
                    }
                }
                break;
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     */
    private void connectDevice(Intent data) {
        // Get the device MAC address
        Bundle extras = data.getExtras();
        if (extras == null) {
            return;
        }
        String address = extras.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device);
    }

    private void disconnect() {
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);
        mConnectMenu = menu.findItem(R.id.connect_scan);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connect_scan: {
                if (item.getTitle().equals(getActivity().getString(R.string.connect))) {
                    // Launch the DeviceListActivity to see devices and do scan
                    Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                } else {
                    disconnect();
                }
                return true;
            }
            case R.id.show_saved: {
                getParentFragmentManager().beginTransaction().replace(R.id.main_fragment, mSavedChartsFragment).addToBackStack(null).commit();
                return true;
            }
        }
        return false;
    }

}
