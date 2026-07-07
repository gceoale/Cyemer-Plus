package com.slither.cyemer.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3532;

@Environment(EnvType.CLIENT)
public class RotationManager {
    private static final class_310 mc = class_310.method_1551();
    private static final SecureRandom random = new SecureRandom();
    private static Supplier<class_243> targetSupplier = null;
    private static boolean isActive = false;
    private static boolean isSilent = false;
    private static double baseStrength = 5.0;
    private static double randomness = 0.0;
    private static long reactionDelayMs = 0L;
    private static RotationManager.RotationMode mode = RotationManager.RotationMode.SMOOTH;
    private static boolean isReleasing = false;
    private static boolean isYawOnly = false;
    private static Object currentOwner = null;
    private static RotationManager.Priority currentPriority = RotationManager.Priority.LOWEST;
    private static float visualYaw;
    private static float visualPitch;
    private static float finalYaw;
    private static float finalPitch;
    private static float lastSetYaw;
    private static float lastSetPitch;
    private static long lastUpdateTime = -1L;
    private static final double TARGET_MS_PER_FRAME = 16.666666666666668;
    private static float velocityYaw = 0.0F;
    private static float velocityPitch = 0.0F;
    private static final float SMOOTHING_FACTOR = 0.25F;
    private static final float YAW_SMOOTHING = 0.22F;
    private static final float PITCH_SMOOTHING = 0.28F;
    private static float pitchLag = 0.0F;
    private static final float PITCH_LAG_FACTOR = 0.85F;
    private static long lastInputTime = 0L;
    private static final List<RotationManager.MouseInput> inputBuffer = new ArrayList<>();
    private static double accumulatedYaw = 0.0;
    private static double accumulatedPitch = 0.0;
    private static long nextReactionTime = 0L;
    private static boolean hasReacted = false;
    private static class_243 lastKnownTarget = null;
    private static long lastTargetUpdateTime = 0L;
    private static boolean lostTarget = false;
    private static long targetLossTime = 0L;
    private static float microAdjustmentYaw = 0.0F;
    private static float microAdjustmentPitch = 0.0F;
    private static long lastMicroAdjustment = 0L;
    private static int yawUpdateCounter = 0;
    private static int pitchUpdateCounter = 0;
    private static final Object LEGACY_OWNER = new Object();

    public static void setRotationSupplier(
        Object owner,
        RotationManager.Priority priority,
        Supplier<class_243> supplier,
        double str,
        RotationManager.RotationMode rotMode,
        double rand,
        boolean silent,
        boolean yawOnly
    ) {
        setRotationSupplier(owner, priority, supplier, str, rotMode, rand, 0L, silent, yawOnly);
    }

    public static void setRotationSupplier(
        Object owner,
        RotationManager.Priority priority,
        Supplier<class_243> supplier,
        double str,
        RotationManager.RotationMode rotMode,
        double rand,
        long reactionTime,
        boolean silent,
        boolean yawOnly
    ) {
        if (mc.field_1724 != null) {
            if (!isActive || currentOwner == owner || priority.ordinal() >= currentPriority.ordinal()) {
                if (!isActive || isReleasing || currentOwner != owner) {
                    isActive = true;
                    isReleasing = false;
                    currentOwner = owner;
                    currentPriority = priority;
                    float currentYaw = mc.field_1724.method_36454();
                    float currentPitch = mc.field_1724.method_36455();
                    visualYaw = currentYaw;
                    visualPitch = currentPitch;
                    finalYaw = currentYaw;
                    finalPitch = currentPitch;
                    lastSetYaw = currentYaw;
                    lastSetPitch = currentPitch;
                    velocityYaw = 0.0F;
                    velocityPitch = 0.0F;
                    pitchLag = 0.0F;
                    inputBuffer.clear();
                    accumulatedYaw = 0.0;
                    accumulatedPitch = 0.0;
                    hasReacted = reactionTime <= 0L;
                    nextReactionTime = System.currentTimeMillis() + reactionTime;
                    lastUpdateTime = System.currentTimeMillis();
                    yawUpdateCounter = 0;
                    pitchUpdateCounter = 0;
                }

                targetSupplier = supplier;
                baseStrength = str;
                mode = rotMode;
                randomness = rand;
                reactionDelayMs = reactionTime;
                isSilent = silent;
                isYawOnly = yawOnly;
            }
        }
    }

