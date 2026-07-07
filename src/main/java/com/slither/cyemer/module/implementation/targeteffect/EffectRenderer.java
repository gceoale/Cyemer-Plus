package com.slither.cyemer.module.implementation.targeteffect;

import com.slither.cyemer.shader.CoreShaderManager;
import com.slither.cyemer.util.render.types.RenderTypes;
import com.slither.cyemer.util.render.types.ShaderRenderLayers;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1921;
import net.minecraft.class_243;
import net.minecraft.class_2960;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_9799;
import net.minecraft.class_4597.class_4598;

@Environment(EnvType.CLIENT)
public class EffectRenderer {
    private final EffectFactory effectFactory = new EffectFactory();
    private final CoreShaderManager shaderManager = CoreShaderManager.getInstance();
    private static final class_1921 GLOW_LAYER = ShaderRenderLayers.getShaderLayerNoDepth("fallback", true);

    public void render(class_4587 matrices, class_243 cameraPos, class_243 playerPos, EffectRenderContext ctx) {
        BaseEffect effect = this.effectFactory.getEffect(ctx.effectType);
        class_9799 allocator = new class_9799(1536);

        label83: {
            try {
                if ("Overlay".equals(ctx.effectType)) {
                    class_4598 vertexConsumers = class_4597.method_22991(allocator);
                    boolean useShader = ctx.theme.usesShader();
                    String shaderName = ctx.theme.getShaderName();
                    if (useShader && shaderName != null) {
                        this.updateShaderUniforms(shaderName, ctx);
                    }

                    class_1921 mainLayer = useShader ? ShaderRenderLayers.getShaderLayerNoDepth(shaderName, false) : RenderTypes.TRIANGLES_NO_DEPTH;
                    class_4588 mainBuffer = vertexConsumers.method_73477(mainLayer);
                    effect.render(matrices, mainBuffer, null, ctx);
                    vertexConsumers.method_22993();
                    if (!ctx.glowEnabled) {
                        break label83;
                    }

                    class_1921 glowLayer = useShader ? ShaderRenderLayers.getShaderLayerNoDepth(shaderName, true) : GLOW_LAYER;
                    if (useShader && shaderName != null) {
                        this.updateShaderUniforms(shaderName, ctx);
                    }

                    class_4588 glowBuffer = vertexConsumers.method_73477(glowLayer);
                    effect.render(matrices, null, glowBuffer, ctx);
                    vertexConsumers.method_22993();
                    break label83;
                }

                class_4598 vertexConsumersx = class_4597.method_22991(allocator);
                matrices.method_22903();
                matrices.method_22904(
                    playerPos.field_1352 - cameraPos.field_1352, playerPos.field_1351 - cameraPos.field_1351, playerPos.field_1350 - cameraPos.field_1350
                );
                boolean useShaderx = ctx.theme.usesShader();
                String shaderNamex = ctx.theme.getShaderName();
                if (useShaderx && shaderNamex != null) {
                    this.updateShaderUniforms(shaderNamex, ctx);
                }

                class_1921 mainLayerx = useShaderx ? ShaderRenderLayers.getShaderLayerNoDepth(shaderNamex, false) : RenderTypes.TRIANGLES_NO_DEPTH;
                class_4588 mainBufferx = vertexConsumersx.method_73477(mainLayerx);
                effect.render(matrices, mainBufferx, null, ctx);
                vertexConsumersx.method_22993();
                if (ctx.glowEnabled) {
                    class_1921 glowLayer = useShaderx ? ShaderRenderLayers.getShaderLayerNoDepth(shaderNamex, true) : GLOW_LAYER;
                    if (useShaderx && shaderNamex != null) {
                        this.updateShaderUniforms(shaderNamex, ctx);
                    }

                    class_4588 glowBuffer = vertexConsumersx.method_73477(glowLayer);
                    effect.render(matrices, null, glowBuffer, ctx);
                    vertexConsumersx.method_22993();
                }

                matrices.method_22909();
            } catch (Throwable var15) {
                try {
                    allocator.close();
                } catch (Throwable var14) {
                    var15.addSuppressed(var14);
                }

                throw var15;
            }

            allocator.close();
            return;
        }

        allocator.close();
    }

    private void updateShaderUniforms(String shaderName, EffectRenderContext ctx) {
        float time = (float)(ctx.time / 1000.0);
        float[] colorArray = new float[]{ctx.color.getRed() / 255.0F, ctx.color.getGreen() / 255.0F, ctx.color.getBlue() / 255.0F, ctx.alpha};
        float speed = (float)ctx.speedMultiplier;
        this.shaderManager.setShaderUniforms(class_2960.method_60655("dynamic_fps", shaderName), time, colorArray, speed);
    }

    public static int calculateLOD(double distance, int maxSegments) {
        if (distance < 16.0) {
            return maxSegments;
        } else if (distance < 32.0) {
            return maxSegments / 2;
        } else {
            return distance < 64.0 ? maxSegments / 3 : Math.max(8, maxSegments / 4);
        }
    }

    public static Color getRainbowColor(long time) {
        float hue = (float)(time % 3000L) / 3000.0F;
        return Color.getHSBColor(hue, 0.8F, 1.0F);
    }
}
