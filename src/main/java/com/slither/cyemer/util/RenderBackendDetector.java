package com.slither.cyemer.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public final class RenderBackendDetector {
    private static final String BACKEND_PROPERTY = "cyemer.render.backend";
    private static final String BACKEND_ENV = "CYEMER_RENDER_BACKEND";
    private static Boolean cachedVulkanBackend;

    private RenderBackendDetector() {
    }

    public static boolean isVulkanBackend() {
        if (cachedVulkanBackend != null) {
            return cachedVulkanBackend;
        } else {
            String explicitBackend = firstNonBlank(System.getProperty("cyemer.render.backend"), System.getenv("CYEMER_RENDER_BACKEND"));
            if (explicitBackend != null) {
                cachedVulkanBackend = "vulkan".equalsIgnoreCase(explicitBackend.trim());
                return cachedVulkanBackend;
            } else if (!"true".equalsIgnoreCase(System.getProperty("cyemer.force.vulkan")) && !"true".equalsIgnoreCase(System.getenv("CYEMER_FORCE_VULKAN"))) {
                try {
                    cachedVulkanBackend = FabricLoader.getInstance().isModLoaded("vulkanmod") || isVulkanModClassPresent();
                } catch (Throwable var2) {
                    cachedVulkanBackend = isVulkanModClassPresent();
                }

                return cachedVulkanBackend;
            } else {
                cachedVulkanBackend = true;
                return true;
            }
        }
    }

    static void resetCacheForTests() {
        cachedVulkanBackend = null;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        } else {
            return second != null && !second.isBlank() ? second : null;
        }
    }

    private static boolean isVulkanModClassPresent() {
        String[] candidates = new String[]{"net.vulkanmod.vulkan.VRenderSystem", "net.vulkanmod.config.Config", "net.vulkanmod.vulkan.Vulkan"};

        for (String name : candidates) {
            try {
                Class.forName(name, false, RenderBackendDetector.class.getClassLoader());
                return true;
            } catch (Throwable var6) {
            }
        }

        return false;
    }
}
