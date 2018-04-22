package com.example.viren.bluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
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

public class ChatActivity extends AppCompatActivity {

    EditText et_message;
    TextView tv_messages,status;
    Button btn_send,viewdata;
    private BluetoothSocket btSocket;
    static final int STATE_LISTENING=1;
    static final int STATE_CONNECTING=2;
    static final int STATE_CONNECTED=3;
    static final int STATE_CONNECTION_FAILED=4;
    static final int STATE_MESSAGE_RECEIVED=5;
    static final int STATE_MESSAGE_SENT=6;
    //ArrayAdapter<String> arrayAdapter;
    boolean flag;
    boolean showchatflag;
    boolean performDbOp;
    String firstMessage;
    String clientName,clientAddress,serverName,serverAddress;
    private String deviceRole;
    DatabaseHelper databaseHelper;
    public SendReceive sendReceive;
    boolean runThread;


    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toast.makeText(this, "onCreate", Toast.LENGTH_SHORT).show();

        flag=false;
        showchatflag=false;
        performDbOp=false;
        sendReceive=null;
        runThread=true;
        deviceRole="server";


        viewdata=findViewById(R.id.viewdata);
        et_message=findViewById(R.id.et_message);
        tv_messages=findViewById(R.id.tv_messages);
        tv_messages.setMovementMethod(new ScrollingMovementMethod());
        btn_send=findViewById(R.id.btn_send);
        status=findViewById(R.id.status);
        databaseHelper=new DatabaseHelper(getApplicationContext());
        btSocket=SocketHandler.getSocket();
        sendReceive=new SendReceive(btSocket);
        sendReceive.start();
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String string= String.valueOf(et_message.getText());
                sendReceive.write(string.getBytes());
                if(performDbOp){
                    storeChat("s",string);
                }

