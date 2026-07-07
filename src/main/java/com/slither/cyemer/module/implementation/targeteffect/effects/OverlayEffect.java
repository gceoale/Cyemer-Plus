package com.slither.cyemer.module.implementation.targeteffect.effects;

import com.slither.cyemer.module.implementation.targeteffect.BaseEffect;
import com.slither.cyemer.module.implementation.targeteffect.EffectRenderContext;
import com.slither.cyemer.util.render.types.ShaderRenderLayers;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12249;
import net.minecraft.class_1921;
import net.minecraft.class_2960;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4608;
import net.minecraft.class_742;
import net.minecraft.class_7833;

@Environment(EnvType.CLIENT)
public class OverlayEffect extends BaseEffect {
    @Override
    public void render(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx) {
        this.renderOverlayPass(matrices, ctx, false);
        if (ctx.glowEnabled) {
            this.renderOverlayPass(matrices, ctx, true);
        }
    }

    @Override
    public void renderTextured(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx) {
        this.render(matrices, mainBuffer, glowBuffer, ctx);
    }

    private void renderOverlayPass(class_4587 matrices, EffectRenderContext ctx, boolean isGlowPass) {
        if (ctx.vertexConsumers != null && ctx.playerModel != null && ctx.renderState != null) {
            matrices.method_22903();
            matrices.method_22907(class_7833.field_40716.rotationDegrees(180.0F - ctx.renderState.field_53446));
            float scale;
            if (isGlowPass) {
                float baseScale = 0.9F + 0.05F * (float)ctx.scaleMultiplier;
                scale = baseScale + (float)ctx.glowSize;
            } else {
                scale = 0.9F + 0.05F * (float)ctx.scaleMultiplier;
            }

            matrices.method_22905(scale, scale, scale);
            matrices.method_22905(-1.0F, -1.0F, 1.0F);
            matrices.method_46416(0.0F, -1.501F, 0.0F);
            if (!(ctx.player instanceof class_742 clientPlayer)) {
                matrices.method_22909();
            } else {
                class_2960 texture = clientPlayer.method_52814().comp_1626().comp_3627();
                ctx.playerModel.method_62110(ctx.renderState);
                boolean useShader = ctx.theme.usesShader();
                String shaderName = ctx.theme.getShaderName();
                class_1921 layer;
                if (useShader && shaderName != null) {
                    layer = ShaderRenderLayers.getShaderEntityLayer(shaderName, texture, isGlowPass);
                } else if (isGlowPass) {
                    layer = class_12249.method_76000(texture);
                } else {
                    layer = ctx.opaqueMode ? class_12249.method_75994(texture) : class_12249.method_76000(texture);
                }

                class_4588 buffer = ctx.vertexConsumers.method_73477(layer);
                int color;
                float alpha;
                if (isGlowPass) {
                    alpha = (float)ctx.glowOpacity;
                    color = ctx.getThemedColor().getRGB();
                } else {
                    alpha = ctx.alpha;
                    color = ctx.color.getRGB();
                }

                int argb = color & 16777215 | (int)(alpha * 255.0F) << 24;
                ctx.playerModel.method_62100(matrices, buffer, ctx.light, class_4608.field_21444, argb);
                matrices.method_22909();
            }
        }
    }
}
