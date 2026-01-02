package com.example.encrypto.model;

import com.google.gson.annotations.SerializedName;

public class Chat {
    public String id;
    public String title;

    @SerializedName("last_message")
    public String lastMessage;

    @SerializedName("last_activity")
    public String updatedAt;
    public String otherUserId;

    @SerializedName("partner_phone")
    public String partnerPhone;

    @SerializedName("profile_pic")
    public String profilePic;

    public Chat() {}

}
