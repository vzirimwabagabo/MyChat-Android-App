// Path: app/src/main/java/com/example/mychat/ChatMessage.java
package com.example.mychat;

import java.util.Date;

public class ChatMessage {

    private String messageText;
    private String messageUser;
    private long messageTime;
    private String messageImageUrl; // To hold the URL for an image

    // A private constructor prevents direct instantiation from outside.
    // All objects will be created via the static factory methods.
    private ChatMessage() {
        this.messageTime = new Date().getTime();
    }

    // --- FIX 1: Factory method for TEXT messages ---
    public static ChatMessage createTextMessage(String messageText, String messageUser) {
        ChatMessage msg = new ChatMessage();
        msg.messageText = messageText;
        msg.messageUser = messageUser;
        msg.messageImageUrl = null; // Ensure imageUrl is null for text messages
        return msg;
    }

    // --- FIX 2: Factory method for IMAGE messages ---
    public static ChatMessage createImageMessage(String messageUser, String messageImageUrl) {
        ChatMessage msg = new ChatMessage();
        msg.messageText = null; // Ensure text is null for image messages
        msg.messageUser = messageUser;
        msg.messageImageUrl = messageImageUrl;
        return msg;
    }

    // --- Getters and Setters ---
    // (Your existing getters and setters remain unchanged)

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getMessageUser() {
        return messageUser;
    }

    public void setMessageUser(String messageUser) {
        this.messageUser = messageUser;
    }

    public long getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(long messageTime) {
        this.messageTime = messageTime;
    }

    public String getMessageImageUrl() {
        return messageImageUrl;
    }

    public void setMessageImageUrl(String messageImageUrl) {
        this.messageImageUrl = messageImageUrl;
    }
}