                String newstring="\n You: "+string;
                tv_messages.append(newstring);
                et_message.setText("");

            }
        });


        viewdata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Cursor cursor=databaseHelper.readAllData();
                if(cursor.getCount() == 0){
                    showMessage("Error","Nothing found");
                    return;
                }else{

                    StringBuffer stringBuffer=new StringBuffer();

                    while(cursor.moveToNext()){

                        stringBuffer.append("Id: "+cursor.getString(0)+"\n");
                        stringBuffer.append("Name: "+cursor.getString(1)+"\n");
                        stringBuffer.append("Lastname: "+cursor.getString(2)+"\n");
                        stringBuffer.append("Marks: "+cursor.getString(3)+"\n\n");
                    }
                    showMessage("data",stringBuffer.toString());
                }
            }
        });

    }


    public void showMessage(String title, String message){
        AlertDialog.Builder alert=new AlertDialog.Builder(this);
        alert.setCancelable(true);
        alert.setTitle(title);
        alert.setMessage(message);
        alert.show();
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

                case STATE_MESSAGE_SENT:
                    status.setText("Message Sent");

                    break;

                case STATE_MESSAGE_RECEIVED:
                    status.setText("Message Received");
                    byte[] readBuffer=(byte[])message.obj;
                    String temp=new String(readBuffer,0,message.arg1);
                    if(temp.equals("eexitToMainActivity")){
                        Toast.makeText(ChatActivity.this, "exxit", Toast.LENGTH_SHORT).show();
                        runThread=false;
                       checkexit();

                    }else {
                        tv_messages.append("\n " + temp);
                        Toast.makeText(ChatActivity.this, ""+performDbOp, Toast.LENGTH_SHORT).show();
                        if (performDbOp){
                            storeChat("r", temp);
                        }
                    }
                    break;

                case 100:
                   // status.setText("Message Received");
                    byte[] readNewBuffer=(byte[])message.obj;
                    String trytemp=new String(readNewBuffer,0,message.arg1);
                    int position=trytemp.indexOf('&');
                    clientName=trytemp.substring(0,position);
                    clientAddress=trytemp.substring(position+1);
                    setTitle(clientName);
                    displayChat();
                    Toast.makeText(ChatActivity.this, "Client Address: "+clientAddress, Toast.LENGTH_SHORT).show();
                    break;

                case 200:

                    displayChat();
                    break;




            }
            return true;
        }
    });

    public void checkexit(){
        try {
            inputStream.close();
            outputStream.close();
            bluetoothSocket.close();
            //sendReceive.stop();
            SocketHandler.setServerName(null);
            SocketHandler.setClientName(null);
            SocketHandler.setClientAddress(null);
            SocketHandler.setServerAddress(null);
            SocketHandler.setSocket(null);
            Intent intent=new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intent);
            return;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void storeChat(String direction,String message){

        String sender,receiver;
        if(direction.equals("s")){

            if(deviceRole.equals("server")){
                sender=serverAddress;
                receiver=clientAddress;
            }else{
                sender=clientAddress;
                receiver=serverAddress;
            }

        }else{

            if(deviceRole.equals("server")){
                sender=clientAddress;
                receiver=serverAddress;
            }else{
                sender=serverAddress;
                receiver=clientAddress;
            }
        }

        boolean result=databaseHelper.insertData(sender,receiver,message);
        if(!result){
            Toast.makeText(this, "Error while inserting message", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "Message inserted", Toast.LENGTH_SHORT).show();
        }

    }


    public class SendReceive extends Thread{



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

            clientName=SocketHandler.getClientName();

            //this is client
            if(clientName!=null && !flag){

                clientAddress=SocketHandler.getClientAddress();
                firstMessage=clientName+"&"+clientAddress;
                byte[] byteAddress=new byte[1024];
                byteAddress=firstMessage.getBytes();
                this.writeName(byteAddress);

                serverName=SocketHandler.getServerName();
                serverAddress=SocketHandler.getServerAddress();
                setTitle(serverName);

                deviceRole="client";
            }else{
                this.writeName("PPersonalChat".getBytes());
                deviceRole="server";
                serverName=SocketHandler.getServerName();
                serverAddress=SocketHandler.getServerAddress();
            }


        }

        public void run(){

            byte[] buffer=new byte[1024];
            int bytes=0;

            try {

                while(runThread){

                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        if (!flag && deviceRole.equals("server")) {

                            performDbOp = true;
                            if (!showchatflag) {
                                //displayChat();
                                showchatflag = true;
                            }

                            handler.obtainMessage(100, bytes, -1, buffer).sendToTarget();
                            flag = true;
                        } else if (!flag && deviceRole.equals("client")) {
                            String message = new String(buffer, 0, bytes);
                            if (message.equals("PPersonalChat")) {
                                performDbOp = true;
                                if (!showchatflag) {
                                    //displayChat();
                                    showchatflag = true;
                                    handler.sendEmptyMessage(200);
                                }

                            } else {
                                performDbOp = false;
                                //String newmessage=new String(buffer,0,bytes);
                                //handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();
                            }
                            //handler.obtainMessage(200,bytes,-1,buffer).sendToTarget();
                            flag = true;
                        } else {
                            handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();
                        }
                    }
                }
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {

            }//end of finally


        }//end of run

        public void write(byte[] bytes){
            try {
                outputStream.write(bytes);
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


    public void displayChat(){

        //Toast.makeText(this, "displaychat called", Toast.LENGTH_SHORT).show();
        Cursor cursor=databaseHelper.readData(serverAddress,clientAddress);
        //Toast.makeText(this, "s: "+serverAddress+"c: "+clientAddress, Toast.LENGTH_SHORT).show();
        if(cursor!=null) {
            //Toast.makeText(this, "if called", Toast.LENGTH_SHORT).show();
            StringBuffer chatmessages = new StringBuffer();
            while (cursor.moveToNext()) {

                String from = cursor.getString(1);
                String to = cursor.getString(2);
                String message = cursor.getString(3);
                if (deviceRole.equals("server")) {
                    if (serverAddress.equals(from)) {
                        chatmessages.append("You: " + message + " \n");
                    } else {
                        chatmessages.append(message + "\n");
                    }
                   }
                   else{
                    if (clientAddress.equals(from)) {
                        chatmessages.append("You: " + message + " \n");
                    } else {
                        chatmessages.append(message + " \n");
                    }
                }
            }
            tv_messages.setText(chatmessages.toString());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Toast.makeText(this, "onStart", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Toast.makeText(this, "onResume", Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "Role: "+deviceRole, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Toast.makeText(this, "onRestart", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "onDestroy", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Toast.makeText(this, "onStop", Toast.LENGTH_SHORT).show();
        runThread=false;
        try {
            String text="eexitToMainActivity";
            sendReceive.write(text.getBytes());
            SocketHandler.setServerName(null);
            SocketHandler.setClientName(null);
            SocketHandler.setClientAddress(null);
            SocketHandler.setServerAddress(null);
            SocketHandler.setSocket(null);
//            inputStream.close();
//            outputStream.close();
//            bluetoothSocket.close();
            //sendReceive.stop();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Toast.makeText(this, "onPause", Toast.LENGTH_SHORT).show();
    }


}
