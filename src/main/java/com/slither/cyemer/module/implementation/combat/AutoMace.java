package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.event.EventBus;
import com.slither.cyemer.event.impl.AutoMaceSyncEvent;
import com.slither.cyemer.event.impl.MaceHitEvent;
import com.slither.cyemer.event.impl.ShieldDrainEvent;
import com.slither.cyemer.friend.FriendManager;
import com.slither.cyemer.manager.TargetManager;
import com.slither.cyemer.mixin.KeyBindingAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.AttackValidator;
import com.slither.cyemer.util.RotationManager;
import com.slither.cyemer.util.render.RenderUtils;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_1743;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1819;
import net.minecraft.class_1887;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_332;
import net.minecraft.class_3486;
import net.minecraft.class_3489;
import net.minecraft.class_3532;
import net.minecraft.class_3966;
import net.minecraft.class_4587;
import net.minecraft.class_5321;
import net.minecraft.class_6880;
import net.minecraft.class_9304;
import net.minecraft.class_9334;
import net.minecraft.class_9362;
import net.minecraft.class_2338.class_2339;

@Environment(EnvType.CLIENT)
public class AutoMace extends Module {
    private static final double ATTACK_RANGE_CAP = 2.95;
    private static final double GRAVITY = 0.08;
    private static final double DRAG = 0.98;
    private static final int MAX_PREDICT_TICKS = 40;

    private final SliderSetting swingRange = new SliderSetting("Swing Range", 3.0, 2.5, 3.0, 1);
    private final SliderSetting aimRange = new SliderSetting("Aim Range", 15.0, 0.0, 10.0, 1);
    private final SliderSetting aimInAir = new SliderSetting("Aim In Air", 4.5, 0.0, 15.0, 1);
    private final SliderSetting aimHeight = new SliderSetting("Aim Height", 0.65, 0.0, 1.0, 2);
    private final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", true);
    private final BooleanSetting swapBack = new BooleanSetting("Swap Back", true);
    private final SliderSetting rotationSpeed = new SliderSetting("Aim Speed", 24.0, 0.0, 35.0, 1);
    private final SliderSetting minFallDist = new SliderSetting("Min Fall Dist", 1.5, 0.0, 5.0, 1);
    private final SliderSetting cooldown = new SliderSetting("Cooldown (ms)", 500.0, 100.0, 2000.0, 0);
    private final SliderSetting maceSwapDelay = new SliderSetting("Mace Swap Delay (ms)", 1.0, 0.0, 100.0, 0);
    private final BooleanSetting stunSlam = new BooleanSetting("Stun Slam", true);
    private final BooleanSetting weaponOnly = new BooleanSetting("Weapon Only", false);
    private final ModeSetting aimMode = new ModeSetting("Aim Mode", "Strict", "Loose", "Horizontal");
    private final ModeSetting stopAim = new ModeSetting("Stop Aim", "Hitbox Edge", "Exact Center");
    private final SliderSetting hitboxAccuracy = new SliderSetting("Hitbox Accuracy", 0.3, 0.0, 1.0, 2);
    private final BooleanSetting ignoreFriends = new BooleanSetting("Ignore Friends", true);
    private final BooleanSetting renderPred = new BooleanSetting("Render Pred", false);
    private final BooleanSetting targetMode = new BooleanSetting("Target Mode", false);

    private final BooleanSetting velocityLead = new BooleanSetting("Velocity Lead", true);
    private final ModeSetting targetPriority = new ModeSetting("Priority", "Closest", "LowestHP", "FacingAway");
    private final BooleanSetting windChain = new BooleanSetting("Wind Chain", true);
    private final BooleanSetting autoBoost = new BooleanSetting("Auto Boost", false);
    private final SliderSetting boostRange = new SliderSetting("Boost Range", 4.0, 2.0, 8.0, 1);
    private final BooleanSetting elytraDive = new BooleanSetting("Elytra Dive", true);

    private class_1657 currentTarget = null;
    private int maceClicksLeft = 0;
    private int originalSlot = -1;
    private int preSequenceSlot = -1;
    private long lastComboTime = 0L;
    private long axeHitTime = 0L;
    private int resetTimer = 0;
    private double highestY = 0.0;
    private boolean wasOnGround = true;
    private boolean shouldAttackThisTick = false;
    private boolean shouldBreakShield = false;
    private boolean shouldMaceSmash = false;
    private int targetSlotForAttack = -1;
    private boolean isSwappingArmor = false;
    private int armorSwapTimer = 0;
    private int armorSwapReturnSlot = -1;

    private BoostState boostState = BoostState.IDLE;
    private int boostTicks = 0;
    private int boostReturnSlot = -1;

    private boolean chainPending = false;

