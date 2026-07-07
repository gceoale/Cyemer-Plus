package com.slither.cyemer.friend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public class FriendManager {
    private static final FriendManager INSTANCE = new FriendManager();
    private final Map<UUID, Friend> friends = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File friendsFile;

    private FriendManager() {
        File cyemerDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "cyemer");
        if (!cyemerDir.exists()) {
            cyemerDir.mkdirs();
        }

        this.friendsFile = new File(cyemerDir, "friends.json");
        this.load();
    }

    public static FriendManager getInstance() {
        return INSTANCE;
    }

    public void addFriend(String name, UUID uuid) {
        if (uuid != null) {
            if (name != null && !name.isEmpty()) {
                Friend friend = new Friend(name, uuid);
                this.friends.put(uuid, friend);
                this.save();
            }
        }
    }

    public void removeFriend(UUID uuid) {
        if (this.friends.remove(uuid) != null) {
            this.save();
        }
    }

    public void removeFriend(String name) {
        Friend toRemove = this.friends.values().stream().filter(f -> f.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (toRemove != null) {
            this.friends.remove(toRemove.getUuid());
            this.save();
        }
    }

    public boolean isFriend(UUID uuid) {
        return uuid != null && this.friends.containsKey(uuid);
    }

    public boolean isFriend(String name) {
        return name != null && !name.isEmpty() ? this.friends.values().stream().anyMatch(f -> f.getName().equalsIgnoreCase(name)) : false;
    }

    public Friend getFriend(UUID uuid) {
        return this.friends.get(uuid);
    }

    public Friend getFriend(String name) {
        return name != null && !name.isEmpty() ? this.friends.values().stream().filter(f -> f.getName().equalsIgnoreCase(name)).findFirst().orElse(null) : null;
    }

    public List<Friend> getFriends() {
        return new ArrayList<>(this.friends.values());
    }

    public void clearFriends() {
        this.friends.clear();
        this.save();
    }

    public void updateFriendNote(UUID uuid, String note) {
        Friend friend = this.friends.get(uuid);
        if (friend != null) {
            friend.setNote(note);
            this.save();
        }
    }

    private void save() {
        try (FileWriter writer = new FileWriter(this.friendsFile)) {
            JsonObject root = new JsonObject();
            JsonArray friendsArray = new JsonArray();

            for (Friend friend : this.friends.values()) {
                JsonObject friendObj = new JsonObject();
                friendObj.addProperty("name", friend.getName());
                friendObj.addProperty("uuid", friend.getUuid().toString());
                friendObj.addProperty("addedTime", friend.getAddedTime());
                friendObj.addProperty("note", friend.getNote());
                friendsArray.add(friendObj);
            }

            root.add("friends", friendsArray);
            this.gson.toJson(root, writer);
            writer.flush();
        } catch (Exception var9) {
        }
    }

    private void load() {
        if (this.friendsFile.exists()) {
            try {
                try (FileReader reader = new FileReader(this.friendsFile)) {
                    JsonObject root = (JsonObject)this.gson.fromJson(reader, JsonObject.class);
                    if (root != null && root.has("friends")) {
                        for (JsonElement element : root.getAsJsonArray("friends")) {
                            JsonObject friendObj = element.getAsJsonObject();
                            String name = friendObj.get("name").getAsString();
                            UUID uuid = UUID.fromString(friendObj.get("uuid").getAsString());
                            long addedTime = friendObj.has("addedTime") ? friendObj.get("addedTime").getAsLong() : System.currentTimeMillis();
                            String note = friendObj.has("note") ? friendObj.get("note").getAsString() : "";
                            Friend friend = new Friend(name, uuid, addedTime, note);
                            this.friends.put(uuid, friend);
                        }

                        return;
                    }
                }
            } catch (Exception var15) {
            }
        }
    }
}
