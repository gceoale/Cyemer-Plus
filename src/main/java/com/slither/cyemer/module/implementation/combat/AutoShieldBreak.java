package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.friend.FriendManager;
import com.slither.cyemer.mixin.PlayerInventoryAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.AttackValidator;
import com.slither.cyemer.util.RotationManager;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1268;
import net.minecraft.class_1657;
import net.minecraft.class_1743;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_3532;
import net.minecraft.class_3965;
import net.minecraft.class_3966;
import net.minecraft.class_239.class_240;

@Environment(EnvType.CLIENT)
public class AutoShieldBreak extends Module {
    public static boolean breakingShield = false;
    private final SliderSetting cps = new SliderSetting("CPS", 20.0, 1.0, 20.0, 0);
    private final SliderSetting reactionDelay = new SliderSetting("Reaction Delay", 0.0, 0.0, 250.0, 0);
    private final SliderSetting swapDelay = new SliderSetting("Swap Delay", 50.0, 0.0, 500.0, 0);
    private final SliderSetting attackDelay = new SliderSetting("Attack Delay", 50.0, 0.0, 500.0, 0);
    private final SliderSetting swapBackDelay = new SliderSetting("Swap Back Delay", 100.0, 0.0, 500.0, 0);
    private final BooleanSetting revertSlot = new BooleanSetting("Revert Slot", true);
    private final BooleanSetting rayTraceCheck = new BooleanSetting("Check Facing", true);
    private final BooleanSetting autoStun = new BooleanSetting("Auto Stun", true);
    private final BooleanSetting disableIfUsingItem = new BooleanSetting("Disable if using item", true);
    private final BooleanSetting ignoreFriends = new BooleanSetting("Ignore Friends", false);
    private final SliderSetting hitboxAccuracy = new SliderSetting("Hitbox Accuracy", 0.0, 0.0, 1.0, 2);
    private final BooleanSetting drain = new BooleanSetting("Drain", false);
    private long lastCpsMs;
    private long lastReactionMs;
    private long lastSwapMs;
    private long lastAttackMs;
    private long lastSwapBackMs;
    private int savedSlot = -1;

    // Drain state machine - triggers after a shield-break attack lands. Uses a
    // per-tick action model so each state runs at most one packet burst per
    // tick, then the next tick moves us forward.
    private enum DrainState {
        IDLE,
        PLACE_COBWEB,
        PLACE_LAVA_ON_COBWEB,
        WAIT_LAVA,
        PICKUP_LAVA,
        PLACE_LAVA_AWAY,
        PLACE_PLANK_ON_LAVA,
        WATCH_WATER,
        INTERCEPT_WATER,
        LOOK_AWAY,
        PLACE_WATER_AWAY,
        PLACE_PLANK_ON_WATER,
        DONE
    }

    private DrainState drainState = DrainState.IDLE;
    private UUID drainTargetId = null;
    private class_2338 drainCobwebPos = null;
    private class_2338 drainLavaPos = null;      // where lava currently is
    private class_2338 drainAwayLavaPos = null;  // where lava got moved to
    private class_2338 drainWaterPos = null;     // enemy-placed water location
    private int drainWaitTicks = 0;
    private int drainSavedSlot = -1;
    private int drainFailCount = 0;              // consecutive placement failures
    private long drainStartedAt = 0L;
    private static final int DRAIN_LAVA_WAIT_TICKS = 5;
    private static final int DRAIN_MAX_FAILS = 15;
    private static final long DRAIN_WATCH_TIMEOUT_MS = 6000L;

    public AutoShieldBreak() {
        super("AutoShieldBreak", "Automatically breaks the opponent's shield", Category.COMBAT);
        this.addSetting(this.cps);
        this.addSetting(this.reactionDelay);
        this.addSetting(this.swapDelay);
        this.addSetting(this.attackDelay);
        this.addSetting(this.swapBackDelay);
        this.addSetting(this.revertSlot);
        this.addSetting(this.rayTraceCheck);
        this.addSetting(this.autoStun);
        this.addSetting(this.disableIfUsingItem);
        this.addSetting(this.ignoreFriends);
        this.addSetting(this.hitboxAccuracy);
        this.addSetting(this.drain);
    }

