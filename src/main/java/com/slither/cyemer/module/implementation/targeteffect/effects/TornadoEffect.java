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
public class TornadoEffect extends BaseEffect {
    @Override
    public void render(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx) {
        this.renderTornado(matrices, mainBuffer, glowBuffer, ctx, false);
    }

    @Override
    public void renderTextured(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx) {
        this.renderTornado(matrices, mainBuffer, glowBuffer, ctx, true);
    }

    private void renderTornado(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx, boolean textured) {
        double height = ctx.player.method_17682();
        double timeOffset = ctx.time * ctx.speedMultiplier / 1000.0;
        int particles = Math.max(16, ctx.segments * 2);
        Color color = textured ? ctx.getThemedColor() : ctx.color;

        for (int i = 0; i < particles; i++) {
            double t = (double)i / particles;
            double y = t * height;
            double radius = ctx.player.method_17681() * 0.2 * ctx.scaleMultiplier * (1.0 + t * 2.0);
            double angle = t * Math.PI * 6.0 + timeOffset * (1.0 + t);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double particleSize = 0.035 * ctx.scaleMultiplier;
            matrices.method_22903();
            matrices.method_22904(x, y, z);
            if (textured) {
                this.renderTexturedParticle(matrices, mainBuffer, glowBuffer, particleSize, color, ctx.alpha, ctx);
            } else {
                if (mainBuffer != null) {
                    RenderHelpers.renderSphereOptimized(matrices, mainBuffer, particleSize, color, ctx.alpha, Math.max(6, ctx.segments / 4));
                }

                if (glowBuffer != null) {
                    double glowRadius = particleSize + ctx.glowSize * 0.5;
                    float glowAlpha = ctx.alpha * (float)ctx.glowOpacity;
                    RenderHelpers.renderSphereOptimized(matrices, glowBuffer, glowRadius, color, glowAlpha, Math.max(4, ctx.segments / 6));
                }
            }

            matrices.method_22909();
        }
    }

    private void renderTexturedParticle(
        class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, double size, Color color, float alpha, EffectRenderContext ctx
    ) {
        Matrix4f matrix = matrices.method_23760().method_23761();
        if (mainBuffer != null) {
            TexturedRenderUtils.drawTexturedSphere(
                mainBuffer, matrix, class_243.field_1353, size, color, alpha, Math.max(6, ctx.segments / 4), ctx.uvOffsetU, ctx.uvOffsetV
            );
        }

        if (glowBuffer != null) {
            double glowRadius = size + ctx.glowSize * 0.5;
            float glowAlpha = alpha * (float)ctx.glowOpacity;
            TexturedRenderUtils.drawTexturedSphere(
                glowBuffer, matrix, class_243.field_1353, glowRadius, color, glowAlpha, Math.max(4, ctx.segments / 6), ctx.uvOffsetU, ctx.uvOffsetV
            );
        }
    }
}
