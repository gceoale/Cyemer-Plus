package com.slither.cyemer.module.implementation.movement;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_304;
import net.minecraft.class_332;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public class SafeWalk extends Module {
    private final SliderSetting minDistance = new SliderSetting("Min Distance", 0.1, 0.05, 1.0, 2);
    private final SliderSetting maxDistance = new SliderSetting("Max Distance", 0.3, 0.05, 1.0, 2);
    private final SliderSetting edgeDistance = new SliderSetting("Edge Distance", 0.3, 0.1, 2.0, 2);
    private final ModeSetting activationMode = new ModeSetting("Mode", "Backwards Only", "Always");
    private final BooleanSetting visualIndicator = new BooleanSetting("Visual Indicator", false);
    private boolean isCrouching = false;
    private double currentThreshold = 0.4;
    private long lastThresholdUpdate = 0L;
    private long lastCrouchToggle = 0L;
    private static final long THRESHOLD_UPDATE_INTERVAL = 500L;
    private static final long CROUCH_TOGGLE_COOLDOWN = 150L;

    public SafeWalk() {
        super("SafeWalk", "Automatically crouches near block edges", Category.MOVEMENT);
        this.addSetting(this.minDistance);
        this.addSetting(this.maxDistance);
        this.addSetting(this.edgeDistance);
        this.addSetting(this.activationMode);
        this.addSetting(this.visualIndicator);
    }

    @Override
    public void onEnable() {
        this.isCrouching = false;
        this.updateRandomThreshold();
    }

    @Override
    public void onDisable() {
        if (this.isCrouching && this.mc.field_1724 != null) {
            class_304.method_1416(this.mc.field_1690.field_1832.method_1429(), false);
            this.isCrouching = false;
        }
    }

    @Override
    public void onTick() {
        if (this.isEnabled() && this.mc.field_1724 != null && this.mc.field_1687 != null) {
            if (System.currentTimeMillis() - this.lastThresholdUpdate > 500L) {
                this.updateRandomThreshold();
            }

            boolean nearEdge = this.isNearEdge();
            long currentTime = System.currentTimeMillis();
            if (nearEdge) {
                if (!this.isCrouching && currentTime - this.lastCrouchToggle >= 150L) {
                    class_304.method_1416(this.mc.field_1690.field_1832.method_1429(), true);
                    this.isCrouching = true;
                    this.lastCrouchToggle = currentTime;
                }
            } else if (this.isCrouching && currentTime - this.lastCrouchToggle >= 150L) {
                class_304.method_1416(this.mc.field_1690.field_1832.method_1429(), false);
                this.isCrouching = false;
                this.lastCrouchToggle = currentTime;
            }
        }
    }

    @Override
    public void onRender(class_332 context, float tickDelta) {
        if (this.visualIndicator.isEnabled() && this.isCrouching) {
            int width = context.method_51421();
            int height = context.method_51443();
            String text = "[SAFEWALK ON]";
            int textWidth = this.mc.field_1772.method_1727(text);
            context.method_51433(this.mc.field_1772, text, (width - textWidth) / 2, height - 60, 16776960, true);
        }
    }

    private void updateRandomThreshold() {
        double min = this.minDistance.getValue();
        double max = this.maxDistance.getValue();
        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }

        this.currentThreshold = min + Math.random() * (max - min);
        this.lastThresholdUpdate = System.currentTimeMillis();
    }

    private boolean isMovingBackwards() {
        class_746 player = this.mc.field_1724;
        if (player == null) {
            return false;
        } else {
            class_243 velocity = player.method_18798();
            double motionX = velocity.field_1352;
            double motionZ = velocity.field_1350;
            double speed = Math.sqrt(motionX * motionX + motionZ * motionZ);
            if (speed < 0.01) {
                return false;
            } else {
                double movementYaw = Math.toDegrees(Math.atan2(motionZ, motionX)) - 90.0;
                float playerYaw = player.method_36454();
                double yawDifference = Math.abs(this.normalizeAngle(Math.toRadians(playerYaw - movementYaw)));
                return yawDifference > Math.toRadians(135.0) && yawDifference < Math.toRadians(225.0);
            }
        }
    }

    private double normalizeAngle(double angle) {
        while (angle > Math.PI) {
            angle -= Math.PI * 2;
        }

        while (angle < -Math.PI) {
            angle += Math.PI * 2;
        }

        return angle;
    }

    private boolean isNearEdge() {
        class_746 player = this.mc.field_1724;
        if (player != null && player.method_24828()) {
            class_243 pos = new class_243(player.method_23317(), player.method_23318(), player.method_23321());
            class_238 boundingBox = player.method_5829();
            double minX = boundingBox.field_1323;
            double maxX = boundingBox.field_1320;
            double minZ = boundingBox.field_1321;
            double maxZ = boundingBox.field_1324;
            double footY = boundingBox.field_1322 - 0.1;
            class_243[] checkPoints;
            if (this.activationMode.getCurrentMode().equals("Backwards Only")) {
                if (!this.isMovingBackwards()) {
                    return false;
                }

                checkPoints = this.getBackwardCheckPoints(pos, footY, boundingBox);
            } else {
                double checkDist = this.edgeDistance.getValue();
                checkPoints = new class_243[]{
                    new class_243(minX - checkDist, footY, minZ - checkDist),
                    new class_243(maxX + checkDist, footY, minZ - checkDist),
                    new class_243(minX - checkDist, footY, maxZ + checkDist),
                    new class_243(maxX + checkDist, footY, maxZ + checkDist),
                    new class_243(pos.field_1352, footY, minZ - checkDist),
                    new class_243(pos.field_1352, footY, maxZ + checkDist),
                    new class_243(minX - checkDist, footY, pos.field_1350),
                    new class_243(maxX + checkDist, footY, pos.field_1350)
                };
            }

            for (class_243 point : checkPoints) {
                class_2338 blockPos = class_2338.method_49637(point.field_1352, point.field_1351, point.field_1350);
                if (this.mc.field_1687.method_8320(blockPos).method_26215() && this.mc.field_1687.method_8320(blockPos.method_10074()).method_26215()) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    private class_243[] getBackwardCheckPoints(class_243 pos, double footY, class_238 boundingBox) {
        float yaw = this.mc.field_1724.method_36454();
        double radians = Math.toRadians(yaw);
        double checkDist = this.edgeDistance.getValue();
        double backwardX = -Math.sin(radians) * -checkDist;
        double backwardZ = Math.cos(radians) * -checkDist;
        double minX = boundingBox.field_1323;
        double maxX = boundingBox.field_1320;
        double minZ = boundingBox.field_1321;
        double maxZ = boundingBox.field_1324;
        double width = (maxX - minX) / 2.0;
        return new class_243[]{
            new class_243(pos.field_1352 + backwardX, footY, pos.field_1350 + backwardZ),
            new class_243(pos.field_1352 + backwardX + width, footY, pos.field_1350 + backwardZ),
            new class_243(pos.field_1352 + backwardX - width, footY, pos.field_1350 + backwardZ),
            new class_243(minX - checkDist * 0.3, footY, pos.field_1350),
            new class_243(maxX + checkDist * 0.3, footY, pos.field_1350),
            new class_243(pos.field_1352, footY, minZ - checkDist * 0.3),
            new class_243(pos.field_1352, footY, maxZ + checkDist * 0.3)
        };
    }
}
