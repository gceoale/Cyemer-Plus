package com.slither.cyemer.util.render;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;
import net.minecraft.class_4588;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class TexturedRenderUtils {
    public static void drawTexturedSphere(
        class_4588 buffer, Matrix4f matrix, class_243 center, double radius, Color color, float alpha, int segments, float uvOffsetU, float uvOffsetV
    ) {
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
                float u0 = (float)j / segments + uvOffsetU;
                float u1 = (float)(j + 1) / segments + uvOffsetU;
                float v0 = (float)i / segments + uvOffsetV;
                float v1 = (float)(i + 1) / segments + uvOffsetV;
                buffer.method_22918(matrix, x0, (float)y0, z0).method_22915(r, g, b, alpha).method_22913(u0, v0);
                buffer.method_22918(matrix, x1, (float)y0, z1).method_22915(r, g, b, alpha).method_22913(u1, v0);
                buffer.method_22918(matrix, x2, (float)y1, z2).method_22915(r, g, b, alpha).method_22913(u0, v1);
                buffer.method_22918(matrix, x1, (float)y0, z1).method_22915(r, g, b, alpha).method_22913(u1, v0);
                buffer.method_22918(matrix, x3, (float)y1, z3).method_22915(r, g, b, alpha).method_22913(u1, v1);
                buffer.method_22918(matrix, x2, (float)y1, z2).method_22915(r, g, b, alpha).method_22913(u0, v1);
            }
        }
    }

    public static void drawTexturedCylinder(
        class_4588 buffer, Matrix4f matrix, double radius, double height, Color color, float alpha, int segments, float uvOffsetU, float uvOffsetV
    ) {
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;

        for (int i = 0; i < segments; i++) {
            double angle0 = (Math.PI * 2) * i / segments;
            double angle1 = (Math.PI * 2) * (i + 1) / segments;
            float x0 = (float)(Math.cos(angle0) * radius);
            float z0 = (float)(Math.sin(angle0) * radius);
            float x1 = (float)(Math.cos(angle1) * radius);
            float z1 = (float)(Math.sin(angle1) * radius);
            float u0 = (float)i / segments + uvOffsetU;
            float u1 = (float)(i + 1) / segments + uvOffsetU;
            buffer.method_22918(matrix, x0, 0.0F, z0).method_22915(r, g, b, alpha).method_22913(u0, uvOffsetV);
            buffer.method_22918(matrix, x0, (float)height, z0).method_22915(r, g, b, alpha).method_22913(u0, 1.0F + uvOffsetV);
            buffer.method_22918(matrix, x1, (float)height, z1).method_22915(r, g, b, alpha).method_22913(u1, 1.0F + uvOffsetV);
            buffer.method_22918(matrix, x0, 0.0F, z0).method_22915(r, g, b, alpha).method_22913(u0, uvOffsetV);
            buffer.method_22918(matrix, x1, (float)height, z1).method_22915(r, g, b, alpha).method_22913(u1, 1.0F + uvOffsetV);
            buffer.method_22918(matrix, x1, 0.0F, z1).method_22915(r, g, b, alpha).method_22913(u1, uvOffsetV);
        }

        for (int i = 0; i < segments; i++) {
            double angle0 = (Math.PI * 2) * i / segments;
            double angle1 = (Math.PI * 2) * (i + 1) / segments;
            float x0 = (float)(Math.cos(angle0) * radius);
            float z0 = (float)(Math.sin(angle0) * radius);
            float x1 = (float)(Math.cos(angle1) * radius);
            float z1 = (float)(Math.sin(angle1) * radius);
            float u0 = (float)(Math.cos(angle0) * 0.5 + 0.5);
            float v0 = (float)(Math.sin(angle0) * 0.5 + 0.5);
            float u1 = (float)(Math.cos(angle1) * 0.5 + 0.5);
            float v1 = (float)(Math.sin(angle1) * 0.5 + 0.5);
            buffer.method_22918(matrix, 0.0F, 0.0F, 0.0F).method_22915(r, g, b, alpha).method_22913(0.5F, 0.5F);
            buffer.method_22918(matrix, x0, 0.0F, z0).method_22915(r, g, b, alpha).method_22913(u0, v0);
            buffer.method_22918(matrix, x1, 0.0F, z1).method_22915(r, g, b, alpha).method_22913(u1, v1);
            buffer.method_22918(matrix, 0.0F, (float)height, 0.0F).method_22915(r, g, b, alpha).method_22913(0.5F, 0.5F);
            buffer.method_22918(matrix, x1, (float)height, z1).method_22915(r, g, b, alpha).method_22913(u1, v1);
            buffer.method_22918(matrix, x0, (float)height, z0).method_22915(r, g, b, alpha).method_22913(u0, v0);
        }
    }

    public static void drawTexturedQuad(
        class_4588 buffer,
        Matrix4f matrix,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        float x3,
        float y3,
        float z3,
        float x4,
        float y4,
        float z4,
        float r,
        float g,
        float b,
        float a,
        float u0,
        float v0,
        float u1,
        float v1
    ) {
        buffer.method_22918(matrix, x1, y1, z1).method_22915(r, g, b, a).method_22913(u0, v0);
        buffer.method_22918(matrix, x2, y2, z2).method_22915(r, g, b, a).method_22913(u1, v0);
        buffer.method_22918(matrix, x3, y3, z3).method_22915(r, g, b, a).method_22913(u1, v1);
        buffer.method_22918(matrix, x1, y1, z1).method_22915(r, g, b, a).method_22913(u0, v0);
        buffer.method_22918(matrix, x3, y3, z3).method_22915(r, g, b, a).method_22913(u1, v1);
        buffer.method_22918(matrix, x4, y4, z4).method_22915(r, g, b, a).method_22913(u0, v1);
    }
}
