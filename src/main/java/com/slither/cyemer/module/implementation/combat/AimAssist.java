package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.friend.FriendManager;
import com.slither.cyemer.manager.TargetManager;
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
import net.minecraft.class_1799;
import net.minecraft.class_1835;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3489;
import net.minecraft.class_3532;
import net.minecraft.class_3675;
import net.minecraft.class_3966;
import net.minecraft.class_9362;

@Environment(EnvType.CLIENT)
public class AimAssist extends Module {
    private final SliderSetting strength = new SliderSetting("Strength", 5.0, 0.0, 20.0, 1);
    private final ModeSetting rotationMode = new ModeSetting("Pattern", "Sine", "Smooth", "Linear", "FPS");
    private final SliderSetting randomness = new SliderSetting("Randomness", 0.35, 0.0, 1.0, 2);
    private final ModeSetting stopMode = new ModeSetting("Stop Mode", "Hitbox Edge", "Off", "Exact Center");
    private final SliderSetting hitboxPadding = new SliderSetting("Hitbox Padding", 0.0, -0.5, 0.0, 2);
    private final SliderSetting distance = new SliderSetting("Distance", 4.5, 3.0, 8.0, 1);
    private final SliderSetting fov = new SliderSetting("FOV", 90.0, 10.0, 180.0, 0);
    private final BooleanSetting visibleOnly = new BooleanSetting("Visible Only", true);
    private final SliderSetting targetDelay = new SliderSetting("Switch Delay (ms)", 200.0, 0.0, 1000.0, 0);
    private final BooleanSetting playersOnly = new BooleanSetting("Players Only", true);
    private final BooleanSetting onlyWeapon = new BooleanSetting("Only Weapon", true);
    private final BooleanSetting onlyOnBind = new BooleanSetting("Only On Bind", false);
    private final BooleanSetting stickyMode = new BooleanSetting("Sticky Mode", false);
    private final BooleanSetting ignoreFriends = new BooleanSetting("Ignore Friends", true);
    private final BooleanSetting targetMode = new BooleanSetting("Target Mode", false);
    private class_1297 finalTarget = null;
    private long lastTargetSwitchTime = 0L;
    private boolean wasActiveLastTick = false;

    public AimAssist() {
        super("AimAssist", "Smooth =/= good, anticheats care about MATH. Test configs before you complain about a ban.", Category.COMBAT);
        this.addSetting(this.strength);
        this.addSetting(this.rotationMode);
        this.addSetting(this.randomness);
        this.addSetting(this.stopMode);
        this.addSetting(this.hitboxPadding);
        this.addSetting(this.distance);
        this.addSetting(this.fov);
        this.addSetting(this.visibleOnly);
        this.addSetting(this.targetDelay);
        this.addSetting(this.playersOnly);
        this.addSetting(this.onlyWeapon);
        this.addSetting(this.onlyOnBind);
        this.addSetting(this.stickyMode);
        this.addSetting(this.ignoreFriends);
        this.addSetting(this.targetMode);
    }

    @Override
    public void onDisable() {
        RotationManager.stop(this);
        this.finalTarget = null;
        this.wasActiveLastTick = false;
    }

    @Override
    public void onTick() {
        if (this.isEnabled()) {
            boolean isHoldingKey = false;
            if (this.onlyOnBind.isEnabled()) {
                int keyCode = this.getKeyCode();
                if (keyCode != -1) {
                    isHoldingKey = class_3675.method_15987(class_310.method_1551().method_22683(), keyCode);
                }
            }

            boolean shouldBeActiveNow = !this.onlyOnBind.isEnabled() || isHoldingKey;
            if (shouldBeActiveNow) {
                if (this.mc.field_1687 == null
                    || this.mc.field_1724 == null
                    || this.mc.field_1755 != null
                    || this.onlyWeapon.isEnabled() && !this.isHoldingWeapon()) {
                    RotationManager.clearTarget(this);
                    return;
                }

                this.wasActiveLastTick = true;
                this.findBestTarget();
                if (this.finalTarget != null) {
                    String currentStopMode = this.stopMode.getCurrentMode();
                    if (currentStopMode.equals("Exact Center") && this.isAimingAtEntity(this.finalTarget)) {
                        RotationManager.clearTarget(this);
                        return;
                    }

                    if (currentStopMode.equals("Hitbox Edge") && this.isCrosshairOnHitbox(this.finalTarget)) {
                        RotationManager.clearTarget(this);
                        return;
                    }

                    Supplier<class_243> targetSupplier = () -> this.calculateAimPoint(this.finalTarget);
                    RotationManager.RotationMode mode = RotationManager.RotationMode.SMOOTH;

                    try {
                        mode = RotationManager.RotationMode.valueOf(this.rotationMode.getCurrentMode().toUpperCase());
                    } catch (Exception var7) {
                    }

                    RotationManager.setRotationSupplier(
                        this, RotationManager.Priority.LOW, targetSupplier, this.strength.getValue(), mode, this.randomness.getValue(), false, true
                    );
                } else {
                    RotationManager.clearTarget(this);
                }
            } else if (this.wasActiveLastTick) {
                RotationManager.clearTarget(this);
                this.finalTarget = null;
                this.wasActiveLastTick = false;
            }
        }
    }

