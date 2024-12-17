package com.example.chat;

public class ContactsModal {
    private String username;
    private String status;

    public ContactsModal(String username, String status) {
        this.username = username;
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public String getStatus() {
        return status;
    }
    public void setUsername(String username) {
        this.username = username;
    }
}