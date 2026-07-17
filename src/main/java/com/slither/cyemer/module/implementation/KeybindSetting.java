package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;

/**
 * Two modes:
 *  - Constructed with a Module: delegates all keybind state to that
 *    module, matching the original single-per-module keybind behavior.
 *  - Constructed without a Module (standalone): stores its own key code
 *    and binding state, so a module can carry extra secondary keybinds.
 */
@Environment(EnvType.CLIENT)
public class KeybindSetting extends Setting {
    private final Module module;
    private int keyCode = -1;
    private boolean binding = false;
    private long bindStartTime = 0L;

    public KeybindSetting(String name, Module module) {
        super(name);
        this.module = module;
    }

    public KeybindSetting(String name) {
        super(name);
        this.module = null;
    }

    public Module getModule() {
        return this.module;
    }

    public boolean isStandalone() {
        return this.module == null;
    }

    public int getKeyCode() {
        return this.module != null ? this.module.getKeyCode() : this.keyCode;
    }

    public void setKeyCode(int keyCode) {
        if (this.module != null) this.module.setKeyCode(keyCode);
        else this.keyCode = keyCode;
    }

    public boolean isBinding() {
        return this.module != null ? this.module.isBinding() : this.binding;
    }

    public void setBinding(boolean binding) {
        if (this.module != null) this.module.setBinding(binding);
        else this.binding = binding;
    }

    public long getBindStartTime() {
        return this.module != null ? this.module.getBindStartTime() : this.bindStartTime;
    }

    public void setBindStartTime(long time) {
        if (this.module != null) this.module.setBindStartTime(time);
        else this.bindStartTime = time;
    }

    public String getKeyDisplayName() {
        return formatKey(this.getKeyCode());
    }

    public static String formatKey(int key) {
        if (key == -1) return "NONE";
        if (key < 0) {
            int button = key + 100;
            return "MOUSE" + (button + 1);
        }
        switch (key) {
            case 256: return "ESC";
            case 260: return "INSERT";
            case 261: return "DELETE";
            case 266: return "PAGE UP";
            case 267: return "PAGE DOWN";
            case 283: return "PRINT SCREEN";
            case 340: return "LSHIFT";
            case 341: return "LCTRL";
            case 342: return "LALT";
            case 344: return "RSHIFT";
            case 345: return "RCTRL";
            case 346: return "RALT";
            default:
                String name = GLFW.glfwGetKeyName(key, 0);
                return name == null ? "UNKNOWN" : name.toUpperCase();
        }
    }
}
