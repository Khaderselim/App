package com.example.chat;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
        contactsRVAdapter.setOnLongClickListener(v -> {
            int position = contactsRV.getChildAdapterPosition(v);
            showPopupMenu(position);
            return true;
        });

        new Thread(new ConnectionThread()).start();
    }
    private void showPopupMenu(int position) {
        PopupMenu popupMenu = new PopupMenu(this, contactsRV.findViewHolderForAdapterPosition(position).itemView);
        popupMenu.getMenuInflater().inflate(R.menu.contact_options_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.delete_contact) {
                ContactsModal selectedContact = contactsModalArrayList.get(position);
                contactsModalArrayList.remove(position);
                dbManager.deleteContact(selectedContact.getUsername());
                contactsRVAdapter.notifyDataSetChanged();
                return true;
            } else if(item.getItemId() == R.id.modify_contact) {
                showEditContactDialog(position, contactsModalArrayList.get(position));
                return true;
            }else {
                return false;
            }
        });
        popupMenu.show();
    }
    private void showEditContactDialog(int position, ContactsModal contact) {
        EditText editText = new EditText(this);
        editText.setText(contact.getUsername());
        new AlertDialog.Builder(this)
                .setTitle("Edit Contact")
                .setView(editText)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newUsername = editText.getText().toString();
                    if (!newUsername.isEmpty()) {
                        editContactValid(newUsername, isValid -> {
                                if (isValid && !newUsername.equals(contact.getUsername())) {
                                    dbManager.updateContact(contact.getUsername(), newUsername);
                                    contact.setUsername(newUsername);
                                    contactsRVAdapter.notifyItemChanged(position);
                                } else {
                                    Toast.makeText(Contact.this, "Failed to find username", Toast.LENGTH_SHORT).show();
                                }
                            });

                    } else {
                        Toast.makeText(Contact.this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadContacts() {
        Cursor contactsCursor = dbManager.getContacts(userId);
        while (contactsCursor.moveToNext()) {
            String contactName = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow("contact_name"));
            contactsModalArrayList.add(new ContactsModal(contactName, "Unknown"));
        }
        contactsRVAdapter.notifyDataSetChanged();
    }
private void editContactValid(String newUsername, ValidationCallback callback) {
    new Thread(() -> {
        if (isConnected()) {
            printwriter.println("***ADD***");
            printwriter.println(newUsername);
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(SocketManager.getInstance().getSocket().getInputStream()));
                String serverResponse = in.readLine();
                if (serverResponse == null || serverResponse.equals("Error") || newUsername.equals(username) || !isUsernameValid(newUsername)) {
                    runOnUiThread(() -> {
                        Toast.makeText(Contact.this, "Failed to find username", Toast.LENGTH_SHORT).show();
                        callback.onValidationResult(false);
                    });
                } else {
                    runOnUiThread(() -> callback.onValidationResult(true));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            runOnUiThread(() -> {
                Toast.makeText(Contact.this, "Not connected to server", Toast.LENGTH_SHORT).show();
                callback.onValidationResult(false);
            });
        }
    }).start();
}

public interface ValidationCallback {
    void onValidationResult(boolean isValid);
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