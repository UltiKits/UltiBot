package com.ultikits.plugins.ultibot.service;

import com.ultikits.ultitools.annotations.Service;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class SkinService {

    private static final String MOJANG_UUID_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String MOJANG_PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";

    private final Map<String, SkinData> cache = new ConcurrentHashMap<>();

    @Getter
    @AllArgsConstructor
    public static class SkinData {
        private final String value;
        private final String signature;
    }

    /**
     * Fetch skin data from Mojang API for the given player name.
     * Returns cached result if available.
     */
    public SkinData fetchSkin(String playerName, Logger logger) {
        SkinData cached = getCachedSkin(playerName);
        if (cached != null) {
            return cached;
        }

        try {
            // Step 1: Get UUID from player name
            String uuidJson = httpGet(MOJANG_UUID_URL + playerName);
            if (uuidJson == null) {
                return null;
            }
            String uuid = parseUuidFromResponse(uuidJson);
            if (uuid == null) {
                return null;
            }

            // Step 2: Get profile with textures
            String profileJson = httpGet(MOJANG_PROFILE_URL + uuid + "?unsigned=false");
            if (profileJson == null) {
                return null;
            }

            SkinData data = parseSkinTexture(profileJson);
            if (data != null) {
                cacheSkin(playerName, data);
            }
            return data;
        } catch (Exception e) {
            if (logger != null) {
                logger.log(Level.WARNING, "Failed to fetch skin for " + playerName, e);
            }
            return null;
        }
    }

    /**
     * Parse UUID from Mojang username lookup response.
     * Expected format: {"id":"hex","name":"..."}
     */
    public String parseUuidFromResponse(String json) {
        if (json == null) {
            return null;
        }
        return extractJsonString(json, "id");
    }

    /**
     * Parse skin texture data from session-server profile response.
     * Expected: {"properties":[{"name":"textures","value":"...","signature":"..."}]}
     */
    public SkinData parseSkinTexture(String json) {
        if (json == null) {
            return null;
        }

        try {
            // Find the properties array
            int propsIdx = json.indexOf("\"properties\"");
            if (propsIdx < 0) {
                return null;
            }

            int arrayStart = json.indexOf('[', propsIdx);
            if (arrayStart < 0) {
                return null;
            }

            int arrayEnd = json.indexOf(']', arrayStart);
            if (arrayEnd < 0 || arrayEnd <= arrayStart + 1) {
                return null; // empty array
            }

            String propsContent = json.substring(arrayStart, arrayEnd + 1);

            // Find the textures property object
            int textureIdx = propsContent.indexOf("\"textures\"");
            if (textureIdx < 0) {
                return null;
            }

            String value = extractJsonString(propsContent, "value");
            if (value == null) {
                return null;
            }

            String signature = extractJsonString(propsContent, "signature");
            return new SkinData(value, signature);
        } catch (Exception e) {
            return null;
        }
    }

    public void cacheSkin(String playerName, SkinData data) {
        cache.put(playerName.toLowerCase(), data);
    }

    public SkinData getCachedSkin(String playerName) {
        return cache.get(playerName.toLowerCase());
    }

    /**
     * Simple JSON string value extractor. Finds "key":"value" in a flat JSON object.
     */
    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) {
            return null;
        }

        // Find the colon after the key
        int colonIdx = json.indexOf(':', keyIdx + search.length());
        if (colonIdx < 0) {
            return null;
        }

        // Find the opening quote of the value
        int valueStart = json.indexOf('"', colonIdx + 1);
        if (valueStart < 0) {
            return null;
        }

        // Find the closing quote (handle escaped quotes)
        int valueEnd = valueStart + 1;
        while (valueEnd < json.length()) {
            if (json.charAt(valueEnd) == '"' && json.charAt(valueEnd - 1) != '\\') {
                break;
            }
            valueEnd++;
        }

        if (valueEnd >= json.length()) {
            return null;
        }

        return json.substring(valueStart + 1, valueEnd);
    }

    private String httpGet(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                return null;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
