package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class KeybindSetting extends Setting {
    private final Module module;

    public KeybindSetting(String name, Module module) {
        super(name);
        this.module = module;
    }

    public Module getModule() {
        return this.module;
    }
}
