package com.example.encrypto.model;

import com.google.gson.annotations.SerializedName;

public class Message {
    public String id;

    @SerializedName("chat_id")
    public String chatId;

    @SerializedName("sender_id")
    public String senderId;

    @SerializedName("content")
    public String text;

    @SerializedName("created_at")
    public String createdAt;
    public boolean seen;

    public Message() {}
}
