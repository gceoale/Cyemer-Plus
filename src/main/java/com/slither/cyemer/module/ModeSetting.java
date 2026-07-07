package com.slither.cyemer.module;

import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ModeSetting extends Setting {
    private final List<String> modes;
    private int currentIndex;

    public ModeSetting(String name, String... modes) {
        super(name);
        this.modes = Arrays.asList(modes);
        this.currentIndex = 0;
    }

    public String getCurrentMode() {
        return this.modes.get(this.currentIndex);
    }

    public void setCurrentMode(String modeName) {
        int newIndex = Math.max(0, this.modes.indexOf(modeName));
        if (this.currentIndex != newIndex) {
            this.currentIndex = newIndex;
            this.callListener();
        }
    }

    public void cycle() {
        this.currentIndex = (this.currentIndex + 1) % this.modes.size();
        this.callListener();
    }

    public List<String> getModes() {
        return this.modes;
    }
}
