package com.example.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class Contact extends AppCompatActivity {
    private PrintWriter printwriter;
    private String username;
    private RecyclerView contactsRV;
    private ContactsRVAdapter contactsRVAdapter;
    private ArrayList<ContactsModal> contactsModalArrayList;
    private EditText usernameET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        contactsRV = findViewById(R.id.idRVContacts);
        contactsRV.setLayoutManager(new LinearLayoutManager(this));
        contactsModalArrayList = new ArrayList<>();
        contactsRVAdapter = new ContactsRVAdapter(contactsModalArrayList);
        contactsRV.setAdapter(contactsRVAdapter);

        usernameET = findViewById(R.id.idETUsername);
        findViewById(R.id.idFABadd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newUsername = usernameET.getText().toString();
                if (!newUsername.isEmpty()) {
                    sendUsernameToServer(newUsername);
                } else {
                    Toast.makeText(Contact.this, "Please enter a username", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Intent intent = getIntent();
        username = intent.getStringExtra("username");

        contactsRVAdapter.setOnItemClickListener(new ContactsRVAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ContactsModal selectedContact = contactsModalArrayList.get(position);
                Intent privateMessageIntent = new Intent(Contact.this, PrivateMessageActivity.class);
                privateMessageIntent.putExtra("contactName", selectedContact.getUsername());
                privateMessageIntent.putExtra("username", username);
                startActivity(privateMessageIntent);
            }
        });

        new Thread(new ConnectionThread()).start();
    }

    private Boolean isUsernameValid(String newUsername) {
        for (ContactsModal contactsModal : contactsModalArrayList) {
            if (contactsModal.getUsername().equals(newUsername)) {
                return false;
            }
        }
        return true;
    }

    private void sendUsernameToServer(String newUsername) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isConnected()) {
                    printwriter.println("***ADD***");
                    printwriter.println(newUsername);
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(SocketManager.getInstance().getSocket().getInputStream()));
                        String serverResponse = in.readLine();
                        if (serverResponse == null || serverResponse.equals("Error") || newUsername.equals(username) || !isUsernameValid(newUsername)) {
                            runOnUiThread(() -> Toast.makeText(Contact.this, "Failed to find username", Toast.LENGTH_SHORT).show());
                        } else {
                            runOnUiThread(() -> {
                                contactsModalArrayList.add(new ContactsModal(newUsername, "Online"));
                                contactsRVAdapter.notifyDataSetChanged();
                                usernameET.setText("");
                                Toast.makeText(Contact.this, "Username sent to server", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(Contact.this, "Not connected to server", Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    private boolean isConnected() {
        return SocketManager.getInstance().getSocket() != null && SocketManager.getInstance().getSocket().isConnected() && !SocketManager.getInstance().getSocket().isClosed();
    }

    class ConnectionThread implements Runnable {
        @Override
        public void run() {
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);
                String defaultMessage = "Requesting server IP";
                byte[] buffer = defaultMessage.getBytes();
                InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, 12345);
                socket.send(packet);

                byte[] responseBuffer = new byte[1024];
                DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                socket.receive(responsePacket);

                InetAddress serverAddress = responsePacket.getAddress();
                String serverIp = serverAddress.getHostAddress();
                runOnUiThread(() -> Toast.makeText(Contact.this, "Server IP: " + serverIp, Toast.LENGTH_LONG).show());

                socket.close();

                // Connect to the server using TCP
                Socket client = new Socket(serverIp, 1234);
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
}