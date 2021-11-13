package com.sulaimanvali.robotarmtest;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.HashMap;
import java.util.Iterator;


public class ControlActivity extends ActionBarActivity {

    private static final int VENDOR_ID  = 4711; // i.e. 0x1267
    private static final int PRODUCT_ID = 0;

    // byte 0
    private static final byte STOP_GRIP_WRIST_ELBOW_SHOULDER = 0x00;
    private static final byte GRIP_CLOSE         = 0x01;
    private static final byte GRIP_OPEN          = 0x02;
    private static final byte WRIST_UP           = 0x04;
    private static final byte WRIST_DOWN         = 0x08;
    private static final byte ELBOW_UP           = 0x10;
    private static final byte ELBOW_DOWN         = 0x20;
    private static final byte STEM_BACKWARD      = 0x40;
    private static final byte STEM_FORWARD       = (byte)0x80;
    // byte 1
    private static final byte BASE_STOP          = 0x00;
    private static final byte BASE_CLOCKWISE     = 0x01;
    private static final byte BASE_ANTICLOCKWISE = 0x02;
    // byte 2
    private static final byte LED_OFF            = 0x00;
    private static final byte LED_ON             = 0x01;

    private UsbDevice device;
    private UsbManager manager;
    private UsbDeviceConnection usbConn;
    private boolean ledOn;
    private static final String ACTION_USB_PERMISSION = "com.sulaimanvali.robotarmtest.USB_PERMISSION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        Button gripClose = (Button) findViewById(R.id.button_gripClose);
        Button gripOpen  = (Button) findViewById(R.id.button_gripOpen);
        Button wristUp   = (Button) findViewById(R.id.button_wristUp);
        Button wristDown = (Button) findViewById(R.id.button_wristDown);
        Button elbowUp   = (Button) findViewById(R.id.button_elbowUp);
        Button elbowDown = (Button) findViewById(R.id.button_elbowDown);
        Button stemBackward = (Button) findViewById(R.id.button_stemBackward);
        Button stemForward  = (Button) findViewById(R.id.button_stemForward);
        Button baseClockwise     = (Button) findViewById(R.id.button_baseClockwise);
        Button baseAnticlockwise = (Button) findViewById(R.id.button_baseAnticlockwise);

        View.OnTouchListener buttonListener = new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if(event.getAction() == MotionEvent.ACTION_DOWN)
                    sendButtonCommand((Button) v, true); //button press started
                else if(event.getAction() == MotionEvent.ACTION_UP)
                    sendButtonCommand((Button) v, false); //finger was lifted

