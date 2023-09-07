package com.example.eyecantalk.uuid;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class UUIDManager {
    private static final String PREFS_NAME = "MyPrefs";
    private static final String UUID_KEY = "uuid";

    public static UUID getUUID(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uuidString = sharedPreferences.getString(UUID_KEY, null);
        UUID uuid;
        if (uuidString == null) {
            uuid = UUID.randomUUID();
            sharedPreferences.edit().putString(UUID_KEY, uuid.toString()).apply();
        } else {
            uuid = UUID.fromString(uuidString);
        }
        return uuid;
    }
}
