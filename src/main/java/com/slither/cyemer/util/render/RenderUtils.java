package com.slither.cyemer.util.render;

import com.slither.cyemer.util.render.types.RenderTypes;
import com.slither.cyemer.util.render.types.ShaderRenderLayers;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class RenderUtils {
    public static void drawBox(class_4587 matrices, class_4597 vertexConsumers, class_238 box, Color color, float alpha, boolean seeThrough) {
        drawBox(matrices, vertexConsumers, box, color, alpha, seeThrough, false);
    }

    public static void drawBox(
        class_4587 matrices, class_4597 vertexConsumers, class_238 box, Color color, float alpha, boolean seeThrough, boolean useEspShader
    ) {
        Matrix4f matrix = matrices.method_23760().method_23761();
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        float minX = (float)box.field_1323;
        float minY = (float)box.field_1322;
        float minZ = (float)box.field_1321;
        float maxX = (float)box.field_1320;
        float maxY = (float)box.field_1325;
        float maxZ = (float)box.field_1324;
        if (useEspShader) {
            if (seeThrough) {
                class_4588 throughBuffer = vertexConsumers.method_73477(ShaderRenderLayers.getEspLayer(true));
                emitBoxFaces(throughBuffer, matrix, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, alpha * 0.72F);
            }

            class_4588 depthBuffer = vertexConsumers.method_73477(ShaderRenderLayers.getEspLayer(false));
            emitBoxFaces(depthBuffer, matrix, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, alpha);
        } else {
            class_4588 buffer = vertexConsumers.method_73477(RenderTypes.TRIANGLES);
            if (seeThrough) {
                emitBoxFaces(buffer, matrix, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, alpha);
            }

            emitBoxFaces(buffer, matrix, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, alpha);
        }
    }

    public static void drawBoxEsp(class_4587 matrices, class_4597 vertexConsumers, class_238 box, Color color, float alpha, boolean seeThrough) {
        drawBox(matrices, vertexConsumers, box, color, alpha, seeThrough, true);
    }

    public static void drawFilledBox(class_4587 matrices, class_4597 vertexConsumers, class_238 box, Color color, float alpha) {
        class_4588 buffer = vertexConsumers.method_73477(RenderTypes.TRIANGLES);
        Matrix4f matrix = matrices.method_23760().method_23761();
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        float minX = (float)box.field_1323;
        float minY = (float)box.field_1322;
        float minZ = (float)box.field_1321;
        float maxX = (float)box.field_1320;
        float maxY = (float)box.field_1325;
        float maxZ = (float)box.field_1324;
        drawQuad(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, alpha);
        drawQuad(buffer, matrix, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, alpha);
        drawQuad(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, alpha);
        drawQuad(buffer, matrix, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, alpha);
        drawQuad(buffer, matrix, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, alpha);
        drawQuad(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, alpha);
    }

    public static void drawSphere(
        class_4587 matrices, class_4597 vertexConsumers, class_243 center, double radius, Color color, float alpha, int segments, boolean ignored
    ) {
        class_4588 buffer = vertexConsumers.method_73477(RenderTypes.TRIANGLES);
        drawSphereInternal(matrices, buffer, center, radius, color, alpha, segments);
    }

    public static void drawSphereEsp(
        class_4587 matrices, class_4597 vertexConsumers, class_243 center, double radius, Color color, float alpha, int segments, boolean seeThrough
    ) {
        class_4588 buffer = vertexConsumers.method_73477(ShaderRenderLayers.getEspLayer(seeThrough));
        drawSphereInternal(matrices, buffer, center, radius, color, alpha, segments);
    }

    private static void drawSphereInternal(class_4587 matrices, class_4588 buffer, class_243 center, double radius, Color color, float alpha, int segments) {
        matrices.method_22903();
        matrices.method_22904(center.field_1352, center.field_1351, center.field_1350);
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        Matrix4f matrix = matrices.method_23760().method_23761();

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
                float x0 = (float)Math.cos(lng0);
                float z0 = (float)Math.sin(lng0);
                float x1 = (float)Math.cos(lng1);
                float z1 = (float)Math.sin(lng1);
                buffer.method_22918(matrix, (float)(x0 * r0), (float)y0, (float)(z0 * r0)).method_22915(r, g, b, alpha);
                buffer.method_22918(matrix, (float)(x1 * r0), (float)y0, (float)(z1 * r0)).method_22915(r, g, b, alpha);
                buffer.method_22918(matrix, (float)(x0 * r1), (float)y1, (float)(z0 * r1)).method_22915(r, g, b, alpha);
                buffer.method_22918(matrix, (float)(x1 * r0), (float)y0, (float)(z1 * r0)).method_22915(r, g, b, alpha);
                buffer.method_22918(matrix, (float)(x1 * r1), (float)y1, (float)(z1 * r1)).method_22915(r, g, b, alpha);
                buffer.method_22918(matrix, (float)(x0 * r1), (float)y1, (float)(z0 * r1)).method_22915(r, g, b, alpha);
            }
        }

        matrices.method_22909();
    }

    public static void drawSphere(class_4587 matrices, class_4597 vertexConsumers, class_243 center, double radius, Color color, float alpha, int segments) {
        drawSphere(matrices, vertexConsumers, center, radius, color, alpha, segments, false);
    }

    public static void drawCylinder(
        class_4587 matrices, class_4597 vertexConsumers, class_243 base, double radius, double height, Color color, float alpha, int segments
    ) {
        class_4588 buffer = vertexConsumers.method_73477(RenderTypes.TRIANGLES);
        drawCylinderInternal(matrices, buffer, base, radius, height, color, alpha, segments);
    }

    public static void drawCylinderEsp(
        class_4587 matrices,
        class_4597 vertexConsumers,
        class_243 base,
        double radius,
        double height,
        Color color,
        float alpha,
        int segments,
        boolean seeThrough
    ) {
        class_4588 buffer = vertexConsumers.method_73477(ShaderRenderLayers.getEspLayer(seeThrough));
        drawCylinderInternal(matrices, buffer, base, radius, height, color, alpha, segments);
    }

    private static void drawCylinderInternal(
        class_4587 matrices, class_4588 buffer, class_243 base, double radius, double height, Color color, float alpha, int segments
    ) {
        matrices.method_22903();
        matrices.method_22904(base.field_1352, base.field_1351, base.field_1350);
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        Matrix4f matrix = matrices.method_23760().method_23761();

        for (int i = 0; i < segments; i++) {
            double angle0 = (Math.PI * 2) * i / segments;
            double angle1 = (Math.PI * 2) * (i + 1) / segments;
            float x0 = (float)(Math.cos(angle0) * radius);
            float z0 = (float)(Math.sin(angle0) * radius);
            float x1 = (float)(Math.cos(angle1) * radius);
            float z1 = (float)(Math.sin(angle1) * radius);
            drawQuad(buffer, matrix, x0, 0.0F, z0, x0, (float)height, z0, x1, (float)height, z1, x1, 0.0F, z1, r, g, b, alpha);
        }

        for (int i = 0; i < segments; i++) {
            double angle0 = (Math.PI * 2) * i / segments;
            double angle1 = (Math.PI * 2) * (i + 1) / segments;
            float x0 = (float)(Math.cos(angle0) * radius);
            float z0 = (float)(Math.sin(angle0) * radius);
            float x1 = (float)(Math.cos(angle1) * radius);
            float z1 = (float)(Math.sin(angle1) * radius);
            buffer.method_22918(matrix, 0.0F, 0.0F, 0.0F).method_22915(r, g, b, alpha);
            buffer.method_22918(matrix, x0, 0.0F, z0).method_22915(r, g, b, alpha);
            buffer.method_22918(matrix, x1, 0.0F, z1).method_22915(r, g, b, alpha);
            buffer.method_22918(matrix, 0.0F, (float)height, 0.0F).method_22915(r, g, b, alpha);
            buffer.method_22918(matrix, x1, (float)height, z1).method_22915(r, g, b, alpha);
            buffer.method_22918(matrix, x0, (float)height, z0).method_22915(r, g, b, alpha);
        }

        matrices.method_22909();
    }

    public static void drawCone(
        class_4587 matrices, class_4597 vertexConsumers, class_243 base, double radius, double height, Color color, float alpha, int segments
    ) {
        class_4588 buffer = vertexConsumers.method_73477(RenderTypes.TRIANGLES);
        drawConeInternal(matrices, buffer, base, radius, height, color, alpha, segments);
    }

    public static void drawConeEsp(
        class_4587 matrices,
        class_4597 vertexConsumers,
        class_243 base,
        double radius,
        double height,
        Color color,
        float alpha,
        int segments,
        boolean seeThrough
    ) {
        class_4588 buffer = vertexConsumers.method_73477(ShaderRenderLayers.getEspLayer(seeThrough));
        drawConeInternal(matrices, buffer, base, radius, height, color, alpha, segments);
    }

    private static void drawConeInternal(
        class_4587 matrices, class_4588 buffer, class_243 base, double radius, double height, Color color, float alpha, int segments
    ) {
        matrices.method_22903();
        matrices.method_22904(base.field_1352, base.field_1351, base.field_1350);
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        Matrix4f matrix = matrices.method_23760().method_23761();

        for (int i = 0; i < segments; i++) {
            double angle0 = (Math.PI * 2) * i / segments;
            double angle1 = (Math.PI * 2) * (i + 1) / segments;
            float x0 = (float)(Math.cos(angle0) * radius);
            float z0 = (float)(Math.sin(angle0) * radius);
            float x1 = (float)(Math.cos(angle1) * radius);
            float z1 = (float)(Math.sin(angle1) * radius);
            buffer.method_22918(matrix, x0, 0.0F, z0).method_22915(r, g, b, alpha);
            buffer.method_22918(matrix, 0.0F, (float)height, 0.0F).method_22915(r, g, b, alpha);
            buffer.method_22918(matrix, x1, 0.0F, z1).method_22915(r, g, b, alpha);
        }

        for (int i = 0; i < segments; i++) {
            double angle0 = (Math.PI * 2) * i / segments;
            double angle1 = (Math.PI * 2) * (i + 1) / segments;
            float x0 = (float)(Math.cos(angle0) * radius);
            float z0 = (float)(Math.sin(angle0) * radius);
            float x1 = (float)(Math.cos(angle1) * radius);
            float z1 = (float)(Math.sin(angle1) * radius);
            buffer.method_22918(matrix, 0.0F, 0.0F, 0.0F).method_22915(r, g, b, alpha);
            buffer.method_22918(matrix, x1, 0.0F, z1).method_22915(r, g, b, alpha);
            buffer.method_22918(matrix, x0, 0.0F, z0).method_22915(r, g, b, alpha);
        }

        matrices.method_22909();
    }

    public static void drawLine(class_4587 matrices, class_4597 vertexConsumers, class_243 start, class_243 end, Color color, float alpha) {
        class_4588 buffer = vertexConsumers.method_73477(RenderTypes.LINES);
        Matrix4f matrix = matrices.method_23760().method_23761();
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        buffer.method_22918(matrix, (float)start.field_1352, (float)start.field_1351, (float)start.field_1350).method_22915(r, g, b, alpha);
        buffer.method_22918(matrix, (float)end.field_1352, (float)end.field_1351, (float)end.field_1350).method_22915(r, g, b, alpha);
    }

    public static void drawCircle(class_4587 matrices, class_4597 vertexConsumers, class_243 center, double radius, Color color, float alpha, int segments) {
        class_4588 buffer = vertexConsumers.method_73477(RenderTypes.LINES);
        matrices.method_22903();
        matrices.method_22904(center.field_1352, center.field_1351, center.field_1350);
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        Matrix4f matrix = matrices.method_23760().method_23761();

        for (int i = 0; i < segments; i++) {
            double angle0 = (Math.PI * 2) * i / segments;
            double angle1 = (Math.PI * 2) * (i + 1) / segments;
            float x0 = (float)(Math.cos(angle0) * radius);
            float z0 = (float)(Math.sin(angle0) * radius);
            float x1 = (float)(Math.cos(angle1) * radius);
            float z1 = (float)(Math.sin(angle1) * radius);
            buffer.method_22918(matrix, x0, 0.0F, z0).method_22915(r, g, b, alpha);
            buffer.method_22918(matrix, x1, 0.0F, z1).method_22915(r, g, b, alpha);
        }

        matrices.method_22909();
    }

    private static void drawQuad(
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
        float a
    ) {
        buffer.method_22918(matrix, x1, y1, z1).method_22915(r, g, b, a);
        buffer.method_22918(matrix, x2, y2, z2).method_22915(r, g, b, a);
        buffer.method_22918(matrix, x3, y3, z3).method_22915(r, g, b, a);
        buffer.method_22918(matrix, x1, y1, z1).method_22915(r, g, b, a);
        buffer.method_22918(matrix, x3, y3, z3).method_22915(r, g, b, a);
        buffer.method_22918(matrix, x4, y4, z4).method_22915(r, g, b, a);
    }

    private static void emitBoxFaces(
        class_4588 buffer, Matrix4f matrix, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a
    ) {
        drawQuad(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        drawQuad(buffer, matrix, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a);
        drawQuad(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, a);
        drawQuad(buffer, matrix, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        drawQuad(buffer, matrix, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        drawQuad(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a);
    }
}
