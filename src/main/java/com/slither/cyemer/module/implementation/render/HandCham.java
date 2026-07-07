package com.slither.cyemer.module.implementation.render;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ColorSetting;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1268;
import net.minecraft.class_1799;
import net.minecraft.class_3532;
import net.minecraft.class_4587;
import net.minecraft.class_7833;

@Environment(EnvType.CLIENT)
public class HandCham extends Module {
    public static HandCham INSTANCE;
    private final ColorSetting color;
    private final BooleanSetting tinted;
    private final BooleanSetting breathing;
    private final ModeSetting quality;
    private final SliderSetting animationSpeed;
    private final SliderSetting outlineWidth;
    private final SliderSetting softness;
    private boolean handChamActive = false;

    public HandCham() {
        super("HandCham", "Vulkan-safe hand/item cham using command-queue outline + transform pulse.", Category.RENDER);
        INSTANCE = this;
        this.color = new ColorSetting("Color", new Color(0, 255, 255, 255));
        this.tinted = new BooleanSetting("Tinted", true);
        this.breathing = new BooleanSetting("Breathing", true);
        this.quality = new ModeSetting("Quality", "Low", "Medium", "High");
        this.animationSpeed = new SliderSetting("Anim Speed", 2.0, 0.1, 10.0, 1);
        this.outlineWidth = new SliderSetting("Outline Width", 10.0, 1.0, 50.0, 1);
        this.softness = new SliderSetting("Glow Intensity", 0.6, 0.1, 1.0, 2);
        this.addSetting(this.color);
        this.addSetting(this.tinted);
        this.addSetting(this.breathing);
        this.addSetting(this.quality);
        this.addSetting(this.animationSpeed);
        this.addSetting(this.outlineWidth);
        this.addSetting(this.softness);
    }

    public void prepareRender() {
        this.handChamActive = this.shouldRenderHandCham();
    }

    public void drawRender() {
        this.handChamActive = false;
    }

    @Override
    public void onDisable() {
        this.handChamActive = false;
    }

    public boolean shouldApplyFor(class_1268 hand, class_1799 stack) {
        if (!this.isEnabled()) {
            return false;
        } else {
            return !this.handChamActive && !this.shouldRenderHandCham() ? false : hand != null && stack != null;
        }
    }

    public void applyTransformations(class_4587 matrices, class_1268 hand, float swingProgress) {
        if (matrices != null && hand != null) {
            float pulse = this.computePulse();
            float softnessFactor = class_3532.method_15363((float)this.softness.getValue(), 0.1F, 1.0F);
            float widthFactor = class_3532.method_15363((float)(this.outlineWidth.getValue() / 50.0), 0.02F, 1.0F);
            float qualityFactor = this.getQualityFactor();
            float handSign = hand == class_1268.field_5808 ? 1.0F : -1.0F;
            float strength = softnessFactor * qualityFactor;
            float scale = 1.0F + 0.006F * strength * widthFactor * pulse;
            float offsetX = handSign * 0.0035F * strength * pulse;
            float offsetY = -0.0035F * strength * (0.5F + class_3532.method_15363(swingProgress, 0.0F, 1.0F) * 0.5F);
            float offsetZ = -0.006F * widthFactor * strength;
            float roll = handSign * 0.9F * strength * pulse;
            matrices.method_46416(offsetX, offsetY, offsetZ);
            matrices.method_22905(scale, scale, scale);
            matrices.method_22907(class_7833.field_40718.rotationDegrees(roll));
        }
    }

    public int getOutlineColor() {
        Color base = this.color.getValue();
        if (base == null) {
            base = new Color(0, 255, 255, 255);
        }

        float pulse = this.computePulse();
        float softnessFactor = class_3532.method_15363((float)this.softness.getValue(), 0.1F, 1.0F);
        float qualityFactor = this.getQualityFactor();
        float brightness = 0.65F + 0.35F * pulse;
        int alpha = class_3532.method_15340(Math.round(base.getAlpha() * (0.35F + softnessFactor * 0.65F) * qualityFactor), 40, 255);
        int red;
        int green;
        int blue;
        if (this.tinted.isEnabled()) {
            red = class_3532.method_15340(Math.round(base.getRed() * brightness), 0, 255);
            green = class_3532.method_15340(Math.round(base.getGreen() * brightness), 0, 255);
            blue = class_3532.method_15340(Math.round(base.getBlue() * brightness), 0, 255);
        } else {
            int mono = class_3532.method_15340(Math.round(210.0F + 45.0F * pulse), 0, 255);
            red = mono;
            green = mono;
            blue = mono;
        }

        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private float getQualityFactor() {
        String mode = this.quality.getCurrentMode();
        if ("High".equalsIgnoreCase(mode)) {
            return 1.0F;
        } else {
            return "Medium".equalsIgnoreCase(mode) ? 0.82F : 0.68F;
        }
    }

    private float computePulse() {
        if (!this.breathing.isEnabled()) {
            return 1.0F;
        } else {
            double speed = Math.max(0.1, this.animationSpeed.getValue());
            double phase = System.nanoTime() / 1.0E9 * speed;
            return (float)(0.55 + 0.45 * ((Math.sin(phase) + 1.0) * 0.5));
        }
    }

    private boolean shouldRenderHandCham() {
        return this.mc.field_1724 == null ? false : this.mc.field_1690.method_31044().method_31034();
    }
}
