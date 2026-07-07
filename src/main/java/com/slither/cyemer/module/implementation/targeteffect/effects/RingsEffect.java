package com.slither.cyemer.module.implementation.targeteffect.effects;

import com.slither.cyemer.module.implementation.targeteffect.BaseEffect;
import com.slither.cyemer.module.implementation.targeteffect.EffectRenderContext;
import com.slither.cyemer.module.implementation.targeteffect.RenderHelpers;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_4587;
import net.minecraft.class_4588;

@Environment(EnvType.CLIENT)
public class RingsEffect extends BaseEffect {
    @Override
    public void render(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx) {
        this.renderRings(matrices, mainBuffer, glowBuffer, ctx, false);
    }

    @Override
    public void renderTextured(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx) {
        this.renderRings(matrices, mainBuffer, glowBuffer, ctx, true);
    }

    private void renderRings(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx, boolean textured) {
        double height = ctx.player.method_17682();
        double timeOffset = ctx.time * ctx.speedMultiplier / 1000.0;
        int ringCount = 5;
        int tubeSides = Math.max(6, ctx.segments / 6);
        Color color = textured ? ctx.getThemedColor() : ctx.color;

        for (int ring = 0; ring < ringCount; ring++) {
            double t = (double)ring / (ringCount - 1);
            double y = t * height;
            double radius = ctx.player.method_17681() * 0.7 * ctx.scaleMultiplier;
            double thickness = 0.05 * ctx.scaleMultiplier;
            double ringRotation = timeOffset * (ring % 2 == 0 ? 1 : -1) * (1.0 + ring * 0.2);
            matrices.method_22903();
            matrices.method_22904(0.0, y, 0.0);
            if (mainBuffer != null) {
                if (textured) {
                    RenderHelpers.renderTorusTextured(
                        matrices, mainBuffer, radius, thickness, ringRotation, color, ctx.alpha, ctx.segments, tubeSides, ctx.uvOffsetU, ctx.uvOffsetV
                    );
                } else {
                    RenderHelpers.renderTorusOptimized(matrices, mainBuffer, radius, thickness, ringRotation, color, ctx.alpha, ctx.segments, tubeSides);
                }
            }

            if (glowBuffer != null) {
                double glowThickness = thickness + ctx.glowSize;
                float glowAlpha = ctx.alpha * (float)ctx.glowOpacity;
                if (textured) {
                    RenderHelpers.renderTorusTextured(
                        matrices,
                        glowBuffer,
                        radius,
                        glowThickness,
                        ringRotation,
                        color,
                        glowAlpha,
                        Math.max(16, ctx.segments / 2),
                        Math.max(4, tubeSides - 2),
                        ctx.uvOffsetU,
                        ctx.uvOffsetV
                    );
                } else {
                    RenderHelpers.renderTorusOptimized(
                        matrices, glowBuffer, radius, glowThickness, ringRotation, color, glowAlpha, Math.max(16, ctx.segments / 2), Math.max(4, tubeSides - 2)
                    );
                }
            }

            matrices.method_22909();
        }
    }
}
