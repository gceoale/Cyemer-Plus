package com.slither.cyemer.module.implementation;

import com.slither.cyemer.mixin.KeyBindingAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.RotationManager;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1792;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_3532;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_239.class_240;
import net.minecraft.class_3959.class_242;
import net.minecraft.class_3959.class_3960;

@Environment(EnvType.CLIENT)
public class AutoDrain extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "Blatant", "Legit");
    private final SliderSetting rangeDistance = new SliderSetting("Range", 4.5, 3.0, 6.0, 1);
    private final SliderSetting legitFOV = new SliderSetting("Legit FOV", 45.0, 10.0, 90.0, 0);
    private final BooleanSetting silentMode = new BooleanSetting("Silent", true);
    private final SliderSetting rotationStrength = new SliderSetting("Strength", 10.0, 1.0, 20.0, 1);
    private final ModeSetting rotPattern = new ModeSetting("Pattern", "Smooth", "Sine", "Linear");
    private final SliderSetting rotRandom = new SliderSetting("Randomness", 0.1, 0.0, 1.0, 2);
    private final SliderSetting startDelay = new SliderSetting("Start Delay", 75.0, 0.0, 100.0, 1);
    private final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", true);
    private final BooleanSetting ignoreOwnWater = new BooleanSetting("Ignore Own Water", true);
    private AutoDrain.State currentState = AutoDrain.State.IDLE;
    private class_2338 targetWaterPos = null;
    private int originalSlot = -1;
    private long lastActionTime = 0L;
    private final Set<class_2338> placedWater = new HashSet<>();
    private static final long ROTATION_WAIT_MS = 50L;
    private static final long COOLDOWN_MS = 150L;

    public AutoDrain() {
        super("AutoDrain", "Automatically drains water.", Category.PLAYER);
        this.addSetting(this.mode);
        this.addSetting(this.rangeDistance);
        this.addSetting(this.legitFOV);
        this.addSetting(this.silentMode);
        this.addSetting(this.rotationStrength);
        this.addSetting(this.rotPattern);
        this.addSetting(this.rotRandom);
        this.addSetting(this.startDelay);
        this.addSetting(this.autoSwitch);
        this.addSetting(this.ignoreOwnWater);
    }

    @Override
    public void onEnable() {
        this.reset();
    }

    @Override
    public void onDisable() {
        RotationManager.stop(this);
        this.restoreOriginalSlot();
        this.reset();
        this.placedWater.clear();
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null && this.mc.field_1755 == null) {
            this.updateLogic();
        }
    }

    public void onItemUse(class_3965 hitResult) {
        if (this.ignoreOwnWater.isEnabled() && this.mc.field_1724 != null) {
            if (this.mc.field_1724.method_6047().method_7909() == class_1802.field_8705) {
                class_2338 targetPos = hitResult.method_17777();
                class_2680 state = this.mc.field_1687.method_8320(targetPos);
                class_2338 waterPlacementPos = state.method_45474() ? targetPos : targetPos.method_10093(hitResult.method_17780());
                this.placedWater.add(waterPlacementPos);
            }

            if (this.mc.field_1724.method_6047().method_7909() == class_1802.field_8550) {
                class_2338 targetPos = hitResult.method_17777();
                class_2680 state = this.mc.field_1687.method_8320(targetPos);
                if (state.method_27852(class_2246.field_10382) && state.method_26227().method_15771()) {
                    this.placedWater.remove(targetPos);
                }
            }
        }
    }

    private void updateLogic() {
        switch (this.currentState) {
            case IDLE:
                this.targetWaterPos = this.scanForWater();
                if (this.targetWaterPos != null) {
                    if (this.autoSwitch.isEnabled()) {
                        int bucketSlot = this.findItemInHotbar(class_1802.field_8550);
                        if (bucketSlot == -1) {
                            this.targetWaterPos = null;
                            return;
                        }

                        this.originalSlot = this.mc.field_1724.method_31548().method_67532();
                        this.mc.field_1724.method_31548().method_61496(bucketSlot);
                    } else if (this.mc.field_1724.method_6047().method_7909() != class_1802.field_8550) {
                        this.targetWaterPos = null;
                        return;
                    }

                    this.currentState = AutoDrain.State.START_DELAY;
                    this.lastActionTime = System.currentTimeMillis();
                }
                break;
            case START_DELAY:
                if (!this.isWaterStillValid()) {
                    this.reset();
                    return;
                }

                if (this.timePassed((long)this.startDelay.getValue())) {
                    this.startRotatingToWater();
                    this.currentState = AutoDrain.State.ROTATING;
                    this.lastActionTime = System.currentTimeMillis();
                }
                break;
            case ROTATING:
                if (!this.isWaterStillValid()) {
                    this.reset();
                    return;
                }

                if (this.timePassed(50L) || this.isRotationComplete()) {
                    this.currentState = AutoDrain.State.READY_TO_CLICK;
                    this.lastActionTime = System.currentTimeMillis();
                }
                break;
            case READY_TO_CLICK:
                if (!this.isWaterStillValid()) {
                    this.reset();
                    return;
                }

                this.clickWater();
                if (this.targetWaterPos != null) {
                    this.placedWater.remove(this.targetWaterPos);
                }

                this.currentState = AutoDrain.State.COOLDOWN;
                this.lastActionTime = System.currentTimeMillis();
                break;
            case COOLDOWN:
                if (this.timePassed(150L)) {
                    this.restoreOriginalSlot();
                    this.reset();
                }
        }
    }

    private void startRotatingToWater() {
        if (this.targetWaterPos != null) {
            RotationManager.setRotationSupplier(
                this,
                RotationManager.Priority.NORMAL,
                () -> this.targetWaterPos.method_46558(),
                this.rotationStrength.getValue(),
                this.getRotationMode(),
                this.rotRandom.getValue(),
                this.silentMode.isEnabled(),
                false
            );
        }
    }

    private RotationManager.RotationMode getRotationMode() {
        try {
            return RotationManager.RotationMode.valueOf(this.rotPattern.getCurrentMode().toUpperCase());
        } catch (Exception var2) {
            return RotationManager.RotationMode.SMOOTH;
        }
    }

    private boolean isRotationComplete() {
        if (this.targetWaterPos == null || this.mc.field_1724 == null) {
            return false;
        } else if (!RotationManager.isActive()) {
            return true;
        } else {
            class_243 targetPoint = this.targetWaterPos.method_46558();
            float[] needed = RotationManager.calculateRotationsToPos(targetPoint, RotationManager.getFinalYaw());
            float currentYaw = RotationManager.getFinalYaw();
            float currentPitch = RotationManager.getFinalPitch();
            float yawDiff = Math.abs(this.wrapDegrees(needed[0] - currentYaw));
            float pitchDiff = Math.abs(needed[1] - currentPitch);
            return yawDiff < 3.0F && pitchDiff < 3.0F;
        }
    }

    private class_2338 scanForWater() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            class_243 playerPos = this.mc.field_1724.method_33571();
            double maxRange = this.rangeDistance.getValue();
            boolean isBlatant = this.mode.getCurrentMode().equals("Blatant");
            class_2338 bestWater = null;
            double bestDistance = Double.MAX_VALUE;
            int range = (int)Math.ceil(maxRange);
            class_2338 playerBlockPos = this.mc.field_1724.method_24515();

            for (int x = -range; x <= range; x++) {
                for (int y = -range; y <= range; y++) {
                    for (int z = -range; z <= range; z++) {
                        class_2338 pos = playerBlockPos.method_10069(x, y, z);
                        class_2680 state = this.mc.field_1687.method_8320(pos);
                        if (state.method_27852(class_2246.field_10382)
                            && state.method_26227().method_15771()
                            && (!this.ignoreOwnWater.isEnabled() || !this.placedWater.contains(pos))) {
                            double distance = playerPos.method_1022(pos.method_46558());
                            if (!(distance > maxRange)) {
                                if (!isBlatant) {
                                    double angle = this.getAngleToPos(pos);
                                    if (angle > this.legitFOV.getValue()) {
                                        continue;
                                    }
                                }

                                if (this.canSeeBlock(pos) && distance < bestDistance) {
                                    bestDistance = distance;
                                    bestWater = pos;
                                }
                            }
                        }
                    }
                }
            }

            return bestWater;
        } else {
            return null;
        }
    }

    private double getAngleToPos(class_2338 pos) {
        if (this.mc.field_1724 == null) {
            return 180.0;
        } else {
            class_243 playerPos = this.mc.field_1724.method_33571();
            class_243 targetPos = pos.method_46558();
            double deltaX = targetPos.field_1352 - playerPos.field_1352;
            double deltaZ = targetPos.field_1350 - playerPos.field_1350;
            float targetYaw = (float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
            return Math.abs(this.wrapDegrees(targetYaw - this.mc.field_1724.method_36454()));
        }
    }

    private boolean canSeeBlock(class_2338 pos) {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            class_243 start = this.mc.field_1724.method_33571();
            class_243 end = pos.method_46558();
            class_3965 result = this.mc.field_1687.method_17742(new class_3959(start, end, class_3960.field_17558, class_242.field_1348, this.mc.field_1724));
            return result.method_17783() == class_240.field_1333 || result.method_17777().equals(pos);
        } else {
            return false;
        }
    }

    private void clickWater() {
        KeyBindingAccessor useKey = (KeyBindingAccessor)this.mc.field_1690.field_1904;
        useKey.setTimesPressed(useKey.getTimesPressed() + 1);
    }

    private boolean isWaterStillValid() {
        return this.targetWaterPos != null && this.mc.field_1687 != null
            ? this.mc.field_1687.method_8320(this.targetWaterPos).method_27852(class_2246.field_10382)
            : false;
    }

    private int findItemInHotbar(class_1792 item) {
        if (this.mc.field_1724 == null) {
            return -1;
        } else {
            for (int i = 0; i < 9; i++) {
                if (this.mc.field_1724.method_31548().method_5438(i).method_7909() == item) {
                    return i;
                }
            }

            return -1;
        }
    }

    private void restoreOriginalSlot() {
        if (this.mc.field_1724 != null && this.originalSlot != -1 && this.autoSwitch.isEnabled()) {
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
            this.originalSlot = -1;
        }
    }

    private boolean timePassed(long milliseconds) {
        return System.currentTimeMillis() - this.lastActionTime >= milliseconds;
    }

    private float wrapDegrees(float degrees) {
        return class_3532.method_15393(degrees);
    }

    private void reset() {
        RotationManager.clearTarget(this);
        this.currentState = AutoDrain.State.IDLE;
        this.targetWaterPos = null;
        this.lastActionTime = 0L;
    }

    @Environment(EnvType.CLIENT)
    private static enum State {
        IDLE,
        START_DELAY,
        ROTATING,
        READY_TO_CLICK,
        COOLDOWN;
    }
}
