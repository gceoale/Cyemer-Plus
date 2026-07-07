package com.slither.cyemer.hud;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Setting;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10799;
import net.minecraft.class_1291;
import net.minecraft.class_1293;
import net.minecraft.class_1294;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_6880;
import net.minecraft.class_7923;

@Environment(EnvType.CLIENT)
public class EffectHudElement extends HUDElement {
    private final BooleanSetting showIcon = new BooleanSetting("Show Icon", true);
    private final ModeSetting timeFormat = new ModeSetting("Time Format", "Minutes:Seconds", "Minutes:Seconds", "Seconds Only", "Both");
    private final BooleanSetting showProgressBar = new BooleanSetting("Show Progress Bar", true);
    private final BooleanSetting sortByDuration = new BooleanSetting("Sort by Duration", true);
    private final SliderSetting bgOpacity = new SliderSetting("BG Opacity", 0.56, 0.0, 1.0, 2);
    private final SliderSetting cornerRadius = new SliderSetting("Corner Radius", 8.0, 0.0, 20.0, 1);
    private final SliderSetting spacing = new SliderSetting("Spacing", 4.0, 0.0, 15.0, 1);
    private final SliderSetting iconSize = new SliderSetting("Icon Size", 18.0, 12.0, 32.0, 0);
    private final BooleanSetting glowEffect = new BooleanSetting("Glow Effect", true);
    private final SliderSetting glowSize = new SliderSetting("Glow Size", 8.0, 0.0, 20.0, 1);
    private final SliderSetting glowIntensity = new SliderSetting("Glow Intensity", 0.5, 0.0, 1.0, 2);
    private final BooleanSetting slideAnimation = new BooleanSetting("Slide Animation", true);
    private final BooleanSetting fadeAnimation = new BooleanSetting("Fade Animation", true);
    private final BooleanSetting pulseOnLowTime = new BooleanSetting("Pulse on Low Time", true);
    private final SliderSetting lowTimeThreshold = new SliderSetting("Low Time Threshold", 10.0, 5.0, 30.0, 0);
    private final SliderSetting progressBarHeight = new SliderSetting("Progress Bar Height", 3.0, 1.0, 8.0, 0);
    private final BooleanSetting coloredProgressBar = new BooleanSetting("Colored Progress Bar", true);
    private final List<EffectHudElement.EffectEntry> effectEntries = new ArrayList<>();
    private long lastUpdateTime = System.currentTimeMillis();

    public EffectHudElement(String name, double defaultX, double defaultY) {
        super(name, defaultX, defaultY);
    }

    @Override
    public List<Setting> getSettings() {
        List<Setting> settings = new ArrayList<>();
        settings.add(this.showIcon);
        settings.add(this.timeFormat);
        settings.add(this.showProgressBar);
        settings.add(this.sortByDuration);
        settings.add(this.bgOpacity);
        settings.add(this.cornerRadius);
        settings.add(this.spacing);
        settings.add(this.iconSize);
        settings.add(this.glowEffect);
        settings.add(this.glowSize);
        settings.add(this.glowIntensity);
        settings.add(this.slideAnimation);
        settings.add(this.fadeAnimation);
        settings.add(this.pulseOnLowTime);
        settings.add(this.lowTimeThreshold);
        settings.add(this.progressBarHeight);
        settings.add(this.coloredProgressBar);
        return settings;
    }

    @Override
    public void render(class_332 context, float delta) {
        class_310 mc = class_310.method_1551();
        if (mc.field_1755 != null) {
            this.renderPlaceholder(context, mc);
        } else if (mc.field_1724 == null) {
            this.setWidth(0.0);
            this.setHeight(0.0);
        } else {
            Collection<class_1293> activeEffects = mc.field_1724.method_6026();
            if (activeEffects.isEmpty()) {
                this.setWidth(0.0);
                this.setHeight(0.0);
                this.effectEntries.clear();
            } else {
                this.updateEffectEntries(activeEffects, delta);
                this.renderEffects(context, mc, delta);
            }
        }
    }

