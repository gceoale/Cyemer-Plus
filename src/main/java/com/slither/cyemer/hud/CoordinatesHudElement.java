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
public class CoordinatesHudElement extends HUDElement {
    private final SliderSetting cornerRadius = new SliderSetting("Corner Radius", 8.0, 0.0, 20.0, 0);
    private final SliderSetting bgOpacity = new SliderSetting("BG Opacity", 0.56, 0.0, 1.0, 2);
    private final BooleanSetting gradientBackground = new BooleanSetting("Gradient BG", true);
    private final BooleanSetting showBorder = new BooleanSetting("Show Border", true);
    private final SliderSetting borderWidth = new SliderSetting("Border Width", 1.5, 0.5, 5.0, 1);
    private final SliderSetting fontSize = new SliderSetting("Font Size", 13.0, 8.0, 32.0, 0);
    private final BooleanSetting glowEffect = new BooleanSetting("Glow Effect", false);
    private final SliderSetting glowSize = new SliderSetting("Glow Size", 8.0, 0.0, 20.0, 0);
    private final SliderSetting glowIntensity = new SliderSetting("Glow Intensity", 0.3, 0.0, 1.0, 2);
    private final BooleanSetting showDecimals = new BooleanSetting("Show Decimals", true);
    private final SliderSetting decimalPlaces = new SliderSetting("Decimal Places", 1.0, 0.0, 3.0, 0);
    private final SliderSetting pulseStrength = new SliderSetting("Pulse Strength", 0.1, 0.0, 0.5, 2);
    private float pulseAnimation = 0.0F;
    private double lastX = 0.0;
    private double lastY = 0.0;
    private double lastZ = 0.0;

    public CoordinatesHudElement(String name, double defaultX, double defaultY) {
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

    public BooleanSetting getShowDecimals() {
        return this.showDecimals;
    }

    public SliderSetting getDecimalPlaces() {
        return this.decimalPlaces;
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
        settings.add(this.showDecimals);
        settings.add(this.decimalPlaces);
        settings.add(this.pulseStrength);
        return settings;
    }

    @Override
    public void render(class_332 context, float delta) {
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 != null) {
            double x = mc.field_1724.method_23317();
            double y = mc.field_1724.method_23318();
            double z = mc.field_1724.method_23321();
            this.lastX = x;
            this.lastY = y;
            this.lastZ = z;
            this.pulseAnimation = Math.max(0.0F, this.pulseAnimation - delta * 2.0F);
            String format = this.showDecimals.isEnabled()
                ? "XYZ: %."
                    + (int)this.decimalPlaces.getValue()
                    + "f, %."
                    + (int)this.decimalPlaces.getValue()
                    + "f, %."
                    + (int)this.decimalPlaces.getValue()
                    + "f"
                : "XYZ: %.0f, %.0f, %.0f";
            String text = String.format(format, x, y, z);
            float fSize = (float)this.fontSize.getValue();
            float textWidth = Renderer.get().getTextWidth(text, fSize);
            this.setWidth(textWidth + 12.0F);
            this.setHeight(fSize + 7.0F);
            int screenWidth = mc.method_22683().method_4486();
            int screenHeight = mc.method_22683().method_4502();
            float pixelRatio = mc.method_22683().method_4495();
            if (Renderer.get().beginFrame(screenWidth, screenHeight, pixelRatio)) {
                float posX = (float)this.getX();
                float posY = (float)this.getY();
                float radius = (float)this.cornerRadius.getValue();
                Color bgColor = new Color(0, 0, 0, (int)(this.bgOpacity.getValue() * 255.0));
                Color textColor = new Color(200, 200, 255);
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
                                posX - layerSize,
                                posY - layerSize,
                                (float)this.getWidth() + layerSize * 2.0F,
                                (float)this.getHeight() + layerSize * 2.0F,
                                radius + layerSize,
                                glowColor
                            );
                    }
                }

                if (this.gradientBackground.isEnabled()) {
                    Color bgColor2 = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), bgColor.getAlpha() / 2);
                    Renderer.get()
                        .drawRoundedRectGradient(context, posX, posY, (float)this.getWidth(), (float)this.getHeight(), radius, bgColor, bgColor2, true);
                } else {
                    Renderer.get().drawRoundedRect(context, posX, posY, (float)this.getWidth(), (float)this.getHeight(), radius, bgColor);
                }

                if (this.showBorder.isEnabled()) {
                    float bWidth = (float)this.borderWidth.getValue();
                    Renderer.get()
                        .drawRoundedRectOutline(
                            context, posX, posY, (float)this.getWidth(), (float)this.getHeight(), radius, bWidth, new Color(255, 255, 255, 100)
                        );
                }

                Renderer.get()
                    .drawCenteredText(context, text, posX + (float)this.getWidth() / 2.0F, posY + (float)this.getHeight() / 2.0F, fSize, textColor, true);
                Renderer.get().endFrame();
            }
        }
    }
}
