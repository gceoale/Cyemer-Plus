package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.event.EventBus;
import com.slither.cyemer.event.EventTarget;
import com.slither.cyemer.event.impl.TickEvent;
import com.slither.cyemer.friend.FriendManager;
import com.slither.cyemer.mixin.MinecraftClientAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.RotationManager;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1792;
import net.minecraft.class_1802;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class AntiDivebomb extends Module {
    private final SliderSetting range = new SliderSetting("Trigger Dist", 8.0, 3.0, 15.0, 1);
    private final SliderSetting minDist = new SliderSetting("Min Dist", 2.0, 0.0, 10.0, 1);
    private final SliderSetting aimSpeed = new SliderSetting("Aim Speed", 35.0, 1.0, 35.0, 1);
    private final ModeSetting aimMode = new ModeSetting("Aim Mode", "Instant", "Smooth", "Sine");
    private final ModeSetting aimTarget = new ModeSetting("Aim Target", "Legs", "Chest", "Head");
    private final BooleanSetting onlyElytra = new BooleanSetting("Only Elytra", false);
    private final BooleanSetting onlyOnWind = new BooleanSetting("Only on Wind", false);
    private final BooleanSetting clickAim = new BooleanSetting("Click Aim", false);
    private final BooleanSetting macro = new BooleanSetting("Macro", false);
    private final SliderSetting macroAimTicks = new SliderSetting("Macro Aim Ticks", 3.0, 0.0, 20.0, 0);
    private final BooleanSetting autoThrow = new BooleanSetting("Auto Throw", true);
    private final BooleanSetting swapBack = new BooleanSetting("SwapBack", true);
    private final BooleanSetting ignoreFriends = new BooleanSetting("Ignore Friends", true);
    private static final float CHARGE_VELOCITY = 4.0F;
    private static final double THROW_LOOK_THRESHOLD = 0.995;
    private static final int MIN_WIND_HOLD_TICKS = 2;
    private static final double EPSILON = 1.0E-4;
    private int cooldown = 0;
    private int pendingThrowSlot = -1;
    private int pendingThrowRestoreSlot = -1;
    private int pendingThrowHoldTicks = 0;
    private int macroAimTickProgress = 0;
    private UUID macroAimTargetUuid = null;

    public AntiDivebomb() {
        super("AntiDivebomb", "Knocks back diving enemies with Wind Charges.", Category.COMBAT);
        this.addSetting(this.range);
        this.addSetting(this.minDist);
        this.addSetting(this.aimSpeed);
        this.addSetting(this.aimMode);
        this.addSetting(this.aimTarget);
        this.addSetting(this.onlyElytra);
        this.addSetting(this.onlyOnWind);
        this.addSetting(this.clickAim);
        this.addSetting(this.macro);
        this.addSetting(this.macroAimTicks);
        this.addSetting(this.autoThrow);
        this.addSetting(this.swapBack);
        this.addSetting(this.ignoreFriends);
        this.aimMode.setCurrentMode("Smooth");
        this.aimTarget.setCurrentMode("Chest");
    }

    @Override
    public void onEnable() {
        EventBus.register(this);
        this.cooldown = 0;
        this.resetPendingThrow();
        this.resetMacroAimProgress();
    }

    @Override
    public void onDisable() {
        EventBus.unregister(this);
        RotationManager.stop(this);
        this.cooldown = 0;
        this.resetPendingThrow();
        this.resetMacroAimProgress();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            if (this.cooldown > 0) {
                this.cooldown--;
            }

            if (this.mc.field_1755 != null) {
                RotationManager.stop(this);
                this.cancelPendingThrowIfNeeded();
                this.finishMacroRun();
            } else if (this.onlyOnWind.isEnabled() && !this.isHoldingInMainHand(class_1802.field_49098)) {
                RotationManager.stop(this);
                this.cancelPendingThrowIfNeeded();
                this.finishMacroRun();
            } else {
                class_1297 target = this.findTarget();
                boolean shouldTrigger = target != null;
                boolean hasPendingThrow = this.pendingThrowSlot >= 0;
                boolean shouldAimNow = shouldTrigger && (this.passesClickAimGate() || hasPendingThrow);
                if (!shouldAimNow) {
                    RotationManager.stop(this);
                    this.cancelPendingThrowIfNeeded();
                    this.finishMacroRun();
                } else {
                    this.performAiming(target);
                    if (this.autoThrow.isEnabled() && this.cooldown == 0) {
                        if (this.macro.isEnabled() && this.findItemSlot(class_1802.field_49098) == -1) {
                            this.cancelPendingThrowIfNeeded();
                            this.finishMacroRun();
                        } else {
                            if (this.macro.isEnabled()) {
                                this.updateMacroAimProgress(target);
                                int requiredTicks = Math.max(0, (int)Math.round(this.macroAimTicks.getValue()));
                                if (this.macroAimTickProgress < requiredTicks) {
                                    return;
                                }
                            } else {
                                this.resetMacroAimProgress();
                            }

                            if (this.isAimedForThrow(target)) {
                                boolean thrown = this.tryPerformItemAction(class_1802.field_49098, this.swapBack.isEnabled());
                                if (thrown) {
                                    this.finishMacroRun();
                                }
                            }
                        }
                    } else {
                        this.cancelPendingThrowIfNeeded();
                        this.finishMacroRun();
                    }
                }
            }
        } else {
            this.finishMacroRun();
        }
    }

    private class_1297 findTarget() {
        class_1297 bestEntity = null;
        double bestDistance = this.range.getValue();

        for (class_1297 entity : this.mc.field_1687.method_18112()) {
            if (this.isValidTarget(entity) && this.isTriggerCandidate(entity)) {
                double distance = this.mc.field_1724.method_5739(entity);
                if (!(distance > this.range.getValue()) && distance < bestDistance) {
                    bestDistance = distance;
                    bestEntity = entity;
                }
            }
        }

        return bestEntity;
    }

    private boolean isTriggerCandidate(class_1297 entity) {
        return this.onlyElytra.isEnabled() && !this.hasElytra(entity) ? false : this.isDiving(entity) || this.isAirborneAboveMinDist(entity);
    }

    private boolean isValidTarget(class_1297 entity) {
        if (!(entity instanceof class_1309) || entity == this.mc.field_1724 || !entity.method_5805()) {
            return false;
        } else {
            return this.ignoreFriends.isEnabled() && entity instanceof class_1657 player ? !FriendManager.getInstance().isFriend(player.method_5667()) : true;
        }
    }

    private boolean isDiving(class_1297 target) {
        class_243 targetVel = target.method_18798();
        if (!(targetVel.field_1351 > -0.1) && !(targetVel.method_1027() < 1.0E-4)) {
            class_243 posDiff = new class_243(this.mc.field_1724.method_23317(), this.mc.field_1724.method_23318(), this.mc.field_1724.method_23321())
                .method_1020(new class_243(target.method_23317(), target.method_23318(), target.method_23321()));
            if (posDiff.method_1027() < 1.0E-4) {
                return false;
            } else {
                double dot = targetVel.method_1029().method_1026(posDiff.method_1029());
                return dot > 0.7;
            }
        } else {
            return false;
        }
    }

    private boolean isAirborneAboveMinDist(class_1297 target) {
        return target.method_24828() ? false : this.getGroundDistance(target) >= this.minDist.getValue();
    }

    private void performAiming(class_1297 target) {
        RotationManager.setRotationSupplier(
            this, RotationManager.Priority.HIGH, () -> this.getDirectAimPoint(target), this.aimSpeed.getValue(), this.getAimRotationMode(), 0.0, false, false
        );
    }

    private class_243 getPredictedPos(class_1297 target) {
        double dist = this.mc.field_1724.method_5739(target);
        double time = dist / 4.0;
        return new class_243(target.method_23317(), target.method_23318(), target.method_23321()).method_1019(target.method_18798().method_1021(time));
    }

    private class_243 getDirectAimPoint(class_1297 target) {
        class_243 predictedPos = this.getPredictedPos(target);
        return new class_243(
            predictedPos.field_1352, predictedPos.field_1351 + target.method_17682() * this.getAimTargetHeightMultiplier(), predictedPos.field_1350
        );
    }

    private RotationManager.RotationMode getAimRotationMode() {
        String mode = this.aimMode.getCurrentMode();
        if ("Instant".equals(mode)) {
            return RotationManager.RotationMode.INSTANT;
        } else {
            return "Sine".equals(mode) ? RotationManager.RotationMode.SINE : RotationManager.RotationMode.SMOOTH;
        }
    }

    private double getAimTargetHeightMultiplier() {
        String mode = this.aimTarget.getCurrentMode();
        if ("Legs".equals(mode)) {
            return 0.08;
        } else {
            return "Head".equals(mode) ? 0.74 : 0.4;
        }
    }

    private boolean tryPerformItemAction(class_1792 item, boolean swapBack) {
        int slot = this.findItemSlot(item);
        if (slot == -1) {
            return false;
        } else {
            int selectedSlot = this.mc.field_1724.method_31548().method_67532();
            if (this.pendingThrowSlot != slot) {
                this.pendingThrowSlot = slot;
                this.pendingThrowHoldTicks = 0;
                this.pendingThrowRestoreSlot = selectedSlot != slot ? selectedSlot : -1;
            }

            if (selectedSlot != slot) {
                this.mc.field_1724.method_31548().method_61496(slot);
                return false;
            } else {
                this.pendingThrowHoldTicks++;
                if (this.pendingThrowHoldTicks < 2) {
                    return false;
                } else {
                    MinecraftClientAccessor accessor = (MinecraftClientAccessor)this.mc;
                    int beforeCooldown = accessor.getItemUseCooldown();
                    int beforeCount = this.mc.field_1724.method_6047().method_7947();
                    this.pressUseAndThrow(accessor);
                    int afterCooldown = accessor.getItemUseCooldown();
                    int afterCount = this.mc.field_1724.method_6047().method_7947();
                    boolean used = afterCooldown > beforeCooldown || afterCount < beforeCount;
                    if (!used) {
                        return false;
                    } else {
                        if (swapBack && this.pendingThrowRestoreSlot >= 0 && this.pendingThrowRestoreSlot != slot) {
                            this.mc.field_1724.method_31548().method_61496(this.pendingThrowRestoreSlot);
                        }

                        this.cooldown = 10;
                        this.resetPendingThrow();
                        return true;
                    }
                }
            }
        }
    }

    private void pressUseAndThrow(MinecraftClientAccessor accessor) {
        this.mc.field_1690.field_1904.method_23481(true);

        try {
            accessor.useItem();
        } finally {
            this.mc.field_1690.field_1904.method_23481(false);
        }
    }

    private int findItemSlot(class_1792 item) {
        for (int i = 0; i < 9; i++) {
            if (this.mc.field_1724.method_31548().method_5438(i).method_7909() == item) {
                return i;
            }
        }

        return -1;
    }

    private boolean isLookingAtTarget(class_1297 target, double threshold) {
        if (target == null) {
            return false;
        } else {
            class_243 lookVec = this.mc.field_1724.method_5828(1.0F).method_1029();
            class_243 toTarget = this.getDirectAimPoint(target).method_1020(this.mc.field_1724.method_33571()).method_1029();
            return lookVec.method_1026(toTarget) > threshold;
        }
    }

    private boolean isAimedForThrow(class_1297 target) {
        return target != null && this.mc.field_1724 != null && this.isLookingAtTarget(target, 0.995);
    }

    private boolean isHoldingInMainHand(class_1792 item) {
        return this.mc.field_1724 != null && this.mc.field_1724.method_6047().method_7909() == item;
    }

    private void resetPendingThrow() {
        this.pendingThrowSlot = -1;
        this.pendingThrowRestoreSlot = -1;
        this.pendingThrowHoldTicks = 0;
    }

    private void cancelPendingThrowIfNeeded() {
        if (this.swapBack.isEnabled() && this.mc.field_1724 != null && this.pendingThrowRestoreSlot >= 0) {
            if (this.pendingThrowSlot >= 0 && this.mc.field_1724.method_31548().method_67532() == this.pendingThrowSlot) {
                this.mc.field_1724.method_31548().method_61496(this.pendingThrowRestoreSlot);
            }

            this.resetPendingThrow();
        } else {
            this.resetPendingThrow();
        }
    }

    private boolean passesClickAimGate() {
        if (this.macro.isEnabled()) {
            return true;
        } else if (!this.clickAim.isEnabled()) {
            return true;
        } else if (!this.mc.field_1690.field_1886.method_1434() && !this.mc.field_1690.field_1904.method_1434()) {
            long handle = this.mc.method_22683().method_4490();
            return GLFW.glfwGetMouseButton(handle, 0) == 1 || GLFW.glfwGetMouseButton(handle, 1) == 1;
        } else {
            return true;
        }
    }

    private boolean finishMacroRun() {
        if (this.macro.isEnabled() && this.isEnabled()) {
            this.cooldown = 0;
            this.resetPendingThrow();
            this.resetMacroAimProgress();
            this.toggle();
            return true;
        } else {
            return false;
        }
    }

    private void updateMacroAimProgress(class_1297 target) {
        if (target == null) {
            this.resetMacroAimProgress();
        } else {
            UUID targetUuid = target.method_5667();
            if (!targetUuid.equals(this.macroAimTargetUuid)) {
                this.macroAimTargetUuid = targetUuid;
                this.macroAimTickProgress = 0;
            }

            this.macroAimTickProgress = Math.min(this.macroAimTickProgress + 1, 100);
        }
    }

    private void resetMacroAimProgress() {
        this.macroAimTickProgress = 0;
        this.macroAimTargetUuid = null;
    }

    private boolean hasElytra(class_1297 entity) {
        return entity instanceof class_1309 living ? living.method_6118(class_1304.field_6174).method_7909() == class_1802.field_8833 : false;
    }

    private double getGroundDistance(class_1297 entity) {
        if (this.mc.field_1687 == null) {
            return 0.0;
        } else {
            int y = (int)Math.floor(entity.method_23318());
            int minY = this.mc.field_1687.method_31607();

            for (int i = y; i >= minY; i--) {
                if (!this.mc.field_1687.method_8320(new class_2338((int)entity.method_23317(), i, (int)entity.method_23321())).method_26215()) {
                    return entity.method_23318() - i - 1.0;
                }
            }

            return 0.0;
        }
    }
}