    private void updateEffectEntries(Collection<class_1293> activeEffects, float delta) {
        long currentTime = System.currentTimeMillis();
        float rawDeltaTime = (float)(currentTime - this.lastUpdateTime) / 1000.0F;
        float deltaTime = Math.min(rawDeltaTime, 0.1F);
        this.lastUpdateTime = currentTime;
        List<class_1293> sortedEffects = new ArrayList<>(activeEffects);
        if (this.sortByDuration.isEnabled()) {
            sortedEffects.sort((a, b) -> Integer.compare(a.method_5584(), b.method_5584()));
        }

        List<EffectHudElement.EffectEntry> newEntries = new ArrayList<>();

        for (class_1293 effect : sortedEffects) {
            EffectHudElement.EffectEntry existing = this.findEntry(effect);
            if (existing != null) {
                existing.effect = effect;
                existing.update(deltaTime);
                newEntries.add(existing);
            } else {
                EffectHudElement.EffectEntry newEntry = new EffectHudElement.EffectEntry(effect);
                newEntry.update(deltaTime);
                newEntries.add(newEntry);
            }
        }

        for (EffectHudElement.EffectEntry entry : this.effectEntries) {
            if (!newEntries.contains(entry)) {
                entry.fadeOut = true;
            }
        }

        this.effectEntries.removeIf(entryx -> {
            if (entryx.fadeOut) {
                entryx.update(deltaTime);
                return entryx.fadeAlpha <= 0.0F;
            } else {
                return false;
            }
        });
        this.effectEntries.clear();
        this.effectEntries.addAll(newEntries);
    }

    private EffectHudElement.EffectEntry findEntry(class_1293 effect) {
        for (EffectHudElement.EffectEntry entry : this.effectEntries) {
            if (entry.effect.method_5579().equals(effect.method_5579())) {
                return entry;
            }
        }

        return null;
    }

