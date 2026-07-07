package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.event.EventBus;
import com.slither.cyemer.event.impl.ShieldDrainEvent;
import com.slither.cyemer.event.impl.TriggerBotReadyEvent;
import com.slither.cyemer.friend.FriendManager;
import com.slither.cyemer.mixin.MinecraftClientAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.AttackValidator;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1743;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1835;
import net.minecraft.class_1934;
import net.minecraft.class_2246;
import net.minecraft.class_3966;
import net.minecraft.class_9334;
import net.minecraft.class_9362;
import net.minecraft.class_239.class_240;

@Environment(EnvType.CLIENT)
public class TriggerBot extends Module {
    private final BooleanSetting critPrio = new BooleanSetting("Crit Prio", true);
    private final BooleanSetting onlyOnLmb = new BooleanSetting("Only on LMB", false);
    private final BooleanSetting ignoreShields = new BooleanSetting("Ignore Shields", false);
    private final BooleanSetting onlyWeapons = new BooleanSetting("Only Weapons", false);
    private final BooleanSetting tpsSync = new BooleanSetting("TPS Sync", false);
    private final BooleanSetting slotRestriction = new BooleanSetting("Slot Restriction", false);
    private final SliderSetting restrictedSlot = new SliderSetting("Attack Slot", 1.0, 1.0, 9.0, 0);
    private final SliderSetting cooldownThreshold = new SliderSetting("Cooldown %", 100.0, 0.0, 100.0, 0);
    private final SliderSetting missChance = new SliderSetting("Miss Chance %", 0.0, 0.0, 100.0, 0);
    private final SliderSetting missDelayMs = new SliderSetting("Miss Delay (ms)", 120.0, 0.0, 1000.0, 0);
    private final BooleanSetting randomization = new BooleanSetting("Randomization", false);
    private final SliderSetting randomDelayMs = new SliderSetting("Random Delay (ms)", 4.0, 0.0, 100.0, 0);
    private final BooleanSetting ignoreFriends = new BooleanSetting("Ignore Friends", true);
    private final BooleanSetting targetMode = new BooleanSetting("Target Mode", false);
    private boolean wasInAir = false;
    private boolean hasPassedPeak = false;
    private int ticksAfterPeak = 0;
    private int lastAttackClientTick = -1;
    private int lastAttackServerTick = -1;
    private final Random random = new Random();
    private long nextAttackTime = 0L;
    private long lastItemUseTime = 0L;
    private long missLockUntilMs = 0L;
    private static boolean warnedAttackValidatorFailure = false;
    private class_1309 lockedTarget = null;
    private boolean wasAttackKeyPressed = false;

