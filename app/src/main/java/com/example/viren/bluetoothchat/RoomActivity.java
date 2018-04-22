package com.example.viren.bluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class RoomActivity extends AppCompatActivity {

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice[] btArray;
    static final int STATE_LISTENING=1;
    static final int STATE_CONNECTING=2;
    static final int STATE_CONNECTED=3;
    static final int STATE_CONNECTION_FAILED=4;
    static final int STATE_MESSAGE_RECEIVED=5;
    static final int STATE_MESSAGE_SENT=6;
    static final int CLIENT=7;
    static final int MESSAGE=8;
    ArrayAdapter<String> arrayAdapter;
    private static final String APP_NAME="BluetoothChat";
    private static final UUID MY_UUID1=UUID.fromString("77211018-3e8a-11e8-b467-0ed5f89f718b");
    private static final UUID MY_UUID2=UUID.fromString("772112c0-3e8a-11e8-b467-0ed5f89f718b");
    private static final UUID MY_UUID3=UUID.fromString("77211414-3e8a-11e8-b467-0ed5f89f718b");
    private static final UUID MY_UUID4=UUID.fromString("7721154a-3e8a-11e8-b467-0ed5f89f718b");
    private static final UUID MY_UUID5=UUID.fromString("77211892-3e8a-11e8-b467-0ed5f89f718b");
    private static final UUID MY_UUID6=UUID.fromString("772119e6-3e8a-11e8-b467-0ed5f89f718b");
    private static final UUID MY_UUID7=UUID.fromString("77211b12-3e8a-11e8-b467-0ed5f89f718b");

    private static final UUID MY_UUIDS[] = {MY_UUID1,MY_UUID2,MY_UUID3,MY_UUID4,MY_UUID5,MY_UUID6,MY_UUID7};
    TextView tv_groupchat,status;
    ClientHandler clientHandler;
    SendReceive[] sendReceive_array=new SendReceive[7];
    int limit=0;
    EditText et_message;
    Button btn_send;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        tv_groupchat=findViewById(R.id.tv_groupchat);
        tv_groupchat.setMovementMethod(new ScrollingMovementMethod());
        et_message=findViewById(R.id.et_message);
        btn_send=findViewById(R.id.btn_send);
        status=findViewById(R.id.tv_status);
        clientHandler=new ClientHandler();
        clientHandler.start();

        btn_send.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                String message1="Server: ";
                String message2=et_message.getText().toString();
                String message=message1+message2;
                for(int m=0;m<limit;m++){
                    sendReceive_array[m].write(message.getBytes());
                }
                tv_groupchat.append("\n "+message+"\n");
              //  Toast.makeText(RoomActivity.this, "Done", Toast.LENGTH_SHORT).show();
                et_message.setText("");
            }
        });


//        ServerClass serverClass=new ServerClass(MY_UUID1);
//        serverClass.start();
    }

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {

            switch (message.what){
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
                    status.setText("Connection Failed");
                    break;

                case STATE_MESSAGE_RECEIVED:
                    status.setText("Message Received");
                    byte[] readBuffer=(byte[])message.obj;
                    String temp=new String(readBuffer);
                    tv_groupchat.append("\n "+temp+"\n");
                    for(int k=0;k<limit;k++){
                        sendReceive_array[k].write(readBuffer);
                    }
                    break;

                case CLIENT:
                    Toast.makeText(RoomActivity.this, ""+message.obj, Toast.LENGTH_SHORT).show();
                    break;

                case MESSAGE:
                    Toast.makeText(RoomActivity.this, ""+message.obj, Toast.LENGTH_SHORT).show();
                    break;

            }
            return true;
        }
    });

    public class ClientHandler extends Thread{

        int i=0;
        private BluetoothServerSocket serverSocket;
        public void run(){
            for(i=0;i<7;i++){
                BluetoothSocket socket=null;

                try {
                    serverSocket=bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,MY_UUIDS[i]);
                    socket=serverSocket.accept();
                    if(socket!=null){
                        sendReceive_array[i]=new SendReceive(socket);
                        sendReceive_array[i].start();
                        serverSocket.close();
                        limit++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }
    }

    public class SendReceive extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        String clientName;
        boolean flag=false;
        String finalmessage;

        public SendReceive(BluetoothSocket socket){
            bluetoothSocket=socket;
            InputStream tempIn=null;
            OutputStream tempOut=null;


            try {
                tempIn=bluetoothSocket.getInputStream();
                tempOut=bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream=tempIn;
            outputStream=tempOut;

        }

        public void run(){


            byte[] buffer=new byte[1024];
            int bytes;
            if(bluetoothSocket.isConnected()) {
                while (true) {
                    try {
                        bytes = inputStream.read(buffer);
                        if (bytes > 0) {
                            if (!flag) {

                                String tempclientName = new String(buffer,0,bytes);
                                int position=tempclientName.indexOf('&');
                                clientName=tempclientName.substring(0,position);
                                handler.obtainMessage(MESSAGE, 0, 0, clientName + " joined").sendToTarget();
                                flag = true;
                                this.write("noPersonal".getBytes());

                            } else {
                                //handler.obtainMessage(CLIENT, 0, 0, clientName).sendToTarget();

                                String message = new String(buffer, 0, bytes);

                                //handler.obtainMessage(MESSAGE, 0, 0, message).sendToTarget();

                                finalmessage = clientName + ": " + message;
                                //handler.obtainMessage(MESSAGE, 0, 0, finalmessage).sendToTarget();
                                byte[] buffer2 = finalmessage.getBytes();
                                handler.obtainMessage(STATE_MESSAGE_RECEIVED, 0, 0, buffer2).sendToTarget();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }else{
                handler.obtainMessage(MESSAGE, 0, 0, clientName + " left").sendToTarget();
                try {
                    inputStream.close();
                    outputStream.close();
                    bluetoothSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes){
            try {
                String test=new String(bytes);
                //handler.obtainMessage(MESSAGE, 0, 0, test).sendToTarget();
                if(!test.equals(finalmessage)) {
                    outputStream.write(bytes);
                }

                handler.sendEmptyMessage(STATE_MESSAGE_SENT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void writeName(byte[] bytes){
            try {

                outputStream.write(bytes);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
//    public class ServerClass extends Thread
//    {
//        private BluetoothServerSocket serverSocket;
//
//        public ServerClass(UUID MY_UUID){
//            try {
//                serverSocket=bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,MY_UUID);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//        }
//
//        public void run(){
//            BluetoothSocket socket=null;
//            Message message=Message.obtain();;
//            while(socket==null){
//                try {
//
//                    message.what=STATE_CONNECTING;
//                    handler.sendMessage(message);
//                    socket=serverSocket.accept();
//
//
//                } catch (IOException e) {
//                    message.what=STATE_CONNECTION_FAILED;
//                    handler.sendMessage(message);
//
//                }
//                if(socket != null){
//
//                    SocketHandler.setSocket(socket);
//                    try{
//
//                        handler.sendEmptyMessage(STATE_CONNECTED);
//
//                    }catch(Exception e){}
//                    Intent intent=new Intent(getApplicationContext(),ChatActivity.class);
//                    startActivity(intent);
//                    break;
//                }
//            }
//
//            //run continued
//
//        }
//    }

}
