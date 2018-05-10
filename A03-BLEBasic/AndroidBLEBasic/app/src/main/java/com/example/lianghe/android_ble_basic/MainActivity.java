/*
    Android App for Updating NightLight Color
    Heavily borrows from Scaffolding provided by TA from https://github.com/jonfroehlich/CSE590Sp2018
    Uses color picker from https://github.com/Pes8/android-material-color-picker-dialog
 */
package com.example.lianghe.android_ble_basic;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.ColorInt;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.lianghe.android_ble_basic.BLE.RBLGattAttributes;
import com.example.lianghe.android_ble_basic.BLE.RBLService;
import com.pes.androidmaterialcolorpickerdialog.ColorPickerCallback;

import java.io.File;
import java.io.FileWriter;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

// https://github.com/Pes8/android-material-color-picker-dialog
import com.pes.androidmaterialcolorpickerdialog.ColorPicker;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    // Define the device name and the length of the name
    // Note the device name and the length should be consistent with the ones defined in the Duo sketch
    private String mTargetDeviceName = "apsuman";
    private int mNameLen = 0x08;

    private final static String TAG = MainActivity.class.getSimpleName();

    // Declare all variables associated with the UI components
    private Button mConnectBtn = null;
    private TextView mDeviceName = null;
    private TextView mRssiValue = null;
    private TextView mUUID = null;
    private String mBluetoothDeviceName = "";
    private String mBluetoothDeviceUUID = "";


    // Declare all Bluetooth stuff
    private BluetoothGattCharacteristic mCharacteristicTx = null;
    private RBLService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice = null;
    private String mDeviceAddress;

    private boolean flag = true;
    private boolean mConnState = false;
    private boolean mScanFlag = false;

    private byte[] mData = new byte[3];
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 1000;   // millis

    final private static char[] hexArray = { '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    // Accelerometer Stuff
    private boolean mUsingAccel = false;
    private SensorManager _sensorManager;
    private Sensor _accelSensor;
    private Button mAccelPickerBtn;
    private TextView accelValues;

    // used for step picker
    private boolean mUsingSteps = false;
    private Button mStepPickerBtn;

    // used for manual picker
    private Button mColorPickerBtn;

    // used for controls picker
    private Button mControlsPickerBtn;

    // STEP COUNTER STUFF
    // smoothing accelerometer signal stuff
    private static int MAX_ACCEL_VALUE = 30;
    private float _rawAccelValues[] = new float[3];
    private static int SMOOTHING_WINDOW_SIZE = 20;
    private float _accelValueHistory[][] = new float[3][SMOOTHING_WINDOW_SIZE];
    private float _runningAccelTotal[] = new float[3];
    private float _curAccelAvg[] = new float[3];
    private int _curReadIndex = 0;

    // mladenovStepDetectionAlgorithm
    public static int _totalSteps = 0;
    private static float CONSTANT_C = 0.8f;
    private static float CONSTANT_K = 10.1f;
    // reduce noise in early steps
    private static int EARLY_STEPS = 2;
    private static float CONSTANT_K_early = 10.3f;
    private static int CHUNKING_SIZE = 10;
    private int _currentChunkPosition = 0;
    private float _smoothMagnitudeValues[] = new float[CHUNKING_SIZE];

    // Process service connection. Created by the RedBear Team
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((RBLService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private void setButtonDisable() {
        flag = false;
        mConnState = false;
        setControlsState(false, false, false, false);
        mConnectBtn.setText("Connect");
        mRssiValue.setText("");
        mDeviceName.setText("");
        mUUID.setText("");
    }

    private void setButtonEnable() {
        flag = true;
        mConnState = true;
        setControlsState(false, true, true, true); // when connecting physical controls is the default
        mConnectBtn.setText("Disconnect");
    }

    // Process the Gatt and get data if there is data coming from Duo board. Created by the RedBear Team
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Disconnected",
                        Toast.LENGTH_SHORT).show();
                setButtonDisable();
            } else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                Toast.makeText(getApplicationContext(), "Connected",
                        Toast.LENGTH_SHORT).show();

                getGattService(mBluetoothLeService.getSupportedGattService());
            } else if (RBLService.ACTION_GATT_RSSI.equals(action)) {
                displayData(intent.getStringExtra(RBLService.EXTRA_DATA));
            }
        }
    };

    // Display the received RSSI on the interface
    private void displayData(String data) {
        if (data != null) {
            mRssiValue.setText(data);
            mDeviceName.setText(mBluetoothDeviceName);
            mUUID.setText(mBluetoothDeviceUUID);
        }
    }


    // Get Gatt service information for setting up the communication
    private void getGattService(BluetoothGattService gattService) {
        if (gattService == null)
            return;

        setButtonEnable();
        startReadRssi();

        mCharacteristicTx = gattService
                .getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);
    }

    // Start a thread to read RSSI from the board
    private void startReadRssi() {
        new Thread() {
            public void run() {

                while (flag) {
                    mBluetoothLeService.readRssi();
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
        }.start();
    }

    // Scan all available BLE-enabled devices
    private void scanLeDevice() {
        new Thread() {

            @Override
            public void run() {
                mBluetoothAdapter.startLeScan(mLeScanCallback);

                try {
                    Thread.sleep(SCAN_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }.start();
    }

    // Callback function to search for the target Duo board which has matched UUID
    // If the Duo board cannot be found, debug if the received UUID matches the predefined UUID on the board
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             final byte[] scanRecord) {

            runOnUiThread(new Runnable() {
				@Override
				public void run() {
					byte[] serviceUuidBytes = new byte[16];
					String serviceUuid = "";
                    for (int i = (21+mNameLen), j = 0; i >= (6+mNameLen); i--, j++) {
                        serviceUuidBytes[j] = scanRecord[i];
                    }
                    /*
                     * This is where you can test if the received UUID matches the defined UUID in the Arduino
                     * Sketch and uploaded to the Duo board: 0x713d0000503e4c75ba943148f18d941e.
                     */
					serviceUuid = bytesToHex(serviceUuidBytes);
					if (stringToUuidString(serviceUuid).equals(
							RBLGattAttributes.BLE_SHIELD_SERVICE
									.toUpperCase(Locale.ENGLISH)) && device.getName().equals(mTargetDeviceName)) {
						mDevice = device;
						mBluetoothDeviceName = mDevice.getName();
						mBluetoothDeviceUUID = serviceUuid;
					}
				}
			});
        }
    };

    // Convert an array of bytes into Hex format string
    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    // Convert a string to a UUID format
    private String stringToUuidString(String uuid) {
        StringBuffer newString = new StringBuffer();
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(0, 8));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(8, 12));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(12, 16));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(16, 20));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(20, 32));

        return newString.toString();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Accelerometer Picker setup
        // See https://developer.android.com/guide/topics/sensors/sensors_motion.html
        _sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        _accelSensor = _sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        _sensorManager.registerListener(this, _accelSensor, SensorManager.SENSOR_DELAY_GAME);
        accelValues = (TextView) findViewById(R.id.accel_values);
        mAccelPickerBtn = (Button) findViewById(R.id.accel_toggle);
        mStepPickerBtn = (Button) findViewById(R.id.step_toggle);

        // Manual Picker setup
        mColorPickerBtn = (Button) findViewById(R.id.colorPickerButton);
        final ColorPicker cp = new ColorPicker(MainActivity.this);

        // controls picker setup
        mControlsPickerBtn = (Button) findViewById(R.id.controls_toggle);

        // Associate all UI components with variables
        mConnectBtn = (Button) findViewById(R.id.connectBtn);
        mDeviceName = (TextView) findViewById(R.id.deviceName);
        mRssiValue = (TextView) findViewById(R.id.rssiValue);
        mUUID = (TextView) findViewById(R.id.uuidValue);


        // Connection button click event
        mConnectBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mScanFlag == false) {
                    // Scan all available devices through BLE
                    scanLeDevice();

                    Timer mTimer = new Timer();
                    mTimer.schedule(new TimerTask() {

                        @Override
                        public void run() {
                            if (mDevice != null) {
                                mDeviceAddress = mDevice.getAddress();
                                mBluetoothLeService.connect(mDeviceAddress);
                                mScanFlag = true;
                            } else {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast toast = Toast
                                                .makeText(
                                                        MainActivity.this,
                                                        "Couldn't search Ble Shiled device!",
                                                        Toast.LENGTH_SHORT);
                                        toast.setGravity(0, 0, Gravity.CENTER);
                                        toast.show();
                                    }
                                });
                            }
                        }
                    }, SCAN_PERIOD);
                }

                System.out.println(mConnState);
                if (mConnState == false) {
                    mBluetoothLeService.connect(mDeviceAddress);
                } else {
                    mBluetoothLeService.disconnect();
                    mBluetoothLeService.close();
                    setButtonDisable();
                }
            }
        });


        mColorPickerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // disable accel picker button
                setControlsState(true, true, true, true);

                /* Show color picker dialog */
                cp.show();

                /* Set a new Listener called when user click "select" */
                cp.setCallback(new ColorPickerCallback() {
                    @Override
                    public void onColorChosen(@ColorInt int color) {

                        sendRGBToBoard(true, Color.red(color), Color.green(color), Color.blue(color));

                        // If the auto-dismiss option is not enable (disabled as default) you have to manually dismiss the dialog
                        cp.dismiss();

                        setControlsState(true, true, true, true);

                    }
                });
            }
        });

        mAccelPickerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setControlsState(true, true, false, true);
            }
        });

        mStepPickerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setControlsState(true, true, true, false);
            }
        });

        mControlsPickerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendRGBToBoard(false, 0, 0, 0);
                // now going to use controls so button should be disabled and other picker toggles should be clickable
                setControlsState(false, true, true, true);
            }
        });


        // Bluetooth setup. Created by the RedBear team.
        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
                    .show();
            finish();
        }

        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        Intent gattServiceIntent = new Intent(MainActivity.this,
                RBLService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check if BLE is enabled on the device. Created by the RedBear team.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

    }

    @Override
    protected void onStop() {
        super.onStop();

        flag = false;

        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mServiceConnection != null)
            unbindService(mServiceConnection);
    }

    // Create a list of intent filters for Gatt updates. Created by the RedBear team.
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(RBLService.ACTION_GATT_RSSI);

        return intentFilter;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT
                && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch(sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:

                if (mUsingAccel) {

                    float x = sensorEvent.values[0];
                    float y = sensorEvent.values[1];
                    float z = sensorEvent.values[2];
                    int r = Math.round(x) % 256;
                    int g = Math.round(y) % 256;
                    int b = Math.round(z) % 256;
                    accelValues.setText(String.format("Accel X: %.2f\tR: %d\nAccel Y: %.2f\tG: %d\nAccel Z: %.2f\tB: %d", x, r, y, g, z, b));
                    sendRGBToBoard(true, r, g, b);


                } else if (mUsingSteps) {
                    // pull raw values
                    _rawAccelValues[0] = sensorEvent.values[0];
                    _rawAccelValues[1] = sensorEvent.values[1];
                    _rawAccelValues[2] = sensorEvent.values[2];

                    smoothSignal();
                    float smoothMagnitudeValue = findMagnitude(_curAccelAvg[0], _curAccelAvg[1], _curAccelAvg[2]);
                    updateRunningMagnitudesValues(smoothMagnitudeValue);
                }

                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    // Send RGB values to Duo board (0 for give control back to physical controls, 1 for send color)
    // It has 5 bytes bytes: maker, data value red, data value green, data value blue, reserved
    private void sendRGBToBoard(boolean givingColor, int red, int green, int blue) {
        int firstVal; // need to do code cleanup later
        if (givingColor) {
            firstVal = 0x01;
        } else {
            firstVal = 0x00;
        }

        byte buf[] = new byte[] { (byte) firstVal, (byte) red, (byte) green, (byte) blue, (byte) 0x00 };
        mCharacteristicTx.setValue(buf);
        mBluetoothLeService.writeCharacteristic(mCharacteristicTx);
    }

    private void setControlsState(boolean controls, boolean manual, boolean accel, boolean step) {
        mControlsPickerBtn.setEnabled(controls);
        mColorPickerBtn.setEnabled(manual);
        mAccelPickerBtn.setEnabled(accel);
        mStepPickerBtn.setEnabled(step);

        /*
        if accel button is clicked
        while connection is present (i.e. some other button is enabled)
        then you should show accel values and send them over
        */
        if (!accel && (controls || manual)) {
            mUsingAccel = true;
            accelValues.setVisibility(View.VISIBLE);
        } else {
            mUsingAccel = false;
            accelValues.setVisibility(View.INVISIBLE);
        }

        /*
        if step button is clicked
        while connection is present (i.e. some other button is enabled)
        then you should show step values and send them over
        */
        if (!step && (controls || manual)) {
            mUsingSteps = true;
            accelValues.setVisibility(View.VISIBLE);
        } else {
            mUsingSteps = false;
            accelValues.setVisibility(View.INVISIBLE);
        }
    }

    private float findMagnitude(float x, float y, float z) {
        return (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
    }

    private void smoothSignal() {
        // Smoothing algorithm adapted from: https://www.arduino.cc/en/Tutorial/Smoothing
        for (int i = 0; i < 3; i++) {
            _runningAccelTotal[i] = _runningAccelTotal[i] - _accelValueHistory[i][_curReadIndex];
            _accelValueHistory[i][_curReadIndex] = _rawAccelValues[i];
            _runningAccelTotal[i] = _runningAccelTotal[i] + _accelValueHistory[i][_curReadIndex];
            _curAccelAvg[i] = _runningAccelTotal[i] / SMOOTHING_WINDOW_SIZE;
        }

        _curReadIndex++;
        if(_curReadIndex >= SMOOTHING_WINDOW_SIZE){
            _curReadIndex = 0;
        }
    }

    private void updateRunningMagnitudesValues(float recentMagnitudeValue) {
        _smoothMagnitudeValues[_currentChunkPosition] = recentMagnitudeValue;
        if (_currentChunkPosition == CHUNKING_SIZE - 1) {
            mladenovStepDetectionAlgorithm(_smoothMagnitudeValues);
            _currentChunkPosition = 0;
        } else {
            _currentChunkPosition++;
        }
    }

    private void mladenovStepDetectionAlgorithm(float magnitudes[]) {

        // Part 1: peak detection & setting threshold
        int peakCount = 0;
        float peakAccumulate = 0f;
        // loop safety variables (1 and CHUNKING_SIZE - 1) given +1 and -1 uses with indexes
        for (int i = 1; i < CHUNKING_SIZE - 1; i++) {
            float forwardSlope = magnitudes[i + 1] - magnitudes[i];
            float backwardSlope = magnitudes[i] - magnitudes[i - 1];
            if (forwardSlope < 0 && backwardSlope > 0) {
                peakCount += 1;
                peakAccumulate += magnitudes[i];
            }
        }
        float peakMean = peakAccumulate / peakCount;

        // Part 2: same peaks with thresholds applied
        int stepCount = 0;
        for (int i = 1; i < CHUNKING_SIZE - 1; i++) {
            //sendRGBToBoard(true, 0, 0, 0);
            float forwardSlope = magnitudes[i + 1] - magnitudes[i];
            float backwardSlope = magnitudes[i] - magnitudes[i - 1];
            if (forwardSlope < 0 && backwardSlope > 0
                    && magnitudes[i] > CONSTANT_C * peakMean ) {
                if ((_totalSteps <= EARLY_STEPS && magnitudes[i] > CONSTANT_K_early) ||
                        (_totalSteps > EARLY_STEPS && magnitudes[i] > CONSTANT_K )) {
                    stepCount += 1;
                    // send step to board
                    byte buf[] = new byte[] { (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
                    mCharacteristicTx.setValue(buf);
                    mBluetoothLeService.writeCharacteristic(mCharacteristicTx);
                }
            }
        }

        // update total steps (across chunks)
        _totalSteps += stepCount;
    }

}