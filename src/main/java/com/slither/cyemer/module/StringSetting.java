package com.slither.cyemer.module;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class StringSetting extends Setting {
    private String value;
    private boolean isEditing = false;

    public StringSetting(String name, String defaultValue) {
        super(name);
        this.value = defaultValue;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isEditing() {
        return this.isEditing;
    }

    public void setEditing(boolean editing) {
        this.isEditing = editing;
    }
}
