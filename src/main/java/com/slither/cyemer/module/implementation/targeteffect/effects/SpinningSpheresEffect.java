package com.slither.cyemer.module.implementation.targeteffect.effects;

import com.slither.cyemer.module.implementation.targeteffect.BaseEffect;
import com.slither.cyemer.module.implementation.targeteffect.EffectRenderContext;
import com.slither.cyemer.util.render.TexturedRenderUtils;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class SpinningSpheresEffect extends BaseEffect {
    @Override
    public void render(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx) {
        this.renderSpheres(matrices, mainBuffer, glowBuffer, ctx, false);
    }

    @Override
    public void renderTextured(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx) {
        this.renderSpheres(matrices, mainBuffer, glowBuffer, ctx, true);
    }

    private void renderSpheres(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx, boolean textured) {
        double radius = 1.2 * ctx.scaleMultiplier;
        double sphereSize = 0.15 * ctx.scaleMultiplier;
        int sphereCount = 3;
        double time = ctx.time * ctx.speedMultiplier * 0.001;
        Color color = textured ? ctx.getThemedColor() : ctx.color;

        for (int i = 0; i < sphereCount; i++) {
            double angle = (time + i * ((Math.PI * 2) / sphereCount)) % (Math.PI * 2);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = Math.sin(time * 2.0 + i) * 0.3;
            class_243 pos = new class_243(x, ctx.player.method_17682() / 2.0F + y, z);
            if (textured) {
                this.renderTexturedSphere(matrices, mainBuffer, glowBuffer, pos, sphereSize, color, ctx);
            } else {
                this.renderSolidSphere(matrices, mainBuffer, glowBuffer, pos, sphereSize, color, ctx);
            }
        }
    }

    private void renderSolidSphere(
        class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, class_243 pos, double size, Color color, EffectRenderContext ctx
    ) {
        matrices.method_22903();
        matrices.method_22904(pos.field_1352, pos.field_1351, pos.field_1350);
        Matrix4f matrix = matrices.method_23760().method_23761();
        if (mainBuffer != null) {
            this.drawColoredSphere(mainBuffer, matrix, size, color, ctx.alpha, ctx.segments);
        }

        if (glowBuffer != null) {
            double glowSize = size + ctx.glowSize;
            this.drawColoredSphere(glowBuffer, matrix, glowSize, color, (float)ctx.glowOpacity, ctx.segments / 2);
        }

        matrices.method_22909();
    }

    private void renderTexturedSphere(
        class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, class_243 pos, double size, Color color, EffectRenderContext ctx
    ) {
        matrices.method_22903();
        matrices.method_22904(pos.field_1352, pos.field_1351, pos.field_1350);
        Matrix4f matrix = matrices.method_23760().method_23761();
        if (mainBuffer != null) {
            TexturedRenderUtils.drawTexturedSphere(mainBuffer, matrix, class_243.field_1353, size, color, ctx.alpha, ctx.segments, ctx.uvOffsetU, ctx.uvOffsetV);
        }

        if (glowBuffer != null) {
            double glowSize = size + ctx.glowSize;
            TexturedRenderUtils.drawTexturedSphere(
                glowBuffer, matrix, class_243.field_1353, glowSize, color, (float)ctx.glowOpacity, ctx.segments / 2, ctx.uvOffsetU, ctx.uvOffsetV
            );
        }

        matrices.method_22909();
    }

    private void drawColoredSphere(class_4588 buffer, Matrix4f matrix, double radius, Color color, float alpha, int segments) {
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;

        for (int i = 0; i < segments; i++) {
            double lat0 = Math.PI * (-0.5 + (double)i / segments);
            double lat1 = Math.PI * (-0.5 + (double)(i + 1) / segments);
            double y0 = Math.sin(lat0) * radius;
            double y1 = Math.sin(lat1) * radius;
            double r0 = Math.cos(lat0) * radius;
            double r1 = Math.cos(lat1) * radius;

            for (int j = 0; j < segments; j++) {
                double lng0 = (Math.PI * 2) * j / segments;
                double lng1 = (Math.PI * 2) * (j + 1) / segments;
                float x0 = (float)(Math.cos(lng0) * r0);
                float z0 = (float)(Math.sin(lng0) * r0);
                float x1 = (float)(Math.cos(lng1) * r0);
                float z1 = (float)(Math.sin(lng1) * r0);
                float x2 = (float)(Math.cos(lng0) * r1);
                float z2 = (float)(Math.sin(lng0) * r1);
                float x3 = (float)(Math.cos(lng1) * r1);
                float z3 = (float)(Math.sin(lng1) * r1);
                buffer.method_22918(matrix, x0, (float)y0, z0).method_22915(r, g, b, alpha);
                buffer.method_22918(matrix, x1, (float)y0, z1).method_22915(r, g, b, alpha);
                buffer.method_22918(matrix, x2, (float)y1, z2).method_22915(r, g, b, alpha);
                buffer.method_22918(matrix, x1, (float)y0, z1).method_22915(r, g, b, alpha);
                buffer.method_22918(matrix, x3, (float)y1, z3).method_22915(r, g, b, alpha);
                buffer.method_22918(matrix, x2, (float)y1, z2).method_22915(r, g, b, alpha);
            }
        }
    }
}
