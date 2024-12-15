package com.example.chat;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class Contact extends AppCompatActivity {
    private PrintWriter printwriter;
    private String username;
    private RecyclerView contactsRV;
    private ContactsRVAdapter contactsRVAdapter;
    private ArrayList<ContactsModal> contactsModalArrayList;
    private EditText usernameET;
    private String serverip;
    private DatabaseManager dbManager;
    private long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        dbManager = new DatabaseManager(this);

        contactsRV = findViewById(R.id.idRVContacts);
        contactsRV.setLayoutManager(new LinearLayoutManager(this));
        contactsModalArrayList = new ArrayList<>();
        contactsRVAdapter = new ContactsRVAdapter(contactsModalArrayList);
        contactsRV.setAdapter(contactsRVAdapter);

        usernameET = findViewById(R.id.idETUsername);
        findViewById(R.id.idFABadd).setOnClickListener(v -> {
            String newUsername = usernameET.getText().toString();
            if (!newUsername.isEmpty()) {
                sendUsernameToServer(newUsername);
            } else {
                Toast.makeText(Contact.this, "Please enter a username", Toast.LENGTH_SHORT).show();
            }
        });

        Intent intent = getIntent();
        username = intent.getStringExtra("username");
        serverip = intent.getStringExtra("serverip");

        Cursor userCursor = dbManager.getUser(username);
        if (userCursor.moveToFirst()) {
            userId = userCursor.getLong(userCursor.getColumnIndexOrThrow("id"));
        }

        loadContacts();

        contactsRVAdapter.setOnItemClickListener(position -> {
            ContactsModal selectedContact = contactsModalArrayList.get(position);
            checkUserOnlineStatus(selectedContact.getUsername());
        });

        new Thread(new ConnectionThread()).start();
    }

    private void loadContacts() {
        Cursor contactsCursor = dbManager.getContacts(userId);
        while (contactsCursor.moveToNext()) {
            String contactName = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow("contact_name"));
            contactsModalArrayList.add(new ContactsModal(contactName, "Unknown"));
        }
        contactsRVAdapter.notifyDataSetChanged();
    }

    private void sendUsernameToServer(String newUsername) {
        new Thread(() -> {
            if (isConnected()) {
                printwriter.println("***ADD***");
                printwriter.println(newUsername);
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(SocketManager.getInstance().getSocket().getInputStream()));
                    String serverResponse = in.readLine();
                    if (serverResponse == null || serverResponse.equals("Error") || newUsername.equals(username) || !isUsernameValid(newUsername)) {
                        runOnUiThread(() -> Toast.makeText(Contact.this, "Failed to find username", Toast.LENGTH_SHORT).show());
                    } else {
                        long contactId = dbManager.addContact(userId, newUsername);
                        if (contactId != -1) {
                            runOnUiThread(() -> {
                                contactsModalArrayList.add(new ContactsModal(newUsername, "Online"));
                                contactsRVAdapter.notifyDataSetChanged();
                                usernameET.setText("");
                                Toast.makeText(Contact.this, "Username sent to server", Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(Contact.this, "Contact already exists", Toast.LENGTH_SHORT).show());
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                runOnUiThread(() -> Toast.makeText(Contact.this, "Not connected to server", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void checkUserOnlineStatus(String contactUsername) {
        new Thread(() -> {
            if (isConnected()) {
                printwriter.println("***PRIVATE***");
                printwriter.println(contactUsername);
                printwriter.flush();
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(SocketManager.getInstance().getSocket().getInputStream()));
                    String serverResponse = in.readLine();
                    if (serverResponse == null || serverResponse.equals("OFFLINE")) {
                        runOnUiThread(() -> Toast.makeText(Contact.this, "User is offline", Toast.LENGTH_SHORT).show());
                    } else if (serverResponse.equals("ONLINE")) {
                        runOnUiThread(() -> {
                            Intent privateMessageIntent = new Intent(Contact.this, PrivateMessageActivity.class);
                            privateMessageIntent.putExtra("contactName", contactUsername);
                            privateMessageIntent.putExtra("username", username);
                            startActivity(privateMessageIntent);
                        });
                    }
                } catch (IOException e) {
                    runOnUiThread(() -> Toast.makeText(Contact.this, "Error communicating with server", Toast.LENGTH_SHORT).show());
                    e.printStackTrace();
                }
            } else {
                runOnUiThread(() -> Toast.makeText(Contact.this, "Not connected to server", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private boolean isUsernameValid(String newUsername) {
        for (ContactsModal contactsModal : contactsModalArrayList) {
            if (contactsModal.getUsername().equals(newUsername)) {
                return false;
            }
        }
        return true;
    }

    private boolean isConnected() {
        return SocketManager.getInstance().getSocket() != null && SocketManager.getInstance().getSocket().isConnected() && !SocketManager.getInstance().getSocket().isClosed();
    }

    class ConnectionThread implements Runnable {
        @Override
        public void run() {
            try {
                Socket client = new Socket(serverip, 1234);
                SocketManager.getInstance().setSocket(client);
                printwriter = new PrintWriter(client.getOutputStream(), true);

                if (username != null) {
                    printwriter.println(username);
                }

                runOnUiThread(() -> {
                    if (isConnected()) {
                        Toast.makeText(Contact.this, "Connected to server", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Contact.this, "Failed to connect to server", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(Contact.this, "Failed to connect to server", Toast.LENGTH_SHORT).show());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}