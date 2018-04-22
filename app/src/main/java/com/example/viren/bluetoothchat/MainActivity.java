package com.example.viren.bluetoothchat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    public ArrayList<String> stringDiscoverableDevices = new ArrayList<>();
    public ArrayList<String> stringPairedDevices = new ArrayList<>();
    public ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();


    public static int REQUEST_ENABLE_BLUETOOTH = 5;

    Button listen, btn_createRoom;
    ListView listView;
    TextView msg_box, status;


    //SendReceive sendReceive;
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice[] btArray;
    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED = 5;
    static final int STATE_CONNECTION_RETRY = 6;
    ArrayAdapter<String> arrayAdapter;
    private static final String APP_NAME = "BluetoothChat";
    private int uuidCounter = 0;
    private int selectedDevicePosition;
    //private static final UUID MY_UUID=UUID.fromString("baf57a68-2bbb-11e8-b467-0ed5f89f718b");

    private static final UUID MY_UUID1 = UUID.fromString("77211018-3e8a-11e8-b467-0ed5f89f718b");
    private static final UUID MY_UUID2 = UUID.fromString("772112c0-3e8a-11e8-b467-0ed5f89f718b");
    private static final UUID MY_UUID3 = UUID.fromString("77211414-3e8a-11e8-b467-0ed5f89f718b");
    private static final UUID MY_UUID4 = UUID.fromString("7721154a-3e8a-11e8-b467-0ed5f89f718b");
    private static final UUID MY_UUID5 = UUID.fromString("77211892-3e8a-11e8-b467-0ed5f89f718b");
    private static final UUID MY_UUID6 = UUID.fromString("772119e6-3e8a-11e8-b467-0ed5f89f718b");
    private static final UUID MY_UUID7 = UUID.fromString("77211b12-3e8a-11e8-b467-0ed5f89f718b");
    private static final UUID MY_UUIDS[] = {MY_UUID1, MY_UUID2, MY_UUID3, MY_UUID4, MY_UUID5, MY_UUID6, MY_UUID7};

    ClientClass clientClass;
    ServerClass serverClass;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewByIdes();