    private void findBestTarget() {
        if (!this.stickyMode.isEnabled() || this.finalTarget == null || !this.isPotentiallyValid(this.finalTarget, true)) {
            class_1297 bestMatch = null;
            double bestAngle = this.fov.getValue();
            if (this.finalTarget != null && this.isPotentiallyValid(this.finalTarget, false)) {
                bestMatch = this.finalTarget;
                bestAngle = this.getAngleToEntity(this.finalTarget);
            }

            if (System.currentTimeMillis() - this.lastTargetSwitchTime > this.targetDelay.getValue()) {
                for (class_1297 entity : this.mc.field_1687.method_18112()) {
                    if (entity != this.finalTarget && this.isPotentiallyValid(entity, false)) {
                        double angle = this.getAngleToEntity(entity);
                        if (angle < bestAngle) {
                            bestMatch = entity;
                            bestAngle = angle;
                        }
                    }
                }
            }

            if (bestMatch != this.finalTarget) {
                this.lastTargetSwitchTime = System.currentTimeMillis();
                this.finalTarget = bestMatch;
            }
        }
    }

    private boolean isPotentiallyValid(class_1297 entity, boolean ignoreFov) {
        if (entity instanceof class_1309 && entity != this.mc.field_1724 && entity.method_5805()) {
            if (this.mc.field_1724.method_5739(entity) > this.distance.getValue()) {
                return false;
            } else if (this.playersOnly.isEnabled() && !(entity instanceof class_1657)) {
                return false;
            } else if (this.visibleOnly.isEnabled() && !this.mc.field_1724.method_6057(entity)) {
                return false;
            } else if (this.ignoreFriends.isEnabled() && entity instanceof class_1657 player && FriendManager.getInstance().isFriend(player.method_5667())) {
                return false;
            } else if (this.targetMode.isEnabled() && !TargetManager.isLocked(entity)) {
                return false;
            } else {
                return ignoreFov ? true : this.getAngleToEntity(entity) <= this.fov.getValue();
            }
        } else {
            return false;
        }
    }

    private double getAngleToEntity(class_1297 entity) {
        class_243 playerPos = this.mc.field_1724.method_33571();
        class_243 entityPos = entity.method_5829().method_1005();
        double deltaX = entityPos.field_1352 - playerPos.field_1352;
        double deltaZ = entityPos.field_1350 - playerPos.field_1350;
        float yaw = (float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
        return Math.abs(class_3532.method_15393(this.mc.field_1724.method_36454() - yaw));
    }

    private boolean isAimingAtEntity(class_1297 entity) {
        return this.mc.field_1765 instanceof class_3966 entityHit ? entityHit.method_17782() == entity : false;
    }

    private boolean isCrosshairOnHitbox(class_1297 entity) {
        if (this.mc.field_1724 == null) {
            return false;
        } else {
            class_243 eyePos = this.mc.field_1724.method_33571();
            class_243 lookVec = this.mc.field_1724.method_5828(1.0F);
            double reachDistance = this.distance.getValue();
            class_243 endPos = eyePos.method_1019(lookVec.method_1021(reachDistance));
            class_238 hitbox = entity.method_5829().method_1014(this.hitboxPadding.getValue());
            return hitbox.method_992(eyePos, endPos).isPresent();
        }
    }

    private boolean isHoldingWeapon() {
        if (this.mc.field_1724 == null) {
            return false;
        } else {
            class_1799 stack = this.mc.field_1724.method_6047();
            return stack.method_31573(class_3489.field_42611)
                || stack.method_31573(class_3489.field_42612)
                || stack.method_7909() instanceof class_9362
                || stack.method_7909() instanceof class_1835;
        }
    }

    private class_243 calculateAimPoint(class_1297 entity) {
        double targetHeight = entity.method_17682() * 0.5;
        double x = class_3532.method_16436(this.mc.method_61966().method_60637(false), entity.field_6038, entity.method_23317());
        double y = class_3532.method_16436(this.mc.method_61966().method_60637(false), entity.field_5971, entity.method_23318());
        double z = class_3532.method_16436(this.mc.method_61966().method_60637(false), entity.field_5989, entity.method_23321());
        return new class_243(x, y + targetHeight, z);
    }
}