    public static void setRotationSupplier(Object owner, RotationManager.Priority priority, Supplier<class_243> supplier, double str, boolean silent) {
        setRotationSupplier(owner, priority, supplier, str, RotationManager.RotationMode.SMOOTH, 0.0, 0L, silent, false);
    }

    public static void clearTarget() {
        clearTarget(LEGACY_OWNER);
    }

    public static void stop() {
        forceStop();
    }

    public static void clearTarget(Object owner) {
        if (isActive && !isReleasing) {
            if (currentOwner == owner || currentOwner == null) {
                targetSupplier = null;
                isReleasing = true;
            }
        }
    }

    public static void stop(Object owner) {
        if (isActive && (currentOwner == null || currentOwner == owner)) {
            forceStop();
        }
    }

    private static void forceStop() {
        if (isSilent && mc.field_1724 != null) {
            mc.field_1724.method_36456(visualYaw);
            mc.field_1724.field_5982 = visualYaw;
            if (!isYawOnly) {
                mc.field_1724.method_36457(visualPitch);
                mc.field_1724.field_6004 = visualPitch;
            }
        }

        isActive = false;
        targetSupplier = null;
        isReleasing = false;
        currentOwner = null;
        currentPriority = RotationManager.Priority.LOWEST;
        velocityYaw = 0.0F;
        velocityPitch = 0.0F;
        pitchLag = 0.0F;
        inputBuffer.clear();
        accumulatedYaw = 0.0;
        accumulatedPitch = 0.0;
        lastKnownTarget = null;
    }

    public static void update(float tickDelta) {
        if (isActive && mc.field_1724 != null) {
            long currentTime = System.currentTimeMillis();
            double deltaTime = 1.0;
            if (lastUpdateTime > 0L) {
                double elapsed = currentTime - lastUpdateTime;
                deltaTime = elapsed / 16.666666666666668;
                // FPS mode wants raw per-frame time; the other modes rely on this clamp
                // to stay well-behaved when the frame budget spikes.
                if (mode != RotationManager.RotationMode.FPS) {
                    deltaTime = class_3532.method_15350(deltaTime, 0.5, 3.0);
                }
            }

            lastUpdateTime = currentTime;
            float mouseYawDelta = class_3532.method_15393(mc.field_1724.method_36454() - lastSetYaw);
            float mousePitchDelta = mc.field_1724.method_36455() - lastSetPitch;
            if (isSilent) {
                visualYaw += mouseYawDelta;
                visualPitch = class_3532.method_15363(visualPitch + mousePitchDelta, -90.0F, 90.0F);
            } else {
                visualYaw = mc.field_1724.method_36454();
                visualPitch = mc.field_1724.method_36455();
            }

            boolean isAiming = targetSupplier != null && !isReleasing;
            if (isAiming && !hasReacted) {
                if (currentTime < nextReactionTime) {
                    return;
                }

                hasReacted = true;
            }

            float targetYaw;
            float targetPitch;
            if (isAiming) {
                class_243 targetPos = targetSupplier.get();
                if (targetPos == null) {
                    isReleasing = true;
                    return;
                }

                float[] needed = calculateRotationsToPos(targetPos, finalYaw);
                targetYaw = needed[0];
                targetPitch = needed[1];
                if (randomness > 0.0 && currentTime - lastMicroAdjustment > 80 + random.nextInt(70)) {
                    microAdjustmentYaw = (float)(random.nextGaussian() * randomness * 0.15);
                    microAdjustmentPitch = (float)(random.nextGaussian() * randomness * 0.15);
                    lastMicroAdjustment = currentTime;
                }

                targetYaw += microAdjustmentYaw;
                targetPitch += microAdjustmentPitch;
            } else {
                if (!isReleasing) {
                    forceStop();
                    return;
                }

                targetYaw = visualYaw;
                targetPitch = visualPitch;
                if (Math.abs(class_3532.method_15393(targetYaw - finalYaw)) < 0.5F && Math.abs(targetPitch - finalPitch) < 0.5F) {
                    forceStop();
                    return;
                }
            }

            applyRotationStep(targetYaw, targetPitch, deltaTime);
            mc.field_1724.method_36456(finalYaw);
            if (!isYawOnly) {
                mc.field_1724.method_36457(finalPitch);
            }

            lastSetYaw = finalYaw;
            lastSetPitch = finalPitch;
        } else {
            if (isActive) {
                forceStop();
            }
        }
    }

