package com.example.chat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
public class Signup extends AppCompatActivity {

    private EditText username;
    private EditText password;
    private EditText confirmPassword;
    private Button button;
    private String serverip;
    private Socket socket;
    private PrintWriter printwriter;
    private DatabaseManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        dbManager = new DatabaseManager(this);

        Intent intent = getIntent();
        serverip = intent.getStringExtra("serverip");
        if (serverip == null || serverip.isEmpty()) {
            Toast.makeText(this, "Server IP not provided", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        username = findViewById(R.id.editTextUsername);
        password = findViewById(R.id.editpassword);
        confirmPassword = findViewById(R.id.editTextconfirmPassword);
        button = findViewById(R.id.button);

        new Thread(new ConnectionThread()).start();
    }

    private class ConnectionThread implements Runnable {
        @Override
        public void run() {
            try {
                socket = SocketManager.getInstance().getSocket();
                printwriter = new PrintWriter(socket.getOutputStream(), true);
                runOnUiThread(() -> button.setOnClickListener(v -> handleSignup()));
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(Signup.this, "Unable to connect to server", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }
    }

    private void handleSignup() {
        String usernameStr = username.getText().toString().trim();
        String passwordStr = password.getText().toString().trim();
        String confirmPasswordStr = confirmPassword.getText().toString();

        if (usernameStr.isEmpty() || passwordStr.isEmpty() || confirmPasswordStr.isEmpty()) {
            Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!passwordStr.equals(confirmPasswordStr)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(new ClientThread(usernameStr, passwordStr)).start();
    }

    private class ClientThread implements Runnable {
        private final String uname;
        private final String pass;

        public ClientThread(String uname, String pass) {
            this.uname = uname;
            this.pass = pass;
        }

        @Override
        public void run() {
            try {
                if (printwriter != null && socket != null) {
                    printwriter.println("***REGISTER***");
                    printwriter.println(uname);
                    printwriter.println(pass);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String serverResponse = in.readLine();

                    runOnUiThread(() -> {
                        if (serverResponse != null) {
                            if (serverResponse.equals("Registration successful")) {
                                long userId = dbManager.addUser(uname, pass);
                                if (userId != -1) {
                                    Toast.makeText(Signup.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(Signup.this, Contact.class);
                                    intent.putExtra("username", uname);
                                    intent.putExtra("serverip", serverip);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(Signup.this, "User already exists in database", Toast.LENGTH_SHORT).show();
                                }
                            } else if (serverResponse.equals("User already exists")) {
                                Toast.makeText(Signup.this, "Username already exists", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(Signup.this, "Unexpected server response: " + serverResponse, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(Signup.this, "No response from server", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(Signup.this, "Connection not established", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(Signup.this, "Error during registration", Toast.LENGTH_SHORT).show());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}