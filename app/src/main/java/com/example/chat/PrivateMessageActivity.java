package com.example.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class PrivateMessageActivity extends AppCompatActivity {
    private TextView contactNameTV;
    private PrintWriter printwriter;
    private ArrayList<String> messages;
    private MessageAdapter adapter;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_message);

        contactNameTV = findViewById(R.id.idTVContactName);
        EditText textField = findViewById(R.id.editText1);
        Button button = findViewById(R.id.button1);
        ListView listView = findViewById(R.id.textView1);
        messages = new ArrayList<>();
        adapter = new MessageAdapter(this, messages);
        listView.setAdapter(adapter);

        Intent intent = getIntent();
        String contactName = intent.getStringExtra("contactName");
        username = intent.getStringExtra("username");
        contactNameTV.setText(contactName);

        // Retrieve the socket from SocketManager
        Socket socket = SocketManager.getInstance().getSocket();
        if (socket != null) {
            new ConnectionThread(socket, contactName).start();
        } else {
            Toast.makeText(this, "Socket is null", Toast.LENGTH_SHORT).show();
        }
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String message = textField.getText().toString();
                if (!message.isEmpty()) {
                    messages.add("Me: " + message);
                    adapter.notifyDataSetChanged();
                    textField.setText("");
                    new Thread(new ClientThread(message)).start();
                }
            }
        });
    }

    class ConnectionThread extends Thread {
        private Socket socket;
        private String contactName;

        public ConnectionThread(Socket socket, String contactName) {
            this.socket = socket;
            this.contactName = contactName;
        }

        @Override
        public void run() {
            try {
                Socket client = socket;
                printwriter = new PrintWriter(client.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                printwriter.println("***PRV0***");
                printwriter.println(contactName);
                new Thread(() -> {
                    try {
                        String serverResponse;
                        while ((serverResponse = in.readLine()) != null) {
                            String finalServerResponse = serverResponse.replace(username+":", "Me:");
                            runOnUiThread(() -> {
                                messages.add(finalServerResponse);
                                adapter.notifyDataSetChanged();
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ClientThread implements Runnable {
        private final String message;

        ClientThread(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            if (printwriter != null) {
                printwriter.println("***PRV***");
                printwriter.println(contactNameTV.getText().toString());
                printwriter.println(message);
            }
        }
    }
}