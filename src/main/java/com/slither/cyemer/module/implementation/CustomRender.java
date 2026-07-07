package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.shader.PostShaderManager;
import com.slither.cyemer.util.RenderBackendDetector;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_3532;

@Environment(EnvType.CLIENT)
public class CustomRender extends Module {
    private static CustomRender instance;
    private final ModeSetting shaderMode = new ModeSetting(
        "Shader", "Blur", "Invert", "Creeper", "Spider", "Rainbow", "Chromatic", "Wave", "Pixelate", "Glitch"
    );
    private final SliderSetting intensity = new SliderSetting("Intensity", 5.0, 0.0, 20.0, 1);
    private final BooleanSetting dynamicIntensity = new BooleanSetting("Dynamic", false);
    private final SliderSetting pulseSpeed = new SliderSetting("Pulse Speed", 1.0, 0.1, 5.0, 1);
    private long startTime;

    public CustomRender() {
        super("CustomRender", "Apply post-processing shaders to the game", Category.RENDER);
        this.addSetting(this.shaderMode);
        this.addSetting(this.intensity);
        this.addSetting(this.dynamicIntensity);
        this.addSetting(this.pulseSpeed);
        instance = this;
        this.startTime = System.currentTimeMillis();
    }

    public static CustomRender getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        PostShaderManager.getInstance().clearEffect();
    }

    @Override
    public void onTick() {
        if (this.isEnabled() && this.mc.field_1724 != null) {
            if (RenderBackendDetector.isVulkanBackend()) {
                PostShaderManager.getInstance().clearEffect();
            } else {
                this.updateShaderUniforms();
                this.applyCurrentShader();
            }
        }
    }

    @Override
    public void onRender(class_332 context, float tickDelta) {
        if (this.isEnabled() && context != null && RenderBackendDetector.isVulkanBackend()) {
            if (this.mc.field_1755 != null) {
                int screenW = this.mc.method_22683().method_4486();
                int screenH = this.mc.method_22683().method_4502();
                float animatedIntensity = this.getAnimatedIntensity();
                float normalized = class_3532.method_15363(animatedIntensity / 20.0F, 0.0F, 1.0F);
                String mode = this.shaderMode.getCurrentMode();
                if ("Blur".equals(mode)) {
                    Renderer.get().drawBlur(context, 0.0F, 0.0F, screenW, screenH, 4.0F + normalized * 14.0F);
                } else {
                    Color tint = this.getVulkanFallbackTint(mode, normalized);
                    if (tint.getAlpha() > 0) {
                        context.method_25294(0, 0, screenW, screenH, tint.getRGB());
                    }
                }
            }
        }
    }

    private void applyCurrentShader() {
        PostShaderManager manager = PostShaderManager.getInstance();
        String mode = this.shaderMode.getCurrentMode();
        switch (mode) {
            case "None":
                manager.clearEffect();
                break;
            case "Blur":
                manager.applyEffect(PostShaderManager.Effects.BLUR);
                break;
            case "Invert":
                manager.applyEffect(PostShaderManager.Effects.INVERT);
                break;
            case "Creeper":
                manager.applyEffect(PostShaderManager.Effects.CREEPER);
                break;
            case "Spider":
                manager.applyEffect(PostShaderManager.Effects.SPIDER);
                break;
            case "Rainbow":
                manager.applyEffect(class_2960.method_60655("dynamic_fps", "rainbow"));
                break;
            case "Chromatic":
                manager.applyEffect(class_2960.method_60655("dynamic_fps", "chromatic"));
                break;
            case "Wave":
                manager.applyEffect(class_2960.method_60655("dynamic_fps", "wave"));
                break;
            case "Pixelate":
                manager.applyEffect(class_2960.method_60655("dynamic_fps", "pixelate"));
                break;
            case "Glitch":
                manager.applyEffect(class_2960.method_60655("dynamic_fps", "glitch"));
        }
    }

    private void updateShaderUniforms() {
        PostShaderManager manager = PostShaderManager.getInstance();
        String mode = this.shaderMode.getCurrentMode();
        float time = (float)(System.currentTimeMillis() - this.startTime) / 1000.0F;
        if (mode.equals("Blur") || mode.equals("Rainbow")) {
            float intensityValue = this.getAnimatedIntensity();
            manager.setUniform("Radius", intensityValue);
        }

        if (mode.equals("Chromatic") || mode.equals("Wave") || mode.equals("Pixelate") || mode.equals("Glitch")) {
            float intensityValue = this.getAnimatedIntensity();
            manager.setUniform("Intensity", intensityValue);
            manager.setUniform("Time", time);
        }

        if (mode.equals("Rainbow")) {
            manager.setUniform("Time", time);
        }
    }

    public double getIntensity() {
        return this.intensity.getValue();
    }

    public void setIntensity(double value) {
        this.intensity.setValue(Math.max(0.0, Math.min(20.0, value)));
        if (this.isEnabled()) {
            this.updateShaderUniforms();
        }
    }

    private float getAnimatedIntensity() {
        float intensityValue = (float)this.intensity.getValue();
        if (!this.dynamicIntensity.isEnabled()) {
            return intensityValue;
        } else {
            float time = (float)(System.currentTimeMillis() - this.startTime) / 1000.0F;
            float pulse = (float)(Math.sin(time * this.pulseSpeed.getValue()) * 0.5 + 0.5);
            return intensityValue * (0.5F + pulse * 0.5F);
        }
    }

    private Color getVulkanFallbackTint(String mode, float normalized) {
        int alphaBase = class_3532.method_15340((int)(normalized * 110.0F), 0, 140);
        switch (mode) {
            case "Invert":
                return new Color(255, 255, 255, Math.max(18, alphaBase / 2));
            case "Creeper":
                return new Color(56, 172, 88, Math.max(12, alphaBase));
            case "Spider":
                return new Color(96, 54, 116, Math.max(10, alphaBase));
            case "Rainbow":
                float t = (float)(System.currentTimeMillis() - this.startTime) / 1000.0F;
                float hue = t * 0.18F % 1.0F;
                Color rainbowColor = Color.getHSBColor(hue, 0.7F, 1.0F);
                return new Color(rainbowColor.getRed(), rainbowColor.getGreen(), rainbowColor.getBlue(), Math.max(12, alphaBase));
            case "Chromatic":
                return new Color(255, 64, 92, Math.max(8, alphaBase / 2));
            case "Wave":
                return new Color(64, 152, 255, Math.max(8, alphaBase / 2));
            case "Pixelate":
                return new Color(20, 20, 20, Math.max(8, alphaBase / 3));
            case "Glitch":
                return new Color(130, 54, 255, Math.max(8, alphaBase / 2));
            default:
                return new Color(0, 0, 0, 0);
        }
    }
}