    public AutoMace() {
        super("AutoMace", "boing boing smash boing", Category.COMBAT);
        this.addSetting(this.swingRange);
        this.addSetting(this.aimRange);
        this.addSetting(this.aimInAir);
        this.addSetting(this.aimHeight);
        this.addSetting(this.autoSwitch);
        this.addSetting(this.swapBack);
        this.addSetting(this.rotationSpeed);
        this.addSetting(this.minFallDist);
        this.addSetting(this.cooldown);
        this.addSetting(this.maceSwapDelay);
        this.addSetting(this.stunSlam);
        this.addSetting(this.weaponOnly);
        this.addSetting(this.aimMode);
        this.addSetting(this.stopAim);
        this.addSetting(this.hitboxAccuracy);
        this.addSetting(this.ignoreFriends);
        this.addSetting(this.renderPred);
        this.addSetting(this.targetMode);
        this.addSetting(this.velocityLead);
        this.addSetting(this.targetPriority);
        this.addSetting(this.windChain);
        this.addSetting(this.autoBoost);
        this.addSetting(this.boostRange);
        this.addSetting(this.elytraDive);
    }

    @Override
    public void onRender(class_332 context, float tickDelta) {
        if (this.isEnabled()) {
            ShieldDrainEvent drainEvent = new ShieldDrainEvent();
            EventBus.post(drainEvent);
            if (!drainEvent.isActive()) {
                this.shouldAttackThisTick = false;
                this.shouldBreakShield = false;
                this.shouldMaceSmash = false;
                this.targetSlotForAttack = -1;
                this.runRenderLogic();
            }
        }
    }

    @Override
    public void onWorldRender(class_4587 matrices, float tickDelta) {
        if (this.isEnabled()) {
            if (this.renderPred.isEnabled() && this.currentTarget != null && this.mc.field_1724 != null) {
                this.renderPredictions(matrices, tickDelta);
            }
        }
    }

    @Override
    public void onTick() {
        if (this.isEnabled()) {
            ShieldDrainEvent drainEvent = new ShieldDrainEvent();
            EventBus.post(drainEvent);
            if (!drainEvent.isActive()) {
                if (this.boostState != BoostState.IDLE) {
                    this.tickBoost();
                    return;
                }
                if (this.isSwappingArmor) {
                    this.manageArmorSwap();
                } else {
                    if (this.shouldBreakShield) {
                        this.executeShieldBreak();
                    } else if (this.shouldMaceSmash) {
                        this.executeMaceSmash();
                    } else if (this.shouldAttackThisTick) {
                        this.executeAttack();
                    }
                }
            }
        }
    }

    private void manageArmorSwap() {
        if (this.mc.field_1724 == null) {
            this.isSwappingArmor = false;
        } else {
            this.armorSwapTimer--;
            if (this.armorSwapTimer == 1) {
                KeyBindingAccessor useKey = (KeyBindingAccessor)this.mc.field_1690.field_1904;
                useKey.setTimesPressed(useKey.getTimesPressed() + 1);
            }

            if (this.armorSwapTimer <= 0) {
                if (this.armorSwapReturnSlot != -1) {
                    this.mc.field_1724.method_31548().method_61496(this.armorSwapReturnSlot);
                }

                this.isSwappingArmor = false;
                this.armorSwapReturnSlot = -1;
            }
        }
    }

    private void triggerArmorSwap(int targetSlot) {
        if (this.mc.field_1724 != null && targetSlot != -1) {
            if (!this.isSwappingArmor) {
                this.armorSwapReturnSlot = this.mc.field_1724.method_31548().method_67532();
                this.mc.field_1724.method_31548().method_61496(targetSlot);
                this.isSwappingArmor = true;
                this.armorSwapTimer = 3;
            }
        }
    }

    private class_243 getAimPos(class_1297 target) {
        if (target != null && this.mc.field_1724 != null) {
            class_238 box = target.method_5829();
            class_243 center = box.method_1005();
            double h = class_3532.method_15350(this.aimHeight.getValue(), 0.0, 1.0);
            double aimY = box.field_1322 + target.method_17682() * h;
            class_243 base = new class_243(center.field_1352, aimY, center.field_1350);

            if (!this.velocityLead.isEnabled() || !(target instanceof class_1657)) {
                return base;
            }

            int ticks = this.estimateTicksToImpact(target);
            if (ticks <= 0) return base;

            class_243 vel = target.method_18798();
            double dx = vel.field_1352 * ticks;
            double dz = vel.field_1350 * ticks;
            double leadCap = 2.0;
            double horiz = Math.sqrt(dx * dx + dz * dz);
            if (horiz > leadCap) {
                double s = leadCap / horiz;
                dx *= s;
                dz *= s;
            }
            return new class_243(base.field_1352 + dx, base.field_1351, base.field_1350 + dz);
        } else {
            return class_243.field_1353;
        }
    }

