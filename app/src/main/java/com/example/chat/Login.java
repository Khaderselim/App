package com.example.chat;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.chat.databinding.ActivityLoginBinding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class Login extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private EditText username;
    private EditText password;
    private Button button;
    private PrintWriter printwriter;
    private String serverip;
    private Socket client;
    private DatabaseManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbManager = new DatabaseManager(this);

        username = findViewById(R.id.editTextUsername);
        password = findViewById(R.id.editTextPassword);
        button = findViewById(R.id.button);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(Login.this, Signup.class);
            intent.putExtra("serverip", serverip);
            startActivity(intent);
        });

        new Thread(new ConnectionThread()).start();
    }

    private class ConnectionThread implements Runnable {
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
                serverip = serverAddress.getHostAddress();
                runOnUiThread(() -> Toast.makeText(Login.this, "Server IP: " + serverip, Toast.LENGTH_LONG).show());

                socket.close();

                client = new Socket(serverip, 9874);
                printwriter = new PrintWriter(client.getOutputStream(), true);
                SocketManager.getInstance().setSocket(client);
                runOnUiThread(() -> {
                    button.setOnClickListener(view -> {
                        String usernameText = username.getText().toString().trim();
                        String passwordText = password.getText().toString().trim();
                        if (!usernameText.isEmpty() && !passwordText.isEmpty()) {
                            new Thread(new ClientThread(usernameText, passwordText)).start();
                        } else {
                            Snackbar.make(view, "Invalid username", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        }
                    });
                });

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(Login.this, "Failed to connect to server", Toast.LENGTH_SHORT).show());
            }
        }
    }

    private class ClientThread implements Runnable {
        private final String uname;
        private final String pwd;

        ClientThread(String uname, String pwd) {
            this.uname = uname;
            this.pwd = pwd;
        }

        @Override
        public void run() {
            try {
                if (printwriter != null) {
                    printwriter.println("***LOGIN***");
                    printwriter.println(uname);
                    printwriter.println(pwd);

                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String serverResponse = in.readLine();

                    if (serverResponse != null) {
                        runOnUiThread(() -> Toast.makeText(Login.this, "Server response: " + serverResponse, Toast.LENGTH_SHORT).show());

                        if (serverResponse.equals("Login successful")) {
                            long userId = dbManager.addUser(uname, pwd);
                            runOnUiThread(() -> {
                                Intent intent = new Intent(Login.this, Contact.class);
                                intent.putExtra("username", uname);
                                intent.putExtra("serverip", serverip);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(Login.this, "Invalid username or password", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(Login.this, "Server response is null", Toast.LENGTH_SHORT).show());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}