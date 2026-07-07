package com.slither.cyemer.shader;

import com.slither.cyemer.util.RenderBackendDetector;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_276;
import net.minecraft.class_279;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_9920;
import net.minecraft.class_9960;

@Environment(EnvType.CLIENT)
public class PostShaderManager {
    private static PostShaderManager INSTANCE;
    private final class_310 client;
    private final Map<class_2960, class_279> loadedEffects = new ConcurrentHashMap<>();
    private final Map<class_2960, PostShaderManager.ShaderConfig> registeredEffects = new HashMap<>();
    private final Stack<class_2960> effectStack = new Stack<>();
    private class_2960 activeEffect = null;
    private boolean enabled = true;
    private class_9920 renderPool;

    private PostShaderManager() {
        this.client = class_310.method_1551();
        this.enabled = !RenderBackendDetector.isVulkanBackend();
        this.renderPool = new class_9920(3);
    }

    public static PostShaderManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PostShaderManager();
        }

        return INSTANCE;
    }

    public void registerShader(class_2960 id, PostShaderManager.ShaderConfig config) {
        this.registeredEffects.put(id, config);
    }

    public void registerShader(class_2960 id) {
        this.registerShader(id, PostShaderManager.ShaderConfig.DEFAULT);
    }

    public class_279 loadShader(class_2960 id) {
        try {
            class_279 processor = this.client.method_62887().method_62941(id, class_9960.field_53902);
            if (processor != null) {
                this.loadedEffects.put(id, processor);
                return processor;
            } else {
                return null;
            }
        } catch (Exception var3) {
            return null;
        }
    }

    public class_279 getOrLoadShader(class_2960 id) {
        class_279 processor = this.loadedEffects.get(id);
        if (processor == null) {
            processor = this.loadShader(id);
        }

        return processor;
    }

    public boolean applyEffect(class_2960 id) {
        if (!this.isPostProcessingSupported()) {
            this.clearEffect();
            return false;
        } else if (!this.enabled) {
            return false;
        } else {
            class_279 processor = this.getOrLoadShader(id);
            if (processor != null) {
                this.activeEffect = id;
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean pushEffect(class_2960 id) {
        if (!this.isPostProcessingSupported()) {
            this.clearEffect();
            return false;
        } else {
            if (this.activeEffect != null) {
                this.effectStack.push(this.activeEffect);
            }

            return this.applyEffect(id);
        }
    }

    public void popEffect() {
        if (!this.effectStack.isEmpty()) {
            class_2960 previous = this.effectStack.pop();
            this.applyEffect(previous);
        } else {
            this.clearEffect();
        }
    }

    public void clearEffect() {
        this.activeEffect = null;
        this.effectStack.clear();
    }

    public void render(class_276 framebuffer) {
        if (this.isPostProcessingSupported()) {
            if (this.enabled && this.activeEffect != null) {
                class_279 processor = this.loadedEffects.get(this.activeEffect);
                if (processor != null) {
                    try {
                        processor.method_1258(framebuffer, this.renderPool);
                    } catch (Exception var4) {
                        this.clearEffect();
                    }
                }
            }
        }
    }

    public void renderOnce(class_2960 id, class_276 framebuffer) {
        if (this.isPostProcessingSupported()) {
            if (this.enabled) {
                class_279 processor = this.getOrLoadShader(id);
                if (processor != null) {
                    try {
                        processor.method_1258(framebuffer, this.renderPool);
                    } catch (Exception var5) {
                    }
                }
            }
        }
    }

    public void setUniform(String uniformName, float value) {
        if (this.activeEffect != null) {
            ;
        }
    }

    public void setUniform(class_2960 id, String uniformName, float value) {
    }

    public boolean isEffectActive(class_2960 id) {
        return this.enabled && Objects.equals(this.activeEffect, id);
    }

    public class_2960 getActiveEffect() {
        return this.activeEffect;
    }

    public boolean isEnabled() {
        return this.enabled && this.isPostProcessingSupported();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled && this.isPostProcessingSupported();
        if (!this.enabled) {
            this.clearEffect();
        }
    }

    private boolean isPostProcessingSupported() {
        return !RenderBackendDetector.isVulkanBackend();
    }

    public void toggle() {
        this.setEnabled(!this.enabled);
    }

    public void clear() {
        this.loadedEffects.clear();
        this.activeEffect = null;
        this.effectStack.clear();
    }

    public void onResized(int width, int height) {
        if (this.activeEffect != null) {
            class_2960 current = this.activeEffect;
            this.loadedEffects.remove(current);
            this.applyEffect(current);
        }
    }

    public Set<class_2960> getRegisteredShaders() {
        return new HashSet<>(this.registeredEffects.keySet());
    }

    public PostShaderManager.ShaderConfig getConfig(class_2960 id) {
        return this.registeredEffects.getOrDefault(id, PostShaderManager.ShaderConfig.DEFAULT);
    }

    @Environment(EnvType.CLIENT)
    public static class Effects {
        public static final class_2960 BLUR = class_2960.method_60656("blur");
        public static final class_2960 CREEPER = class_2960.method_60656("creeper");
        public static final class_2960 SPIDER = class_2960.method_60656("spider");
        public static final class_2960 INVERT = class_2960.method_60656("invert");

        public static void applyBlur(float radius) {
            PostShaderManager manager = PostShaderManager.getInstance();
            manager.applyEffect(BLUR);
            manager.setUniform("Radius", radius);
        }

        public static void applyTimed(class_2960 effect, long durationMs) {
            PostShaderManager manager = PostShaderManager.getInstance();
            manager.applyEffect(effect);
            new Thread(() -> {
                try {
                    Thread.sleep(durationMs);
                    manager.clearEffect();
                } catch (InterruptedException var4) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    @Environment(EnvType.CLIENT)
    public static class ShaderConfig {
        public static final PostShaderManager.ShaderConfig DEFAULT = new PostShaderManager.ShaderConfig();
        private final boolean autoReload;
        private final boolean cacheable;
        private final Map<String, Float> defaultUniforms;

        public ShaderConfig() {
            this(true, true, new HashMap<>());
        }

        public ShaderConfig(boolean autoReload, boolean cacheable, Map<String, Float> defaultUniforms) {
            this.autoReload = autoReload;
            this.cacheable = cacheable;
            this.defaultUniforms = new HashMap<>(defaultUniforms);
        }

        public boolean shouldAutoReload() {
            return this.autoReload;
        }

        public boolean isCacheable() {
            return this.cacheable;
        }

        public Map<String, Float> getDefaultUniforms() {
            return new HashMap<>(this.defaultUniforms);
        }

        public static PostShaderManager.ShaderConfig.Builder builder() {
            return new PostShaderManager.ShaderConfig.Builder();
        }

        @Environment(EnvType.CLIENT)
        public static class Builder {
            private boolean autoReload = true;
            private boolean cacheable = true;
            private final Map<String, Float> defaultUniforms = new HashMap<>();

            public PostShaderManager.ShaderConfig.Builder autoReload(boolean autoReload) {
                this.autoReload = autoReload;
                return this;
            }

            public PostShaderManager.ShaderConfig.Builder cacheable(boolean cacheable) {
                this.cacheable = cacheable;
                return this;
            }

            public PostShaderManager.ShaderConfig.Builder uniform(String name, float value) {
                this.defaultUniforms.put(name, value);
                return this;
            }

            public PostShaderManager.ShaderConfig build() {
                return new PostShaderManager.ShaderConfig(this.autoReload, this.cacheable, this.defaultUniforms);
            }
        }
    }
}
