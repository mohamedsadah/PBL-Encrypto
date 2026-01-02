package com.example.encrypto.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {

    private static final long SECOND_MILLIS = 1000;
    private static final long MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final long HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final long DAY_MILLIS = 24 * HOUR_MILLIS;

    public static String getRelativeTime(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) return "";

        try {
            String cleanTime = sanitizeTimestamp(isoTimestamp);

            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
            Date date = parser.parse(cleanTime);
            if (date == null) return "";

            long time = date.getTime();
            long now = System.currentTimeMillis();
            long diff = now - time;

            if (diff < MINUTE_MILLIS) {
                return "Just now";
            } else if (diff < 2 * MINUTE_MILLIS) {
                return "1 min ago";
            } else if (diff < 50 * MINUTE_MILLIS) {
                return diff / MINUTE_MILLIS + " min ago";
            } else if (diff < 90 * MINUTE_MILLIS) {
                return "1 hr ago";
            } else if (diff < 24 * HOUR_MILLIS) {
                // check if if hour or hours
                if (diff / HOUR_MILLIS == 1) {
                    return "1 hr ago";
                }else {
                    return diff / HOUR_MILLIS + " hrs ago";
                }
            } else if (diff < 48 * HOUR_MILLIS) {
                return "Yesterday";
            } else {
                // Fallback to date for older items
                SimpleDateFormat formatter = new SimpleDateFormat("MMM dd", Locale.getDefault());
                return formatter.format(date);
            }

        } catch (Exception e) {
            return "";
        }
    }

    private static String sanitizeTimestamp(String fullTime) {
        int dotIndex = fullTime.indexOf('.');
        int offsetIndex = fullTime.indexOf('+');

        if (dotIndex != -1 && offsetIndex != -1 && offsetIndex > dotIndex) {
            return fullTime.substring(0, dotIndex + 4) + fullTime.substring(offsetIndex);
        }
        return fullTime;
    }
}