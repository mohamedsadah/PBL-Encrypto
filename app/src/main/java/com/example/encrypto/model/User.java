package com.example.encrypto.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class User implements Serializable {
    public String id;
    public String phone;
    public String email;
    @SerializedName("full_name")
    public String displayName;
    @SerializedName("profile_pic")
    public String avatarUrl;

    public String status;

    public User() {}
}
