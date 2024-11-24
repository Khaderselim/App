package com.example.chat;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private Socket client;
    private PrintWriter printwriter;
    private EditText textField;
    private Button button;
    private String message;
    private TextView textView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textField = findViewById(R.id.editText1);
        button = findViewById(R.id.button1);
        textView = findViewById(R.id.textView1);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                message = textField.getText().toString();
                new Thread(new ClientThread(message)).start();
            }
        });

        // Start the connection thread
        new Thread(new ConnectionThread()).start();
    }

    private boolean isConnected() {
        return client != null && client.isConnected() && !client.isClosed();
    }

    class ConnectionThread implements Runnable {
        @Override
        public void run() {
            try {
                client = new Socket("192.168.232.122", 1234);
                printwriter = new PrintWriter(client.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                new Thread(() -> {
                    try {
                        String serverResponse;
                        while ((serverResponse = in.readLine()) != null) {
                            String finalServerResponse = serverResponse;
                            textView.post(() -> textView.setText(textView.getText() + finalServerResponse+"\n"));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

                runOnUiThread(() -> {
                    if (isConnected()) {
                        Toast.makeText(MainActivity.this, "Connected to server", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to connect to server", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to connect to server", Toast.LENGTH_SHORT).show());
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
            if (isConnected()) {
                printwriter.println(message);
                runOnUiThread(() -> textField.setText(""));
            } else {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Not connected to server", Toast.LENGTH_SHORT).show());
            }
        }
    }
}