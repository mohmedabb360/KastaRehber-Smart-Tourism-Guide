package com.example.turist_rehberi;

public class ChatMessage {
    private String text;
    private boolean isFromAI;

    public ChatMessage(String text, boolean isFromAI) {
        this.text = text;
        this.isFromAI = isFromAI;
    }

    public String getText() { return text; }
    public boolean isFromAI() { return isFromAI; }
}