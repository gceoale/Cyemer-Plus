package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.friend.FriendManager;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.RotationManager;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1753;
import net.minecraft.class_1764;
import net.minecraft.class_1799;
import net.minecraft.class_243;
import net.minecraft.class_3532;

@Environment(EnvType.CLIENT)
public class BowAimbot extends Module {
    private static final double ARROW_GRAVITY = 0.05;
    private static final float ARROW_DRAG = 0.99F;
    private static final int MAX_SIMULATION_TICKS = 1200;
    private static final double EPSILON = 1.0E-4;
    private final SliderSetting range = new SliderSetting("Range", 60.0, 10.0, 100.0, 0);
    private final SliderSetting fov = new SliderSetting("FOV", 120.0, 10.0, 360.0, 0);
    private final ModeSetting targetPart = new ModeSetting("Aim At", "Auto", "Chest", "Head", "Legs");
    private final SliderSetting autoHeightThreshold = new SliderSetting("Auto Height Diff", 3.0, 0.0, 50.0, 0);
    private final SliderSetting autoDistanceThreshold = new SliderSetting("Auto Distance", 12.0, 0.0, 50.0, 0);
    private final BooleanSetting visibleOnly = new BooleanSetting("Visible Only", true);
    private final BooleanSetting ignoreFriends = new BooleanSetting("Ignore Friends", true);
    private final ModeSetting rotationMode = new ModeSetting("Rotation", "Smooth", "Sine", "Linear");
    private final SliderSetting rotationSpeed = new SliderSetting("Rotation Speed", 10.0, 1.0, 20.0, 1);
    private final SliderSetting randomness = new SliderSetting("Randomness", 0.0, 0.0, 1.0, 2);
    private class_1297 currentTarget;

    public BowAimbot() {
        super("BowAimbot", "Automatically aims bows for you.", Category.COMBAT);
        this.addSetting(this.range);
        this.addSetting(this.fov);
        this.addSetting(this.targetPart);
        this.addSetting(this.autoHeightThreshold);
        this.addSetting(this.autoDistanceThreshold);
        this.addSetting(this.visibleOnly);
        this.addSetting(this.ignoreFriends);
        this.addSetting(this.rotationMode);
        this.addSetting(this.rotationSpeed);
        this.addSetting(this.randomness);
    }

    @Override
    public void onDisable() {
        RotationManager.stop(this);
        this.currentTarget = null;
    }

    @Override
    public void onTick() {
        if (this.isEnabled() && this.mc.field_1724 != null && this.mc.field_1687 != null) {
            class_1799 mainHand = this.mc.field_1724.method_6047();
            boolean isBow = mainHand.method_7909() instanceof class_1753;
            boolean isCrossbow = mainHand.method_7909() instanceof class_1764;
            boolean isValidWeapon = isBow || isCrossbow;
            if (!isValidWeapon) {
                RotationManager.stop(this);
            } else {
                boolean isAimingBow = isBow && this.mc.field_1724.method_6115() && this.mc.field_1724.method_6030() == mainHand;
                boolean isChargedCrossbow = isCrossbow && class_1764.method_7781(mainHand);
                if (!isAimingBow && !isChargedCrossbow) {
                    RotationManager.stop(this);
                } else {
                    this.findTarget();
                    if (this.currentTarget != null) {
                        this.performAiming();
                    } else {
                        RotationManager.stop(this);
                    }
                }
            }
        } else {
            RotationManager.stop(this);
        }
    }

    private void performAiming() {
        float solvedPitch = this.solvePitchForTarget(this.currentTarget);
        class_243 targetPoint = this.getAimPointForPitch(this.currentTarget, solvedPitch);
        Supplier<class_243> targetSupplier = () -> targetPoint;

        RotationManager.RotationMode mode;
        try {
            mode = RotationManager.RotationMode.valueOf(this.rotationMode.getCurrentMode().toUpperCase());
        } catch (Exception var6) {
            mode = RotationManager.RotationMode.SMOOTH;
        }

        RotationManager.setRotationSupplier(
            this, RotationManager.Priority.HIGH, targetSupplier, this.rotationSpeed.getValue(), mode, this.randomness.getValue(), false, false
        );
    }

    private void findTarget() {
        class_1297 bestEntity = null;
        double bestDistance = this.range.getValue();
        double maxAngle = this.fov.getValue() / 2.0;

        for (class_1297 entity : this.mc.field_1687.method_18112()) {
            if (this.isValidTarget(entity)) {
                double distance = this.mc.field_1724.method_5739(entity);
                if (!(distance > this.range.getValue()) && (!this.visibleOnly.isEnabled() || this.mc.field_1724.method_6057(entity))) {
                    double angle = this.getAngleToEntity(entity);
                    if (!(angle > maxAngle) && distance < bestDistance) {
                        bestDistance = distance;
                        bestEntity = entity;
                    }
                }
            }
        }

        this.currentTarget = bestEntity;
    }

    private boolean isValidTarget(class_1297 entity) {
        if (!(entity instanceof class_1309) || entity == this.mc.field_1724 || !entity.method_5805()) {
            return false;
        } else {
            return this.ignoreFriends.isEnabled() && entity instanceof class_1657 player ? !FriendManager.getInstance().isFriend(player.method_5667()) : true;
        }
    }

