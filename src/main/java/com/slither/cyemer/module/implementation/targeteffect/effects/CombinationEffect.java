package com.slither.cyemer.module.implementation.targeteffect.effects;

import com.slither.cyemer.module.implementation.targeteffect.BaseEffect;
import com.slither.cyemer.module.implementation.targeteffect.EffectRenderContext;
import com.slither.cyemer.module.implementation.targeteffect.RenderHelpers;
import com.slither.cyemer.util.render.TexturedRenderUtils;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class CombinationEffect extends BaseEffect {
    private final ShockwaveRingsEffect shockwave = new ShockwaveRingsEffect();
    private final ScanLinesEffect scanLines = new ScanLinesEffect();
    private final GalaxyEffect galaxy = new GalaxyEffect();

    @Override
    public void render(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx) {
        this.renderCombination(matrices, mainBuffer, glowBuffer, ctx, false);
    }

    @Override
    public void renderTextured(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx) {
        this.renderCombination(matrices, mainBuffer, glowBuffer, ctx, true);
    }

    private void renderCombination(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx, boolean textured) {
        matrices.method_22903();
        if (textured) {
            this.shockwave.renderTextured(matrices, mainBuffer, glowBuffer, ctx);
        } else {
            this.shockwave.render(matrices, mainBuffer, glowBuffer, ctx);
        }

        matrices.method_22909();
        matrices.method_22903();
        EffectRenderContext scanCtx = new EffectRenderContext(
            ctx.player,
            ctx.effectType,
            ctx.color,
            ctx.alpha * 0.5F,
            ctx.time,
            ctx.speedMultiplier,
            ctx.scaleMultiplier * 0.8,
            ctx.segments,
            ctx.glowEnabled,
            ctx.glowSize,
            ctx.glowOpacity,
            ctx.opaqueMode,
            ctx.theme
        );
        if (textured) {
            this.scanLines.renderTextured(matrices, mainBuffer, glowBuffer, scanCtx);
        } else {
            this.scanLines.render(matrices, mainBuffer, glowBuffer, scanCtx);
        }

        matrices.method_22909();
        matrices.method_22903();
        EffectRenderContext galaxyCtx = new EffectRenderContext(
            ctx.player,
            ctx.effectType,
            ctx.color,
            ctx.alpha * 0.7F,
            ctx.time,
            ctx.speedMultiplier * 1.5,
            ctx.scaleMultiplier,
            ctx.segments,
            ctx.glowEnabled,
            ctx.glowSize,
            ctx.glowOpacity,
            ctx.opaqueMode,
            ctx.theme
        );
        if (textured) {
            this.galaxy.renderTextured(matrices, mainBuffer, glowBuffer, galaxyCtx);
        } else {
            this.galaxy.render(matrices, mainBuffer, glowBuffer, galaxyCtx);
        }

        matrices.method_22909();
        matrices.method_22903();
        this.renderCoreSphere(matrices, mainBuffer, glowBuffer, ctx, textured);
        matrices.method_22909();
        matrices.method_22903();
        this.renderEnergyPillars(matrices, mainBuffer, glowBuffer, ctx, textured);
        matrices.method_22909();
    }

    private void renderCoreSphere(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx, boolean textured) {
        double torsoHeight = ctx.player.method_17682() / 2.0;
        double pulse = Math.sin(ctx.time / 300.0) * 0.3 + 0.7;
        double radius = 0.2 * ctx.scaleMultiplier * pulse;
        Color color = textured ? ctx.getThemedColor() : ctx.color;
        matrices.method_22903();
        matrices.method_22904(0.0, torsoHeight, 0.0);
        if (mainBuffer != null) {
            if (textured) {
                TexturedRenderUtils.drawTexturedSphere(
                    mainBuffer,
                    matrices.method_23760().method_23761(),
                    class_243.field_1353,
                    radius,
                    color,
                    ctx.alpha * 0.8F,
                    Math.max(8, ctx.segments / 4),
                    ctx.uvOffsetU,
                    ctx.uvOffsetV
                );
            } else {
                RenderHelpers.renderSphereOptimized(matrices, mainBuffer, radius, color, ctx.alpha * 0.8F, Math.max(8, ctx.segments / 4));
            }
        }

        if (glowBuffer != null) {
            double glowRadius = radius * 1.5;
            if (textured) {
                TexturedRenderUtils.drawTexturedSphere(
                    glowBuffer,
                    matrices.method_23760().method_23761(),
                    class_243.field_1353,
                    glowRadius,
                    color,
                    ctx.alpha * (float)ctx.glowOpacity * 0.6F,
                    Math.max(6, ctx.segments / 6),
                    ctx.uvOffsetU,
                    ctx.uvOffsetV
                );
            } else {
                RenderHelpers.renderSphereOptimized(
                    matrices, glowBuffer, glowRadius, color, ctx.alpha * (float)ctx.glowOpacity * 0.6F, Math.max(6, ctx.segments / 6)
                );
            }
        }

        matrices.method_22909();
    }

    private void renderEnergyPillars(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx, boolean textured) {
        double height = ctx.player.method_17682();
        double radius = ctx.player.method_17681() * 0.6 * ctx.scaleMultiplier;
        double pillarRadius = 0.03 * ctx.scaleMultiplier;
        int pillarCount = 4;
        Color color = textured ? ctx.getThemedColor() : ctx.color;

        for (int i = 0; i < pillarCount; i++) {
            double angle = (Math.PI * 2) * i / pillarCount + ctx.time / 1000.0;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            matrices.method_22903();
            matrices.method_22904(x, 0.0, z);
            if (mainBuffer != null) {
                if (textured) {
                    this.renderVerticalCylinderTextured(matrices, mainBuffer, pillarRadius, height, color, ctx.alpha * 0.6F, 8, ctx);
                } else {
                    this.renderVerticalCylinder(matrices, mainBuffer, pillarRadius, height, color, ctx.alpha * 0.6F, 8);
                }
            }

            if (glowBuffer != null) {
                if (textured) {
                    this.renderVerticalCylinderTextured(
                        matrices, glowBuffer, pillarRadius + ctx.glowSize * 0.5, height, color, ctx.alpha * (float)ctx.glowOpacity * 0.4F, 6, ctx
                    );
                } else {
                    this.renderVerticalCylinder(
                        matrices, glowBuffer, pillarRadius + ctx.glowSize * 0.5, height, color, ctx.alpha * (float)ctx.glowOpacity * 0.4F, 6
                    );
                }
            }

            matrices.method_22909();
        }
    }

    private void renderVerticalCylinder(class_4587 matrices, class_4588 buffer, double radius, double height, Color color, float alpha, int segments) {
        Matrix4f matrix = matrices.method_23760().method_23761();
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;

        for (int i = 0; i < segments; i++) {
            double angle1 = (Math.PI * 2) * i / segments;
            double angle2 = (Math.PI * 2) * (i + 1) / segments;
            float x1 = (float)(Math.cos(angle1) * radius);
            float z1 = (float)(Math.sin(angle1) * radius);
            float x2 = (float)(Math.cos(angle2) * radius);
            float z2 = (float)(Math.sin(angle2) * radius);
            buffer.method_22918(matrix, x1, 0.0F, z1).method_22915(r, g, b, alpha);
            buffer.method_22918(matrix, x2, 0.0F, z2).method_22915(r, g, b, alpha);
            buffer.method_22918(matrix, x2, (float)height, z2).method_22915(r, g, b, alpha * 0.5F);
            buffer.method_22918(matrix, x1, 0.0F, z1).method_22915(r, g, b, alpha);
            buffer.method_22918(matrix, x2, (float)height, z2).method_22915(r, g, b, alpha * 0.5F);
            buffer.method_22918(matrix, x1, (float)height, z1).method_22915(r, g, b, alpha * 0.5F);
        }
    }

    private void renderVerticalCylinderTextured(
        class_4587 matrices, class_4588 buffer, double radius, double height, Color color, float alpha, int segments, EffectRenderContext ctx
    ) {
        Matrix4f matrix = matrices.method_23760().method_23761();
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;

        for (int i = 0; i < segments; i++) {
            double angle1 = (Math.PI * 2) * i / segments;
            double angle2 = (Math.PI * 2) * (i + 1) / segments;
            float x1 = (float)(Math.cos(angle1) * radius);
            float z1 = (float)(Math.sin(angle1) * radius);
            float x2 = (float)(Math.cos(angle2) * radius);
            float z2 = (float)(Math.sin(angle2) * radius);
            float u1 = (float)i / segments + ctx.uvOffsetU;
            float u2 = (float)(i + 1) / segments + ctx.uvOffsetU;
            float v0 = 0.0F + ctx.uvOffsetV;
            float v1 = 1.0F + ctx.uvOffsetV;
            buffer.method_22918(matrix, x1, 0.0F, z1).method_22915(r, g, b, alpha).method_22913(u1, v0);
            buffer.method_22918(matrix, x2, 0.0F, z2).method_22915(r, g, b, alpha).method_22913(u2, v0);
            buffer.method_22918(matrix, x2, (float)height, z2).method_22915(r, g, b, alpha * 0.5F).method_22913(u2, v1);
            buffer.method_22918(matrix, x1, 0.0F, z1).method_22915(r, g, b, alpha).method_22913(u1, v0);
            buffer.method_22918(matrix, x2, (float)height, z2).method_22915(r, g, b, alpha * 0.5F).method_22913(u2, v1);
            buffer.method_22918(matrix, x1, (float)height, z1).method_22915(r, g, b, alpha * 0.5F).method_22913(u1, v1);
        }
    }
}
