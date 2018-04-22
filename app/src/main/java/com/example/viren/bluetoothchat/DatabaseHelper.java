package com.example.viren.bluetoothchat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by viren on 4/16/2018.
 */

public class DatabaseHelper extends SQLiteOpenHelper {


    public static final String DATABASE_NAME="BTChat.db";
    public static final String TABLE_NAME="chatbox";
    public static final String ID="ID";
    public static final String SENDER="SENDER";
    public static final String RECEIVER="RECEIVER";
    public static final String MESSAGE="MESSAGE";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        sqLiteDatabase.execSQL("CREATE TABLE "+TABLE_NAME+" (ID INTEGER PRIMARY KEY AUTOINCREMENT, SENDER TEXT, RECEIVER TEXT, MESSAGE TEXT )");

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        onCreate(sqLiteDatabase);

    }

    public boolean insertData(String sender,String receiver,String message){
        SQLiteDatabase db=this.getWritableDatabase();
        ContentValues contentValues=new ContentValues();
        contentValues.put("SENDER",sender);
        contentValues.put("RECEIVER",receiver);
        contentValues.put("MESSAGE",message);
        long result =db.insert(TABLE_NAME,null,contentValues);
        if(result==-1)
            return false;
        else
            return true;
    }

    public Cursor readData(String device1,String device2){
        SQLiteDatabase db=this.getWritableDatabase();
        String sql="SELECT * FROM "+TABLE_NAME+" WHERE (SENDER = '"+ device1+"' AND RECEIVER = '"+device2+"' ) OR (SENDER = '"+ device2+"' AND RECEIVER = '"+device1+"' )";
        Cursor cursor=db.rawQuery(sql,null);
        if(cursor.getCount()>0)
        return cursor;
        else
            return null;
    }

    public Cursor readAllData(){
        SQLiteDatabase db=this.getWritableDatabase();
        String sql="SELECT * FROM "+TABLE_NAME;
        Cursor cursor=db.rawQuery(sql,null);
        return cursor;
    }

}
