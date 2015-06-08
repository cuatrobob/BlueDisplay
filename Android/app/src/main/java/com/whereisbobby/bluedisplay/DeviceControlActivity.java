package com.whereisbobby.bluedisplay;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;

    EditText edtSend;
    ScrollView svResult;
    Button btnSend;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            Log.i(TAG, "Bluetooth initialized.");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.i(TAG, "GATT connected.");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                invalidateOptionsMenu();
                btnSend.setEnabled(false);
                Log.i(TAG, "GATT disconnected.");
                clearUI();
            }else if(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                mConnected = true;
                mDataField.setText("");
                ShowDialog();
                btnSend.setEnabled(true);
                Log.i(TAG, "GATT services discovered.");
                invalidateOptionsMenu();
            }else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.i(TAG, "GATT data received.");
                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                if (data != null) {
                    if (mDataField.length() > 500)
                        mDataField.setText("");
                    mDataField.append(data);
                    svResult.post(new Runnable() {
                        public void run() {
                            svResult.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
                }
            }
        }
    };

    private void clearUI() {
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        mDataField = (TextView) findViewById(R.id.data_value);
        edtSend = (EditText) this.findViewById(R.id.edtSend);
//        edtSend.setText("");
        svResult = (ScrollView) this.findViewById(R.id.svResult);

        btnSend = (Button) this.findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new ClickEvent());
        btnSend.setEnabled(false);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        Log.d(TAG, "bindService = " + bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE));

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result = " + result);
        } else {

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(mGattUpdateReceiver);
        //unbindService(mServiceConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //this.unregisterReceiver(mGattUpdateReceiver);
        //unbindService(mServiceConnection);
        if(mBluetoothLeService != null)
        {
            mBluetoothLeService.close();
            mBluetoothLeService = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {  //menu item selected
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                if(mConnected)
                {
                    mBluetoothLeService.disconnect();
                    mConnected = false;
                }
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void ShowDialog()
    {
        Toast.makeText(this, "Successfully connected!", Toast.LENGTH_SHORT).show();
    }

    // button click
    class ClickEvent implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v == btnSend) {
                if(!mConnected) return;

                String edtSendValue = edtSend.getText().toString();

                if (edtSendValue.length() == 0) {
                    Toast.makeText(DeviceControlActivity.this, "Input required.", Toast.LENGTH_SHORT).show();
                    return;
                } else if (edtSendValue.length() > 0 && edtSendValue.length() <= 20) {
                    mBluetoothLeService.writeValue(edtSendValue);
                    Log.i(TAG, "Length of input <20.");
                } else if (edtSendValue.length() > 20 && edtSendValue.length() <= 40) {
                    mBluetoothLeService.writeValue(edtSendValue.substring(0, 19));
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mBluetoothLeService.writeValue(edtSendValue.substring(19, edtSend.length()));
                    Log.i(TAG, "Length of input >20, <40.");
                } else if (edtSendValue.length() > 40 && edtSendValue.length() <= 60) {
                    mBluetoothLeService.writeValue(edtSendValue.substring(0, 19));
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mBluetoothLeService.writeValue(edtSendValue.substring(19, 39));
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mBluetoothLeService.writeValue(edtSendValue.substring(39, edtSend.length()));
                    Log.i(TAG, "Length of input >40, <60.");
                } else if (edtSendValue.length() > 60 && edtSendValue.length() <= 80) {
                    mBluetoothLeService.writeValue(edtSendValue.substring(0, 19));
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mBluetoothLeService.writeValue(edtSendValue.substring(19, 39));
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mBluetoothLeService.writeValue(edtSendValue.substring(39, 59));
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mBluetoothLeService.writeValue(edtSendValue.substring(59, edtSend.length()));
                    Log.i(TAG, "Length of input >60, <80.");
                } else if (edtSendValue.length() > 80 && edtSendValue.length() <= 84) {
                    mBluetoothLeService.writeValue(edtSendValue.substring(0, 19));
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mBluetoothLeService.writeValue(edtSendValue.substring(19, 39));
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mBluetoothLeService.writeValue(edtSendValue.substring(39, 59));
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mBluetoothLeService.writeValue(edtSendValue.substring(59, 79));
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mBluetoothLeService.writeValue(edtSendValue.substring(79, edtSend.length()));
                    Log.i(TAG, "Length of input >80, <84.");
                } else if (edtSendValue.length() > 84) {
                    Toast.makeText(DeviceControlActivity.this, "Length of input exceeded the maximum length.", Toast.LENGTH_SHORT).show();
                    return;
                }

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if(imm.isActive())
                    imm.hideSoftInputFromWindow(edtSend.getWindowToken(), 0);
                //todo Send data
            }
        }

    }

    private static IntentFilter makeGattUpdateIntentFilter() { // register transfer
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothDevice.ACTION_UUID);
        return intentFilter;
    }
}
