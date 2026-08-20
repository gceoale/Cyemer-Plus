package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.event.EventBus;
import com.slither.cyemer.event.impl.ShieldDrainEvent;
import com.slither.cyemer.event.impl.TriggerBotReadyEvent;
import com.slither.cyemer.friend.FriendManager;
import com.slither.cyemer.mixin.MinecraftClientAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.AttackValidator;
import java.util.Random;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1743;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1819;
import net.minecraft.class_1835;
import net.minecraft.class_1934;
import net.minecraft.class_2246;
import net.minecraft.class_3966;
import net.minecraft.class_9334;
import net.minecraft.class_9362;
import net.minecraft.class_239.class_240;

@Environment(EnvType.CLIENT)
public class TriggerBot extends Module {
    /**
     * A remote entity's hurtTime is only set when the server's damage packet
     * arrives, so a landed hit shows up somewhere between 1 tick and a full
     * round trip later. Credit any damage seen on the target within this many
     * ticks of our swing as ours.
     */
    private static final int HIT_CREDIT_TICKS = 10;
    /** Ticks a landed hit keeps the combo alive. */
    private static final int COMBO_WINDOW_TICKS = 20;
    /** No landed hit for this long means the next swing is opening an exchange. */
    private static final int OPENING_IDLE_TICKS = 40;
    /** Vanilla axe shield disable lasts 100 ticks; stay aggressive slightly under that. */
    private static final long SHIELD_DISABLE_MS = 4500L;
    private static final long MISS_BACKOFF_MS = 200L;

    private final BooleanSetting adaptive = new BooleanSetting("Adaptive", true);
    private final BooleanSetting openingBurst = new BooleanSetting("Opening Burst", true);
    private final BooleanSetting critPrio = new BooleanSetting("Crit Prio", true);
    private final SliderSetting critWaitMax = new SliderSetting("Crit Wait Max (ticks)", 5.0, 0.0, 40.0, 0);
    private final ModeSetting shieldPolicy = new ModeSetting("Shields", "Smart", "Skip", "Attack");
    private final BooleanSetting hurtTimeGate = new BooleanSetting("Hurt Time Gate", true);
    private final BooleanSetting punishItems = new BooleanSetting("Punish Items", true);
    private final BooleanSetting comboMode = new BooleanSetting("Combo Mode", true);
    private final BooleanSetting finisher = new BooleanSetting("Finisher", true);
    private final SliderSetting finishHp = new SliderSetting("Finish HP", 6.0, 0.0, 20.0, 1);
    private final SliderSetting punishCooldown = new SliderSetting("Punish Cooldown %", 90.0, 0.0, 100.0, 0);
    private final BooleanSetting missAdapt = new BooleanSetting("Miss Backoff", false);
    private final SliderSetting missBackoffCount = new SliderSetting("Miss Backoff Count", 5.0, 2.0, 15.0, 0);
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

    private boolean wasInAir = false;
    private boolean hasPassedPeak = false;
    private int ticksAfterPeak = 0;
    private int critWaitTicks = 0;
    private int lastAttackClientTick = -1;
    private int lastAttackServerTick = -1;
    private final Random random = new Random();
    private long nextAttackTime = 0L;
    private long lastItemUseTime = 0L;
    private long missLockUntilMs = 0L;
    private static boolean warnedAttackValidatorFailure = false;
    private class_1309 lockedTarget = null;
    private boolean wasAttackKeyPressed = false;

    private UUID trackedTargetId = null;
    private int trackedHurtTime = 0;
    private int pendingSwingTick = -1;
    private boolean pendingSwingIntoBlock = false;
    private boolean pendingSwingWithAxe = false;
    private int lastLandedTick = -10000;
    private int consecutiveMisses = 0;
    private long shieldDisabledUntilMs = 0L;
    private long backoffUntilMs = 0L;

    public TriggerBot() {
        super("TriggerBot", "Automatically attacks when looking at an entity", Category.COMBAT);
        this.addSetting(this.adaptive);
        this.addSetting(this.openingBurst);
        this.addSetting(this.critPrio);
        this.addSetting(this.critWaitMax);
        this.addSetting(this.shieldPolicy);
        this.addSetting(this.hurtTimeGate);
        this.addSetting(this.punishItems);
        this.addSetting(this.comboMode);
        this.addSetting(this.finisher);
        this.addSetting(this.finishHp);
        this.addSetting(this.punishCooldown);
        this.addSetting(this.missAdapt);
        this.addSetting(this.missBackoffCount);
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
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 == null || this.mc.field_1687 == null || this.mc.field_1755 != null) {
            return;
        }

