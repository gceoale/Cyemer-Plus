package com.slither.cyemer.module;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ColorSetting extends Setting {
    private Color value;

    public ColorSetting(String name, Color defaultValue) {
        super(name);
        this.value = defaultValue;
    }

    public Color getValue() {
        return this.value;
    }

    public void setValue(Color value) {
        this.value = value;
    }
}
