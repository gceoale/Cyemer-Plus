package com.slither.cyemer.event.impl;

import com.slither.cyemer.event.Event;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ShieldDrainEvent implements Event {
    private boolean active = false;

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