        ShieldDrainEvent drainEvent = new ShieldDrainEvent();
        EventBus.post(drainEvent);
        if (drainEvent.isActive()) {
            return;
        }

        if (this.mc.field_1724.method_6115()) {
            this.lastItemUseTime = System.currentTimeMillis();
            this.resetCritState();
            return;
        }
        if (System.currentTimeMillis() - this.lastItemUseTime < 1L) {
            this.resetCritState();
            return;
        }

        boolean isAttackKeyPressed = this.mc.field_1690.field_1886.method_1434();
        boolean manualAttack = isAttackKeyPressed && !this.wasAttackKeyPressed;
        this.wasAttackKeyPressed = isAttackKeyPressed;

        if (!(this.mc.field_1765 instanceof class_3966 entityHit) || !(entityHit.method_17782() instanceof class_1309 target)) {
            this.trackDamage(null);
            this.resetCritState();
            return;
        }

        this.trackDamage(target);

        if (!target.method_5805() || target.method_6032() <= 0.0F) {
            this.resetCritState();
            return;
        }

        if (this.ignoreFriends.isEnabled() && target instanceof class_1657 player && FriendManager.getInstance().isFriend(player.method_5667())) {
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

        boolean smart = this.adaptive.isEnabled();
        long currentTime = System.currentTimeMillis();

        if (!this.passesShieldPolicy(target, smart)) {
            this.resetCritState();
            return;
        }

        if (currentTime < this.missLockUntilMs) {
            return;
        }

        if (smart && this.missAdapt.isEnabled() && currentTime < this.backoffUntilMs) {
            return;
        }

        if (this.randomization.isEnabled() && currentTime < this.nextAttackTime) {
            return;
        }

        // Tempo urgency: take the hit now rather than hold for a better one.
        boolean skipCrit = smart && this.shouldSkipCrit(target, currentTime);
        // Damage urgency: a slightly under-charged hit beats no hit at all,
        // but only when landing it *now* is what matters (interrupting an
        // item use, or closing out a target who is about to die).
        boolean relaxCooldown = smart && this.shouldRelaxCooldown(target);

        if (smart && this.hurtTimeGate.isEnabled() && !relaxCooldown && target.field_6235 > 3) {
            return;
        }

        double cooldownProgress = this.mc.field_1724.method_7261(0.5F);
        double threshold = this.cooldownThreshold.getValue() / 100.0;
        if (relaxCooldown) {
            threshold = Math.min(threshold, this.punishCooldown.getValue() / 100.0);
        }
        if (cooldownProgress < threshold) {
            return;
        }

        TriggerBotReadyEvent event = new TriggerBotReadyEvent();
        EventBus.post(event);
        if (event.isCancelled()) {
            return;
        }

        if (this.critPrio.isEnabled() && this.shouldWaitForCrit()) {
            this.critWaitTicks++;
            boolean giveUpWaiting = smart && (skipCrit || this.critWaitTicks > (int) this.critWaitMax.getValue());
            if (!giveUpWaiting) {
                return;
            }
        } else {
            this.critWaitTicks = 0;
        }

        if (this.shouldMissAttack()) {
            this.missLockUntilMs = currentTime + (long) this.missDelayMs.getValue();
            return;
        }

        this.executeAttack(target);
    }

    /**
     * Reasons to stop holding a swing for a crit. Waiting costs tempo, and a
     * hit that never lands does zero damage, so any of these beat a crit:
     * we are opening an exchange and want the first hit in, the combo is live
     * and jumping would drop the knockback chain, the target is mid item-use,
     * they are nearly dead, or their shield is disabled and the clock is running.
     */
    private boolean shouldSkipCrit(class_1309 target, long now) {
        if (this.openingBurst.isEnabled() && this.isOpening()) {
            return true;
        }
        if (this.comboMode.isEnabled() && this.inCombo()) {
            return true;
        }
        if (now < this.shieldDisabledUntilMs) {
            return true;
        }
        return this.shouldRelaxCooldown(target);
    }

    private boolean shouldRelaxCooldown(class_1309 target) {
        if (this.punishItems.isEnabled() && this.isTargetUsingNonShield(target)) {
            return true;
        }
        return this.finisher.isEnabled() && this.effectiveHealth(target) <= this.finishHp.getValue();
    }

    private boolean isOpening() {
        return this.mc.field_1724 != null && this.mc.field_1724.field_6012 - this.lastLandedTick > OPENING_IDLE_TICKS;
    }

