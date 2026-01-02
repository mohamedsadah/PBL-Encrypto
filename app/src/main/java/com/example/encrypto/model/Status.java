package com.example.encrypto.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Map;

public class Status implements Serializable {
    public String id;

    @SerializedName("user_id")
    public String userId;

    @SerializedName("media_url")
    public String mediaUrl;

    public String caption;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("expires_at")
    public String expiresAt;

    @SerializedName("user")
    public Map user;
}