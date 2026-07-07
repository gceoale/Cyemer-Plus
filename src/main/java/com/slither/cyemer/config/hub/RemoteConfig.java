package com.slither.cyemer.config.hub;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class RemoteConfig {
    public String name;
    public String author;
    public String description;
    public String fileName;

    public String getDisplayName() {
        return this.name + " by " + this.author;
    }
}
