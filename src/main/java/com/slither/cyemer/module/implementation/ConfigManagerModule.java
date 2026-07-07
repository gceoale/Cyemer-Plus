package com.slither.cyemer.module.implementation;

import com.slither.cyemer.config.ConfigManager;
import com.slither.cyemer.gui.new_ui.ConfigScreen;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_437;

@Environment(EnvType.CLIENT)
public class ConfigManagerModule extends Module {
    public ConfigManagerModule() {
        super("ConfigManager", "Manage configs.", Category.CLIENT);
    }

    @Override
    public void onEnable() {
        if (this.mc != null) {
            class_437 parent = this.mc.field_1755;
            this.mc.method_1507(new ConfigScreen(parent, ConfigManager.getInstance()));
        }

        this.toggle();
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onTick() {
    }
}
