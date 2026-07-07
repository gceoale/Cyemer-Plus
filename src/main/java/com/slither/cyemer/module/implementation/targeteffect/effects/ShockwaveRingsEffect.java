package com.slither.cyemer.module.implementation.targeteffect.effects;

import com.slither.cyemer.module.implementation.targeteffect.BaseEffect;
import com.slither.cyemer.module.implementation.targeteffect.EffectRenderContext;
import com.slither.cyemer.module.implementation.targeteffect.RenderHelpers;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_4587;
import net.minecraft.class_4588;

@Environment(EnvType.CLIENT)
public class ShockwaveRingsEffect extends BaseEffect {
    private final List<ShockwaveRingsEffect.Shockwave> activeWaves = new ArrayList<>();
    private long lastSpawnTime = 0L;

    @Override
    public void render(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx) {
        this.renderShockwave(matrices, mainBuffer, glowBuffer, ctx, false);
    }

    @Override
    public void renderTextured(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx) {
        this.renderShockwave(matrices, mainBuffer, glowBuffer, ctx, true);
    }

    private void renderShockwave(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx, boolean textured) {
        long currentTime = ctx.time;
        double maxRadius = ctx.player.method_17681() * 3.0 * ctx.scaleMultiplier;
        Color color = textured ? ctx.getThemedColor() : ctx.color;
        int spawnInterval = (int)(800.0 / ctx.speedMultiplier);
        if (currentTime - this.lastSpawnTime > spawnInterval) {
            this.activeWaves.add(new ShockwaveRingsEffect.Shockwave(currentTime));
            this.lastSpawnTime = currentTime;
        }

        this.activeWaves.removeIf(wavex -> wavex.isDead(currentTime, maxRadius));
        matrices.method_22903();
        matrices.method_22904(0.0, 0.05, 0.0);

        for (ShockwaveRingsEffect.Shockwave wave : this.activeWaves) {
            double radius = wave.getRadius(currentTime, ctx.speedMultiplier);
            float alpha = wave.getAlpha(currentTime, maxRadius) * ctx.alpha;
            double thickness = 0.08 * ctx.scaleMultiplier;
            double pulse = Math.sin(currentTime / 100.0 + wave.spawnTime) * 0.2 + 1.0;
            thickness *= pulse;
            if (mainBuffer != null) {
                if (textured) {
                    RenderHelpers.renderTorusTextured(
                        matrices, mainBuffer, radius, thickness, 0.0, color, alpha, Math.max(16, ctx.segments), 8, ctx.uvOffsetU, ctx.uvOffsetV
                    );
                } else {
                    RenderHelpers.renderTorusOptimized(matrices, mainBuffer, radius, thickness, 0.0, color, alpha, Math.max(16, ctx.segments), 8);
                }
            }

            if (glowBuffer != null) {
                double glowThickness = thickness + ctx.glowSize;
                float glowAlpha = alpha * (float)ctx.glowOpacity;
                if (textured) {
                    RenderHelpers.renderTorusTextured(
                        matrices, glowBuffer, radius, glowThickness, 0.0, color, glowAlpha, Math.max(12, ctx.segments / 2), 6, ctx.uvOffsetU, ctx.uvOffsetV
                    );
                } else {
                    RenderHelpers.renderTorusOptimized(matrices, glowBuffer, radius, glowThickness, 0.0, color, glowAlpha, Math.max(12, ctx.segments / 2), 6);
                }
            }
        }

        matrices.method_22909();
        if (ctx.time % 100L < 50L) {
            this.renderGroundGlow(matrices, mainBuffer, glowBuffer, ctx, textured, color);
        }
    }

    private void renderGroundGlow(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx, boolean textured, Color color) {
        double radius = ctx.player.method_17681() * 0.4 * ctx.scaleMultiplier;
        double pulse = Math.sin(ctx.time / 200.0) * 0.5 + 0.5;
        float glowAlpha = ctx.alpha * (float)pulse * 0.3F;
        matrices.method_22903();
        matrices.method_22904(0.0, 0.01, 0.0);
        if (mainBuffer != null) {
            if (textured) {
                RenderHelpers.renderTorusTextured(
                    matrices, mainBuffer, radius, 0.05 * ctx.scaleMultiplier, 0.0, color, glowAlpha, 24, 6, ctx.uvOffsetU, ctx.uvOffsetV
                );
            } else {
                RenderHelpers.renderTorusOptimized(matrices, mainBuffer, radius, 0.05 * ctx.scaleMultiplier, 0.0, color, glowAlpha, 24, 6);
            }
        }

        if (glowBuffer != null) {
            if (textured) {
                RenderHelpers.renderTorusTextured(
                    matrices,
                    glowBuffer,
                    radius * 1.2,
                    0.08 * ctx.scaleMultiplier,
                    0.0,
                    color,
                    glowAlpha * (float)ctx.glowOpacity,
                    16,
                    4,
                    ctx.uvOffsetU,
                    ctx.uvOffsetV
                );
            } else {
                RenderHelpers.renderTorusOptimized(
                    matrices, glowBuffer, radius * 1.2, 0.08 * ctx.scaleMultiplier, 0.0, color, glowAlpha * (float)ctx.glowOpacity, 16, 4
                );
            }
        }

        matrices.method_22909();
    }

    @Environment(EnvType.CLIENT)
    private static class Shockwave {
        long spawnTime;
        double initialRadius;

        Shockwave(long spawnTime) {
            this.spawnTime = spawnTime;
            this.initialRadius = 0.0;
        }

        double getRadius(long currentTime, double speed) {
            double age = (currentTime - this.spawnTime) / 1000.0;
            return this.initialRadius + age * 2.0 * speed;
        }

        float getAlpha(long currentTime, double maxRadius) {
            double radius = this.getRadius(currentTime, 1.0);
            return (float)Math.max(0.0, 1.0 - radius / maxRadius);
        }

        boolean isDead(long currentTime, double maxRadius) {
            return this.getRadius(currentTime, 1.0) > maxRadius;
        }
    }
}
