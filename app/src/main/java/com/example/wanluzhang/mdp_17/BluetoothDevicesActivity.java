package com.example.wanluzhang.mdp_17;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Set;


/**
 * Created by wanluzhang on 2/9/16.
 */
public class BluetoothDevicesActivity extends Activity{
    private BluetoothAdapter BA;
    private ArrayAdapter<String> pairedDevicesArrayAdapter;
    private ArrayAdapter<String> availDevicesArrayAdapter;
    public static String SELECTED_DEVICE_ADDR;
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    @Override
    protected void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_bluetooth_devices);

        setResult(Activity.RESULT_CANCELED);

        ToggleButton scan = (ToggleButton) findViewById(R.id.button_scan);
        Button cancel = (Button) findViewById(R.id.cancel_bluetooth);

        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                discover();
//                v.setVisibility(View.GONE);
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        pairedDevicesArrayAdapter = new ArrayAdapter<String>(this,
                R.layout.text);
        availDevicesArrayAdapter = new ArrayAdapter<String>(this,
                R.layout.text);

        // Find and set up the ListView for paired devices
        ListView pairedDevicesListView = (ListView) findViewById(R.id.paired_devices);
        pairedDevicesListView.setAdapter(pairedDevicesArrayAdapter);
        pairedDevicesListView.setOnItemClickListener(deviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView availDevicesListView = (ListView) findViewById(R.id.avail_devices);
        availDevicesListView.setAdapter(availDevicesArrayAdapter);
        availDevicesListView.setOnItemClickListener(deviceClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(receiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(receiver, filter);

        // Get the local Bluetooth adapter
        BA = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = BA.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n"
                        + device.getAddress());
            }
        } else {
            String noDevices = "No paired device";
            pairedDevicesArrayAdapter.add(noDevices);
        }
    }

    public void discover(){
        // indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle("Scanning for devices...");

        // turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // if already discovering, stop it
        if(BA.isDiscovering()){
            BA.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        BA.startDiscovery();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        // make sure not doing discovery anymore
        if(BA != null){
            BA.cancelDiscovery();
        }
        // unregister broadcast listener
        this.unregisterReceiver(receiver);
    }

    public AdapterView.OnItemClickListener deviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // cancle discovery since it's costly
            // time to connect
            BA.cancelDiscovery();
            Log.d("enter", "OnItemClickListener");
            // Get the text MAC address, which is the last 17 chars in the view
            String info = ((TextView) view).getText().toString();
            System.out.println(info);
            String address = info.substring(info.length() - 17);
            SELECTED_DEVICE_ADDR = address;

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device;

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    availDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                //else if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){
                //
                //}
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle("Select a device to connect");
                if (availDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = "Device not found";
                    availDevicesArrayAdapter.add(noDevices);
                }
            } else if (BluetoothDevice.ACTION_UUID.equals(action)){
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Parcelable[] uuidExtra = intent.getParcelableArrayExtra("android.bluetooth.device.extra.UUID");

                for (int i=0; i<uuidExtra.length; i++) {
                    String k = "\n  Device: " + device.getName() + ", " + device + ", Service: " + uuidExtra[i].toString();
                    Toast.makeText(getApplicationContext(), k, Toast.LENGTH_SHORT).show();

                }
            }
        }
    };


}
