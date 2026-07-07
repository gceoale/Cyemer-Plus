package com.slither.cyemer.module.implementation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class GuiModule extends ClickGUIModule {
    public GuiModule() {
        this.setName("ClickGUI");
        this.setDescription("Opens the ClickGUI interface");
    }
}
