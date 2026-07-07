package com.slither.cyemer.module.implementation.targeteffect;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_7833;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class RenderHelpers {
    public static void renderContinuousTube(class_4587 matrices, class_4588 buffer, List<class_243> points, double radius, Color color, float alpha, int sides) {
        if (points.size() >= 2) {
            Matrix4f matrix = matrices.method_23760().method_23761();
            float r = color.getRed() / 255.0F;
            float g = color.getGreen() / 255.0F;
            float b = color.getBlue() / 255.0F;
            List<Vector3f[]> allRings = new ArrayList<>();

            for (int i = 0; i < points.size(); i++) {
                class_243 current = points.get(i);
                class_243 tangent;
                if (i == 0) {
                    tangent = points.get(1).method_1020(current).method_1029();
                } else if (i == points.size() - 1) {
                    tangent = current.method_1020(points.get(i - 1)).method_1029();
                } else {
                    class_243 prev = points.get(i - 1);
                    class_243 next = points.get(i + 1);
                    tangent = next.method_1020(prev).method_1029();
                }

                class_243 up = Math.abs(tangent.field_1351) > 0.9 ? new class_243(1.0, 0.0, 0.0) : new class_243(0.0, 1.0, 0.0);
                class_243 right = tangent.method_1036(up).method_1029();
                up = right.method_1036(tangent).method_1029();
                Vector3f[] ring = new Vector3f[sides];

                for (int j = 0; j < sides; j++) {
                    double angle = (Math.PI * 2) * j / sides;
                    double c = Math.cos(angle) * radius;
                    double s = Math.sin(angle) * radius;
                    class_243 offset = right.method_1021(c).method_1019(up.method_1021(s));
                    class_243 pos = current.method_1019(offset);
                    ring[j] = new Vector3f((float)pos.field_1352, (float)pos.field_1351, (float)pos.field_1350);
                }

                allRings.add(ring);
            }

            for (int i = 0; i < allRings.size() - 1; i++) {
                Vector3f[] ring1 = allRings.get(i);
                Vector3f[] ring2 = allRings.get(i + 1);
                float progress = (float)i / allRings.size();
                float pulse = (float)Math.sin(progress * 10.0F - System.currentTimeMillis() / 100.0);
                float pulseAlpha = alpha * (0.7F + 0.3F * pulse);

                for (int j = 0; j < sides; j++) {
                    int nextJ = (j + 1) % sides;
                    Vector3f v0 = ring1[j];
                    Vector3f v1 = ring1[nextJ];
                    Vector3f v2 = ring2[nextJ];
                    Vector3f v3 = ring2[j];
                    buffer.method_22918(matrix, v0.x, v0.y, v0.z).method_22915(r, g, b, pulseAlpha);
                    buffer.method_22918(matrix, v1.x, v1.y, v1.z).method_22915(r, g, b, pulseAlpha);
                    buffer.method_22918(matrix, v2.x, v2.y, v2.z).method_22915(r, g, b, pulseAlpha);
                    buffer.method_22918(matrix, v0.x, v0.y, v0.z).method_22915(r, g, b, pulseAlpha);
                    buffer.method_22918(matrix, v2.x, v2.y, v2.z).method_22915(r, g, b, pulseAlpha);
                    buffer.method_22918(matrix, v3.x, v3.y, v3.z).method_22915(r, g, b, pulseAlpha);
                }
            }
        }
    }

    public static void renderSphereOptimized(class_4587 matrices, class_4588 buffer, double radius, Color color, float alpha, int segments) {
        Matrix4f matrix = matrices.method_23760().method_23761();
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        int actualSegments = Math.max(4, Math.min(segments, (int)(radius * 100.0)));

        for (int i = 0; i < actualSegments; i++) {
            double lat0 = Math.PI * (-0.5 + (double)i / actualSegments);
            double lat1 = Math.PI * (-0.5 + (double)(i + 1) / actualSegments);
            double y0 = Math.sin(lat0) * radius;
            double y1 = Math.sin(lat1) * radius;
            double r0 = Math.cos(lat0) * radius;
            double r1 = Math.cos(lat1) * radius;

            for (int j = 0; j < actualSegments; j++) {
                double lng0 = (Math.PI * 2) * j / actualSegments;
                double lng1 = (Math.PI * 2) * (j + 1) / actualSegments;
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
    }

    public static void renderTorusTextured(
        class_4587 matrices,
        class_4588 buffer,
        double majorRadius,
        double minorRadius,
        double rotation,
        Color color,
        float alpha,
        int majorSegments,
        int minorSegments,
        float uvOffsetU,
        float uvOffsetV
    ) {
        matrices.method_22903();
        matrices.method_22907(class_7833.field_40716.rotationDegrees((float)Math.toDegrees(rotation)));
        Matrix4f matrix = matrices.method_23760().method_23761();
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;

        for (int i = 0; i < majorSegments; i++) {
            double theta0 = (Math.PI * 2) * i / majorSegments;
            double theta1 = (Math.PI * 2) * (i + 1) / majorSegments;
            double cosTheta0 = Math.cos(theta0);
            double sinTheta0 = Math.sin(theta0);
            double cosTheta1 = Math.cos(theta1);
            double sinTheta1 = Math.sin(theta1);

            for (int j = 0; j < minorSegments; j++) {
                double phi0 = (Math.PI * 2) * j / minorSegments;
                double phi1 = (Math.PI * 2) * (j + 1) / minorSegments;
                double cosPhi0 = Math.cos(phi0);
                double sinPhi0 = Math.sin(phi0);
                double cosPhi1 = Math.cos(phi1);
                double sinPhi1 = Math.sin(phi1);
                float x0 = (float)((majorRadius + minorRadius * cosPhi0) * cosTheta0);
                float y0 = (float)(minorRadius * sinPhi0);
                float z0 = (float)((majorRadius + minorRadius * cosPhi0) * sinTheta0);
                float x1 = (float)((majorRadius + minorRadius * cosPhi0) * cosTheta1);
                float y1 = (float)(minorRadius * sinPhi0);
                float z1 = (float)((majorRadius + minorRadius * cosPhi0) * sinTheta1);
                float x2 = (float)((majorRadius + minorRadius * cosPhi1) * cosTheta0);
                float y2 = (float)(minorRadius * sinPhi1);
                float z2 = (float)((majorRadius + minorRadius * cosPhi1) * sinTheta0);
                float x3 = (float)((majorRadius + minorRadius * cosPhi1) * cosTheta1);
                float y3 = (float)(minorRadius * sinPhi1);
                float z3 = (float)((majorRadius + minorRadius * cosPhi1) * sinTheta1);
                float u0 = (float)i / majorSegments + uvOffsetU;
                float u1 = (float)(i + 1) / majorSegments + uvOffsetU;
                float v0 = (float)j / minorSegments + uvOffsetV;
                float v1 = (float)(j + 1) / minorSegments + uvOffsetV;
                buffer.method_22918(matrix, x0, y0, z0).method_22915(r, g, b, alpha).method_22913(u0, v0);
                buffer.method_22918(matrix, x1, y1, z1).method_22915(r, g, b, alpha).method_22913(u1, v0);
                buffer.method_22918(matrix, x2, y2, z2).method_22915(r, g, b, alpha).method_22913(u0, v1);
                buffer.method_22918(matrix, x1, y1, z1).method_22915(r, g, b, alpha).method_22913(u1, v0);
                buffer.method_22918(matrix, x3, y3, z3).method_22915(r, g, b, alpha).method_22913(u1, v1);
                buffer.method_22918(matrix, x2, y2, z2).method_22915(r, g, b, alpha).method_22913(u0, v1);
            }
        }

        matrices.method_22909();
    }

    public static void renderTorusOptimized(
        class_4587 matrices,
        class_4588 buffer,
        double majorRadius,
        double minorRadius,
        double rotation,
        Color color,
        float alpha,
        int majorSegments,
        int minorSegments
    ) {
        Matrix4f matrix = matrices.method_23760().method_23761();
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;

        for (int i = 0; i < majorSegments; i++) {
            double angle0 = (Math.PI * 2) * i / majorSegments + rotation;
            double angle1 = (Math.PI * 2) * (i + 1) / majorSegments + rotation;
            double x0 = Math.cos(angle0) * majorRadius;
            double z0 = Math.sin(angle0) * majorRadius;
            double x1 = Math.cos(angle1) * majorRadius;
            double z1 = Math.sin(angle1) * majorRadius;
            double nx0 = Math.cos(angle0);
            double nz0 = Math.sin(angle0);
            double nx1 = Math.cos(angle1);
            double nz1 = Math.sin(angle1);

            for (int j = 0; j < minorSegments; j++) {
                double tubeAngle0 = (Math.PI * 2) * j / minorSegments;
                double tubeAngle1 = (Math.PI * 2) * (j + 1) / minorSegments;
                double ty0 = Math.sin(tubeAngle0) * minorRadius;
                double tr0 = Math.cos(tubeAngle0) * minorRadius;
                double ty1 = Math.sin(tubeAngle1) * minorRadius;
                double tr1 = Math.cos(tubeAngle1) * minorRadius;
                float px0 = (float)(x0 + nx0 * tr0);
                float py0 = (float)ty0;
                float pz0 = (float)(z0 + nz0 * tr0);
                float px1 = (float)(x1 + nx1 * tr0);
                float py1 = (float)ty0;
                float pz1 = (float)(z1 + nz1 * tr0);
                float px2 = (float)(x1 + nx1 * tr1);
                float py2 = (float)ty1;
                float pz2 = (float)(z1 + nz1 * tr1);
                float px3 = (float)(x0 + nx0 * tr1);
                float py3 = (float)ty1;
                float pz3 = (float)(z0 + nz0 * tr1);
                buffer.method_22918(matrix, px0, py0, pz0).method_22915(r, g, b, alpha);
                buffer.method_22918(matrix, px1, py1, pz1).method_22915(r, g, b, alpha);
                buffer.method_22918(matrix, px2, py2, pz2).method_22915(r, g, b, alpha);
                buffer.method_22918(matrix, px0, py0, pz0).method_22915(r, g, b, alpha);
                buffer.method_22918(matrix, px2, py2, pz2).method_22915(r, g, b, alpha);
                buffer.method_22918(matrix, px3, py3, pz3).method_22915(r, g, b, alpha);
            }
        }
    }

    public static void renderContinuousTubeTextured(
        class_4587 matrices,
        class_4588 buffer,
        List<class_243> pathPoints,
        double tubeRadius,
        Color color,
        float alpha,
        int sides,
        float uvOffsetU,
        float uvOffsetV
    ) {
        if (pathPoints.size() >= 2) {
            Matrix4f matrix = matrices.method_23760().method_23761();
            float r = color.getRed() / 255.0F;
            float g = color.getGreen() / 255.0F;
            float b = color.getBlue() / 255.0F;
            double totalLength = 0.0;

            for (int i = 0; i < pathPoints.size() - 1; i++) {
                totalLength += pathPoints.get(i).method_1022(pathPoints.get(i + 1));
            }

            double accumulatedLength = 0.0;

            for (int i = 0; i < pathPoints.size() - 1; i++) {
                class_243 p1 = pathPoints.get(i);
                class_243 p2 = pathPoints.get(i + 1);
                class_243 dir = p2.method_1020(p1).method_1029();
                class_243 up = Math.abs(dir.field_1351) < 0.9 ? new class_243(0.0, 1.0, 0.0) : new class_243(1.0, 0.0, 0.0);
                class_243 right = dir.method_1036(up).method_1029();
                class_243 actualUp = right.method_1036(dir).method_1029();
                double segmentLength = p1.method_1022(p2);
                float v0 = (float)(accumulatedLength / totalLength) + uvOffsetV;
                float v1 = (float)((accumulatedLength + segmentLength) / totalLength) + uvOffsetV;
                accumulatedLength += segmentLength;

                for (int j = 0; j < sides; j++) {
                    double angle1 = (Math.PI * 2) * j / sides;
                    double angle2 = (Math.PI * 2) * (j + 1) / sides;
                    class_243 offset1a = right.method_1021(Math.cos(angle1) * tubeRadius).method_1019(actualUp.method_1021(Math.sin(angle1) * tubeRadius));
                    class_243 offset1b = right.method_1021(Math.cos(angle2) * tubeRadius).method_1019(actualUp.method_1021(Math.sin(angle2) * tubeRadius));
                    class_243 v1a = p1.method_1019(offset1a);
                    class_243 v1b = p1.method_1019(offset1b);
                    class_243 v2a = p2.method_1019(offset1a);
                    class_243 v2b = p2.method_1019(offset1b);
                    float u0 = (float)j / sides + uvOffsetU;
                    float u1 = (float)(j + 1) / sides + uvOffsetU;
                    buffer.method_22918(matrix, (float)v1a.field_1352, (float)v1a.field_1351, (float)v1a.field_1350)
                        .method_22915(r, g, b, alpha)
                        .method_22913(u0, v0);
                    buffer.method_22918(matrix, (float)v1b.field_1352, (float)v1b.field_1351, (float)v1b.field_1350)
                        .method_22915(r, g, b, alpha)
                        .method_22913(u1, v0);
                    buffer.method_22918(matrix, (float)v2b.field_1352, (float)v2b.field_1351, (float)v2b.field_1350)
                        .method_22915(r, g, b, alpha)
                        .method_22913(u1, v1);
                    buffer.method_22918(matrix, (float)v1a.field_1352, (float)v1a.field_1351, (float)v1a.field_1350)
                        .method_22915(r, g, b, alpha)
                        .method_22913(u0, v0);
                    buffer.method_22918(matrix, (float)v2b.field_1352, (float)v2b.field_1351, (float)v2b.field_1350)
                        .method_22915(r, g, b, alpha)
                        .method_22913(u1, v1);
                    buffer.method_22918(matrix, (float)v2a.field_1352, (float)v2a.field_1351, (float)v2a.field_1350)
                        .method_22915(r, g, b, alpha)
                        .method_22913(u0, v1);
                }
            }
        }
    }
}
