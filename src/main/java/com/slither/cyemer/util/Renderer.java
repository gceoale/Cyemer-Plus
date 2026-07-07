package com.slither.cyemer.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Renderer {
    private static ICyemerRenderer instance;

    public static ICyemerRenderer get() {
        if (instance == null) {
            instance = new VanillaRendererImpl();
            instance.init();
        }

        return instance;
    }

    public static void forceVanillaRenderer() {
        try {
            if (instance != null) {
                instance.cleanup();
            }
        } catch (Throwable var1) {
        }

        instance = new VanillaRendererImpl();
        instance.init();
    }

    public static boolean isNanoVGActive() {
        return false;
    }

    public static boolean canUseNanoVG() {
        return false;
    }

    public static boolean isVulkanBackendActive() {
        return RenderBackendDetector.isVulkanBackend();
    }
}