    private int estimateTicksToImpact(class_1297 target) {
        if (this.mc.field_1724 == null) return 0;
        double py = this.mc.field_1724.method_23318();
        double ty = target.method_23318();
        if (py <= ty + 0.5) return 0;
        double vy = this.mc.field_1724.method_18798().field_1351;
        int ticks = 0;
        while (py > ty && ticks < MAX_PREDICT_TICKS) {
            vy = (vy - GRAVITY) * DRAG;
            py += vy;
            ticks++;
        }
        return ticks;
    }

    private boolean canExecuteAttack() {
        if (this.mc.field_1724 == null || this.currentTarget == null) {
            return false;
        } else if (!AttackValidator.canAttack(this.mc)) {
            return false;
        } else {
            double effectiveRange = this.getEffectiveAttackRange();
            return this.mc.field_1724.method_6057(this.currentTarget) && this.isWithinLegitReach(this.currentTarget, effectiveRange)
                ? this.mc.field_1765 instanceof class_3966 ehr && ehr.method_17782() == this.currentTarget
                : false;
        }
    }

    private boolean isHorizontalMode() {
        return this.aimMode.getCurrentMode().equals("Horizontal");
    }

    private boolean shouldRotate() {
        return !this.isHorizontalMode() ? this.aimRange.getValue() > 0.0 : this.aimRange.getValue() > 0.0 || this.aimInAir.getValue() > 0.0;
    }