    private void renderEffects(class_332 context, class_310 mc, float delta) {
        if (this.effectEntries.isEmpty()) {
            this.setWidth(0.0);
            this.setHeight(0.0);
        } else {
            int padding = 6;
            int iconSizePx = (int)this.iconSize.getValue();
            int spacingPx = (int)this.spacing.getValue();
            int progressHeight = (int)this.progressBarHeight.getValue();
            float fontSize = 13.0F;
            int gap = 6;
            int maxTextWidth = 0;

            for (EffectHudElement.EffectEntry entry : this.effectEntries) {
                String effectName = ((class_1291)entry.effect.method_5579().comp_349()).method_5560().getString();
                String timeStr = this.formatTime(entry.effect);
                String displayText = effectName + " " + timeStr;
                int textWidth = (int)Renderer.get().getTextWidth(displayText, 13.0F);
                maxTextWidth = Math.max(maxTextWidth, textWidth);
            }

            int contentWidth = maxTextWidth;
            if (this.showIcon.isEnabled()) {
                contentWidth = maxTextWidth + iconSizePx + 6;
            }

            int singleEffectHeight = Math.max(iconSizePx, 15);
            if (this.showProgressBar.isEnabled()) {
                singleEffectHeight += progressHeight + 2;
            }

            int totalWidth = contentWidth + 12;
            int totalHeight = this.effectEntries.size() * singleEffectHeight + (this.effectEntries.size() - 1) * spacingPx + 12;
            this.setWidth(totalWidth);
            this.setHeight(totalHeight);
            int screenWidth = mc.method_22683().method_4486();
            int screenHeight = mc.method_22683().method_4502();
            float pixelRatio = mc.method_22683().method_4495();
            float baseX = (float)this.getX();
            float baseY = (float)this.getY();
            if (Renderer.get().beginFrame(screenWidth, screenHeight, pixelRatio)) {
                try {
                    Color bgColor = new Color(0, 0, 0, (int)(this.bgOpacity.getValue() * 255.0));
                    float radius = (float)this.cornerRadius.getValue();
                    if (this.glowEffect.isEnabled()) {
                        int numGlowLayers = 6;

                        for (int i = numGlowLayers; i >= 1; i--) {
                            float layerProgress = (float)i / numGlowLayers;
                            float layerSize = (float)this.glowSize.getValue() * layerProgress;
                            float glowAlpha = (float)this.glowIntensity.getValue() * (1.0F - layerProgress) / numGlowLayers;
                            Color glowColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), (int)(glowAlpha * 255.0F));
                            Renderer.get()
                                .drawRoundedRect(
                                    context,
                                    baseX - layerSize,
                                    baseY - layerSize,
                                    totalWidth + layerSize * 2.0F,
                                    totalHeight + layerSize * 2.0F,
                                    radius + layerSize,
                                    glowColor
                                );
                        }
                    }

                    Renderer.get().drawRoundedRect(context, baseX, baseY, totalWidth, totalHeight, radius, bgColor);
                    Renderer.get().drawRoundedRectOutline(context, baseX, baseY, totalWidth, totalHeight, radius, 2.0F, new Color(255, 255, 255, 80));
                    float currentY = baseY + 6.0F;

                    for (EffectHudElement.EffectEntry entry : this.effectEntries) {
                        this.renderSingleEffect(
                            context, entry, baseX + 6.0F, currentY, contentWidth, singleEffectHeight, iconSizePx, 6, 13.0F, progressHeight, mc
                        );
                        currentY += singleEffectHeight + spacingPx;
                    }
                } finally {
                    Renderer.get().endFrame();
                }

                if (this.showIcon.isEnabled()) {
                    float currentY = baseY + 6.0F;

                    for (EffectHudElement.EffectEntry entry : this.effectEntries) {
                        float slideOffset = this.slideAnimation.isEnabled() ? entry.slideOffset : 0.0F;
                        float iconX = baseX + 6.0F + slideOffset;
                        class_2960 effectId = class_7923.field_41174.method_10221((class_1291)entry.effect.method_5579().comp_349());
                        if (effectId != null) {
                            class_2960 iconTexture = class_2960.method_60655(effectId.method_12836(), "textures/mob_effect/" + effectId.method_12832() + ".png");
                            context.method_25302(
                                class_10799.field_56883, iconTexture, (int)iconX, (int)currentY, 0.0F, 0.0F, iconSizePx, iconSizePx, 18, 18, 18, 18
                            );
                        }

                        currentY += singleEffectHeight + spacingPx;
                    }
                }
            }
        }
    }

    private void renderSingleEffect(
        class_332 context,
        EffectHudElement.EffectEntry entry,
        float x,
        float y,
        int width,
        int height,
        int iconSizePx,
        int gap,
        float fontSize,
        int progressHeight,
        class_310 mc
    ) {
        float slideOffset = this.slideAnimation.isEnabled() ? entry.slideOffset : 0.0F;
        float fadeAlpha = this.fadeAnimation.isEnabled() ? entry.fadeAlpha : 1.0F;
        float textX = x + slideOffset;
        if (this.showIcon.isEnabled()) {
            textX += iconSizePx + gap;
        }

        float textY = y + 2.0F;
        String effectName = ((class_1291)entry.effect.method_5579().comp_349()).method_5560().getString();
        int amplifier = entry.effect.method_5578();
        if (amplifier > 0) {
            effectName = effectName + " " + this.toRomanNumeral(amplifier + 1);
        }

        String timeStr = this.formatTime(entry.effect);
        String displayText = effectName + " " + timeStr;
        boolean isLowTime = entry.effect.method_5584() <= this.lowTimeThreshold.getValue() * 20.0;
        float pulseValue = isLowTime && this.pulseOnLowTime.isEnabled() ? entry.pulseAnimation : 0.0F;
        int textAlpha = (int)(255.0F * fadeAlpha);
        int pulseBoost = (int)(50.0F * pulseValue);
        Color textColor = new Color(Math.min(255, 255 + pulseBoost), Math.max(0, 255 - pulseBoost), Math.max(0, 255 - pulseBoost), textAlpha);
        Renderer.get().drawText(context, displayText, textX, textY, fontSize, textColor, true);
        if (this.showProgressBar.isEnabled()) {
            float barY = y + height - progressHeight - 2.0F;
            float barWidth = width;
            if (!entry.hasInitialDuration) {
                entry.initialMaxDuration = entry.effect.method_5584();
                entry.hasInitialDuration = true;
            }

            float progress = Math.max(0.0F, Math.min(1.0F, entry.smoothProgress));
            Color barBgColor = new Color(50, 50, 50, (int)(150.0F * fadeAlpha));
            Renderer.get().drawRect(context, textX - (this.showIcon.isEnabled() ? iconSizePx + gap : 0), barY, barWidth, progressHeight, barBgColor);
            Color barColor;
            if (this.coloredProgressBar.isEnabled()) {
                if (((class_1291)entry.effect.method_5579().comp_349()).method_5573()) {
                    barColor = new Color(50, 255, 50, (int)(200.0F * fadeAlpha));
                } else {
                    barColor = new Color(255, 50, 50, (int)(200.0F * fadeAlpha));
                }

                if (isLowTime && this.pulseOnLowTime.isEnabled()) {
                    int brightness = (int)(255.0F * (0.7F + 0.3F * pulseValue));
                    barColor = new Color(brightness, 50, 50, (int)(200.0F * fadeAlpha));
                }
            } else {
                barColor = new Color(255, 255, 255, (int)(200.0F * fadeAlpha));
            }

            float filledWidth = barWidth * progress;
            Renderer.get().drawRect(context, textX - (this.showIcon.isEnabled() ? iconSizePx + gap : 0), barY, filledWidth, progressHeight, barColor);
        }
    }

    private String formatTime(class_1293 effect) {
        int ticks = effect.method_5584();
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds %= 60;
        String mode = this.timeFormat.getCurrentMode();
        switch (mode) {
            case "Minutes:Seconds":
                return String.format("%d:%02d", minutes, seconds);
            case "Seconds Only":
                return String.format("%ds", ticks / 20);
            case "Both":
                return String.format("%d:%02d (%ds)", minutes, seconds, ticks / 20);
            default:
                return String.format("%d:%02d", minutes, seconds);
        }
    }

    private String toRomanNumeral(int number) {
        switch (number) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            case 5:
                return "V";
            case 6:
                return "VI";
            case 7:
                return "VII";
            case 8:
                return "VIII";
            case 9:
                return "IX";
            case 10:
                return "X";
            default:
                return String.valueOf(number);
        }
    }

    private void renderPlaceholder(class_332 context, class_310 mc) {
        int padding = 6;
        int iconSizePx = (int)this.iconSize.getValue();
        int gap = 6;
        float fontSize = 13.0F;
        int spacingPx = (int)this.spacing.getValue();
        int progressHeight = (int)this.progressBarHeight.getValue();
        String[] sampleEffects = new String[]{"Speed II 1:23", "Regeneration 0:45", "Strength 2:10"};
        int maxTextWidth = 0;

        for (String effect : sampleEffects) {
            int textWidth = (int)Renderer.get().getTextWidth(effect, 13.0F);
            maxTextWidth = Math.max(maxTextWidth, textWidth);
        }

        int contentWidth = maxTextWidth;
        if (this.showIcon.isEnabled()) {
            contentWidth = maxTextWidth + iconSizePx + 6;
        }

        int singleEffectHeight = Math.max(iconSizePx, 15);
        if (this.showProgressBar.isEnabled()) {
            singleEffectHeight += progressHeight + 2;
        }

        int totalWidth = contentWidth + 12;
        int totalHeight = sampleEffects.length * singleEffectHeight + (sampleEffects.length - 1) * spacingPx + 12;
        this.setWidth(totalWidth);
        this.setHeight(totalHeight);
        int screenWidth = mc.method_22683().method_4486();
        int screenHeight = mc.method_22683().method_4502();
        float pixelRatio = mc.method_22683().method_4495();
        float baseX = (float)this.getX();
        float baseY = (float)this.getY();
        if (Renderer.get().beginFrame(screenWidth, screenHeight, pixelRatio)) {
            try {
                Color bgColor = new Color(0, 0, 0, (int)(this.bgOpacity.getValue() * 255.0));
                float radius = (float)this.cornerRadius.getValue();
                if (this.glowEffect.isEnabled()) {
                    int numGlowLayers = 12;

                    for (int i = numGlowLayers; i >= 1; i--) {
                        float layerProgress = (float)i / numGlowLayers;
                        float layerSize = (float)this.glowSize.getValue() * layerProgress;
                        float glowAlpha = (float)this.glowIntensity.getValue() * (1.0F - layerProgress) / numGlowLayers;
                        Color glowColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), (int)(glowAlpha * 255.0F));
                        Renderer.get()
                            .drawRoundedRect(
                                context,
                                baseX - layerSize,
                                baseY - layerSize,
                                totalWidth + layerSize * 2.0F,
                                totalHeight + layerSize * 2.0F,
                                radius + layerSize,
                                glowColor
                            );
                    }
                }

                Renderer.get().drawRoundedRect(context, baseX, baseY, totalWidth, totalHeight, radius, bgColor);
                Renderer.get().drawRoundedRectOutline(context, baseX, baseY, totalWidth, totalHeight, radius, 2.0F, new Color(255, 255, 255, 80));
                float currentY = baseY + 6.0F;
                float[] progressValues = new float[]{0.7F, 0.3F, 0.85F};
                Color[] effectColors = new Color[]{new Color(50, 255, 50, 200), new Color(50, 255, 50, 200), new Color(255, 50, 50, 200)};

                for (int i = 0; i < sampleEffects.length; i++) {
                    float textX = baseX + 6.0F;
                    if (this.showIcon.isEnabled()) {
                        textX += iconSizePx + 6;
                    }

                    float textY = currentY + 2.0F;
                    Renderer.get().drawText(context, sampleEffects[i], textX, textY, 13.0F, Color.WHITE, true);
                    if (this.showProgressBar.isEnabled()) {
                        float barY = currentY + singleEffectHeight - progressHeight - 2.0F;
                        float barWidth = contentWidth;
                        float barX = baseX + 6.0F;
                        Renderer.get().drawRect(context, barX, barY, barWidth, progressHeight, new Color(50, 50, 50, 150));
                        float filledWidth = barWidth * progressValues[i];
                        Color barColor = this.coloredProgressBar.isEnabled() ? effectColors[i] : new Color(255, 255, 255, 200);
                        Renderer.get().drawRect(context, barX, barY, filledWidth, progressHeight, barColor);
                    }

                    currentY += singleEffectHeight + spacingPx;
                }
            } finally {
                Renderer.get().endFrame();
            }

            if (this.showIcon.isEnabled() && mc.field_1724 != null) {
                float currentY = baseY + 6.0F;
                class_6880[] effects = new class_6880[]{class_1294.field_5904, class_1294.field_5924, class_1294.field_5910};

                for (Object obj : effects) {
                    class_6880<class_1291> effect = (class_6880<class_1291>)obj;
                    class_2960 effectId = class_7923.field_41174.method_10221((class_1291)effect.comp_349());
                    if (effectId != null) {
                        class_2960 iconTexture = class_2960.method_60655(effectId.method_12836(), "textures/mob_effect/" + effectId.method_12832() + ".png");
                        context.method_25302(
                            class_10799.field_56883, iconTexture, (int)(baseX + 6.0F), (int)currentY, 0.0F, 0.0F, iconSizePx, iconSizePx, 18, 18, 18, 18
                        );
                    }

                    currentY += singleEffectHeight + spacingPx;
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private class EffectEntry {
        class_1293 effect;
        float slideOffset = -50.0F;
        float fadeAlpha = 0.0F;
        float pulseAnimation = 0.0F;
        boolean fadeOut = false;
        float pulseDirection = 1.0F;
        int initialMaxDuration = 0;
        boolean hasInitialDuration = false;
        float targetY = 0.0F;
        float currentY = 0.0F;
        float smoothProgress = 1.0F;

        public EffectEntry(class_1293 effect) {
            this.effect = effect;
        }

        public void update(float deltaTime) {
            if (EffectHudElement.this.slideAnimation.isEnabled() && !this.fadeOut) {
                this.slideOffset = this.slideOffset + (0.0F - this.slideOffset) * deltaTime * 8.0F;
                if (Math.abs(this.slideOffset) < 0.5F) {
                    this.slideOffset = 0.0F;
                }
            }

            if (EffectHudElement.this.fadeAnimation.isEnabled()) {
                if (this.fadeOut) {
                    this.fadeAlpha -= deltaTime * 3.0F;
                    if (this.fadeAlpha < 0.0F) {
                        this.fadeAlpha = 0.0F;
                    }
                } else {
                    this.fadeAlpha += deltaTime * 4.0F;
                    if (this.fadeAlpha > 1.0F) {
                        this.fadeAlpha = 1.0F;
                    }
                }
            } else {
                this.fadeAlpha = this.fadeOut ? 0.0F : 1.0F;
            }

            this.currentY = this.currentY + (this.targetY - this.currentY) * deltaTime * 10.0F;
            if (Math.abs(this.targetY - this.currentY) < 0.5F) {
                this.currentY = this.targetY;
            }

            int maxDuration = ((class_1291)this.effect.method_5579().comp_349()).method_5561() ? 1 : this.initialMaxDuration;
            if (maxDuration > 0) {
                float actualProgress = (float)this.effect.method_5584() / maxDuration;
                this.smoothProgress = this.smoothProgress + (actualProgress - this.smoothProgress) * deltaTime * 5.0F;
                if (Math.abs(actualProgress - this.smoothProgress) < 0.01F) {
                    this.smoothProgress = actualProgress;
                }
            }

            if (EffectHudElement.this.pulseOnLowTime.isEnabled() && this.effect.method_5584() <= EffectHudElement.this.lowTimeThreshold.getValue() * 20.0) {
                this.pulseAnimation = this.pulseAnimation + this.pulseDirection * deltaTime * 3.0F;
                if (this.pulseAnimation >= 1.0F) {
                    this.pulseAnimation = 1.0F;
                    this.pulseDirection = -1.0F;
                } else if (this.pulseAnimation <= 0.0F) {
                    this.pulseAnimation = 0.0F;
                    this.pulseDirection = 1.0F;
                }
            } else {
                this.pulseAnimation = 0.0F;
                this.pulseDirection = 1.0F;
            }
        }
    }
}
