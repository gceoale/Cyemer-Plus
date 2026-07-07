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
public class ReachHudElement extends HUDElement {
    private final SliderSetting cornerRadius = new SliderSetting("Corner Radius", 8.0, 0.0, 20.0, 0);
    private final SliderSetting bgOpacity = new SliderSetting("BG Opacity", 0.56, 0.0, 1.0, 2);
    private final BooleanSetting gradientBackground = new BooleanSetting("Gradient BG", false);
    private final BooleanSetting showBorder = new BooleanSetting("Show Border", true);
    private final SliderSetting borderWidth = new SliderSetting("Border Width", 1.5, 0.5, 5.0, 1);
    private final SliderSetting fontSize = new SliderSetting("Font Size", 13.0, 8.0, 32.0, 0);
    private final BooleanSetting glowEffect = new BooleanSetting("Glow Effect", false);
    private final BooleanSetting glowOnSuspicious = new BooleanSetting("Glow on Suspicious", true);
    private final SliderSetting glowSize = new SliderSetting("Glow Size", 8.0, 0.0, 20.0, 0);
    private final SliderSetting glowIntensity = new SliderSetting("Glow Intensity", 0.3, 0.0, 1.0, 2);
    private final BooleanSetting colorCodedReach = new BooleanSetting("Color Coded", true);
    private final BooleanSetting pulseOnHit = new BooleanSetting("Pulse on Hit", true);
    private final SliderSetting pulseStrength = new SliderSetting("Pulse Strength", 0.1, 0.0, 0.5, 2);
    private final SliderSetting suspiciousThreshold = new SliderSetting("Suspicious Threshold", 3.0, 2.5, 6.0, 1);
    private final SliderSetting veryHighThreshold = new SliderSetting("Very High Threshold", 3.5, 3.0, 6.0, 1);
    private float pulseAnimation = 0.0F;
    private double lastReach = 0.0;

    public ReachHudElement(String name, double defaultX, double defaultY) {
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

    public BooleanSetting getGlowOnSuspicious() {
        return this.glowOnSuspicious;
    }

    public SliderSetting getGlowSize() {
        return this.glowSize;
    }

    public SliderSetting getGlowIntensity() {
        return this.glowIntensity;
    }

    public BooleanSetting getColorCodedReach() {
        return this.colorCodedReach;
    }

    public BooleanSetting getPulseOnHit() {
        return this.pulseOnHit;
    }

    public SliderSetting getPulseStrength() {
        return this.pulseStrength;
    }

    public SliderSetting getSuspiciousThreshold() {
        return this.suspiciousThreshold;
    }

    public SliderSetting getVeryHighThreshold() {
        return this.veryHighThreshold;
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
        settings.add(this.glowOnSuspicious);
        settings.add(this.glowSize);
        settings.add(this.glowIntensity);
        settings.add(this.colorCodedReach);
        settings.add(this.pulseOnHit);
        settings.add(this.pulseStrength);
        settings.add(this.suspiciousThreshold);
        settings.add(this.veryHighThreshold);
        return settings;
    }

    @Override
    public void render(class_332 context, float delta) {
        class_310 mc = class_310.method_1551();
        double currentReach = HUDManager.getLastHitReach();
        if (currentReach != this.lastReach) {
            if (this.pulseOnHit.isEnabled()) {
                this.pulseAnimation = 1.0F;
            }

            this.lastReach = currentReach;
        }

        this.pulseAnimation = Math.max(0.0F, this.pulseAnimation - delta * 2.0F);
        String text = String.format("Reach: %.2fm", currentReach);
        float fSize = (float)this.fontSize.getValue();
        float textWidth = Renderer.get().getTextWidth(text, fSize);
        this.setWidth(textWidth + 12.0F);
        this.setHeight(fSize + 7.0F);
        int screenWidth = mc.method_22683().method_4486();
        int screenHeight = mc.method_22683().method_4502();
        float pixelRatio = mc.method_22683().method_4495();
        if (Renderer.get().beginFrame(screenWidth, screenHeight, pixelRatio)) {
            float x = (float)this.getX();
            float y = (float)this.getY();
            float radius = (float)this.cornerRadius.getValue();
            Color reachColor = Color.WHITE;
            if (this.colorCodedReach.isEnabled()) {
                if (currentReach >= this.veryHighThreshold.getValue()) {
                    reachColor = new Color(255, 100, 100);
                } else if (currentReach >= this.suspiciousThreshold.getValue()) {
                    reachColor = new Color(255, 200, 100);
                } else {
                    reachColor = new Color(100, 255, 100);
                }
            }

            float scale = 1.0F;
            if (this.pulseOnHit.isEnabled() && this.pulseAnimation > 0.0F) {
                scale = 1.0F + this.pulseAnimation * (float)this.pulseStrength.getValue();
            }

            boolean isSuspicious = currentReach >= this.suspiciousThreshold.getValue();
            if (this.glowEffect.isEnabled() || this.glowOnSuspicious.isEnabled() && isSuspicious) {
                float gSize = (float)this.glowSize.getValue();
                float gIntensity = (float)this.glowIntensity.getValue();

                for (int i = 20; i >= 1; i--) {
                    float layerProgress = i / 20.0F;
                    float layerSize = gSize * layerProgress;
                    float glowAlpha = gIntensity * (1.0F - layerProgress) / 20.0F;
                    Color glowColor = new Color(reachColor.getRed(), reachColor.getGreen(), reachColor.getBlue(), (int)(glowAlpha * 255.0F));
                    Renderer.get()
                        .drawRoundedRect(
                            context,
                            x - layerSize * scale,
                            y - layerSize * scale,
                            (float)this.getWidth() + layerSize * 2.0F * scale,
                            (float)this.getHeight() + layerSize * 2.0F * scale,
                            radius + layerSize,
                            glowColor
                        );
                }
            }

            Color bgColor = new Color(0, 0, 0, (int)(this.bgOpacity.getValue() * 255.0));
            if (this.gradientBackground.isEnabled()) {
                Color bgColor2 = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), bgColor.getAlpha() / 2);
                Renderer.get().drawRoundedRectGradient(context, x, y, (float)this.getWidth(), (float)this.getHeight(), radius, bgColor, bgColor2, true);
            } else {
                Renderer.get().drawRoundedRect(context, x, y, (float)this.getWidth(), (float)this.getHeight(), radius, bgColor);
            }

            if (this.showBorder.isEnabled()) {
                float bWidth = (float)this.borderWidth.getValue();
                if (this.pulseOnHit.isEnabled() && this.pulseAnimation > 0.0F) {
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
                        new Color(reachColor.getRed(), reachColor.getGreen(), reachColor.getBlue(), 150)
                    );
            }

            Renderer.get().drawCenteredText(context, text, x + (float)this.getWidth() / 2.0F, y + (float)this.getHeight() / 2.0F, fSize, reachColor, true);
            Renderer.get().endFrame();
        }
    }
}
