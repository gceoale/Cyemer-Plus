package com.slither.cyemer.friend;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class FriendKeybindManager {
    private static final FriendKeybindManager INSTANCE = new FriendKeybindManager();
    private int keyCode = 70;
    private boolean binding = false;
    private long bindStartTime = 0L;
    private final File keybindFile;

    private FriendKeybindManager() {
        File cyemerDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "cyemer");
        if (!cyemerDir.exists()) {
            cyemerDir.mkdirs();
        }

        this.keybindFile = new File(cyemerDir, "friend_keybind.json");
        this.load();
    }

    public static FriendKeybindManager getInstance() {
        return INSTANCE;
    }

    public int getKeyCode() {
        return this.keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
        this.save();
    }

    public boolean isBinding() {
        return this.binding;
    }

    public void setBinding(boolean binding) {
        this.binding = binding;
        if (binding) {
            this.bindStartTime = System.currentTimeMillis();
        }
    }

    public long getBindStartTime() {
        return this.bindStartTime;
    }

    public void setBindStartTime(long bindStartTime) {
        this.bindStartTime = bindStartTime;
    }

    public String getKeyDisplayName() {
        if (this.keyCode == -1) {
            return "NONE";
        } else if (this.keyCode < 0) {
            int button = this.keyCode + 100;
            return "MOUSE" + (button + 1);
        } else {
            switch (this.keyCode) {
                case 256:
                    return "ESC";
                case 260:
                    return "INSERT";
                case 261:
                    return "DELETE";
                case 266:
                    return "PAGE UP";
                case 267:
                    return "PAGE DOWN";
                case 283:
                    return "PRINT SCREEN";
                case 340:
                    return "LSHIFT";
                case 341:
                    return "LCTRL";
                case 342:
                    return "LALT";
                case 344:
                    return "RSHIFT";
                case 345:
                    return "RCTRL";
                case 346:
                    return "RALT";
                default:
                    String name = GLFW.glfwGetKeyName(this.keyCode, 0);
                    return name == null ? "UNKNOWN" : name.toUpperCase();
            }
        }
    }

    public boolean checkKeyPress(int pressedKey) {
        return !this.binding && this.keyCode != -1 && pressedKey == this.keyCode;
    }

    private void save() {
        try (FileWriter writer = new FileWriter(this.keybindFile)) {
            JsonObject json = new JsonObject();
            json.addProperty("keyCode", this.keyCode);
            writer.write(json.toString());
        } catch (Exception var6) {
        }
    }

    private void load() {
        if (this.keybindFile.exists()) {
            try (FileReader reader = new FileReader(this.keybindFile)) {
                JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                if (json.has("keyCode")) {
                    this.keyCode = json.get("keyCode").getAsInt();
                }
            } catch (Exception var6) {
            }
        }
    }
}