    private static void applyRotationStep(float targetYaw, float targetPitch, double deltaTime) {
        float yawDiff = class_3532.method_15393(targetYaw - finalYaw);
        float pitchDiff = targetPitch - finalPitch;
        double absYaw = Math.abs(yawDiff);
        double absPitch = Math.abs(pitchDiff);
        double dist = Math.sqrt(absYaw * absYaw + absPitch * absPitch);
        if (mode == RotationManager.RotationMode.FPS) {
            if (dist < 1.0E-4) {
                return;
            }
            // Frame-rate-independent exponential lerp. alpha is the fraction of
            // remaining distance we cover this frame; strength scales the time
            // constant so higher strength = faster convergence. No GCD snap, no
            // velocity smoothing, no easing — those are what stepped visibly.
            double dtSeconds = deltaTime * (TARGET_MS_PER_FRAME / 1000.0);
            double alpha = 1.0 - Math.exp(-baseStrength * dtSeconds * 3.0);
            float yawStep = (float)(yawDiff * alpha);
            float pitchStep = (float)(pitchDiff * alpha);
            velocityYaw = yawStep;
            velocityPitch = pitchStep;
            finalYaw += yawStep;
            finalPitch = class_3532.method_15363(finalPitch + pitchStep, -90.0F, 90.0F);
            return;
        }
        if (!(dist < 0.05)) {
            double degreesPerSecond = baseStrength * 60.0;
            double baseSpeed = degreesPerSecond / 60.0 * deltaTime;
            double yawSpeedVar = 0.97 + random.nextDouble() * 0.06;
            double pitchSpeedVar = 0.96 + random.nextDouble() * 0.08;
            double yawSpeed = baseSpeed * yawSpeedVar;
            double pitchSpeed = baseSpeed * pitchSpeedVar * 0.85F;
            double yawEasing = calculateEasing(absYaw, dist);
            double pitchEasing = calculateEasing(absPitch, dist);
            if (mode == RotationManager.RotationMode.SINE) {
                double phaseOffset = 0.1;
                pitchEasing = Math.sin(absPitch / 90.0 * Math.PI / 2.0 + phaseOffset);
                pitchEasing = Math.max(pitchEasing, 0.2);
            }

            yawSpeed *= yawEasing;
            pitchSpeed *= pitchEasing;
            yawSpeed = Math.min(yawSpeed, absYaw);
            pitchSpeed = Math.min(pitchSpeed, absPitch);
            float desiredVelYaw = (float)(yawDiff / absYaw * yawSpeed);
            float desiredVelPitch = (float)(pitchDiff / absPitch * pitchSpeed);
            velocityYaw = class_3532.method_16439(0.22F, velocityYaw, desiredVelYaw);
            velocityPitch = class_3532.method_16439(0.28F, velocityPitch, desiredVelPitch);
            yawUpdateCounter++;
            pitchUpdateCounter++;
            float yawMove = velocityYaw;
            float pitchMove = velocityPitch;
            if (yawUpdateCounter % 7 == 0 && absYaw < 3.0) {
                yawMove *= 0.3F;
            }

            if (pitchUpdateCounter % 5 == 0 && absPitch < 2.0) {
                pitchMove *= 0.4F;
            }

            float finalYawMove = (float)applyGCD(yawMove);
            float finalPitchMove = (float)applyGCD(pitchMove);
            finalYaw += finalYawMove;
            finalPitch += finalPitchMove;
            finalPitch = class_3532.method_15363(finalPitch, -90.0F, 90.0F);
        }
    }

    private static double calculateEasing(double axisDist, double totalDist) {
        double easingFactor = 1.0;
        if (mode == RotationManager.RotationMode.SMOOTH) {
            double normalizedDist = Math.min(axisDist / 90.0, 1.0);
            easingFactor = 1.0 - Math.pow(1.0 - normalizedDist, 3.0);
            easingFactor = Math.max(easingFactor, 0.15);
        } else if (mode == RotationManager.RotationMode.SINE) {
            double normalizedDist = Math.min(axisDist / 90.0, 1.0);
            easingFactor = Math.sin(normalizedDist * Math.PI / 2.0);
            easingFactor = Math.max(easingFactor, 0.2);
        } else if (mode == RotationManager.RotationMode.LINEAR) {
            if (axisDist < 5.0) {
                easingFactor = Math.max(axisDist / 5.0, 0.3);
            }
        } else if (mode == RotationManager.RotationMode.INSTANT) {
            easingFactor = 1.0;
        }

        return easingFactor;
    }

