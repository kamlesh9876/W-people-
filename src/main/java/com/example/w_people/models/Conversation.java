package com.example.w_people.models;

public class Conversation {
    private String senderId;
    private String receiverId;
    private String lastMessage;
    private long timestamp;
    private String receiverUsername;

    public Conversation() {
        // Default constructor for Firestore deserialization
    }

    public Conversation(String senderId, String receiverId, String lastMessage, long timestamp, String receiverUsername) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.receiverUsername = receiverUsername;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getReceiverUsername() {
        return receiverUsername;
    }
}
