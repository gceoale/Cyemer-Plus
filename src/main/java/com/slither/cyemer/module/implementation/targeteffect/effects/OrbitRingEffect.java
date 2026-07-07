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
public class OrbitRingEffect extends BaseEffect {
    @Override
    public void render(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx) {
        this.renderRing(matrices, mainBuffer, glowBuffer, ctx, false);
    }

    @Override
    public void renderTextured(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx) {
        this.renderRing(matrices, mainBuffer, glowBuffer, ctx, true);
    }

    private void renderRing(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx, boolean textured) {
        double torsoHeight = ctx.player.method_17682() / 2.0;
        double radius = ctx.player.method_17681() * 0.8 * ctx.scaleMultiplier;
        double thickness = 0.08 * ctx.scaleMultiplier;
        double rotation = ctx.time * ctx.speedMultiplier / 1000.0;
        Color color = textured ? ctx.getThemedColor() : ctx.color;
        matrices.method_22903();
        matrices.method_22904(0.0, torsoHeight, 0.0);
        if (mainBuffer != null) {
            if (textured) {
                RenderHelpers.renderTorusTextured(
                    matrices, mainBuffer, radius, thickness, rotation, color, ctx.alpha, ctx.segments, 8, ctx.uvOffsetU, ctx.uvOffsetV
                );
            } else {
                RenderHelpers.renderTorusOptimized(matrices, mainBuffer, radius, thickness, rotation, color, ctx.alpha, ctx.segments, 8);
            }
        }

        if (glowBuffer != null) {
            double glowThickness = thickness + ctx.glowSize;
            float glowAlpha = ctx.alpha * (float)ctx.glowOpacity;
            if (textured) {
                RenderHelpers.renderTorusTextured(
                    matrices, glowBuffer, radius, glowThickness, rotation, color, glowAlpha, Math.max(16, ctx.segments / 2), 6, ctx.uvOffsetU, ctx.uvOffsetV
                );
            } else {
                RenderHelpers.renderTorusOptimized(matrices, glowBuffer, radius, glowThickness, rotation, color, glowAlpha, Math.max(16, ctx.segments / 2), 6);
            }
        }

        matrices.method_22909();
    }
}
