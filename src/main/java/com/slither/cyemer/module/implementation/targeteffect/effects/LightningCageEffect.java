package com.slither.cyemer.module.implementation.targeteffect.effects;

import com.slither.cyemer.module.implementation.targeteffect.BaseEffect;
import com.slither.cyemer.module.implementation.targeteffect.EffectRenderContext;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class LightningCageEffect extends BaseEffect {
    private final Random random = new Random();
    private final List<LightningCageEffect.LightningBolt> activeBolts = new ArrayList<>();
    private long lastSpawnTime = 0L;

    @Override
    public void render(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx) {
        this.renderLightning(matrices, mainBuffer, glowBuffer, ctx, false);
    }

    @Override
    public void renderTextured(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx) {
        this.renderLightning(matrices, mainBuffer, glowBuffer, ctx, true);
    }

    private void renderLightning(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx, boolean textured) {
        long currentTime = ctx.time;
        double height = ctx.player.method_17682();
        double radius = ctx.player.method_17681() * 0.6 * ctx.scaleMultiplier;
        Color color = textured ? ctx.getThemedColor() : ctx.color;
        int spawnInterval = (int)(200.0 / ctx.speedMultiplier);
        if (currentTime - this.lastSpawnTime > spawnInterval) {
            this.spawnNewBolt(radius, height);
            this.lastSpawnTime = currentTime;
        }

        this.activeBolts.removeIf(boltx -> currentTime - boltx.spawnTime > 300L);

        for (LightningCageEffect.LightningBolt bolt : this.activeBolts) {
            long age = currentTime - bolt.spawnTime;
            float ageAlpha = 1.0F - (float)age / 300.0F;
            float alpha = ctx.alpha * ageAlpha * bolt.intensity;
            if (mainBuffer != null) {
                if (textured) {
                    this.renderLightningSegmentsTextured(matrices, mainBuffer, bolt.segments, color, alpha, 0.02 * ctx.scaleMultiplier, ctx);
                } else {
                    this.renderLightningSegments(matrices, mainBuffer, bolt.segments, color, alpha, 0.02 * ctx.scaleMultiplier);
                }
            }

            if (glowBuffer != null) {
                float glowAlpha = alpha * (float)ctx.glowOpacity;
                if (textured) {
                    this.renderLightningSegmentsTextured(matrices, glowBuffer, bolt.segments, color, glowAlpha, 0.04 * ctx.scaleMultiplier, ctx);
                } else {
                    this.renderLightningSegments(matrices, glowBuffer, bolt.segments, color, glowAlpha, 0.04 * ctx.scaleMultiplier);
                }
            }
        }
    }

    private void spawnNewBolt(double radius, double height) {
        double angle1 = this.random.nextDouble() * Math.PI * 2.0;
        double angle2 = this.random.nextDouble() * Math.PI * 2.0;
        double y1 = this.random.nextDouble() * height;
        double y2 = this.random.nextDouble() * height;
        class_243 start = new class_243(Math.cos(angle1) * radius, y1, Math.sin(angle1) * radius);
        class_243 end = new class_243(Math.cos(angle2) * radius, y2, Math.sin(angle2) * radius);
        this.activeBolts.add(new LightningCageEffect.LightningBolt(start, end, System.currentTimeMillis()));
    }

    private void renderLightningSegments(class_4587 matrices, class_4588 buffer, List<class_243> segments, Color color, float alpha, double thickness) {
        Matrix4f matrix = matrices.method_23760().method_23761();
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;

        for (int i = 0; i < segments.size() - 1; i++) {
            class_243 p1 = segments.get(i);
            class_243 p2 = segments.get(i + 1);
            class_243 dir = p2.method_1020(p1).method_1029();
            class_243 perp = new class_243(-dir.field_1350, 0.0, dir.field_1352).method_1029().method_1021(thickness);
            class_243 v1 = p1.method_1019(perp);
            class_243 v2 = p1.method_1020(perp);
            class_243 v3 = p2.method_1020(perp);
            class_243 v4 = p2.method_1019(perp);
            buffer.method_22918(matrix, (float)v1.field_1352, (float)v1.field_1351, (float)v1.field_1350).method_22915(r, g, b, alpha);
            buffer.method_22918(matrix, (float)v2.field_1352, (float)v2.field_1351, (float)v2.field_1350).method_22915(r, g, b, alpha);
            buffer.method_22918(matrix, (float)v3.field_1352, (float)v3.field_1351, (float)v3.field_1350).method_22915(r, g, b, alpha);
            buffer.method_22918(matrix, (float)v1.field_1352, (float)v1.field_1351, (float)v1.field_1350).method_22915(r, g, b, alpha);
            buffer.method_22918(matrix, (float)v3.field_1352, (float)v3.field_1351, (float)v3.field_1350).method_22915(r, g, b, alpha);
            buffer.method_22918(matrix, (float)v4.field_1352, (float)v4.field_1351, (float)v4.field_1350).method_22915(r, g, b, alpha);
        }
    }

    private void renderLightningSegmentsTextured(
        class_4587 matrices, class_4588 buffer, List<class_243> segments, Color color, float alpha, double thickness, EffectRenderContext ctx
    ) {
        Matrix4f matrix = matrices.method_23760().method_23761();
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        double totalLength = 0.0;

        for (int i = 0; i < segments.size() - 1; i++) {
            totalLength += segments.get(i).method_1022(segments.get(i + 1));
        }

        double accumulatedLength = 0.0;

        for (int i = 0; i < segments.size() - 1; i++) {
            class_243 p1 = segments.get(i);
            class_243 p2 = segments.get(i + 1);
            double segmentLength = p1.method_1022(p2);
            class_243 dir = p2.method_1020(p1).method_1029();
            class_243 perp = new class_243(-dir.field_1350, 0.0, dir.field_1352).method_1029().method_1021(thickness);
            class_243 v1 = p1.method_1019(perp);
            class_243 v2 = p1.method_1020(perp);
            class_243 v3 = p2.method_1020(perp);
            class_243 v4 = p2.method_1019(perp);
            float v0 = (float)(accumulatedLength / totalLength) + ctx.uvOffsetV;
            float ev1 = (float)((accumulatedLength + segmentLength) / totalLength) + ctx.uvOffsetV;
            float u0 = 0.0F + ctx.uvOffsetU;
            float u1 = 1.0F + ctx.uvOffsetU;
            accumulatedLength += segmentLength;
            buffer.method_22918(matrix, ev1, ev1, ev1).method_22915(r, g, b, alpha).method_22913(u0, v0);
            buffer.method_22918(matrix, (float)v2.field_1352, (float)v2.field_1351, (float)v2.field_1350).method_22915(r, g, b, alpha).method_22913(u1, v0);
            buffer.method_22918(matrix, (float)v3.field_1352, (float)v3.field_1351, (float)v3.field_1350).method_22915(r, g, b, alpha).method_22913(u1, ev1);
            buffer.method_22918(matrix, ev1, ev1, ev1).method_22915(r, g, b, alpha).method_22913(u0, v0);
            buffer.method_22918(matrix, (float)v3.field_1352, (float)v3.field_1351, (float)v3.field_1350).method_22915(r, g, b, alpha).method_22913(u1, ev1);
            buffer.method_22918(matrix, (float)v4.field_1352, (float)v4.field_1351, (float)v4.field_1350).method_22915(r, g, b, alpha).method_22913(u0, ev1);
        }
    }

    @Environment(EnvType.CLIENT)
    private static class LightningBolt {
        class_243 start;
        class_243 end;
        long spawnTime;
        float intensity;
        List<class_243> segments;

        LightningBolt(class_243 start, class_243 end, long spawnTime) {
            this.start = start;
            this.end = end;
            this.spawnTime = spawnTime;
            this.intensity = 0.8F + (float)Math.random() * 0.2F;
            this.segments = this.generateJaggedPath(start, end, 5);
        }

        private List<class_243> generateJaggedPath(class_243 start, class_243 end, int subdivisions) {
            List<class_243> points = new ArrayList<>();
            points.add(start);

            for (int i = 1; i < subdivisions; i++) {
                double t = (double)i / subdivisions;
                class_243 midpoint = start.method_35590(end, t);
                double offsetMagnitude = 0.1 * (1.0 - Math.abs(t - 0.5) * 2.0);
                class_243 offset = new class_243(
                    (Math.random() - 0.5) * offsetMagnitude, (Math.random() - 0.5) * offsetMagnitude, (Math.random() - 0.5) * offsetMagnitude
                );
                points.add(midpoint.method_1019(offset));
            }

            points.add(end);
            return points;
        }
    }
}