    private void runRenderLogic() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            if (!this.isSwappingArmor && this.boostState == BoostState.IDLE) {
                if (this.isInLiquidOrWeb()) {
                    this.stopAiming();
                } else {
                    boolean isOnGroundNow = this.mc.field_1724.method_24828();
                    if (isOnGroundNow) {
                        this.highestY = this.mc.field_1724.method_23318();
                    } else {
                        this.highestY = Math.max(this.highestY, this.mc.field_1724.method_23318());
                    }

                    double manualFallDist = Math.max(0.0, this.highestY - this.mc.field_1724.method_23318());
                    this.wasOnGround = isOnGroundNow;
                    int bestMaceSlot = this.findBestMace();
                    boolean isHoldingMace = this.mc.field_1724.method_6047().method_7909() instanceof class_9362;
                    boolean canUseMace = isHoldingMace || this.autoSwitch.isEnabled() && bestMaceSlot != -1;
                    boolean canHorizontalWeaponAim = this.isHorizontalMode()
                        && this.weaponOnly.isEnabled()
                        && this.isInAirForHorizontalAssist()
                        && this.isHoldingWeapon();
                    if (!canUseMace && !canHorizontalWeaponAim) {
                        this.stopAiming();
                    } else if (this.weaponOnly.isEnabled() && !this.isHoldingWeapon()) {
                        this.stopAiming();
                    } else if (this.resetTimer > 0) {
                        this.handleResetSequence();
                    } else if (this.maceClicksLeft > 0) {
                        this.calculateMaceLogic();
                    } else if (!(System.currentTimeMillis() - this.lastComboTime < this.cooldown.getValue())) {
                        this.currentTarget = this.findTarget();
                        if (this.currentTarget == null) {
                            this.stopAiming();
                        } else if (this.isHorizontalMode() && !this.isTargetInHorizontalFov(this.currentTarget)) {
                            this.stopAiming();
                        } else {
                            boolean gameSaysFalling = this.mc.field_1724.field_6017 >= this.minFallDist.getValue();
                            boolean manualSaysFalling = manualFallDist >= this.minFallDist.getValue();
                            boolean isFalling = gameSaysFalling || manualSaysFalling;
                            boolean elytraDescending = this.elytraDive.isEnabled()
                                && this.mc.field_1724.method_6128()
                                && this.mc.field_1724.method_18798().field_1351 < -0.1;
                            if (!isFalling && !elytraDescending && this.minFallDist.getValue() > 0.1) {
                                if (this.tryStartBoost()) return;
                                this.stopAiming();
                            } else {
                                boolean isBlocking = this.isTargetBlocking(this.currentTarget);
                                boolean canStunSlam = this.stunSlam.isEnabled() && isBlocking;
                                EventBus.post(new AutoMaceSyncEvent());
                                if (canStunSlam) {
                                    this.calculateStunSlam();
                                } else {
                                    this.calculateDirectMaceLogic();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean tryStartBoost() {
        if (!this.autoBoost.isEnabled()) return false;
        if (this.mc.field_1724 == null) return false;
        if (!this.mc.field_1724.method_24828()) return false;
        if (this.currentTarget == null) return false;
        double horizDist = Math.sqrt(this.mc.field_1724.method_5858(this.currentTarget));
        if (horizDist > this.boostRange.getValue()) return false;
        int chargeSlot = this.findWindCharge();
        if (chargeSlot == -1) return false;

        this.boostReturnSlot = this.mc.field_1724.method_31548().method_67532();
        this.mc.field_1724.method_31548().method_61496(chargeSlot);
        RotationManager.setRotationSupplier(this, RotationManager.Priority.HIGHEST, () -> {
            if (this.mc.field_1724 == null) return null;
            return this.mc.field_1724.method_73189().method_1031(0.0, -5.0, 0.0);
        }, this.rotationSpeed.getValue(), RotationManager.RotationMode.SMOOTH, 0.0, true, false);
        this.boostState = BoostState.AIMING;
        this.boostTicks = 3;
        return true;
    }

    private void tickBoost() {
        if (this.mc.field_1724 == null) {
            this.abortBoost();
            return;
        }
        this.boostTicks--;
        switch (this.boostState) {
            case AIMING:
                if (RotationManager.getFinalPitch() >= 85.0F && this.boostTicks <= 0) {
                    this.boostState = BoostState.USING;
                    this.boostTicks = 2;
                } else if (this.boostTicks <= -6) {
                    this.abortBoost();
                }
                break;
            case USING:
                KeyBindingAccessor useKey = (KeyBindingAccessor)this.mc.field_1690.field_1904;
                useKey.setTimesPressed(useKey.getTimesPressed() + 1);
                this.boostState = BoostState.RECOVERING;
                this.boostTicks = 3;
                break;
            case RECOVERING:
                if (this.boostTicks <= 0) {
                    if (this.boostReturnSlot != -1) {
                        this.mc.field_1724.method_31548().method_61496(this.boostReturnSlot);
                    }
                    RotationManager.clearTarget(this);
                    this.boostState = BoostState.IDLE;
                    this.boostReturnSlot = -1;
                }
                break;
            default:
        }
    }

    private void abortBoost() {
        if (this.mc.field_1724 != null && this.boostReturnSlot != -1) {
            this.mc.field_1724.method_31548().method_61496(this.boostReturnSlot);
        }
        RotationManager.clearTarget(this);
        this.boostState = BoostState.IDLE;
        this.boostReturnSlot = -1;
        this.boostTicks = 0;
    }

    private int findWindCharge() {
        if (this.mc.field_1724 == null) return -1;
        for (int i = 0; i < 9; i++) {
            class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
            if (stack.method_7909() == class_1802.field_49098) {
                return i;
            }
        }
        return -1;
    }

    private boolean isInLiquidOrWeb() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            class_238 box = this.mc.field_1724.method_5829();
            class_2338 min = class_2338.method_49637(box.field_1323, box.field_1322, box.field_1321);
            class_2338 max = class_2338.method_49637(box.field_1320, box.field_1325, box.field_1324);
            class_2339 mutable = new class_2339();

            for (int x = min.method_10263(); x <= max.method_10263(); x++) {
                for (int y = min.method_10264(); y <= max.method_10264(); y++) {
                    for (int z = min.method_10260(); z <= max.method_10260(); z++) {
                        mutable.method_10103(x, y, z);
                        class_2680 state = this.mc.field_1687.method_8320(mutable);
                        if (state.method_26227().method_15767(class_3486.field_15517) || state.method_26204() == class_2246.field_10343) {
                            return true;
                        }
                    }
                }
            }

            return false;
        } else {
            return false;
        }
    }

    private void calculateStunSlam() {
        double maxRange = this.getTrackingRange(this.currentTarget);
        if (this.mc.field_1724.method_5739(this.currentTarget) > maxRange) {
            this.stopAiming();
            this.currentTarget = null;
            this.maceClicksLeft = 0;
            this.originalSlot = -1;
        } else {
            class_243 aimPos = this.getAimPos(this.currentTarget);
            if (this.shouldRotate()) {
                this.applyAimRotation(aimPos, RotationManager.Priority.HIGH);
            }

            if (this.canExecuteAttack()) {
                int axeSlot = this.findAxe();
                int maceSlot = this.findBestMace();
                if (axeSlot != -1 && maceSlot != -1) {
                    if (this.preSequenceSlot == -1) {
                        this.preSequenceSlot = this.mc.field_1724.method_31548().method_67532();
                    }

                    this.shouldBreakShield = true;
                    this.targetSlotForAttack = axeSlot;
                    this.originalSlot = maceSlot;
                }
            }
        }
    }

    private void executeShieldBreak() {
        if (this.currentTarget != null) {
            if (this.syncToAttackSlot()) {
                if (this.canExecuteAttack()) {
                    boolean success = AttackValidator.tryAttack(this.mc, "combat.attack.automace");
                    if (success) {
                        EventBus.post(new MaceHitEvent());
                        this.maceClicksLeft = 1;
                        this.axeHitTime = System.currentTimeMillis();
                    } else {
                        this.swapBackToPreSequence();
                        this.originalSlot = -1;
                    }
                }
            }
        }
    }

    private void calculateMaceLogic() {
        double maxRange = this.getTrackingRange(this.currentTarget);
        if (this.currentTarget != null && this.currentTarget.method_5805() && !(this.mc.field_1724.method_5739(this.currentTarget) > maxRange)) {
            class_243 aimPos = this.getAimPos(this.currentTarget);
            if (this.shouldRotate()) {
                this.applyAimRotation(aimPos, RotationManager.Priority.HIGHEST);
            }

            long timeSinceAxe = System.currentTimeMillis() - this.axeHitTime;
            if (!(timeSinceAxe < this.maceSwapDelay.getValue())) {
                if (timeSinceAxe > 1500L) {
                    this.swapBackToPreSequence();
                    this.maceClicksLeft = 0;
                    this.originalSlot = -1;
                    this.stopAiming();
                } else {
                    if (this.canExecuteAttack()) {
                        this.shouldMaceSmash = true;
                        this.targetSlotForAttack = this.originalSlot;
                    }
                }
            }
        } else {
            this.swapBackToPreSequence();
            this.maceClicksLeft = 0;
            this.originalSlot = -1;
            this.stopAiming();
        }
    }

    private void executeMaceSmash() {
        if (this.syncToAttackSlot()) {
            if (this.canExecuteAttack()) {
                boolean success = AttackValidator.tryAttack(this.mc, "combat.attack.automace");
                if (success) {
                    EventBus.post(new MaceHitEvent());
                    this.maceClicksLeft = 0;
                    this.resetTimer = 8;
                    this.lastComboTime = System.currentTimeMillis();
                    this.chainPending = this.windChain.isEnabled() && this.hasWindBurstEquipped();
                }
            } else {
                this.swapBackToPreSequence();
                this.maceClicksLeft = 0;
                this.originalSlot = -1;
            }
        }
    }

    private boolean hasWindBurstEquipped() {
        if (this.mc.field_1724 == null) return false;
        class_1799 held = this.mc.field_1724.method_6047();
        if (!(held.method_7909() instanceof class_9362)) {
            int slot = this.findBestMace();
            if (slot == -1) return false;
            held = this.mc.field_1724.method_31548().method_5438(slot);
        }
        return this.hasEnchant(held, "wind_burst");
    }

    private boolean hasEnchant(class_1799 stack, String idContains) {
        if (stack.method_7960()) return false;
        class_9304 enchantments = (class_9304)stack.method_58694(class_9334.field_49633);
        if (enchantments == null) return false;
        for (class_6880<class_1887> entry : enchantments.method_57534()) {
            if (entry.method_40230().isPresent()) {
                String id = ((class_5321)entry.method_40230().get()).method_29177().method_12832();
                if (id.contains(idContains)) return true;
            }
        }
        return false;
    }

    private void calculateDirectMaceLogic() {
        double maxRange = this.getTrackingRange(this.currentTarget);
        if (this.currentTarget != null && this.currentTarget.method_5805() && !(this.mc.field_1724.method_5739(this.currentTarget) > maxRange)) {
            class_243 aimPos = this.getAimPos(this.currentTarget);
            if (this.shouldRotate()) {
                this.applyAimRotation(aimPos, RotationManager.Priority.HIGH);
            }

            if (this.canExecuteAttack()) {
                int maceSlot = this.findBestMace();
                if (maceSlot != -1) {
                    if (this.preSequenceSlot == -1) {
                        this.preSequenceSlot = this.mc.field_1724.method_31548().method_67532();
                    }

                    this.shouldAttackThisTick = true;
                    this.targetSlotForAttack = maceSlot;
                }
            }
        } else {
            this.stopAiming();
        }
    }

    private void executeAttack() {
        if (this.syncToAttackSlot()) {
            if (this.canExecuteAttack()) {
                boolean success = AttackValidator.tryAttack(this.mc, "combat.attack.automace");
                if (success) {
                    EventBus.post(new MaceHitEvent());
                    this.lastComboTime = System.currentTimeMillis();
                    this.resetTimer = 5;
                    this.chainPending = this.windChain.isEnabled() && this.hasWindBurstEquipped();
                } else {
                    this.swapBackToPreSequence();
                }
            }
        }
    }

    private void swapBackToPreSequence() {
        if (this.swapBack.isEnabled() && this.autoSwitch.isEnabled() && this.preSequenceSlot >= 0 && this.preSequenceSlot < 9) {
            this.mc.field_1724.method_31548().method_61496(this.preSequenceSlot);
        }

        this.preSequenceSlot = -1;
    }

    private void resetSlot() {
        if (this.autoSwitch.isEnabled() && this.originalSlot >= 0 && this.originalSlot < 9) {
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
        }
    }

    private void handleResetSequence() {
        this.resetTimer--;
        double maxRange = this.getTrackingRange(this.currentTarget);
        if (this.currentTarget != null
            && this.currentTarget.method_5805()
            && this.mc.field_1724.method_5739(this.currentTarget) <= maxRange
            && this.shouldRotate()) {
            class_243 aimPos = this.getAimPos(this.currentTarget);
            this.applyAimRotation(aimPos, RotationManager.Priority.HIGH);
        }

        if (this.resetTimer <= 0) {
            this.swapBackToPreSequence();
            if (this.chainPending && this.currentTarget != null && this.currentTarget.method_5805()) {
                this.lastComboTime = 0L;
                this.highestY = this.mc.field_1724 != null ? this.mc.field_1724.method_23318() : 0.0;
                this.chainPending = false;
            } else {
                this.stopAiming();
                this.chainPending = false;
            }
        }
    }

    private boolean isHoldingWeapon() {
        if (this.mc.field_1724 == null) {
            return false;
        } else {
            class_1799 stack = this.mc.field_1724.method_6047();
            return stack.method_7909() instanceof class_9362 || stack.method_31573(class_3489.field_42612) || stack.method_31573(class_3489.field_42611);
        }
    }

    private boolean isTargetBlocking(class_1657 target) {
        if (target == null) {
            return false;
        } else if (target.method_6039()) {
            return true;
        } else if (!target.method_6115()) {
            return false;
        } else {
            class_1799 active = target.method_6030();
            return !active.method_7960() && active.method_7909() instanceof class_1819;
        }
    }

    private boolean isTargetInHorizontalFov(class_1657 target) {
        if (this.mc.field_1724 == null || target == null) {
            return false;
        } else if (!this.mc.field_1724.method_6057(target)) {
            return false;
        } else {
            class_243 eyePos = this.mc.field_1724.method_33571();
            class_243 center = target.method_5829().method_1005();
            double dx = center.field_1352 - eyePos.field_1352;
            double dz = center.field_1350 - eyePos.field_1350;
            if (dx * dx + dz * dz <= 1.0E-6) {
                return true;
            } else {
                float targetYaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
                float yawDiff = Math.abs(class_3532.method_15393(this.mc.field_1724.method_36454() - targetYaw));
                double fov = class_3532.method_15350(((Integer)this.mc.field_1690.method_41808().method_41753()).intValue(), 30.0, 170.0);
                return yawDiff <= fov * 0.5;
            }
        }
    }

    private int findBestMace() {
        int bestSlot = -1;
        int maxDensity = -1;

        for (int i = 0; i < 9; i++) {
            class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
            if (stack.method_7909() instanceof class_9362) {
                int densityLevel = this.getDensityLevel(stack);
                if (densityLevel > maxDensity) {
                    maxDensity = densityLevel;
                    bestSlot = i;
                }
            }
        }

        return bestSlot;
    }

    private int getDensityLevel(class_1799 stack) {
        if (stack.method_7960()) {
            return 0;
        } else {
            class_9304 enchantments = (class_9304)stack.method_58694(class_9334.field_49633);
            if (enchantments == null) {
                return 0;
            } else {
                for (class_6880<class_1887> entry : enchantments.method_57534()) {
                    if (entry.method_40230().isPresent()) {
                        String id = ((class_5321)entry.method_40230().get()).method_29177().method_12832();
                        if (id.contains("density")) {
                            return enchantments.method_57536(entry);
                        }
                    }
                }

                return 0;
            }
        }
    }

    private int findAxe() {
        for (int i = 0; i < 9; i++) {
            if (this.mc.field_1724.method_31548().method_5438(i).method_7909() instanceof class_1743) {
                return i;
            }
        }

        return -1;
    }

    private void stopAiming() {
        RotationManager.stop(this);
        this.currentTarget = null;
        this.maceClicksLeft = 0;
        this.shouldAttackThisTick = false;
        this.shouldBreakShield = false;
        this.shouldMaceSmash = false;
        this.targetSlotForAttack = -1;
        this.originalSlot = -1;
    }

    private boolean syncToAttackSlot() {
        if (this.mc.field_1724 == null) {
            return false;
        } else if (this.autoSwitch.isEnabled() && this.targetSlotForAttack >= 0 && this.targetSlotForAttack <= 8) {
            int selected = this.mc.field_1724.method_31548().method_67532();
            if (selected != this.targetSlotForAttack) {
                this.mc.field_1724.method_31548().method_61496(this.targetSlotForAttack);
            }

            return true;
        } else {
            return true;
        }
    }

    private boolean isWithinLegitReach(class_1297 target, double range) {
        if (this.mc.field_1724 != null && target != null) {
            class_243 eyePos = this.mc.field_1724.method_33571();
            class_238 box = target.method_5829();
            double clampedX = class_3532.method_15350(eyePos.field_1352, box.field_1323, box.field_1320);
            double clampedY = class_3532.method_15350(eyePos.field_1351, box.field_1322, box.field_1325);
            double clampedZ = class_3532.method_15350(eyePos.field_1350, box.field_1321, box.field_1324);
            double maxRange = Math.max(0.0, range);
            return eyePos.method_1028(clampedX, clampedY, clampedZ) <= maxRange * maxRange;
        } else {
            return false;
        }
    }

    private void applyAimRotation(class_243 aimPos, RotationManager.Priority priority) {
        if (this.mc.field_1724 != null) {
            if (this.currentTarget != null && this.shouldStopAim(this.currentTarget, this.getEffectiveAttackRange())) {
                RotationManager.clearTarget(this);
            } else if (this.isHorizontalMode()) {
                RotationManager.setRotationSupplier(this, priority, () -> {
                    if (this.mc.field_1724 != null && this.currentTarget != null) {
                        class_243 liveAim = this.getAimPos(this.currentTarget);
                        return new class_243(liveAim.field_1352, this.mc.field_1724.method_23320(), liveAim.field_1350);
                    } else {
                        return null;
                    }
                }, this.rotationSpeed.getValue(), RotationManager.RotationMode.SMOOTH, 0.0, false, true);
            } else {
                RotationManager.setRotationSupplier(
                    this,
                    priority,
                    () -> this.currentTarget != null ? this.getAimPos(this.currentTarget) : null,
                    this.rotationSpeed.getValue(),
                    RotationManager.RotationMode.SMOOTH,
                    0.0,
                    false,
                    false
                );
            }
        }
    }

    private boolean shouldStopAim(class_1297 target, double reachDistance) {
        if (this.mc.field_1724 != null && target != null) {
            if (this.mc.field_1724.method_5739(target) > reachDistance) {
                return false;
            } else {
                String mode = this.stopAim.getCurrentMode();
                if (!mode.equals("Exact Center")) {
                    return this.isOnHitboxWithAccuracy(target);
                } else {
                    return this.isHorizontalMode() && !this.isCrosshairOnTarget(target) ? false : this.isAimingAtCenter(target);
                }
            }
        } else {
            return false;
        }
    }

    private boolean isCrosshairOnTarget(class_1297 target) {
        return this.mc.field_1765 instanceof class_3966 ehr && ehr.method_17782() == target;
    }

    private boolean isAimingAtCenter(class_1297 target) {
        if (this.mc.field_1724 != null && target != null) {
            class_243 center = target.method_5829().method_1005();
            float[] required = RotationManager.calculateRotationsToPos(center, RotationManager.getFinalYaw());
            float yawDiff = Math.abs(class_3532.method_15393(RotationManager.getFinalYaw() - required[0]));
            if (this.isHorizontalMode()) {
                return yawDiff <= 1.0F;
            } else {
                float pitchDiff = Math.abs(class_3532.method_15393(RotationManager.getFinalPitch() - required[1]));
                return yawDiff <= 1.0F && pitchDiff <= 1.0F;
            }
        } else {
            return false;
        }
    }

    private boolean isOnHitboxWithAccuracy(class_1297 target) {
        if (this.mc.field_1724 == null || target == null) {
            return false;
        } else if (!this.isCrosshairOnTarget(target)) {
            return false;
        } else {
            class_243 eyePos = this.mc.field_1724.method_33571();
            class_238 box = target.method_5829();
            class_243 center = box.method_1005();
            float[] centerRot = RotationManager.calculateRotationsToPos(center, RotationManager.getFinalYaw());
            double yawDiff = Math.abs(class_3532.method_15393(RotationManager.getFinalYaw() - centerRot[0]));
            double dx = center.field_1352 - eyePos.field_1352;
            double dz = center.field_1350 - eyePos.field_1350;
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            double safeDist = Math.max(horizontalDist, 0.001);
            double halfWidth = Math.max(box.method_17939(), box.method_17941()) * 0.5;
            double yawHalfSpan = Math.toDegrees(Math.atan2(Math.max(halfWidth, 0.001), safeDist));
            double yawInside = 1.0 - Math.min(1.0, yawDiff / Math.max(yawHalfSpan, 0.001));
            double requiredInside = class_3532.method_15350(this.hitboxAccuracy.getValue(), 0.0, 1.0);
            if (this.isHorizontalMode()) {
                return yawInside >= requiredInside;
            } else {
                double verticalDist = Math.sqrt(dx * dx + dz * dz);
                double safeVertical = Math.max(verticalDist, 0.001);
                double halfHeight = box.method_17940() * 0.5;
                double pitchHalfSpan = Math.toDegrees(Math.atan2(Math.max(halfHeight, 0.001), safeVertical));
                double pitchDiff = Math.abs(class_3532.method_15393(RotationManager.getFinalPitch() - centerRot[1]));
                double pitchInside = 1.0 - Math.min(1.0, pitchDiff / Math.max(pitchHalfSpan, 0.001));
                return Math.min(yawInside, pitchInside) >= requiredInside;
            }
        }
    }

    private double getBaseRange() {
        return this.aimRange.getValue() > 0.0 ? this.aimRange.getValue() : this.swingRange.getValue();
    }

    private double getEffectiveAttackRange() {
        return Math.min(this.swingRange.getValue(), ATTACK_RANGE_CAP);
    }

    private double getTrackingRange(class_1657 target) {
        double base = this.getBaseRange();
        if (this.aimInAir.getValue() > 0.0 && this.isInAirForHorizontalAssist()) {
            if (target != null && this.mc.field_1724 != null && this.mc.field_1724.method_23318() > target.method_23318()) {
                return Math.max(base, this.aimInAir.getValue());
            }
        }
        return base;
    }

    private boolean isInAirForHorizontalAssist() {
        return this.mc.field_1724 == null ? false : !this.mc.field_1724.method_24828() && this.mc.field_1724.field_6017 > 0.0;
    }

    private class_1657 findTarget() {
        if (this.mc.field_1687 == null || this.mc.field_1724 == null) return null;
        String priority = this.targetPriority.getCurrentMode();
        double range = this.getBaseRange();
        double rangeSq = range * range;
        class_1657 best = null;
        double bestScore = Double.MAX_VALUE;

        for (class_1657 entity : this.mc.field_1687.method_18456()) {
            if (entity == this.mc.field_1724) continue;
            if (this.ignoreFriends.isEnabled() && FriendManager.getInstance().isFriend(entity.method_5477().getString())) continue;
            if (this.targetMode.isEnabled() && !TargetManager.isLocked(entity)) continue;
            if (!entity.method_5805()) continue;
            double distSq = this.mc.field_1724.method_5858(entity);
            if (distSq > rangeSq) continue;

            double score;
            switch (priority) {
                case "LowestHP":
                    score = entity.method_6032();
                    break;
                case "FacingAway":
                    score = this.facingAwayScore(entity) + distSq * 0.01;
                    break;
                case "Closest":
                default:
                    score = distSq;
            }
            if (score < bestScore) {
                bestScore = score;
                best = entity;
            }
        }
        return best;
    }

    private double facingAwayScore(class_1657 target) {
        if (this.mc.field_1724 == null) return Double.MAX_VALUE;
        class_243 targetLook = target.method_5720();
        class_243 fromTargetToMe = this.mc.field_1724.method_33571().method_1020(target.method_33571()).method_1029();
        double dot = targetLook.method_1026(fromTargetToMe);
        return dot;
    }

    private void renderPredictions(class_4587 matrices, float tickDelta) {
        if (this.currentTarget != null) {
            class_243 currentPos = this.currentTarget.method_30950(tickDelta);
            class_238 baseBox = this.currentTarget.method_5829();
            class_238 currentBox = baseBox.method_997(
                currentPos.method_1020(new class_243(this.currentTarget.method_23317(), this.currentTarget.method_23318(), this.currentTarget.method_23321()))
            );
            RenderUtils.drawBox(matrices, this.mc.method_22940().method_23000(), currentBox, new Color(120, 120, 120), 0.3F, false);

            if (this.velocityLead.isEnabled()) {
                int ticks = this.estimateTicksToImpact(this.currentTarget);
                if (ticks > 0) {
                    class_243 vel = this.currentTarget.method_18798();
                    double dx = vel.field_1352 * ticks;
                    double dz = vel.field_1350 * ticks;
                    double leadCap = 2.0;
                    double horiz = Math.sqrt(dx * dx + dz * dz);
                    if (horiz > leadCap) {
                        double s = leadCap / horiz;
                        dx *= s;
                        dz *= s;
                    }
                    class_238 predictedBox = currentBox.method_989(dx, 0.0, dz);
                    RenderUtils.drawBox(matrices, this.mc.method_22940().method_23000(), predictedBox, new Color(255, 60, 60), 0.5F, false);
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private static enum BoostState {
        IDLE,
        AIMING,
        USING,
        RECOVERING
    }
}
