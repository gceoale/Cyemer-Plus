package com.slither.cyemer.util.combat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;

@Environment(EnvType.CLIENT)
public class ProjectileSolver {
    public static float solvePitch(class_243 origin, class_243 target, float velocity, float gravity, float drag) {
        double dx = target.field_1352 - origin.field_1352;
        double dy = target.field_1351 - origin.field_1351;
        double dz = target.field_1350 - origin.field_1350;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float minPitch = -90.0F;
        float maxPitch = 90.0F;
        float bestPitch = 0.0F;
        double minDiff = Double.MAX_VALUE;

        for (int i = 0; i < 20; i++) {
            float pitch = (minPitch + maxPitch) / 2.0F;
            double landingY = simulateY(dist, pitch, velocity, gravity, drag);
            double diff = landingY - dy;
            if (Math.abs(diff) < minDiff) {
                minDiff = Math.abs(diff);
                bestPitch = pitch;
            }

            if (diff > 0.0) {
                minPitch = pitch;
            } else {
                maxPitch = pitch;
            }
        }

        return bestPitch;
    }

    private static double simulateY(double distance, float pitch, float velocity, float gravity, float drag) {
        double vy = Math.sin(Math.toRadians(-pitch)) * velocity;
        double vH = Math.cos(Math.toRadians(-pitch)) * velocity;
        double y = 0.0;
        double x = 0.0;

        for (int i = 0; i < 300; i++) {
            x += vH;
            y += vy;
            if (x >= distance) {
                double remaining = x - distance;
                return y - vy * (remaining / vH);
            }

            vy *= drag;
            vy -= gravity;
            vH *= drag;
        }

        return Double.NEGATIVE_INFINITY;
    }
}