    private static double applyGCD(double deltaRotation) {
        if (Math.abs(deltaRotation) < 0.001) {
            return 0.0;
        } else {
            float sensitivity = ((Double)mc.field_1690.method_42495().method_41753()).floatValue();
            float f = sensitivity * 0.6F + 0.2F;
            float gcd = f * f * f * 1.2F;
            long steps = Math.round(deltaRotation / gcd);
            return (float)steps * gcd;
        }
    }

    public static class_243 getCurrentRotation(Object owner) {
        if (isActive && currentOwner == owner && mc.field_1724 != null) {
            class_243 eyePos = mc.field_1724.method_33571();
            float yawRad = (float)Math.toRadians(finalYaw);
            float pitchRad = (float)Math.toRadians(finalPitch);
            double xDir = -Math.sin(yawRad) * Math.cos(pitchRad);
            double yDir = -Math.sin(pitchRad);
            double zDir = Math.cos(yawRad) * Math.cos(pitchRad);
            class_243 direction = new class_243(xDir, yDir, zDir).method_1029();
            return eyePos.method_1019(direction.method_1021(100.0));
        } else {
            return null;
        }
    }

    public static float[] calculateRotationsToPos(class_243 targetPos, float currentYaw) {
        if (mc.field_1724 == null) {
            return new float[]{0.0F, 0.0F};
        } else {
            class_243 playerPos = mc.field_1724.method_33571();
            double deltaX = targetPos.field_1352 - playerPos.field_1352;
            double deltaY = targetPos.field_1351 - playerPos.field_1351;
            double deltaZ = targetPos.field_1350 - playerPos.field_1350;
            double dist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            float baseYaw = (float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
            float pitch = (float)(-Math.toDegrees(Math.atan2(deltaY, dist)));
            float yawDiff = baseYaw - currentYaw;
            float unwrappedYaw = currentYaw + class_3532.method_15393(yawDiff);
            return new float[]{unwrappedYaw, class_3532.method_15363(pitch, -90.0F, 90.0F)};
        }
    }

    public static boolean isActive() {
        return isActive;
    }

    public static boolean isSilentRotationActive() {
        return isActive && isSilent;
    }

    public static float getVisualYaw() {
        return isActive ? visualYaw : (mc.field_1724 != null ? mc.field_1724.method_36454() : 0.0F);
    }

    public static float getVisualPitch() {
        return isActive ? visualPitch : (mc.field_1724 != null ? mc.field_1724.method_36455() : 0.0F);
    }

    public static float getFinalYaw() {
        return isActive ? finalYaw : (mc.field_1724 != null ? mc.field_1724.method_36454() : 0.0F);
    }

    public static float getFinalPitch() {
        return isActive ? finalPitch : (mc.field_1724 != null ? mc.field_1724.method_36455() : 0.0F);
    }

    public static boolean isRotationComplete(float threshold) {
        if (isActive && targetSupplier != null && !isReleasing) {
            class_243 targetPos = targetSupplier.get();
            if (targetPos == null) {
                return false;
            } else {
                float[] targetRots = calculateRotationsToPos(targetPos, finalYaw);
                float yawDiff = Math.abs(class_3532.method_15393(finalYaw - targetRots[0]));
                float pitchDiff = Math.abs(class_3532.method_15393(finalPitch - targetRots[1]));
                return yawDiff < threshold && (isYawOnly || pitchDiff < threshold);
            }
        } else {
            return false;
        }
    }

    @Environment(EnvType.CLIENT)
    private static class MouseInput {
        double yaw;
        double pitch;
        long timestamp;

        MouseInput(double y, double p, long t) {
            this.yaw = y;
            this.pitch = p;
            this.timestamp = t;
        }
    }

    @Environment(EnvType.CLIENT)
    public static enum Priority {
        LOWEST,
        LOW,
        NORMAL,
        HIGH,
        HIGHEST;
    }

    @Environment(EnvType.CLIENT)
    public static enum RotationMode {
        INSTANT,
        SMOOTH,
        SINE,
        LINEAR,
        FPS;
    }
}
