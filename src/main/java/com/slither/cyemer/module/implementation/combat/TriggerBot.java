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
import java.util.HashMap;
import java.util.Map;
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
import net.minecraft.class_1268;
import net.minecraft.class_2246;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_3489;
import net.minecraft.class_3532;
import net.minecraft.class_3966;
import net.minecraft.class_9334;
import net.minecraft.class_9362;
import net.minecraft.class_239.class_240;

@Environment(EnvType.CLIENT)
public class TriggerBot extends Module {
    /** Vanilla player fall physics, used to project when a crit becomes available. */
    private static final double GRAVITY = 0.08;
    private static final double FALL_DRAG = 0.98;
    /** A descent further out than this is not worth holding a swing for. */
    private static final int MAX_CRIT_LOOKAHEAD_TICKS = 20;
    /** Drop opponents from the read once they have been out of sight this long. */
    private static final int SWING_STATE_TTL_TICKS = 200;
    /**
     * Ceiling on a measured reach. Vanilla tops out at 3.0, so anything past
     * this came from lag or desync rather than the opponent genuinely reaching
     * that far, and must not be allowed to define their threat range.
     */
    private static final double LEARNED_REACH_MAX = 4.0;
    /** Per-sweep bleed-off so one long outlier cannot poison the read forever. */
    private static final double LEARNED_REACH_DECAY = 0.1;

    private final BooleanSetting noSweep = new BooleanSetting("No Sweep", true);
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
    private final SliderSetting cooldownMargin = new SliderSetting("Their CD Margin (ticks)", 1.0, 0.0, 10.0, 0);
    private final SliderSetting maxHold = new SliderSetting("Max Hold (ticks)", 12.0, 0.0, 100.0, 0);
    private final BooleanSetting learnReach = new BooleanSetting("Learn Reach", true);
    private final BooleanSetting predictKb = new BooleanSetting("Predict KB", true);
    private final BooleanSetting critInWindow = new BooleanSetting("Crit In Window", true);
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
    private final Map<UUID, SwingState> swingStates = new HashMap<>();
    private int prevOwnHurtTime = 0;
    private int holdTicks = 0;

    /** Per-opponent combat read, kept across ticks even while we look away. */
    private static final class SwingState {
        boolean swinging;
        int swingTicks;
        int lastSwingTick = -1;
        int lastSeenTick = -1;
        double observedReach = 0.0;
    }