//        Intent intent=getIntent();
//        if(intent!=null) {
//            String acton = intent.getStringExtra("ACTION");
//            if (acton.equals("RESTART")) {
//                handler.sendEmptyMessage(300);
//            }
//        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean pm = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        if (!pm) {
            Toast.makeText(this, "no bt found", Toast.LENGTH_SHORT).show();
            finish();

        } else {
            // Toast.makeText(this, "bt found", Toast.LENGTH_SHORT).show();

            if (!bluetoothAdapter.isEnabled()) {

                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
            } else {
                displayDevices();
            }


            btn_createRoom.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getApplicationContext(), RoomActivity.class);
                    startActivity(intent);
                }
            });
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        handler.sendEmptyMessage(300);
        clientClass = null;
        serverClass = null;
        selectedDevicePosition = -1;


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                displayDevices();
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
                Toast.makeText(this, "Failed to turn on Bluetooth", Toast.LENGTH_LONG).show();
            }
        }
    }//onActivityResult

    private void displayDevices() {

        Set<BluetoothDevice> bt = bluetoothAdapter.getBondedDevices();
        final String[] strings = new String[bt.size()];
        btArray = new BluetoothDevice[bt.size()];
        //bt.toArray(btArray);
        int index = 0;

        if (bt.size() > 0) {
            // Toast.makeText(this, "bt.size()>0", Toast.LENGTH_SHORT).show();
            for (BluetoothDevice device : bt) {
                btArray[index] = device;
                strings[index] = device.getName();
                index++;
            }
        }

        arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, strings);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                clientClass = new ClientClass(btArray[i]);
                selectedDevicePosition = i;
                SocketHandler.setServerName(strings[i]);
                clientClass.start();
                status.setText("Connecting");
            }
        });
    }

    public void btn_listen_clicked(View v) {

        serverClass = new ServerClass();
        serverClass.start();

    }


    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {

            switch (message.what) {
                case STATE_LISTENING:
                    status.setText("Listening");
                    break;

                case STATE_CONNECTING:
                    status.setText("Connecting");
                    break;

                case STATE_CONNECTED:
                    status.setText("Connected");
                    break;

                case STATE_CONNECTION_FAILED:
                    status.setText("Failed");
                    break;

                case STATE_CONNECTION_RETRY:
                    status.setText("Retrying");
                    uuidCounter++;
                    clientClass = new ClientClass(btArray[selectedDevicePosition]);
                    clientClass.start();
                    break;

                case STATE_MESSAGE_RECEIVED:
                    status.setText("Message Received");
                    byte[] readBuffer = (byte[]) message.obj;
                    String temp = new String(readBuffer, 0, message.arg1);
                    msg_box.setText(temp);
                    break;

                case 300:
                    status.setText("status");
                    break;

            }
            return true;
        }
    });


    public void findViewByIdes() {

        listView = findViewById(R.id.listView);
        listen = findViewById(R.id.listen);
        //   send=findViewById(R.id.send);
        btn_createRoom = findViewById(R.id.btn_createRoom);
        //  msg_box=findViewById(R.id.msg_box);
        status = findViewById(R.id.status);
        //writeMsg=findViewById(R.id.writeMsg);
    }


    public class ServerClass extends Thread {
        private BluetoothServerSocket serverSocket;

        public ServerClass() {
            try {

                //serverSocket=bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,MY_UUID);
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUIDS[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public void run() {
            BluetoothSocket socket = null;
            Message message = Message.obtain();
            ;

            while (socket == null) {
                try {

                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);
                    socket = serverSocket.accept();


                } catch (IOException e) {
                    //e.printStackTrace();
                    // Toast.makeText(MainActivity.this, "Exception "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);

                }
                if (socket != null) {
                    //                   message.what=STATE_CONNECTED;
//                    handler.sendMessage(message);

                    SocketHandler.setSocket(socket);
                    SocketHandler.setServerName(bluetoothAdapter.getName());
                    SocketHandler.setServerAddress(bluetoothAdapter.getAddress());

                    try {

                        handler.sendEmptyMessage(STATE_CONNECTED);

                    } catch (Exception e) {
                    }
                    Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                    startActivity(intent);
                    break;
                }
            }

            //run continued

        }
    }

    public class ClientClass extends Thread {
        private BluetoothDevice bluetoothDevice;
        private BluetoothSocket bluetoothSocket;

        public ClientClass(BluetoothDevice device) {
            bluetoothDevice = device;
            String name = bluetoothDevice.getName();
            SocketHandler.setServerName(name);
            String address = bluetoothDevice.getAddress();
            SocketHandler.setServerAddress(address);

        }

        public void run() {
            establish_connection();
        }

        public void establish_connection() {
            try {
                //i++;
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUIDS[uuidCounter]);
                bluetoothSocket.connect();
//                 Toast.makeText(MainActivity.this, "yolo it's connected", Toast.LENGTH_SHORT).show();

                String clientname = bluetoothAdapter.getName();
                SocketHandler.setClientName(clientname);
                String address = bluetoothAdapter.getAddress();
                SocketHandler.setClientAddress(address);

                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);

                SocketHandler.setSocket(bluetoothSocket);

                Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                startActivity(intent);


            } catch (Exception e) {
                e.printStackTrace();
                Message message = Message.obtain();
                if (uuidCounter > 7) {

                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);

                } else {
                    message.what = STATE_CONNECTION_RETRY;
                    handler.sendMessage(message);

                }

            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Toast.makeText(this, "MA:onStart", Toast.LENGTH_SHORT).show();
        clientClass = null;
        serverClass = null;
        selectedDevicePosition = -1;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Toast.makeText(this, "MA:onResume", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Toast.makeText(this, "MA:onPause", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Toast.makeText(this, "MA:onStop", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "MA:onDestroy", Toast.LENGTH_SHORT).show();
    }

}