    public TriggerBot() {
        super("TriggerBot", "Automatically attacks when looking at an entity", Category.COMBAT);
        this.addSetting(this.critPrio);
        this.addSetting(this.onlyOnLmb);
        this.addSetting(this.ignoreShields);
        this.addSetting(this.onlyWeapons);
        this.addSetting(this.tpsSync);
        this.addSetting(this.slotRestriction);
        this.addSetting(this.restrictedSlot);
        this.addSetting(this.cooldownThreshold);
        this.addSetting(this.missChance);
        this.addSetting(this.missDelayMs);
        this.addSetting(this.randomization);
        this.addSetting(this.randomDelayMs);
        this.addSetting(this.ignoreFriends);
        this.addSetting(this.targetMode);
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null && this.mc.field_1755 == null) {
            ShieldDrainEvent drainEvent = new ShieldDrainEvent();
            EventBus.post(drainEvent);
            if (!drainEvent.isActive()) {
                if (this.mc.field_1724.method_6115()) {
                    this.lastItemUseTime = System.currentTimeMillis();
                    this.resetCritState();
                } else if (System.currentTimeMillis() - this.lastItemUseTime < 1L) {
                    this.resetCritState();
                } else {
                    boolean isAttackKeyPressed = this.mc.field_1690.field_1886.method_1434();
                    boolean manualAttack = isAttackKeyPressed && !this.wasAttackKeyPressed;
                    this.wasAttackKeyPressed = isAttackKeyPressed;
                    if (this.mc.field_1765 instanceof class_3966 entityHit && entityHit.method_17782() instanceof class_1309 target) {
                        if (!target.method_5805() || target.method_6032() <= 0.0F) {
                            this.resetCritState();
                            return;
                        }

                        if (this.ignoreFriends.isEnabled() && target instanceof class_1657 player && FriendManager.getInstance().isFriend(player.method_5667())
                            )
                         {
                            this.resetCritState();
                            return;
                        }

                        if (this.targetMode.isEnabled()) {
                            if (this.lockedTarget != null
                                && (
                                    !this.lockedTarget.method_5805()
                                        || this.lockedTarget.method_31481()
                                        || this.lockedTarget.method_6032() <= 0.0F
                                        || this.mc.field_1724.method_5739(this.lockedTarget) > 10.0
                                )) {
                                this.lockedTarget = null;
                            }

                            if (this.lockedTarget == null || manualAttack && target != this.lockedTarget) {
                                this.lockedTarget = target;
                            }

                            if (this.lockedTarget != null && target != this.lockedTarget) {
                                this.resetCritState();
                                return;
                            }
                        }

                        if (this.targetMode.isEnabled()) {
                            if (this.lockedTarget != null
                                && (
                                    !this.lockedTarget.method_5805()
                                        || this.lockedTarget.method_31481()
                                        || this.lockedTarget.method_6032() <= 0.0F
                                        || this.mc.field_1724.method_5739(this.lockedTarget) > 10.0
                                )) {
                                this.lockedTarget = null;
                            }

                            if (this.lockedTarget == null || manualAttack && target != this.lockedTarget) {
                                this.lockedTarget = target;
                            }

                            if (this.lockedTarget != null && target != this.lockedTarget) {
                                this.resetCritState();
                                return;
                            }
                        }

                        if (this.ignoreShields.isEnabled() && target.method_6039()) {
                            this.resetCritState();
                            return;
                        }

                        if (this.onlyWeapons.isEnabled() && !this.isHoldingWeapon()) {
                            this.resetCritState();
                            return;
                        }

                        if (this.slotRestriction.isEnabled()) {
                            int selectedSlot = this.mc.field_1724.method_31548().method_67532() + 1;
                            if (selectedSlot != this.restrictedSlot.getValue()) {
                                this.resetCritState();
                                return;
                            }
                        }

                        if (this.onlyOnLmb.isEnabled() && !this.mc.field_1690.field_1886.method_1434()) {
                            this.resetCritState();
                            return;
                        }

                        long currentTime = System.currentTimeMillis();
                        if (currentTime < this.missLockUntilMs) {
                            return;
                        }

                        if (this.randomization.isEnabled() && currentTime < this.nextAttackTime) {
                            return;
                        }

                        double cooldownProgress = this.mc.field_1724.method_7261(0.5F);
                        double threshold = this.cooldownThreshold.getValue() / 100.0;
                        if (cooldownProgress < threshold) {
                            return;
                        }

                        TriggerBotReadyEvent event = new TriggerBotReadyEvent();
                        EventBus.post(event);
                        if (event.isCancelled()) {
                            return;
                        }

                        if (this.critPrio.isEnabled() && this.shouldWaitForCrit()) {
                            return;
                        }

                        if (this.shouldMissAttack()) {
                            this.missLockUntilMs = currentTime + (long)this.missDelayMs.getValue();
                            return;
                        }

                        this.executeAttack();
                    } else {
                        this.resetCritState();
                    }
                }
            }
        }
    }

    private boolean isHoldingWeapon() {
        class_1799 mainHandStack = this.mc.field_1724.method_6047();
        if (mainHandStack.method_7960()) {
            return false;
        } else {
            class_1792 item = mainHandStack.method_7909();
            return item.method_57347().method_57832(class_9334.field_50077) && item.method_57347().method_57832(class_9334.field_49636)
                ? true
                : item instanceof class_1743 || item instanceof class_1835 || item instanceof class_9362 || item.toString().toLowerCase().contains("sword");
        }
    }

    private void generateRandomDelay() {
        if (this.randomization.isEnabled()) {
            int maxDelay = (int)this.randomDelayMs.getValue();
            int randomDelay = this.random.nextInt(maxDelay * 2 + 1) - maxDelay;
            this.nextAttackTime = System.currentTimeMillis() + randomDelay;
        }
    }

    private boolean shouldMissAttack() {
        double chance = this.missChance.getValue();
        return chance <= 0.0 ? false : this.random.nextDouble() * 100.0 < chance;
    }

    private int getServerTick() {
        return this.mc.field_1724.field_6012;
    }

    private boolean shouldWaitForCrit() {
        if (this.isInWeb()) {
            this.resetCritState();
            return false;
        } else if (!this.mc.field_1724.method_24828()
            && !this.mc.field_1724.method_6101()
            && !this.mc.field_1724.method_5799()
            && !this.mc.field_1724.method_31549().field_7479) {
            double velocityY = this.mc.field_1724.method_18798().field_1351;
            if (!this.wasInAir && !this.mc.field_1724.method_24828()) {
                this.wasInAir = true;
                this.hasPassedPeak = false;
            }

            if (this.wasInAir && !this.hasPassedPeak && velocityY < 0.0) {
                this.hasPassedPeak = true;
                this.ticksAfterPeak = 0;
            }

            if (this.hasPassedPeak) {
                this.ticksAfterPeak++;
            }

            if (this.mc.field_1724.method_24828()) {
                this.resetCritState();
            }

            boolean isCritValid = this.wasInAir && this.hasPassedPeak && velocityY < -0.1;
            return !isCritValid;
        } else {
            this.resetCritState();
            return false;
        }
    }

    private void resetCritState() {
        this.wasInAir = false;
        this.hasPassedPeak = false;
        this.ticksAfterPeak = 0;
    }

    private boolean isInWeb() {
        return this.mc.field_1687.method_8320(this.mc.field_1724.method_24515()).method_27852(class_2246.field_10343)
            || this.mc.field_1687.method_8320(this.mc.field_1724.method_24515().method_10084()).method_27852(class_2246.field_10343);
    }

    private boolean executeAttack() {
        int clientTick = this.mc.field_1724.field_6012;
        if (clientTick == this.lastAttackClientTick) {
            return false;
        } else {
            int serverTick = -1;
            if (this.tpsSync.isEnabled()) {
                serverTick = this.getServerTick();
                if (serverTick == this.lastAttackServerTick) {
                    return false;
                }
            }

            if (!this.tryAttackSafe()) {
                return false;
            } else {
                this.lastAttackClientTick = clientTick;
                if (this.tpsSync.isEnabled()) {
                    this.lastAttackServerTick = serverTick;
                }

                this.generateRandomDelay();
                return true;
            }
        }
    }

    private void stopMining() {
        if (this.mc.field_1690.field_1886.method_1434()) {
            this.mc.field_1690.field_1886.method_23481(false);
        }
    }

    private boolean tryAttackSafe() {
        try {
            return AttackValidator.tryAttack(this.mc, "combat.attack.triggerbot");
        } catch (Throwable var2) {
            if (!warnedAttackValidatorFailure) {
                warnedAttackValidatorFailure = true;
            }

            return this.tryAttackFallback();
        }
    }

    private boolean tryAttackFallback() {
        if (this.mc.field_1724 == null || this.mc.field_1687 == null || this.mc.field_1761 == null) {
            return false;
        } else if (this.mc.field_1771 <= 0 && this.mc.field_1765 != null) {
            class_1799 itemStack = this.mc.field_1724.method_6047();
            if (!itemStack.method_7960() && !itemStack.method_45435(this.mc.field_1687.method_45162())) {
                return false;
            } else if (this.mc.field_1761.method_2920() == class_1934.field_9219) {
                return false;
            } else {
                if (this.mc.field_1765.method_17783() == class_240.field_1331) {
                    class_1297 target = ((class_3966)this.mc.field_1765).method_17782();
                    if (!target.method_5805()) {
                        return false;
                    }
                }

                return ((MinecraftClientAccessor)this.mc).attack();
            }
        } else {
            return false;
        }
    }

    @Override
    public void onDisable() {
        this.stopMining();
        this.resetCritState();
        this.missLockUntilMs = 0L;
    }
}