    public TriggerBot() {
        super("TriggerBot", "Automatically attacks when looking at an entity", Category.COMBAT);
        this.addSetting(this.noSweep);
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
        this.addSetting(this.learnReach);
        this.addSetting(this.predictKb);
        this.addSetting(this.critInWindow);
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null && this.mc.field_1755 == null) {
            this.updateCombatTracking();
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

                        boolean selecting = this.hitSelect.isEnabled() && target instanceof class_1657;
                        int freeWindow = selecting ? this.freeWindowTicks((class_1657)target) : Integer.MAX_VALUE;

                        if (selecting && !this.isFavorableExchange((class_1657)target, freeWindow)) {
                            return;
                        }

                        if (this.critPrio.isEnabled() && this.shouldWaitForCrit()) {
                            // Hold for the crit only when it actually arrives
                            // inside the free window. If the descent is further
                            // off than the window is long, they load up mid-hold
                            // and the crit we saved for becomes a trade instead.
                            boolean critArrivesInTime = freeWindow > this.ticksUntilCrit();
                            boolean windowClosing = selecting
                                && this.critInWindow.isEnabled()
                                && !critArrivesInTime;
                            if (!windowClosing) {
                                return;
                            }
                        }

                        if (this.shouldMissAttack()) {
                            this.missLockUntilMs = currentTime + (long)this.missDelayMs.getValue();
                            return;
                        }

                        // Last gate before the swing so nothing above can slip a
                        // sweep through. Deliberately does not reset crit state:
                        // holding here is how we wait out into a sprint or crit.
                        if (this.noSweep.isEnabled() && this.wouldSweep()) {
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
     * Exact vanilla sweep predicate for 1.21.11, mirroring PlayerEntity's
     * internal sweep check. Sweeping needs a full charge, no sprint knockback,
     * both feet on the ground, near-stationary movement, and a sword. A crit
     * cannot coincide with it because crits require being airborne.
     *
     * A sweep is the worst hit available: no crit multiplier, no sprint
     * knockback, and it splashes anything standing next to the target.
     */
    private boolean wouldSweep() {
        class_1657 self = this.mc.field_1724;
        if (self == null) {
            return false;
        }
        if (!self.method_24828()) {
            return false;
        }
        if (self.method_5624()) {
            return false;
        }
        if (!(self.method_7261(0.5F) > 0.9F)) {
            return false;
        }
        double horizontalSq = self.method_60478().method_37268();
        if (horizontalSq >= class_3532.method_33723(self.method_6029() * 2.5)) {
            return false;
        }
        return self.method_5998(class_1268.field_5808).method_31573(class_3489.field_42611);
    }

    /**
     * Keeps a per-opponent read of every nearby player, updated every tick
     * rather than only while one is under the crosshair, so looking away does
     * not blind the cooldown model.
     *
     * Two signals feed it. Swing animations are broadcast for tracked entities,
     * and swinging at air counts because vanilla resets attack cooldown on a
     * miss too. Damage landing on us is the stronger signal: whoever swung last
     * within a few ticks definitely just spent their cooldown, and the distance
     * at that moment is a direct measurement of how far they can actually
     * reach - worth more than any assumed constant.
     */
    private void updateCombatTracking() {
        class_1657 self = this.mc.field_1724;
        if (self == null || this.mc.field_1687 == null) {
            return;
        }
        int tick = self.field_6012;

        int ownHurt = self.field_6235;
        boolean tookDamage = ownHurt > this.prevOwnHurtTime;
        this.prevOwnHurtTime = ownHurt;

        class_1657 attacker = null;
        double attackerDistance = Double.MAX_VALUE;

        for (class_1657 player : this.mc.field_1687.method_18456()) {
            if (player == self || !player.method_5805()) {
                continue;
            }
            if (self.method_5739(player) > 12.0) {
                continue;
            }

            SwingState state = this.swingStates.computeIfAbsent(player.method_5667(), k -> new SwingState());
            state.lastSeenTick = tick;

            boolean swinging = player.field_6252;
            int swingTicks = player.field_6279;
            if (swinging && (!state.swinging || swingTicks < state.swingTicks)) {
                state.lastSwingTick = tick;
            }
            state.swinging = swinging;
            state.swingTicks = swingTicks;

            if (tookDamage && state.lastSwingTick >= 0 && tick - state.lastSwingTick <= 5) {
                double distance = this.distanceFromEyesToUs(player);
                if (distance < attackerDistance) {
                    attackerDistance = distance;
                    attacker = player;
                }
            }
        }

        if (attacker != null) {
            SwingState state = this.swingStates.get(attacker.method_5667());
            if (state != null) {
                state.lastSwingTick = tick;
                if (this.learnReach.isEnabled() && attackerDistance > state.observedReach) {
                    state.observedReach = Math.min(attackerDistance, LEARNED_REACH_MAX);
                }
            }
        }

        if ((tick & 63) == 0) {
            // Bleed the learned reach back down. Without this a single laggy hit
            // registering at long range would keep us inside their assumed threat
            // range permanently, and the bot would hold more and more the longer
            // a fight ran.
            for (SwingState state : this.swingStates.values()) {
                if (state.observedReach > 0.0) {
                    state.observedReach = Math.max(0.0, state.observedReach - LEARNED_REACH_DECAY);
                }
            }
            this.swingStates.entrySet().removeIf(e -> tick - e.getValue().lastSeenTick > SWING_STATE_TTL_TICKS);
        }
    }

    /**
     * Ticks remaining before the target could answer a hit right now.
     * Integer.MAX_VALUE means they simply cannot: committed to an item, or
     * too far to reach us. Zero means they are loaded and in range, so any
     * swing is a straight trade.
     */
    private int freeWindowTicks(class_1657 target) {
        class_1657 self = this.mc.field_1724;
        if (self == null) {
            return Integer.MAX_VALUE;
        }
        if (target.method_6039() || target.method_6115()) {
            return Integer.MAX_VALUE;
        }

        SwingState state = this.swingStates.get(target.method_5667());
        double cooldownTicks = this.targetCooldownTicks(target);
        int untilReady = 0;
        if (state != null && state.lastSwingTick >= 0) {
            int sinceSwing = self.field_6012 - state.lastSwingTick;
            untilReady = (int)Math.ceil(cooldownTicks - this.cooldownMargin.getValue()) - sinceSwing;
            if (untilReady < 0) {
                untilReady = 0;
            }
        }

        double reach = this.theirReach.getValue() + this.reachMargin.getValue();
        if (state != null && state.observedReach > 0.0) {
            reach = Math.max(reach, state.observedReach + this.reachMargin.getValue());
        }

        double distance = this.distanceFromEyesToUs(target);
        if (this.predictKb.isEnabled() && untilReady > 0) {
            // Knockback carries them away during their own recovery, so judge
            // reach against where they will be when they can actually swing.
            distance += Math.max(0.0, this.radialSpeedAway(target)) * untilReady * 0.85;
        }

        return distance > reach ? Integer.MAX_VALUE : untilReady;
    }

    /**
     * Hit selection: only swing when the exchange is one we win. A hit the
     * opponent can immediately answer is a trade, and a trade is a loss
     * whenever a free hit was available instead.
     */
    private boolean isFavorableExchange(class_1657 target, int freeWindow) {
        class_1657 self = this.mc.field_1724;
        if (self == null) {
            return true;
        }

        if (freeWindow > 0) {
            this.holdTicks = 0;
            return true;
        }

        double ourHealth = self.method_6032() + self.method_6067();
        double theirHealth = target.method_6032() + target.method_6067();
        if (ourHealth - theirHealth >= this.tradeHpLead.getValue()) {
            this.holdTicks = 0;
            return true;
        }

        // Neither side can be starved forever, and without movement control the
        // bot cannot disengage the way a player would, so give up the hold
        // rather than stall the fight outright.
        this.holdTicks++;
        if (this.holdTicks > (int)this.maxHold.getValue()) {
            this.holdTicks = 0;
            return true;
        }
        return false;
    }

    /**
     * Ticks until we are falling fast enough to crit, by stepping vanilla fall
     * physics forward from our current vertical velocity. Zero means the crit
     * is available now; MAX_VALUE means no descent is coming soon enough to be
     * worth holding a swing for.
     */
    private int ticksUntilCrit() {
        class_1657 self = this.mc.field_1724;
        if (self == null) {
            return Integer.MAX_VALUE;
        }
        double vy = self.method_18798().field_1351;
        if (vy < -0.1) {
            return 0;
        }
        for (int t = 1; t <= MAX_CRIT_LOOKAHEAD_TICKS; t++) {
            vy = (vy - GRAVITY) * FALL_DRAG;
            if (vy < -0.1) {
                return t;
            }
        }
        return Integer.MAX_VALUE;
    }

    /** Component of the target's velocity pointing directly away from us. */
    private double radialSpeedAway(class_1309 target) {
        class_1657 self = this.mc.field_1724;
        class_243 delta = new class_243(
            target.method_23317() - self.method_23317(),
            target.method_23318() - self.method_23318(),
            target.method_23321() - self.method_23321()
        );
        if (delta.method_1027() < 1.0E-6) {
            return 0.0;
        }
        return target.method_18798().method_1026(delta.method_1029());
    }

    /** Distance from the target's eyes to the nearest point of our hitbox. */
    private double distanceFromEyesToUs(class_1309 target) {
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
        this.swingStates.clear();
        this.prevOwnHurtTime = 0;
        this.holdTicks = 0;
    }
}