    private boolean inCombo() {
        return this.mc.field_1724 != null && this.mc.field_1724.field_6012 - this.lastLandedTick <= COMBO_WINDOW_TICKS;
    }

    private double effectiveHealth(class_1309 target) {
        return target.method_6032() + target.method_6067();
    }

    private boolean isTargetUsingNonShield(class_1309 target) {
        if (!target.method_6115()) {
            return false;
        }
        class_1799 active = target.method_6030();
        return !active.method_7960() && !(active.method_7909() instanceof class_1819);
    }

    /**
     * Smart mode never feeds a swing into a raised shield unless the swing
     * can actually break it: an axe disables the shield for 5 seconds, so
     * that hit is worth the cooldown. Anything else just eats the cooldown
     * for near-zero damage, so we hold and let the player reposition.
     */
    private boolean passesShieldPolicy(class_1309 target, boolean smart) {
        boolean blocking = target.method_6039();
        if (!blocking) {
            return true;
        }
        if (!smart) {
            return !this.ignoreShields.isEnabled();
        }
        return switch (this.shieldPolicy.getCurrentMode()) {
            case "Skip" -> false;
            case "Attack" -> true;
            default -> this.isHoldingAxe();
        };
    }

    private boolean isHoldingAxe() {
        return this.mc.field_1724 != null && this.mc.field_1724.method_6047().method_7909() instanceof class_1743;
    }

    /**
     * Watches the target's hurtTime for a rising edge, which is how a landed
     * hit becomes visible on the client once the server's damage packet lands.
     * Damage seen within HIT_CREDIT_TICKS of our swing counts as ours and
     * keeps the combo clock alive.
     */
    private void trackDamage(class_1309 target) {
        if (target == null || this.mc.field_1724 == null) {
            this.trackedTargetId = null;
            this.trackedHurtTime = 0;
            return;
        }

        int clientTick = this.mc.field_1724.field_6012;
        UUID id = target.method_5667();
        if (!id.equals(this.trackedTargetId)) {
            this.trackedTargetId = id;
            this.trackedHurtTime = target.field_6235;
            return;
        }

        int hurtNow = target.field_6235;
        boolean tookDamage = hurtNow > this.trackedHurtTime;
        this.trackedHurtTime = hurtNow;

        if (tookDamage && this.pendingSwingTick >= 0 && clientTick - this.pendingSwingTick <= HIT_CREDIT_TICKS) {
            this.lastLandedTick = clientTick;
            this.consecutiveMisses = 0;
            if (this.pendingSwingIntoBlock && this.pendingSwingWithAxe) {
                this.shieldDisabledUntilMs = System.currentTimeMillis() + SHIELD_DISABLE_MS;
            }
            this.pendingSwingTick = -1;
            return;
        }

        if (this.pendingSwingTick >= 0 && clientTick - this.pendingSwingTick > HIT_CREDIT_TICKS) {
            this.pendingSwingTick = -1;
            this.consecutiveMisses++;
            if (this.consecutiveMisses >= (int) this.missBackoffCount.getValue()) {
                this.backoffUntilMs = System.currentTimeMillis() + MISS_BACKOFF_MS;
                this.consecutiveMisses = 0;
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
            if (maxDelay <= 0) {
                this.nextAttackTime = System.currentTimeMillis();
                return;
            }
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
            if (!this.wasInAir) {
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
        this.critWaitTicks = 0;
    }

    private boolean isInWeb() {
        return this.mc.field_1687.method_8320(this.mc.field_1724.method_24515()).method_27852(class_2246.field_10343)
            || this.mc.field_1687.method_8320(this.mc.field_1724.method_24515().method_10084()).method_27852(class_2246.field_10343);
    }

    private boolean executeAttack(class_1309 target) {
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

            boolean wasBlocking = target.method_6039();
            boolean withAxe = this.isHoldingAxe();

            if (!this.tryAttackSafe()) {
                return false;
            } else {
                this.lastAttackClientTick = clientTick;
                if (this.tpsSync.isEnabled()) {
                    this.lastAttackServerTick = serverTick;
                }

                this.pendingSwingTick = clientTick;
                this.pendingSwingIntoBlock = wasBlocking;
                this.pendingSwingWithAxe = withAxe;
                this.critWaitTicks = 0;

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
        this.backoffUntilMs = 0L;
        this.shieldDisabledUntilMs = 0L;
        this.pendingSwingTick = -1;
        this.trackedTargetId = null;
        this.consecutiveMisses = 0;
        this.lastLandedTick = -10000;
    }
}