    private class_243 getAimPointForPitch(class_1297 target, float pitch) {
        class_243 eyePos = this.mc.field_1724.method_33571();
        class_243 predictedPos = this.getPredpos(target);
        double dx = predictedPos.field_1352 - eyePos.field_1352;
        double dz = predictedPos.field_1350 - eyePos.field_1350;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        double yOffset = Math.tan(Math.toRadians(-pitch)) * horizontalDist;
        return new class_243(predictedPos.field_1352, eyePos.field_1351 + yOffset, predictedPos.field_1350);
    }

    private class_243 getPredpos(class_1297 target) {
        double t = this.mc.method_61966().method_60637(false);
        double x = class_3532.method_16436(t, target.field_6038, target.method_23317());
        double y = class_3532.method_16436(t, target.field_5971, target.method_23318());
        double z = class_3532.method_16436(t, target.field_5989, target.method_23321());
        return new class_243(x, y, z);
    }

    private float solvePitchForTarget(class_1297 target) {
        class_1799 stack = this.mc.field_1724.method_6047();
        float velocity;
        if (stack.method_7909() instanceof class_1764 && class_1764.method_7781(stack)) {
            velocity = 3.15F;
        } else if (stack.method_7909() instanceof class_1753 && this.mc.field_1724.method_6115()) {
            int useTicks = this.mc.field_1724.method_6048();
            float charge = useTicks / 20.0F;
            charge = (charge * charge + charge * 2.0F) / 3.0F;
            charge = Math.min(charge, 1.0F);
            velocity = charge * 3.0F;
            velocity = Math.max(velocity, 0.1F);
        } else {
            velocity = 3.0F;
        }

        class_243 eyePos = this.mc.field_1724.method_33571();
        class_243 predictedPos = this.getPredpos(target);
        double targetY = predictedPos.field_1351 + this.getYOffset(target);
        double dx = predictedPos.field_1352 - eyePos.field_1352;
        double dz = predictedPos.field_1350 - eyePos.field_1350;
        double dy = targetY - eyePos.field_1351;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float directPitch = (float)Math.toDegrees(Math.atan2(dy, horizontalDist));
        double directHitY = this.simulateArrow(velocity, directPitch, horizontalDist);
        return Math.abs(directHitY - dy) < target.method_17682() * 0.6 ? -directPitch : -this.binSearch(velocity, horizontalDist, dy, directPitch);
    }

    private double getYOffset(class_1297 target) {
        String part = this.targetPart.getCurrentMode();
        if (part.equals("Auto")) {
            part = this.determinePart(target);
        }
        return switch (part) {
            case "Head" -> target.method_17682() * 0.85;
            case "Legs" -> target.method_17682() * 0.1;
            default -> target.method_17682() * 0.5;
        };
    }

    private String determinePart(class_1297 target) {
        return "Head";
    }

    private float binSearch(float velocity, double horizontalDist, double targetY, float directPitch) {
        float minPitch = -89.0F;
        float maxPitch = 89.0F;
        float bestPitch = directPitch;
        double minError = Double.MAX_VALUE;

        for (int i = 0; i < 50; i++) {
            float midPitch = (minPitch + maxPitch) / 2.0F;
            double hitY = this.simulateArrow(velocity, midPitch, horizontalDist);
            if (Double.isInfinite(hitY)) {
                maxPitch = midPitch;
            } else {
                double error = Math.abs(hitY - targetY);
                if (error < minError) {
                    minError = error;
                    bestPitch = midPitch;
                }

                if (error < 0.01) {
                    break;
                }

                if (hitY < targetY) {
                    minPitch = midPitch;
                } else {
                    maxPitch = midPitch;
                }
            }
        }

        return bestPitch;
    }

    private double simulateArrow(float speed, float pitch, double targetDist) {
        double radPitch = Math.toRadians(pitch);
        double vx = speed * Math.cos(radPitch);
        double vy = speed * Math.sin(radPitch);
        double x = 0.0;
        double y = 0.0;

        for (int tick = 0; tick < 1200; tick++) {
            double prevX = x;
            double prevY = y;
            x += vx;
            y += vy;
            if (x >= targetDist) {
                double delta = x - prevX;
                if (Math.abs(delta) < 1.0E-4) {
                    return y;
                }

                double ratio = (targetDist - prevX) / delta;
                return prevY + (y - prevY) * ratio;
            }

            vx *= 0.99F;
            vy *= 0.99F;
            vy -= 0.05;
        }

        return Double.NEGATIVE_INFINITY;
    }

    private double getAngleToEntity(class_1297 entity) {
        class_243 playerPos = this.mc.field_1724.method_33571();
        class_243 entityPos = entity.method_5829().method_1005();
        double deltaX = entityPos.field_1352 - playerPos.field_1352;
        double deltaZ = entityPos.field_1350 - playerPos.field_1350;
        float targetYaw = (float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
        return Math.abs(class_3532.method_15393(this.mc.field_1724.method_36454() - targetYaw));
    }
}
