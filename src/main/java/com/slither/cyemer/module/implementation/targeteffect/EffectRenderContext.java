package com.slither.cyemer.module.implementation.targeteffect;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10055;
import net.minecraft.class_1657;
import net.minecraft.class_4597;
import net.minecraft.class_591;

@Environment(EnvType.CLIENT)
public class EffectRenderContext {
    public final class_1657 player;
    public final String effectType;
    public final Color color;
    public final float alpha;
    public final long time;
    public final double speedMultiplier;
    public final double scaleMultiplier;
    public final int segments;
    public final boolean glowEnabled;
    public final double glowSize;
    public final double glowOpacity;
    public final boolean opaqueMode;
    public final TextureTheme theme;
    public final float uvOffsetU;
    public final float uvOffsetV;
    public final class_4597 vertexConsumers;
    public final class_591 playerModel;
    public final class_10055 renderState;
    public final int light;

    public EffectRenderContext(
        class_1657 player,
        String effectType,
        Color color,
        float alpha,
        long time,
        double speedMultiplier,
        double scaleMultiplier,
        int segments,
        boolean glowEnabled,
        double glowSize,
        double glowOpacity,
        boolean opaqueMode,
        TextureTheme theme
    ) {
        this(
            player,
            effectType,
            color,
            alpha,
            time,
            speedMultiplier,
            scaleMultiplier,
            segments,
            glowEnabled,
            glowSize,
            glowOpacity,
            opaqueMode,
            theme,
            null,
            null,
            null,
            0
        );
    }

    public EffectRenderContext(
        class_1657 player,
        String effectType,
        Color color,
        float alpha,
        long time,
        double speedMultiplier,
        double scaleMultiplier,
        int segments,
        boolean glowEnabled,
        double glowSize,
        double glowOpacity,
        boolean opaqueMode,
        TextureTheme theme,
        class_4597 vertexConsumers,
        class_591 playerModel,
        class_10055 renderState,
        int light
    ) {
        this.player = player;
        this.effectType = effectType;
        this.color = color;
        this.alpha = alpha;
        this.time = time;
        this.speedMultiplier = speedMultiplier;
        this.scaleMultiplier = scaleMultiplier;
        this.segments = segments;
        this.glowEnabled = glowEnabled;
        this.glowSize = glowSize;
        this.glowOpacity = glowOpacity;
        this.opaqueMode = opaqueMode;
        this.theme = theme;
        float[] uvOffsets = theme.getAnimatedUVs(time, speedMultiplier);
        this.uvOffsetU = uvOffsets[0];
        this.uvOffsetV = uvOffsets[1];
        this.vertexConsumers = vertexConsumers;
        this.playerModel = playerModel;
        this.renderState = renderState;
        this.light = light;
    }

    public Color getThemedColor() {
        return this.theme.applyThemeTint(this.color);
    }
}
