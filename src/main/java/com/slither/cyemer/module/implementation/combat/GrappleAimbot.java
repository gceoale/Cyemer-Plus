package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.event.EventBus;
import com.slither.cyemer.event.impl.PearlThrowEvent;
import com.slither.cyemer.friend.FriendManager;
import com.slither.cyemer.mixin.MinecraftClientAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.RotationManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1792;
import net.minecraft.class_1802;
import net.minecraft.class_243;
import net.minecraft.class_3532;

@Environment(EnvType.CLIENT)
public class GrappleAimbot extends Module {
    private final SliderSetting range = new SliderSetting("Range", 25.0, 5.0, 50.0, 1);
    private final SliderSetting fov = new SliderSetting("FOV", 90.0, 10.0, 180.0, 0);
    private final BooleanSetting requireHoldingPearl = new BooleanSetting("Require Holding Pearl", true);
    private final BooleanSetting autoThrow = new BooleanSetting("Auto Throw", true);
    private final BooleanSetting ignoreFriends = new BooleanSetting("Ignore Friends", true);
    private final SliderSetting aimSpeed = new SliderSetting("Aim Speed", 15.0, 1.0, 30.0, 1);
    private final ModeSetting aimMode = new ModeSetting("Aim Mode", "Smooth", "Instant", "Sine");
    private final ModeSetting aimTarget = new ModeSetting("Aim Target", "Chest", "Legs", "Head");
    private class_1297 target = null;
    private int throwCooldown = 0;

    public GrappleAimbot() {
        super("GrappleAimbot", "Aims and throws pearls at enemies in the air.", Category.COMBAT);
        this.addSetting(this.range);
        this.addSetting(this.fov);
        this.addSetting(this.requireHoldingPearl);
        this.addSetting(this.autoThrow);
        this.addSetting(this.ignoreFriends);
        this.addSetting(this.aimSpeed);
        this.addSetting(this.aimMode);
        this.addSetting(this.aimTarget);
    }

    @Override
    public void onEnable() {
        this.target = null;
        this.throwCooldown = 0;
    }

    @Override
    public void onDisable() {
        RotationManager.stop(this);
        this.target = null;
        this.throwCooldown = 0;
    }

    @Override
    public void onTick() {
        if (this.isEnabled() && this.mc.field_1724 != null && this.mc.field_1687 != null) {
            if (this.throwCooldown > 0) {
                this.throwCooldown--;
            }

            if (this.requireHoldingPearl.isEnabled() && this.mc.field_1724.method_6047().method_7909() != class_1802.field_8634) {
                RotationManager.stop(this);
            } else {
                this.target = this.findBestTarget();
                if (this.target != null) {
                    this.performAiming(this.target);
                    if (this.autoThrow.isEnabled() && this.throwCooldown <= 0 && this.isAimingAt(this.target)) {
                        int pearlSlot = this.findItemSlot(class_1802.field_8634);
                        if (pearlSlot != -1 && this.mc.field_1724.method_31548().method_67532() == pearlSlot) {
                            EventBus.post(new PearlThrowEvent());
                            ((MinecraftClientAccessor)this.mc).useItem();
                            this.throwCooldown = 10;
                        }
                    }
                } else {
                    RotationManager.stop(this);
                }
            }
        } else {
            RotationManager.stop(this);
        }
    }

    private boolean isAimingAt(class_1297 target) {
        class_243 eyePos = this.mc.field_1724.method_33571();
        class_243 lookVec = this.mc.field_1724.method_5828(1.0F);
        class_243 aimPoint = this.calculateAimPoint(target);
        class_243 toTarget = aimPoint.method_1020(eyePos).method_1029();
        double dot = lookVec.method_1026(toTarget);
        return dot > 0.99;
    }

    private class_1297 findBestTarget() {
        class_1297 bestEntity = null;
        double bestAngle = this.fov.getValue();

        for (class_1297 entity : this.mc.field_1687.method_18112()) {
            if (this.isValidTarget(entity)) {
                double angle = this.getAngleToEntity(entity);
                if (angle < bestAngle) {
                    bestAngle = angle;
                    bestEntity = entity;
                }
            }
        }

        return bestEntity;
    }

    private double getAngleToEntity(class_1297 entity) {
        class_243 playerPos = this.mc.field_1724.method_33571();
        class_243 entityPos = entity.method_5829().method_1005();
        double deltaX = entityPos.field_1352 - playerPos.field_1352;
        double deltaZ = entityPos.field_1350 - playerPos.field_1350;
        float yaw = (float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
        return Math.abs(class_3532.method_15393(this.mc.field_1724.method_36454() - yaw));
    }

    private int findItemSlot(class_1792 item) {
        for (int i = 0; i < 9; i++) {
            if (this.mc.field_1724.method_31548().method_5438(i).method_7909() == item) {
                return i;
            }
        }

        return -1;
    }

    private boolean isValidTarget(class_1297 entity) {
        if (entity instanceof class_1309 && entity != this.mc.field_1724 && entity.method_5805()) {
            if (this.mc.field_1724.method_5739(entity) > this.range.getValue()) {
                return false;
            } else if (entity.method_24828()) {
                return false;
            } else {
                return this.ignoreFriends.isEnabled() && entity instanceof class_1657 player
                    ? !FriendManager.getInstance().isFriend(player.method_5667())
                    : true;
            }
        } else {
            return false;
        }
    }

    private void performAiming(class_1297 target) {
        class_243 targetPoint = this.calculateAimPoint(target);
        String modeStr = this.aimMode.getCurrentMode();
        RotationManager.RotationMode mode;
        if ("Instant".equals(modeStr)) {
            mode = RotationManager.RotationMode.INSTANT;
        } else if ("Sine".equals(modeStr)) {
            mode = RotationManager.RotationMode.SINE;
        } else {
            mode = RotationManager.RotationMode.SMOOTH;
        }

        RotationManager.setRotationSupplier(this, RotationManager.Priority.HIGH, () -> targetPoint, this.aimSpeed.getValue(), mode, 0.0, false, false);
    }

    private class_243 calculateAimPoint(class_1297 entity) {
        double targetHeight = entity.method_17682() * this.getAimTargetHeightMultiplier();
        double x = class_3532.method_16436(this.mc.method_61966().method_60637(false), entity.field_6038, entity.method_23317());
        double y = class_3532.method_16436(this.mc.method_61966().method_60637(false), entity.field_5971, entity.method_23318());
        double z = class_3532.method_16436(this.mc.method_61966().method_60637(false), entity.field_5989, entity.method_23321());
        return new class_243(x, y + targetHeight, z);
    }

    private double getAimTargetHeightMultiplier() {
        String mode = this.aimTarget.getCurrentMode();
        if ("Legs".equals(mode)) {
            return 0.08;
        } else {
            return "Head".equals(mode) ? 0.74 : 0.4;
        }
    }
}