    @Override
    public void onTick() {
        this.tickDrain();
    }

    private boolean canRunAuto() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null && this.mc.field_1755 == null) {
            int axeSlot = this.findAxe();
            if (axeSlot == -1) {
                return false;
            } else {
                return this.mc.field_1724.method_6115() && this.disableIfUsingItem.isEnabled()
                    ? false
                    : System.currentTimeMillis() - this.lastCpsMs >= (long)(1000.0 / Math.max(0.1, this.cps.getValue()));
            }
        } else {
            return false;
        }
    }

    private class_1657 getTargetPlayer() {
        if (this.mc.field_1765 != null && this.mc.field_1765.method_17783() == class_240.field_1331) {
            class_3966 hit = (class_3966)this.mc.field_1765;
            if (hit.method_17782() instanceof class_1657 target) {
                return this.ignoreFriends.isEnabled() && FriendManager.getInstance().isFriend(target.method_5667()) ? null : target;
            } else {
                return null;
            }
        } else {
            return null;
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

    @Override
    public void onTickPost() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            class_1657 target = this.getTargetPlayer();
            if (this.savedSlot != -1 && System.currentTimeMillis() - this.lastSwapBackMs >= this.swapBackDelay.getValue()) {
                boolean shouldSwapBack = false;
                if (target == null) {
                    shouldSwapBack = true;
                } else {
                    boolean isBlocking = target.method_6039() && target.method_24518(class_1802.field_8255);
                    boolean canBreak = true;
                    if (this.rayTraceCheck.isEnabled()) {
                        class_243 dirToMe = this.mc.field_1724.method_73189().method_1020(target.method_73189()).method_1029();
                        class_243 targetLook = target.method_5828(1.0F).method_1029();
                        if (dirToMe.method_1026(targetLook) <= -0.6) {
                            canBreak = false;
                        }
                    }

                    if (canBreak && !this.isLookingAtHitbox(target)) {
                        canBreak = false;
                    }

                    if (!isBlocking || !canBreak) {
                        shouldSwapBack = true;
                    }
                }

                if (shouldSwapBack) {
                    if (this.revertSlot.isEnabled() && ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).getSelectedSlot() != this.savedSlot) {
                        ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).setSelectedSlot(this.savedSlot);
                    }

                    this.savedSlot = -1;
                    breakingShield = false;
                    return;
                }
            }

            if (this.canRunAuto()) {
                if (target != null) {
                    boolean isBlockingx = target.method_6039() && target.method_24518(class_1802.field_8255);
                    boolean canBreakx = true;
                    if (this.rayTraceCheck.isEnabled()) {
                        class_243 dirToMe = this.mc.field_1724.method_73189().method_1020(target.method_73189()).method_1029();
                        class_243 targetLook = target.method_5828(1.0F).method_1029();
                        if (dirToMe.method_1026(targetLook) <= -0.6) {
                            canBreakx = false;
                        }
                    }

                    if (canBreakx && !this.isLookingAtHitbox(target)) {
                        canBreakx = false;
                    }

                    if (!isBlockingx || !canBreakx) {
                        if (System.currentTimeMillis() - this.lastReactionMs < (long)(this.reactionDelay.getValue() / 2.0)) {
                            this.lastReactionMs = System.currentTimeMillis();
                        }
                    } else if (!(this.mc.field_1724.method_6047().method_7909() instanceof class_1743)) {
                        if (System.currentTimeMillis() - this.lastReactionMs >= (long)this.reactionDelay.getValue()
                            && System.currentTimeMillis() - this.lastSwapMs >= (long)this.swapDelay.getValue()) {
                            breakingShield = true;
                            if (this.savedSlot == -1) {
                                this.savedSlot = ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).getSelectedSlot();
                            }

                            int axeSlot = this.findAxe();
                            if (axeSlot != -1) {
                                ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).setSelectedSlot(axeSlot);
                            }

                            this.lastAttackMs = System.currentTimeMillis();
                            this.lastSwapMs = System.currentTimeMillis();
                        }
                    } else {
                        if (System.currentTimeMillis() - this.lastAttackMs >= (long)this.attackDelay.getValue() || this.savedSlot == -1) {
                            AttackValidator.tryAttack(this.mc, "combat.attack.autoshieldbreak.recode");
                            boolean stunFired = false;
                            if (this.autoStun.isEnabled()) {
                                AttackValidator.tryAttack(this.mc, "combat.attack.autoshieldbreak.recode.stun");
                                stunFired = true;
                            }
                            if (this.drain.isEnabled() && this.drainState == DrainState.IDLE) {
                                this.startDrain(target, stunFired);
                            }

                            this.lastCpsMs = System.currentTimeMillis();
                            this.lastAttackMs = System.currentTimeMillis();
                            this.lastSwapBackMs = System.currentTimeMillis();
                            breakingShield = false;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        if (this.mc.field_1724 != null && this.savedSlot != -1 && this.revertSlot.isEnabled()) {
            ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).setSelectedSlot(this.savedSlot);
        }

        this.savedSlot = -1;
        breakingShield = false;
        this.resetDrain();
        super.onDisable();
    }

    private void startDrain(class_1657 target, boolean stunFired) {
        if (target == null || this.mc.field_1724 == null || this.mc.field_1687 == null) return;
        class_2338 cobwebPos = this.predictCobwebPos(target, stunFired);
        if (cobwebPos == null) return;
        if (!this.mc.field_1687.method_8320(cobwebPos).method_45474()) return;

        this.drainTargetId = target.method_5667();
        this.drainCobwebPos = cobwebPos;
        this.drainStartedAt = System.currentTimeMillis();
        this.drainSavedSlot = ((PlayerInventoryAccessor) this.mc.field_1724.method_31548()).getSelectedSlot();
        this.drainState = DrainState.PLACE_COBWEB;
        this.drainWaitTicks = 0;
        this.drainFailCount = 0;
    }

    private class_2338 predictCobwebPos(class_1657 target, boolean stunFired) {
        // The old peak-prediction placed cobweb in mid-air with no adjacent
        // solid to click against, so placement always failed. Cobwebs work
        // fine at the target's feet - they stand in / land in them either
        // way, and there's always a floor block below to place against.
        return target.method_24515();
    }

    private void tickDrain() {
        if (this.drainState == DrainState.IDLE) return;
        if (this.mc.field_1724 == null || this.mc.field_1687 == null || this.mc.field_1761 == null) {
            this.resetDrain();
            return;
        }

        switch (this.drainState) {
            case PLACE_COBWEB:
                this.drainPlaceCobweb();
                break;
            case PLACE_LAVA_ON_COBWEB:
                this.drainPlaceLavaOnCobweb();
                break;
            case WAIT_LAVA:
                if (++this.drainWaitTicks >= DRAIN_LAVA_WAIT_TICKS) {
                    this.drainWaitTicks = 0;
                    this.drainState = DrainState.PICKUP_LAVA;
                }
                break;
            case PICKUP_LAVA:
                this.drainPickupLava();
                break;
            case PLACE_LAVA_AWAY:
                this.drainPlaceLavaAway();
                break;
            case PLACE_PLANK_ON_LAVA:
                this.drainPlacePlankOn(this.drainAwayLavaPos);
                this.drainState = DrainState.WATCH_WATER;
                this.drainWaitTicks = 0;
                break;
            case WATCH_WATER:
                this.drainWatchWater();
                break;
            case INTERCEPT_WATER:
                this.drainInterceptWater();
                break;
            case LOOK_AWAY:
                // A single tick "look away" step - packet sequence is fine
                // without a real rotation; kept as a state slot for clarity.
                this.drainState = DrainState.PLACE_WATER_AWAY;
                break;
            case PLACE_WATER_AWAY:
                this.drainPlaceWaterAway();
                break;
            case PLACE_PLANK_ON_WATER:
                this.drainPlacePlankOn(this.drainWaterPos);
                this.drainState = DrainState.DONE;
                break;
            case DONE:
                this.resetDrain();
                break;
        }
    }

    private void drainPlaceCobweb() {
        int cobwebSlot = this.findItemInHotbar(class_1802.field_8786);
        if (cobwebSlot == -1) { this.resetDrain(); return; }
        if (!this.placeBlockAt(this.drainCobwebPos, cobwebSlot)) {
            this.drainFailCount++;
            if (this.drainFailCount >= DRAIN_MAX_FAILS) this.resetDrain();
            return;
        }
        this.drainFailCount = 0;
        this.drainState = DrainState.PLACE_LAVA_ON_COBWEB;
    }

    private void drainPlaceLavaOnCobweb() {
        int lavaSlot = this.findItemInHotbar(class_1802.field_8187);
        if (lavaSlot == -1) { this.resetDrain(); return; }
        class_2338 lavaAt = this.drainCobwebPos.method_10084();
        if (!this.placeLiquidAt(lavaAt, lavaSlot)) {
            this.drainFailCount++;
            if (this.drainFailCount >= DRAIN_MAX_FAILS) this.resetDrain();
            return;
        }
        this.drainFailCount = 0;
        this.drainLavaPos = lavaAt;
        this.drainState = DrainState.WAIT_LAVA;
        this.drainWaitTicks = 0;
    }

    private void drainPickupLava() {
        int bucketSlot = this.findItemInHotbar(class_1802.field_8550);
        if (bucketSlot == -1) {
            // No empty bucket - fall through to placement flow anyway,
            // enemy will still be stuck in cobweb.
            this.drainState = DrainState.WATCH_WATER;
            this.drainWaitTicks = 0;
            return;
        }
        ((PlayerInventoryAccessor) this.mc.field_1724.method_31548()).setSelectedSlot(bucketSlot);
        class_3965 hit = this.hitAt(this.drainLavaPos, class_2350.field_11036);
        this.mc.field_1761.method_2896(this.mc.field_1724, class_1268.field_5808, hit);
        this.drainState = DrainState.PLACE_LAVA_AWAY;
    }

    private void drainPlaceLavaAway() {
        int lavaSlot = this.findItemInHotbar(class_1802.field_8187);
        if (lavaSlot == -1) {
            this.drainState = DrainState.WATCH_WATER;
            this.drainWaitTicks = 0;
            return;
        }
        // Two blocks away in the direction of the player (safe from enemy).
        class_2338 awayPos = this.pickAwayPos(this.drainCobwebPos);
        if (awayPos == null) {
            this.drainState = DrainState.WATCH_WATER;
            this.drainWaitTicks = 0;
            return;
        }
        if (!this.placeLiquidAt(awayPos, lavaSlot)) return;
        this.drainAwayLavaPos = awayPos;
        this.drainState = DrainState.PLACE_PLANK_ON_LAVA;
    }

    private void drainPlacePlankOn(class_2338 pos) {
        if (pos == null) return;
        int plankSlot = this.findAnyPlankSlot();
        if (plankSlot == -1) return;
        // Plank goes ON TOP of the target position.
        class_2338 top = pos.method_10084();
        if (!this.placeBlockAt(top, plankSlot)) return;
    }

    private void drainWatchWater() {
        if (System.currentTimeMillis() - this.drainStartedAt > DRAIN_WATCH_TIMEOUT_MS) {
            this.resetDrain();
            return;
        }
        // Enemy escapes cobweb by placing water. Poll the cobweb position for
        // a water block appearing.
        class_2680 state = this.mc.field_1687.method_8320(this.drainCobwebPos);
        if (state.method_27852(class_2246.field_10382)) {
            this.drainWaterPos = this.drainCobwebPos;
            this.drainState = DrainState.INTERCEPT_WATER;
        }
        // Also check the block above (some servers place water above cobweb).
        class_2338 above = this.drainCobwebPos.method_10084();
        class_2680 aboveState = this.mc.field_1687.method_8320(above);
        if (aboveState.method_27852(class_2246.field_10382)) {
            this.drainWaterPos = above;
            this.drainState = DrainState.INTERCEPT_WATER;
        }
    }

    private void drainInterceptWater() {
        int bucketSlot = this.findItemInHotbar(class_1802.field_8550);
        if (bucketSlot == -1) { this.resetDrain(); return; }
        ((PlayerInventoryAccessor) this.mc.field_1724.method_31548()).setSelectedSlot(bucketSlot);
        class_3965 hit = this.hitAt(this.drainWaterPos, class_2350.field_11036);
        this.mc.field_1761.method_2896(this.mc.field_1724, class_1268.field_5808, hit);
        this.drainState = DrainState.LOOK_AWAY;
    }

    private void drainPlaceWaterAway() {
        int waterSlot = this.findItemInHotbar(class_1802.field_8705);
        if (waterSlot == -1) {
            // We picked it up but the item didn't sync in yet - retry.
            return;
        }
        class_2338 dumpAt = this.pickAwayPos(this.drainWaterPos);
        if (dumpAt == null) { this.resetDrain(); return; }
        if (!this.placeLiquidAt(dumpAt, waterSlot)) return;
        this.drainState = DrainState.PLACE_PLANK_ON_WATER;
    }

    private class_2338 pickAwayPos(class_2338 from) {
        // Try positions 2 blocks away in the 4 cardinal directions, prefer
        // the one closest to the player so the "safe" side is where we are.
        class_243 p = this.mc.field_1724.method_73189();
        class_2350[] dirs = { class_2350.field_11034, class_2350.field_11039, class_2350.field_11035, class_2350.field_11043 };
        class_2338 best = null;
        double bestDist = Double.MAX_VALUE;
        for (class_2350 d : dirs) {
            class_2338 cand = from.method_10093(d).method_10093(d);
            if (!this.mc.field_1687.method_8320(cand).method_45474()) continue;
            double dd = cand.method_10262(new class_2338((int) p.field_1352, (int) p.field_1351, (int) p.field_1350));
            if (dd < bestDist) { bestDist = dd; best = cand; }
        }
        return best;
    }

    private boolean placeBlockAt(class_2338 pos, int slot) {
        // Find any solid adjacent block to click against.
        class_2338 against = this.findAdjacentSolid(pos);
        if (against == null) return false;
        class_2350 face = class_2350.method_10147(
                pos.method_10263() - against.method_10263(),
                pos.method_10264() - against.method_10264(),
                pos.method_10260() - against.method_10260()
        );
        if (face == null) return false;
        ((PlayerInventoryAccessor) this.mc.field_1724.method_31548()).setSelectedSlot(slot);
        this.mc.field_1761.method_2896(this.mc.field_1724, class_1268.field_5808, this.hitAt(against, face));
        return true;
    }

    private boolean placeLiquidAt(class_2338 pos, int slot) {
        // Liquid buckets need a click on a solid block (or a replaceable block).
        // Clicking the adjacent solid places the liquid at pos.
        class_2338 against = this.findAdjacentSolid(pos);
        if (against == null) return false;
        class_2350 face = class_2350.method_10147(
                pos.method_10263() - against.method_10263(),
                pos.method_10264() - against.method_10264(),
                pos.method_10260() - against.method_10260()
        );
        if (face == null) return false;
        ((PlayerInventoryAccessor) this.mc.field_1724.method_31548()).setSelectedSlot(slot);
        this.mc.field_1761.method_2896(this.mc.field_1724, class_1268.field_5808, this.hitAt(against, face));
        return true;
    }

    private class_2338 findAdjacentSolid(class_2338 pos) {
        class_2350[] dirs = {
                class_2350.field_11033, class_2350.field_11036,
                class_2350.field_11043, class_2350.field_11035,
                class_2350.field_11039, class_2350.field_11034
        };
        for (class_2350 dir : dirs) {
            class_2338 adj = pos.method_10093(dir);
            if (!this.mc.field_1687.method_8320(adj).method_45474()) return adj;
        }
        return null;
    }

    private class_3965 hitAt(class_2338 blockPos, class_2350 face) {
        class_243 hit = new class_243(
                blockPos.method_10263() + 0.5 + face.method_10148() * 0.5,
                blockPos.method_10264() + 0.5 + face.method_10164() * 0.5,
                blockPos.method_10260() + 0.5 + face.method_10165() * 0.5
        );
        return new class_3965(hit, face, blockPos, false);
    }

    private int findItemInHotbar(class_1792 item) {
        for (int i = 0; i < 9; i++) {
            if (this.mc.field_1724.method_31548().method_5438(i).method_7909() == item) return i;
        }
        return -1;
    }

    private int findAnyPlankSlot() {
        for (int i = 0; i < 9; i++) {
            class_1799 s = this.mc.field_1724.method_31548().method_5438(i);
            if (s == null || s.method_7960()) continue;
            String name = s.method_7909().toString().toLowerCase();
            if (name.contains("planks")) return i;
        }
        return -1;
    }

    private void resetDrain() {
        // Restore slot if we still have one saved (avoids stranding user on a
        // bucket after an aborted cycle).
        if (this.drainSavedSlot != -1 && this.mc.field_1724 != null) {
            ((PlayerInventoryAccessor) this.mc.field_1724.method_31548()).setSelectedSlot(this.drainSavedSlot);
        }
        this.drainSavedSlot = -1;
        this.drainState = DrainState.IDLE;
        this.drainTargetId = null;
        this.drainCobwebPos = null;
        this.drainLavaPos = null;
        this.drainAwayLavaPos = null;
        this.drainWaterPos = null;
        this.drainWaitTicks = 0;
        this.drainFailCount = 0;
    }

    private boolean isLookingAtHitbox(class_1657 target) {
        if (this.mc.field_1724 == null) {
            return false;
        } else if (this.mc.field_1765 instanceof class_3966 entityHit && entityHit.method_17782() == target) {
            double requiredInside = class_3532.method_15350(this.hitboxAccuracy.getValue(), 0.0, 1.0);
            if (requiredInside <= 0.0) {
                return true;
            } else {
                class_243 eyePos = this.mc.field_1724.method_33571();
                class_238 box = target.method_5829();
                class_243 center = box.method_1005();
                double dx = center.field_1352 - eyePos.field_1352;
                double dy = center.field_1351 - eyePos.field_1351;
                double dz = center.field_1350 - eyePos.field_1350;
                double horizontalDist = Math.sqrt(dx * dx + dz * dz);
                float targetYaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
                float targetPitch = (float)(-Math.toDegrees(Math.atan2(dy, horizontalDist)));
                double yawDiff = Math.abs(class_3532.method_15393(this.mc.field_1724.method_36454() - targetYaw));
                double pitchDiff = Math.abs(class_3532.method_15393(this.mc.field_1724.method_36455() - targetPitch));
                double safeDist = Math.max(horizontalDist, 0.001);
                double halfWidth = Math.max(box.method_17939(), box.method_17941()) * 0.5;
                double yawHalfSpan = Math.toDegrees(Math.atan2(Math.max(halfWidth, 0.001), safeDist));
                double yawInside = 1.0 - Math.min(1.0, yawDiff / Math.max(yawHalfSpan, 0.001));
                double safeVertical = Math.max(horizontalDist, 0.001);
                double halfHeight = box.method_17940() * 0.5;
                double pitchHalfSpan = Math.toDegrees(Math.atan2(Math.max(halfHeight, 0.001), safeVertical));
                double pitchInside = 1.0 - Math.min(1.0, pitchDiff / Math.max(pitchHalfSpan, 0.001));
                return Math.min(yawInside, pitchInside) >= requiredInside;
            }
        } else {
            return false;
        }
    }
}
