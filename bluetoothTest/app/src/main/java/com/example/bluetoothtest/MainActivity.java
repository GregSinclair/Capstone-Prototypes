package com.example.bluetoothtest;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
//import android.support.*;
//annotation.Nullable;
//import android.support.v7.app.AppCompatActivity;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.bluetoothtest.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.UUID;



public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_ENABLE_BT=1;
    ListView lv_paired_devices;
    Set<BluetoothDevice> set_pairedDevices;
    ArrayAdapter adapter_paired_devices;
    BluetoothAdapter bluetoothAdapter;

    public static UUID MY_UUID;
    public static final int MESSAGE_READ=0;
    public static final int MESSAGE_WRITE=1;
    public static final int CONNECTING=2;
    public static final int CONNECTED=3;
    public static final int NO_SOCKET_FOUND=4;

    ConnectedThread mConnectedThread;

    String bluetooth_message="00";

    private static final String TAG = "MainActivity";


    @SuppressLint("HandlerLeak")
    Handler mHandler=new Handler()
    {
        @Override
        public void handleMessage(Message msg_type) {
            super.handleMessage(msg_type);

            switch (msg_type.what){
                case MESSAGE_READ:

                    //byte[] readbuf=(byte[])msg_type.obj;
                    String recieved = (String)msg_type.obj;
                    //String string_recieved=new String(readbuf);
                    Log.d(TAG, "message is: " + recieved);
                    //do some task based on recieved string
                    Log.d(TAG, "Handler: MESSAGE_READ");
                    break;
                case MESSAGE_WRITE:
                    /*
                    if(msg_type.obj!=null){
                        mConnectedThread=new ConnectedThread((BluetoothSocket)msg_type.obj);
                        mConnectedThread.write(bluetooth_message.getBytes());
                        Log.d(TAG, "message sent");

                    }
                    */
                    if(mConnectedThread!=null) {
                        mConnectedThread.write(bluetooth_message.getBytes());
                    }
                    Log.d(TAG, "Handler: MESSAGE_WRITE");
                    break;

                case CONNECTED:
                    Toast.makeText(getApplicationContext(),"Connected",Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Handler: CONNECTED");
                    break;

                case CONNECTING:
                    Toast.makeText(getApplicationContext(),"Connecting...",Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Handler: CONNECTING");
                    break;

                case NO_SOCKET_FOUND:
                    Toast.makeText(getApplicationContext(),"No socket found",Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Handler: SOCKET_NOT_FOUND");
                    break;
            }
        }
    };



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        String zeros = "000000";
        Random rnd = new Random();
        String s = Integer.toString(rnd.nextInt(0X1000000), 16);
        s = zeros.substring(s.length()) + s;
        MY_UUID = UUID.fromString( "12345601-0000-1000-8000-008051234567");
        //MY_UUID = UUID.fromString( s + "01-0000-1000-8000-008051234567");
        Log.d(TAG, "Handler: " + s + "01-0000-1000-8000-00805F9B34FB");
        Log.d(TAG, "Handler: ON_CREATE");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.multiplayer_bluetooth);
        initialize_layout();
        initialize_bluetooth();
        start_accepting_connection();
        initialize_clicks();

    }

    public void start_accepting_connection()
    {
        Log.d(TAG, "Handler: start_accepting_connection");
        //call this on button click as suited by you

        AcceptThread acceptThread = new AcceptThread();
        acceptThread.start();
        Toast.makeText(getApplicationContext(),"accepting",Toast.LENGTH_SHORT).show();
    }
    public void initialize_clicks()
    {
        Log.d(TAG, "Handler: initialize_clicks");
        lv_paired_devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Object[] objects = set_pairedDevices.toArray();
                BluetoothDevice device = (BluetoothDevice) objects[position];

                ConnectThread connectThread = new ConnectThread(device);
                connectThread.start();

                Toast.makeText(getApplicationContext(),"device choosen "+device.getName(),Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void initialize_layout()
    {
        Log.d(TAG, "Handler: initialize_layout");
        lv_paired_devices = (ListView)findViewById(R.id.lv_paired_devices);
        adapter_paired_devices = new ArrayAdapter(getApplicationContext(),R.layout.support_simple_spinner_dropdown_item);
        lv_paired_devices.setAdapter(adapter_paired_devices);
    }

    public void initialize_bluetooth()
    {
        Log.d(TAG, "Handler: initialize_bluetooth");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(getApplicationContext(),"Your Device doesn't support bluetooth. you can play as Single player",Toast.LENGTH_SHORT).show();
            finish();
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        else {
            set_pairedDevices = bluetoothAdapter.getBondedDevices();

            if (set_pairedDevices.size() > 0) {

                for (BluetoothDevice device : set_pairedDevices) {
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address

                    adapter_paired_devices.add(device.getName() + "\n" + device.getAddress());
                }
            }
        }
    }


    public class AcceptThread extends Thread
    {
        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {
            Log.d(TAG, "Accept Thread: OnCreate");
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("NAME",MY_UUID);
            } catch (IOException e) { Log.d(TAG, "Accept Thread: OnCreate Catch"); }
            serverSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Accept Thread: Run");
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    Log.d(TAG, "Accept Thread: Run Loop Try1");
                    socket = serverSocket.accept();
                    Log.d(TAG, "Accept Thread: Run Loop Try2");
                } catch (IOException e) {
                    Log.d(TAG, "Accept Thread: Run Loop Break");
                    break;
                }

                // If a connection was accepted
                if (socket != null)
                {
                    // Do work to manage the connection (in a separate thread)
                    Log.d(TAG, "onReceive: STATE OFF");
                    mHandler.obtainMessage(CONNECTED).sendToTarget();
                    connected(socket);
                    mHandler.obtainMessage(MESSAGE_WRITE,socket).sendToTarget();
                    socket=null;
                }
            }
        }
    }


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            Log.d(TAG, "Connect Thread: OnCreate");
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Connect Thread: Run");
            // Cancel discovery because it will slow down the connection
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mHandler.obtainMessage(CONNECTING).sendToTarget();

                mmSocket.connect();
                if(mmSocket != null){
                    //connected(mmSocket);
                    Log.d(TAG, "Connect Thread: Connected Start");
                    // Do work to manage the connection (in a separate thread)
                    bluetooth_message = "Initial message*and the second line*";
                    connected(mmSocket);
                    mHandler.obtainMessage(MESSAGE_WRITE,mmSocket).sendToTarget();

                }
                else{Log.d(TAG, "Connect Thread: Connected Socket Null");}
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                Log.d(TAG, "Connect Thread: Cannot Start");
                return;
            }





        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
    private class ConnectedThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "Connected Thread: Connected Start");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                Log.d(TAG, "Connected Thread: Initialization Success");
            } catch (IOException e) {
                Log.d(TAG, "Connected Thread: Initialization Failure");

            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.d(TAG, "Connected Thread: Run");
            byte[] buffer = new byte[1];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs

            String message="";

            while (true) {
                Log.d(TAG, "Connected Thread: Looping");
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    String oneChar= new String(buffer);
                    Log.d(TAG, "Connected Thread: got character: "+oneChar);
                    if(oneChar.contains("*")){
                        Log.d(TAG, "Connected Thread: Sending full message");
                        mHandler.obtainMessage(MESSAGE_READ, message).sendToTarget();
                        message="";
                    }
                    else{
                        message+=oneChar;
                    }
                    // Send the obtained bytes to the UI activity
                    //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();

                    Log.d(TAG, "Connected Thread: Sent Read");
                } catch (IOException e) {
                    Log.d(TAG, "Connected Thread: Loop Escape");
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            Log.d(TAG, "Connect Thread: Write");
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { Log.d(TAG, "Connected Thread: Write Error"); }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private void connected(BluetoothSocket mmSocket) {
        Log.d(TAG, "Connect Thread: NA");

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

}