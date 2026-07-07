package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class StreamerModeModule extends Module {
    private static StreamerModeModule INSTANCE;

    public StreamerModeModule() {
        super("StreamerMode", "Hides sensitive information for streaming.", Category.CLIENT);
        INSTANCE = this;
    }

    public static boolean isStreamerModeActive() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }
}
