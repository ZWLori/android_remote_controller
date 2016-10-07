package com.example.wanluzhang.mdp_17;

/**
 * Created by wanluzhang on 2/9/16.
 */


import android.content.Context;
import java.util.UUID;
import android.os.Handler;
import android.os.Bundle;
import android.os.Message;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class Bluetooth {
    private static final UUID UUID_MDP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = "Bluetooth : ";
    private final BluetoothAdapter mBA;
    private Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private int mState;

    public static final int STATE_IDLE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private static final String NAME_SECURE = "BluetoothChartSecure";
    private static final String NAME_INSECURE = "BluetoothChartInsecure";

    // constructor
    // @param context : the UI Activity Context
    // @param handler : a handler to send msg back to the UI Activity
    public Bluetooth(Context context, Handler handler) {
        mBA = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_IDLE;
        mHandler = handler;
    }

    public void setHandler(Handler handler){
        mHandler = handler;
    }

    // set state
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getState(){
        return mState;
    }

    // start the chat service
    // specifically start AcceptThread to begin a session in listening (server) mode
    // called by the Activity onResume()
    public synchronized void start(){
        // cancel any thread attempting to make a connection
        if(mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // cancel any thread currently running a connection
        if(mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

        // start the thread to listen on a BluetoothServerSocket
        if(mSecureAcceptThread == null){
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if(mInsecureAcceptThread == null){
            mInsecureAcceptThread = new AcceptThread(true);
            mInsecureAcceptThread.start();
        }
    }

    // start the ConnectThread to initiate a connection to a remote device
    // @param device : the BluetoothDevice to connect
    // @param secure : Socket Security type - Secure(true) Insecure(false)
    public synchronized void connect(BluetoothDevice device, boolean secure){
        // cancel any thread attempt to make a connection
        if(mState == STATE_CONNECTING){
            if(mConnectThread != null){
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // cancel any thread currently running a connection
        if(mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    // start the ConnectedThread to begin managing a Bluetooth connection
    // @param socket : the BluetoothSocket on which the connection was made
    // @param device : the BluetoothDevice that has been connected
    public  synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType){
        Log.d(TAG, "connected, Socket Type:" + socketType);
        // cancel the thread that completed the connection
        if(mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // cancel any thread currently running a connection
        if(mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // cancel the accept thread because we only want to connect to one device
        if(mSecureAcceptThread != null){
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if(mInsecureAcceptThread != null){
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    // stop all threads
    public synchronized void stop(){
        if(mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if(mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if(mSecureAcceptThread != null){
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if(mInsecureAcceptThread != null){
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        setState(STATE_IDLE);
    }

    // write to the ConnectedThread in an unsynchronized manner
    // @param out : the bytes to write
    // @see ConnectedThread#write(byte[])
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    // indicate that the connection attempt failed and notify the UI Activity
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        Bluetooth.this.start();
    }

    // indicate that the connection was lost and notify the UI Activity
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        Bluetooth.this.start();
    }

    // this thread runs while listening for incoming connections
    // it behaves like a server-side client
    // it runs until a connection is accepted (or until cancelled)
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure":"Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mBA.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            UUID_MDP);
                } else {
                    tmp = mBA.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, UUID_MDP);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (Bluetooth.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                            case STATE_IDLE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }

    // This thread uns while attempting to make an outgoing connection with a device.
    // It runs straight through; the connection either succeeds or fails.
    private class ConnectThread extends Thread {
        private final BluetoothSocket mSocket;
        private final BluetoothDevice mDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            UUID_MDP);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            UUID_MDP);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mBA.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (Bluetooth.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mSocket, mDevice, mSocketType);
        }

        public void cancel(){
            try {
                mSocket.close();
            } catch (IOException e){
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    // this thread runs during a connection with a remote device
    // it handles all incoming and outgoing transmissions
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mInStream = tmpIn;
            mOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    // Start the service over to restart listening mode
                    Bluetooth.this.start();
                    break;
                }
            }
        }

        // write to the connected OutStream
        // @param buffer : the bytes to write
        public void write(byte[] buffer) {
            try {
                mOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

}
