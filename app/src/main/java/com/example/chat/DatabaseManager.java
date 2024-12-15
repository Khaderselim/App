package com.example.chat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseManager {
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public DatabaseManager(Context context) {
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
    }

    public long addUser(String username, String password) {
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", password);
        return database.insertWithOnConflict("user", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public long addContact(long userId, String contactName) {
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("contact_name", contactName);
        return database.insert("contact", null, values);
    }

    public Cursor getUser(String username) {
        return database.query("user", null, "username=?", new String[]{username}, null, null, null);
    }

    public Cursor getContacts(long userId) {
        return database.query("contact", null, "user_id=?", new String[]{String.valueOf(userId)}, null, null, null);
    }

    public void close() {
        dbHelper.close();
    }
}