                return true;
            }
        };
        gripClose.setOnTouchListener(buttonListener);
        gripOpen.setOnTouchListener(buttonListener);
        wristUp.setOnTouchListener(buttonListener);
        wristDown.setOnTouchListener(buttonListener);
        elbowUp.setOnTouchListener(buttonListener);
        elbowDown.setOnTouchListener(buttonListener);
        stemBackward.setOnTouchListener(buttonListener);
        stemForward.setOnTouchListener(buttonListener);
        baseClockwise.setOnTouchListener(buttonListener);
        baseAnticlockwise.setOnTouchListener(buttonListener);

        openUsbDevice();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        closeUsbDevice(device);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        openUsbDevice();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        closeUsbDevice(device);
    }

    private void openUsbDevice() {
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);

        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);

        HashMap<String , UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        while (deviceIterator.hasNext()) {
            device = deviceIterator.next();
            if (device.getVendorId() == VENDOR_ID && device.getProductId() == PRODUCT_ID)
            {
                manager.requestPermission(device, mPermissionIntent);
                String info = "\n" + "DeviceID: " + device.getDeviceId() + "\n"
                        + "DeviceName: " + device.getDeviceName() + "\n"
                        + "DeviceClass: " + device.getDeviceClass() + " - "
                        + "DeviceSubClass: " + device.getDeviceSubclass() + "\n"
                        + "VendorID: " + device.getVendorId() + "\n"
                        + "ProductID: " + device.getProductId() + "\n";
                Log.i(getLocalClassName(), info);
                break; // our USB device found, so break out
            }
        }
    }

    private void closeUsbDevice(UsbDevice dev)
    {
        if (usbConn != null) {
            usbConn.releaseInterface(dev.getInterface(0));
            usbConn.close();
            usbConn = null;
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice dev = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (dev != null) {
                        if (intent.getBooleanExtra(
                                UsbManager.EXTRA_PERMISSION_GRANTED, true)) {
                            usbConn = manager.openDevice(dev);
                            device = dev;
                        } else {
                            Log.d("ERROR", "permission denied for device " + dev);
                        }
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice dev = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (dev != null) {
                    // call your method that cleans up and closes communication with the device
                    Toast.makeText(context, "USB device disconnected", Toast.LENGTH_SHORT).show();
                    closeUsbDevice(dev);
                }
            }
        }
    };

    public void onLedPressed(View view)
    {
        ToggleButton ledTog = (ToggleButton) view;
        if (usbConn == null) {
            ledTog.setChecked(false);
            ledOn = false;
            Toast.makeText(this, "Robot Arm not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        ledOn = ledTog.isChecked();
        sendCommand((byte)0, (byte)0, (ledOn ? LED_ON : LED_OFF));
    }

    private void sendButtonCommand(Button button, boolean pressed)
    {
        if (usbConn == null) {
            if (pressed) // only show this annoying popup upon button down
                Toast.makeText(this, "Robot Arm not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        byte byte0 = 0;
        byte byte1 = 0;
        byte byte2 = ledOn ? LED_ON : LED_OFF;

        // set bytes 0 & 1
        switch (button.getId())
        {
            // byte 0
            case R.id.button_gripClose: byte0 = pressed ? GRIP_CLOSE : STOP_GRIP_WRIST_ELBOW_SHOULDER; break;
            case R.id.button_gripOpen:  byte0 = pressed ? GRIP_OPEN : STOP_GRIP_WRIST_ELBOW_SHOULDER; break;
            case R.id.button_wristUp:   byte0 = pressed ? WRIST_UP : STOP_GRIP_WRIST_ELBOW_SHOULDER; break;
            case R.id.button_wristDown: byte0 = pressed ? WRIST_DOWN : STOP_GRIP_WRIST_ELBOW_SHOULDER; break;
            case R.id.button_elbowUp:   byte0 = pressed ? ELBOW_UP : STOP_GRIP_WRIST_ELBOW_SHOULDER; break;
            case R.id.button_elbowDown: byte0 = pressed ? ELBOW_DOWN : STOP_GRIP_WRIST_ELBOW_SHOULDER; break;
            case R.id.button_stemBackward: byte0 = pressed ? STEM_BACKWARD : STOP_GRIP_WRIST_ELBOW_SHOULDER; break;
            case R.id.button_stemForward:  byte0 = pressed ? STEM_FORWARD : STOP_GRIP_WRIST_ELBOW_SHOULDER; break;
            // byte 1
            case R.id.button_baseClockwise:     byte1 = pressed ? BASE_CLOCKWISE : BASE_STOP; break;
            case R.id.button_baseAnticlockwise: byte1 = pressed ? BASE_ANTICLOCKWISE : BASE_STOP; break;

            default: byte0 = STOP_GRIP_WRIST_ELBOW_SHOULDER; byte1 = BASE_STOP; break;
        }

        sendCommand(byte0, byte1, byte2);
    }

    private void sendCommand(byte byte0, byte byte1, byte byte2) {
        byte[] cmdBuf = new byte[]{byte0, byte1, byte2};

        //Toast.makeText(this, String.format("Command - %x %x %x", byte0, byte1, byte2), Toast.LENGTH_SHORT).show();

        usbConn.controlTransfer(
                UsbConstants.USB_TYPE_VENDOR, //0x40 	bmRequestType,
                6, //uint8_t 	bRequest,
                0x100, //uint16_t 	wValue,
                0,//uint16_t 	wIndex,
                cmdBuf,
                cmdBuf.length,
                100); //do in another thread
    }
}
