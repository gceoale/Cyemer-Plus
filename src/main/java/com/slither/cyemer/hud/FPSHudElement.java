package com.slither.cyemer.hud;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Setting;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public class FPSHudElement extends HUDElement {
    private final SliderSetting cornerRadius = new SliderSetting("Corner Radius", 8.0, 0.0, 20.0, 0);
    private final SliderSetting bgOpacity = new SliderSetting("BG Opacity", 0.56, 0.0, 1.0, 2);
    private final BooleanSetting gradientBackground = new BooleanSetting("Gradient BG", false);
    private final BooleanSetting showBorder = new BooleanSetting("Show Border", true);
    private final SliderSetting borderWidth = new SliderSetting("Border Width", 2.0, 0.5, 5.0, 1);
    private final SliderSetting fontSize = new SliderSetting("Font Size", 14.0, 8.0, 32.0, 0);
    private final BooleanSetting glowEffect = new BooleanSetting("Glow Effect", true);
    private final SliderSetting glowSize = new SliderSetting("Glow Size", 8.0, 0.0, 20.0, 0);
    private final SliderSetting glowIntensity = new SliderSetting("Glow Intensity", 0.3, 0.0, 1.0, 2);
    private final BooleanSetting colorCodedFPS = new BooleanSetting("Color Coded", true);
    private final BooleanSetting flashOnChange = new BooleanSetting("Flash on Change", true);
    private final BooleanSetting pulseOnChange = new BooleanSetting("Pulse on Change", false);
    private final SliderSetting pulseStrength = new SliderSetting("Pulse Strength", 0.1, 0.0, 0.5, 2);
    private float fpsAnimation = 0.0F;
    private float pulseAnimation = 0.0F;
    private int lastFps = 0;

    public FPSHudElement(String name, double defaultX, double defaultY) {
        super(name, defaultX, defaultY);
    }

    public SliderSetting getCornerRadius() {
        return this.cornerRadius;
    }

    public SliderSetting getBgOpacity() {
        return this.bgOpacity;
    }

    public BooleanSetting getGradientBackground() {
        return this.gradientBackground;
    }

    public BooleanSetting getShowBorder() {
        return this.showBorder;
    }

    public SliderSetting getBorderWidth() {
        return this.borderWidth;
    }

    public SliderSetting getFontSize() {
        return this.fontSize;
    }

    public BooleanSetting getGlowEffect() {
        return this.glowEffect;
    }

    public SliderSetting getGlowSize() {
        return this.glowSize;
    }

    public SliderSetting getGlowIntensity() {
        return this.glowIntensity;
    }

    public BooleanSetting getColorCodedFPS() {
        return this.colorCodedFPS;
    }

    public BooleanSetting getFlashOnChange() {
        return this.flashOnChange;
    }

    public BooleanSetting getPulseOnChange() {
        return this.pulseOnChange;
    }

    public SliderSetting getPulseStrength() {
        return this.pulseStrength;
    }

    @Override
    public List<Setting> getSettings() {
        List<Setting> settings = new ArrayList<>();
        settings.add(this.fontSize);
        settings.add(this.cornerRadius);
        settings.add(this.bgOpacity);
        settings.add(this.gradientBackground);
        settings.add(this.showBorder);
        settings.add(this.borderWidth);
        settings.add(this.glowEffect);
        settings.add(this.glowSize);
        settings.add(this.glowIntensity);
        settings.add(this.colorCodedFPS);
        settings.add(this.flashOnChange);
        settings.add(this.pulseOnChange);
        settings.add(this.pulseStrength);
        return settings;
    }

    @Override
    public void render(class_332 context, float delta) {
        class_310 mc = class_310.method_1551();
        int currentFps = mc.method_47599();
        String text = "FPS: " + currentFps;
        if (currentFps != this.lastFps) {
            if (this.flashOnChange.isEnabled()) {
                this.fpsAnimation = 1.0F;
            }

            if (this.pulseOnChange.isEnabled()) {
                this.pulseAnimation = 1.0F;
            }

            this.lastFps = currentFps;
        }

        this.fpsAnimation = Math.max(0.0F, this.fpsAnimation - delta * 2.0F);
        this.pulseAnimation = Math.max(0.0F, this.pulseAnimation - delta * 2.0F);
        float fSize = (float)this.fontSize.getValue();
        float textWidth = Renderer.get().getTextWidth(text, fSize);
        this.setWidth(textWidth + 12.0F);
        this.setHeight(fSize + 6.0F);
        int screenWidth = mc.method_22683().method_4486();
        int screenHeight = mc.method_22683().method_4502();
        float pixelRatio = mc.method_22683().method_4495();
        if (Renderer.get().beginFrame(screenWidth, screenHeight, pixelRatio)) {
            float x = (float)this.getX();
            float y = (float)this.getY();
            float radius = (float)this.cornerRadius.getValue();
            Color textColor = Color.WHITE;
            if (this.colorCodedFPS.isEnabled()) {
                if (currentFps < 30) {
                    textColor = new Color(255, 100, 100);
                } else if (currentFps < 60) {
                    textColor = new Color(255, 200, 100);
                } else {
                    textColor = new Color(100, 255, 100);
                }
            }

            float scale = 1.0F;
            if (this.pulseOnChange.isEnabled() && this.pulseAnimation > 0.0F) {
                scale = 1.0F + this.pulseAnimation * (float)this.pulseStrength.getValue();
            }

            if (this.glowEffect.isEnabled()) {
                float gSize = (float)this.glowSize.getValue();
                float gIntensity = (float)this.glowIntensity.getValue();

                for (int i = 20; i >= 1; i--) {
                    float layerProgress = i / 20.0F;
                    float layerSize = gSize * layerProgress;
                    float glowAlpha = gIntensity * (1.0F - layerProgress) / 20.0F;
                    Color glowColor = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), (int)(glowAlpha * 255.0F));
                    Renderer.get()
                        .drawRoundedRect(
                            context,
                            x - layerSize * scale,
                            y - layerSize * scale,
                            (float)this.getWidth() * scale + layerSize * 2.0F,
                            (float)this.getHeight() * scale + layerSize * 2.0F,
                            radius + layerSize,
                            glowColor
                        );
                }
            }

            Color bgColor = new Color(0, 0, 0, (int)(this.bgOpacity.getValue() * 255.0));
            if (this.flashOnChange.isEnabled() && this.fpsAnimation > 0.0F) {
                int alpha = bgColor.getAlpha() + (int)(50.0F * this.fpsAnimation);
                bgColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), Math.min(255, alpha));
            }

            if (this.gradientBackground.isEnabled()) {
                Color bgColor2 = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), bgColor.getAlpha() / 2);
                Renderer.get().drawRoundedRectGradient(context, x, y, (float)this.getWidth(), (float)this.getHeight(), radius, bgColor, bgColor2, true);
            } else {
                Renderer.get().drawRoundedRect(context, x, y, (float)this.getWidth(), (float)this.getHeight(), radius, bgColor);
            }

            if (this.showBorder.isEnabled()) {
                float bWidth = (float)this.borderWidth.getValue();
                if (this.pulseOnChange.isEnabled() && this.pulseAnimation > 0.0F) {
                    bWidth += this.pulseAnimation * 2.0F;
                }

                Renderer.get()
                    .drawRoundedRectOutline(
                        context,
                        x,
                        y,
                        (float)this.getWidth(),
                        (float)this.getHeight(),
                        radius,
                        bWidth,
                        new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), 150)
                    );
            }

            Renderer.get().drawCenteredText(context, text, x + (float)this.getWidth() / 2.0F, y + (float)this.getHeight() / 2.0F, fSize, textColor, true);
            Renderer.get().endFrame();
        }
    }
}
