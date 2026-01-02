package com.example.encrypto;

public final class Constants {
    public static final String SUPABASE_URL = "https://xghicfinwyfvbhkihtxf.supabase.co";
    public static final String SUPABASE_ANON_KEY = "x";

    public static String realtimeWsUrl() {
        return SUPABASE_URL
                .replaceFirst("^https?://", "wss://")
                + "/realtime/v1?apikey=" + SUPABASE_ANON_KEY;
    }
}
