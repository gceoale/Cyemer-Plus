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
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1642;
import net.minecraft.class_1657;
import net.minecraft.class_1743;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1835;
import net.minecraft.class_1934;
import net.minecraft.class_2246;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_3532;
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
    private final BooleanSetting strayBypass = new BooleanSetting("Stray Bypass", false);
    private final BooleanSetting hitSelect = new BooleanSetting("Hit Select", true);
    private final SliderSetting theirReach = new SliderSetting("Their Reach", 3.0, 2.5, 4.5, 2);
    private final SliderSetting reachMargin = new SliderSetting("Reach Margin", 0.15, 0.0, 1.0, 2);
    private final SliderSetting tradeHpLead = new SliderSetting("Trade HP Lead", 4.0, 0.0, 20.0, 1);
    private final SliderSetting cooldownMargin = new SliderSetting("Their CD Margin (ticks)", 2.0, 0.0, 10.0, 0);
    private final SliderSetting maxHold = new SliderSetting("Max Hold (ticks)", 30.0, 0.0, 100.0, 0);
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
    private UUID swingTrackedId = null;
    private boolean prevSwinging = false;
    private int prevSwingTicks = 0;
    private int targetLastSwingTick = -1;
    private int holdTicks = 0;

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
        this.addSetting(this.strayBypass);
        this.addSetting(this.hitSelect);
        this.addSetting(this.theirReach);
        this.addSetting(this.reachMargin);
        this.addSetting(this.tradeHpLead);
        this.addSetting(this.cooldownMargin);
        this.addSetting(this.maxHold);
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
                    if (this.mc.field_1765 instanceof class_3966 entityHit
                            && entityHit.method_17782() instanceof class_1309 target) {
                        if (!target.method_5805() || target.method_6032() <= 0.0F) {
                            this.resetCritState();
                            return;
                        }

                        this.trackSwing(target);

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

                        if (this.hitSelect.isEnabled() && !this.isFavorableExchange(target)) {
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

    /**
     * Records the tick the target last started a swing. The server broadcasts
     * swing animations for tracked entities, so handSwinging/handSwingTicks are
     * live for remote players. Swinging at air counts too, since vanilla resets
     * the attack cooldown on a miss as well as on a hit.
     */
    private void trackSwing(class_1309 target) {
        if (this.mc.field_1724 == null) {
            return;
        }
        UUID id = target.method_5667();
        if (!id.equals(this.swingTrackedId)) {
            this.swingTrackedId = id;
            this.prevSwinging = target.field_6252;
            this.prevSwingTicks = target.field_6279;
            this.targetLastSwingTick = -1;
            this.holdTicks = 0;
            return;
        }

        boolean swinging = target.field_6252;
        int swingTicks = target.field_6279;
        boolean newSwing = swinging && (!this.prevSwinging || swingTicks < this.prevSwingTicks);
        if (newSwing) {
            this.targetLastSwingTick = this.mc.field_1724.field_6012;
        }
        this.prevSwinging = swinging;
        this.prevSwingTicks = swingTicks;
    }

    /**
     * Hit selection: only swing when the exchange is one we win.
     *
     * A hit taken while the opponent can immediately answer it is a trade, and
     * a trade is a loss whenever a free hit was available instead. So the swing
     * goes out when they physically cannot punish it - they are blocking or
     * mid item-use, they are out of their own reach of us, or their attack
     * cooldown is still spent from their last swing. When both of us are loaded
     * and in range it is a genuine trade, taken only while our effective health
     * lead means we win that race.
     */
    private boolean isFavorableExchange(class_1309 target) {
        if (this.mc.field_1724 == null) {
            return true;
        }

        if (target.method_6039() || target.method_6115()) {
            this.holdTicks = 0;
            return true;
        }

        double reach = this.theirReach.getValue() + this.reachMargin.getValue();
        if (this.targetDistanceToUs(target) > reach) {
            this.holdTicks = 0;
            return true;
        }

        double cooldownTicks = this.targetCooldownTicks(target);
        int sinceSwing = this.mc.field_1724.field_6012 - this.targetLastSwingTick;
        boolean theirCooldownReady = this.targetLastSwingTick < 0
            || sinceSwing >= cooldownTicks - this.cooldownMargin.getValue();
        if (!theirCooldownReady) {
            this.holdTicks = 0;
            return true;
        }

        double ourHealth = this.mc.field_1724.method_6032() + this.mc.field_1724.method_6067();
        double theirHealth = target.method_6032() + target.method_6067();
        if (ourHealth - theirHealth >= this.tradeHpLead.getValue()) {
            this.holdTicks = 0;
            return true;
        }

        // Neither side can be starved forever; without movement control the bot
        // cannot disengage, so give up the hold rather than stall the fight.
        this.holdTicks++;
        if (this.holdTicks > (int)this.maxHold.getValue()) {
            this.holdTicks = 0;
            return true;
        }
        return false;
    }

    /** Distance from the target's eyes to the nearest point of our hitbox. */
    private double targetDistanceToUs(class_1309 target) {
        class_243 theirEye = target.method_33571();
        class_238 ourBox = this.mc.field_1724.method_5829();
        double cx = class_3532.method_15350(theirEye.field_1352, ourBox.field_1323, ourBox.field_1320);
        double cy = class_3532.method_15350(theirEye.field_1351, ourBox.field_1322, ourBox.field_1325);
        double cz = class_3532.method_15350(theirEye.field_1350, ourBox.field_1321, ourBox.field_1324);
        return theirEye.method_1022(new class_243(cx, cy, cz));
    }

    /** Attack cooldown length in ticks for whatever the target is holding. */
    private double targetCooldownTicks(class_1309 target) {
        class_1792 item = target.method_6047().method_7909();
        double speed;
        if (item instanceof class_9362) {
            speed = 0.6;
        } else if (item instanceof class_1743) {
            speed = 1.0;
        } else if (item instanceof class_1835) {
            speed = 1.1;
        } else if (item.toString().toLowerCase().contains("sword")) {
            speed = 1.6;
        } else {
            speed = 4.0;
        }
        return 20.0 / speed;
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
        this.swingTrackedId = null;
        this.targetLastSwingTick = -1;
        this.prevSwinging = false;
        this.prevSwingTicks = 0;
        this.holdTicks = 0;
    }
}
