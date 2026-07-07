package com.slither.cyemer.friend;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.slither.cyemer.util.GameProfileCompat;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_640;

@Environment(EnvType.CLIENT)
public class UUIDFetcher {
    private static final String MOJANG_API_URL = "https://api.mojang.com/users/profiles/minecraft/";

    public static CompletableFuture<UUID> fetchUUID(String username) {
        return CompletableFuture.supplyAsync(() -> {
            UUID inGameUUID = getUUIDFromGame(username);
            return inGameUUID != null ? inGameUUID : fetchUUIDFromMojang(username);
        });
    }

    private static UUID getUUIDFromGame(String username) {
        class_310 client = class_310.method_1551();
        if (client.method_1562() == null) {
            return null;
        } else {
            for (class_640 entry : client.method_1562().method_2880()) {
                String name = GameProfileCompat.getName(entry.method_2966());
                if (name != null && name.equalsIgnoreCase(username)) {
                    return GameProfileCompat.getId(entry.method_2966());
                }
            }

            return null;
        }
    }

    private static UUID fetchUUIDFromMojang(String username) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return responseCode != 204 && responseCode != 404 ? null : null;
            } else {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                String uuidString = json.get("id").getAsString();
                String formattedUUID = uuidString.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
                return UUID.fromString(formattedUUID);
            }
        } catch (Exception var10) {
            return null;
        }
    }

    public static UUID fetchUUIDSync(String username) {
        try {
            return fetchUUID(username).get();
        } catch (Exception var2) {
            return null;
        }
    }
}
