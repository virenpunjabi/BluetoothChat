package com.example.viren.bluetoothchat;

import android.bluetooth.BluetoothSocket;

import java.net.Socket;

/**
 * Created by viren on 4/7/2018.
 */

public class SocketHandler {

    private static BluetoothSocket socket;
    private static String clientAddress=null,serverName=null,clientName=null,serverAddress=null;

    public static synchronized void setSocket(BluetoothSocket socket){
        SocketHandler.socket = socket;
    }

    public static synchronized BluetoothSocket getSocket(){
        return socket;
    }

    public static synchronized void setClientName(String address){
        clientName=address;
    }

    public static synchronized String getClientName(){
        return clientName;
    }

    public static synchronized void setServerName(String name){
        serverName=name;
    }

    public static synchronized String getServerName(){
        return serverName;
    }

    public static synchronized void setClientAddress(String address){
        clientAddress=address;
    }
    public static synchronized String getClientAddress(){return clientAddress; }

    public static synchronized void setServerAddress(String address){
        serverAddress=address;
    }
    public static synchronized String getServerAddress(){return serverAddress;}
}
