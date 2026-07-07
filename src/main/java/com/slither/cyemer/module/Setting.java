package com.slither.cyemer.module;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class Setting {
    private String name;
    private Runnable changeListener;

    public Setting(String name) {
        this.name = name;
        this.changeListener = null;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Setting onChange(Runnable listener) {
        this.changeListener = listener;
        return this;
    }

    protected void callListener() {
        if (this.changeListener != null) {
            this.changeListener.run();
        }
    }
}
