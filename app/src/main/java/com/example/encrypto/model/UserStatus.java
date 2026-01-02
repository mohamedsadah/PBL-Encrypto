package com.example.encrypto.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserStatus {
    public String userId;
    public Map user;
    public String localName;

    public List<Status> statuses = new ArrayList<>();

    public String getLatestTimestamp() {
        if (statuses.isEmpty()) return "";
        return statuses.get(0).createdAt;
    }

    public String getLatestMediaUrl() {
        if (statuses.isEmpty()) return null;
        return statuses.get(0).mediaUrl;
    }
}