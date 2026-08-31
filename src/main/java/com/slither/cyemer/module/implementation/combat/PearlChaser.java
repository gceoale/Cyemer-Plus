package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.friend.FriendManager;
import com.slither.cyemer.mixin.KeyBindingAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.RotationManager;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1684;
import net.minecraft.class_1776;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_3532;

/**
 * Answers an opponent's escape pearl by throwing one that lands on top of theirs.
 *
 * When a foreign ender pearl appears we run its flight forward under vanilla
 * thrown-entity physics to find where it will touch down, then solve the launch
 * angle that drops our own pearl through that same point. Only fires while we
 * are ahead on effective health, since chasing someone down while losing the
 * fight just delivers us to them.
 */
@Environment(EnvType.CLIENT)
public class PearlChaser extends Module {
    /** class_1682.method_7490 - thrown-entity gravity, applied after drag each tick. */
    private static final double PEARL_GRAVITY = 0.03;
    /** class_1682 tick - velocity multiplier in air. */
    private static final double PEARL_DRAG = 0.99;
    /** Fallback if the live launch power ever reads as unset. */
    private static final float FALLBACK_POWER = 1.5F;
    private static final int MAX_SIM_TICKS = 300;
    private static final int SETTLE_TIMEOUT_TICKS = 30;

    private final SliderSetting range = new SliderSetting("Range", 48.0, 8.0, 96.0, 0);
    private final SliderSetting hpMargin = new SliderSetting("HP Margin", 0.0, 0.0, 20.0, 1);
    private final SliderSetting reactionMs = new SliderSetting("Reaction (ms)", 220.0, 0.0, 1000.0, 0);
    private final SliderSetting aimSpeed = new SliderSetting("Aim Speed", 18.0, 1.0, 35.0, 1);
    private final SliderSetting aimTolerance = new SliderSetting("Aim Tolerance", 1.5, 0.1, 10.0, 1);
    private final SliderSetting maxError = new SliderSetting("Max Error", 3.0, 0.5, 15.0, 1);
    private final BooleanSetting silent = new BooleanSetting("Silent Aim", false);
    private final BooleanSetting ignoreFriends = new BooleanSetting("Ignore Friends", true);
    private final BooleanSetting swapBack = new BooleanSetting("Swap Back", true);

    private enum Phase { IDLE, WAITING, AIMING, THROWING, RECOVER }

    private Phase phase = Phase.IDLE;
    private final Set<UUID> handled = new HashSet<>();
    private class_243 chaseTarget = null;
    private long reactAtMs = 0L;
    private int originalSlot = -1;
    private int phaseTicks = 0;
    private float solvedYaw = 0.0F;
    private float solvedPitch = 0.0F;

    public PearlChaser() {
        super("PearlChaser", "Throws a pearl to land where an enemy's escape pearl will.", Category.COMBAT);
        this.addSetting(this.range);
        this.addSetting(this.hpMargin);
        this.addSetting(this.reactionMs);
        this.addSetting(this.aimSpeed);
        this.addSetting(this.aimTolerance);
        this.addSetting(this.maxError);
        this.addSetting(this.silent);
        this.addSetting(this.ignoreFriends);
        this.addSetting(this.swapBack);
    }

    @Override
    public void onDisable() {
        this.abort();
        this.handled.clear();
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 == null || this.mc.field_1687 == null) {
            this.abort();
            return;
        }

        if (this.phase != Phase.IDLE) {
            this.tickChase();
            return;
        }

        // Forget pearls that have despawned so the set cannot grow without bound.
        this.handled.removeIf(id -> this.findPearl(id) == null);

        class_1684 pearl = this.findChasablePearl();
        if (pearl == null) {
            return;
        }

        class_243 landing = this.predictLanding(pearl);
        if (landing == null) {
            this.handled.add(pearl.method_5667());
            return;
        }

        this.handled.add(pearl.method_5667());
        this.chaseTarget = landing;
        this.reactAtMs = System.currentTimeMillis() + (long) this.reactionMs.getValue();
        this.phase = Phase.WAITING;
        this.phaseTicks = 0;
    }

    // ------------------------------------------------------------------ chase

    private void tickChase() {
        this.phaseTicks++;

        switch (this.phase) {
            case WAITING -> {
                if (System.currentTimeMillis() < this.reactAtMs) {
                    return;
                }
                if (!this.beginAim()) {
                    this.abort();
                }
            }
            case AIMING -> {
                float yawGap = Math.abs(class_3532.method_15393(RotationManager.getFinalYaw() - this.solvedYaw));
                float pitchGap = Math.abs(RotationManager.getFinalPitch() - this.solvedPitch);
                double tol = this.aimTolerance.getValue();
                if (yawGap <= tol && pitchGap <= tol) {
                    this.phase = Phase.THROWING;
                    this.phaseTicks = 0;
                } else if (this.phaseTicks > SETTLE_TIMEOUT_TICKS) {
                    // Rotation never converged - drop it rather than hurl a pearl
                    // somewhere arbitrary.
                    this.abort();
                }
            }
            case THROWING -> {
                if (this.mc.field_1724.method_6047().method_7909() != class_1802.field_8634) {
                    this.abort();
                    return;
                }
                this.mc.field_1690.field_1904.method_23481(true);
                if (this.mc.field_1690.field_1904 instanceof KeyBindingAccessor accessor) {
                    accessor.setTimesPressed(accessor.getTimesPressed() + 1);
                }
                this.phase = Phase.RECOVER;
                this.phaseTicks = 0;
            }
            case RECOVER -> {
                if (this.phaseTicks >= 2) {
                    this.abort();
                }
            }
            default -> this.abort();
        }
    }

    private boolean beginAim() {
        int pearlSlot = this.findPearlSlot();
        if (pearlSlot == -1 || this.chaseTarget == null) {
            return false;
        }

        class_243 aimPoint = this.solveAimPoint(this.chaseTarget);
        if (aimPoint == null) {
            return false;
        }

        float[] rot = RotationManager.calculateRotationsToPos(aimPoint, RotationManager.getFinalYaw());
        this.solvedYaw = rot[0];
        this.solvedPitch = rot[1];

        this.originalSlot = this.mc.field_1724.method_31548().method_67532();
        this.mc.field_1724.method_31548().method_61496(pearlSlot);

        RotationManager.setRotationSupplier(
                this,
                RotationManager.Priority.HIGH,
                () -> aimPoint,
                this.aimSpeed.getValue(),
                RotationManager.RotationMode.LINEAR,
                0.0,
                this.silent.isEnabled(),
                false
        );

        this.phase = Phase.AIMING;
        this.phaseTicks = 0;
        return true;
    }

    private void abort() {
        if (this.mc.field_1690 != null) {
            this.mc.field_1690.field_1904.method_23481(false);
        }
        if (this.swapBack.isEnabled() && this.originalSlot != -1 && this.mc.field_1724 != null) {
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
        }
        RotationManager.stop(this);
        this.originalSlot = -1;
        this.chaseTarget = null;
        this.phase = Phase.IDLE;
        this.phaseTicks = 0;
    }

    // --------------------------------------------------------------- targeting

    /**
     * A pearl is worth chasing when someone else threw it, we can see who, and
     * we are the healthier of the two. An ownerless pearl is skipped rather than
     * guessed at - without an owner there is no health to compare against.
     */
    private class_1684 findChasablePearl() {
        class_1657 self = this.mc.field_1724;
        double maxRange = this.range.getValue();
        class_1684 best = null;
        double bestDist = Double.MAX_VALUE;

        for (class_1297 entity : this.mc.field_1687.method_18112()) {
            if (!(entity instanceof class_1684 pearl) || !pearl.method_5805()) {
                continue;
            }
            if (this.handled.contains(pearl.method_5667())) {
                continue;
            }

            class_1297 owner = pearl.method_24921();
            if (owner == null || owner == self) {
                continue;
            }
            if (!(owner instanceof class_1309 thrower)) {
                continue;
            }
            if (this.ignoreFriends.isEnabled() && owner instanceof class_1657 p
                    && FriendManager.getInstance().isFriend(p.method_5667())) {
                continue;
            }
            if (!this.healthierThan(thrower)) {
                continue;
            }

            double dist = self.method_5739(pearl);
            if (dist > maxRange || dist >= bestDist) {
                continue;
            }
            bestDist = dist;
            best = pearl;
        }
        return best;
    }

    private class_1684 findPearl(UUID id) {
        for (class_1297 entity : this.mc.field_1687.method_18112()) {
            if (entity instanceof class_1684 pearl && pearl.method_5667().equals(id)) {
                return pearl;
            }
        }
        return null;
    }

    private boolean healthierThan(class_1309 other) {
        class_1657 self = this.mc.field_1724;
        double mine = self.method_6032() + self.method_6067();
        double theirs = other.method_6032() + other.method_6067();
        return mine > theirs + this.hpMargin.getValue();
    }

    // ---------------------------------------------------------------- physics

    /**
     * Runs a live pearl forward from its current position and velocity until it
     * meets a block. Returns null if it is still airborne past the sim cap, since
     * a landing spot we never found is not one worth throwing at.
     */
    private class_243 predictLanding(class_1684 pearl) {
        double x = pearl.method_23317();
        double y = pearl.method_23318();
        double z = pearl.method_23321();
        class_243 vel = pearl.method_18798();
        double vx = vel.field_1352;
        double vy = vel.field_1351;
        double vz = vel.field_1350;

        for (int t = 0; t < MAX_SIM_TICKS; t++) {
            double px = x;
            double py = y;
            double pz = z;
            x += vx;
            y += vy;
            z += vz;

            if (this.isSolid(x, y, z)) {
                return new class_243(px, py, pz);
            }
            if (y < this.mc.field_1687.method_31607() - 8) {
                return null;
            }

            vx *= PEARL_DRAG;
            vy *= PEARL_DRAG;
            vz *= PEARL_DRAG;
            vy -= PEARL_GRAVITY;
        }
        return null;
    }

    /**
     * Turns a landing spot into a point to look at. Solves the launch pitch that
     * carries a pearl the required horizontal distance and drop, then re-expresses
     * that angle as an aim position so RotationManager can drive to it.
     */
    private class_243 solveAimPoint(class_243 target) {
        class_243 eye = this.mc.field_1724.method_33571();
        double dx = target.field_1352 - eye.field_1352;
        double dz = target.field_1350 - eye.field_1350;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double dy = target.field_1351 - eye.field_1351;
        if (horizontal < 0.01) {
            return null;
        }

        float power = class_1776.field_55033 > 0.0F ? class_1776.field_55033 : FALLBACK_POWER;
        Float pitch = this.solvePitch(power, horizontal, dy);
        if (pitch == null) {
            return null;
        }

        double yOffset = Math.tan(Math.toRadians(-pitch)) * horizontal;
        return new class_243(target.field_1352, eye.field_1351 + yOffset, target.field_1350);
    }

    /**
     * Two pitches reach any given point: a flat fast one and a lofted slow one.
     * We always want the flat one - it spends the least time in the air, which is
     * the whole point when chasing someone who is already gone.
     *
     * Height-at-distance is unimodal in pitch, peaking at the max-range angle, so
     * each side of that peak is monotonic and safe to bisect. In Minecraft's pitch
     * convention negative is upward, which puts the flat solutions above the apex
     * angle and the lofted ones below it. Only if no flat arc can cover the
     * distance do we fall back to lofting it. Null when neither lands within
     * Max Error - past roughly 55 blocks a pearl cannot reach at all.
     */
    private Float solvePitch(float power, double horizontal, double dy) {
        float apex = this.findApexPitch(power, horizontal);
        Float flat = this.bisect(power, horizontal, dy, apex, 89.0F, false);
        if (flat != null) {
            return flat;
        }
        return this.bisect(power, horizontal, dy, -89.0F, apex, true);
    }

    /**
     * Ternary-searches the pitch where height-at-distance peaks. The peak drifts
     * with range - near-vertical up close, shallow far out - so a fixed split
     * would leave one branch straddling the apex and no longer monotonic, which
     * is what let the solver settle on near-vertical lobs. Infinities compare
     * below any real height, so unreachable pitches push the search away on
     * their own.
     */
    private float findApexPitch(float power, double horizontal) {
        float lo = -89.0F;
        float hi = 89.0F;
        for (int i = 0; i < 60; i++) {
            float m1 = lo + (hi - lo) / 3.0F;
            float m2 = hi - (hi - lo) / 3.0F;
            if (this.simulateThrow(power, m1, horizontal) < this.simulateThrow(power, m2, horizontal)) {
                lo = m1;
            } else {
                hi = m2;
            }
        }
        return (lo + hi) / 2.0F;
    }

    /**
     * @param increasing whether height-at-distance rises as pitch rises across [lo, hi]
     */
    private Float bisect(float power, double horizontal, double dy, float lo, float hi, boolean increasing) {
        float best = (lo + hi) / 2.0F;
        double bestErr = Double.MAX_VALUE;

        for (int i = 0; i < 40; i++) {
            float mid = (lo + hi) / 2.0F;
            double hitY = this.simulateThrow(power, mid, horizontal);
            if (Double.isInfinite(hitY)) {
                // Never covered the distance. The unreachable end of each branch is
                // the one furthest from the apex, so step away from it.
                if (increasing) {
                    lo = mid;
                } else {
                    hi = mid;
                }
                continue;
            }

            double err = hitY - dy;
            if (Math.abs(err) < bestErr) {
                bestErr = Math.abs(err);
                best = mid;
            }
            if (Math.abs(err) < 0.05) {
                return mid;
            }

            boolean tooLow = err < 0;
            if (increasing == tooLow) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return bestErr <= this.maxError.getValue() ? best : null;
    }

    /** Height of a pearl thrown at this pitch by the time it has covered targetDist. */
    private double simulateThrow(float power, float pitch, double targetDist) {
        double rad = Math.toRadians(pitch);
        double vx = Math.cos(rad) * power;
        double vy = -Math.sin(rad) * power;
        double x = 0.0;
        double y = 0.0;

        for (int t = 0; t < MAX_SIM_TICKS; t++) {
            double prevX = x;
            double prevY = y;
            x += vx;
            y += vy;
            if (x >= targetDist) {
                double span = x - prevX;
                if (Math.abs(span) < 1.0E-6) {
                    return y;
                }
                double ratio = (targetDist - prevX) / span;
                return prevY + (y - prevY) * ratio;
            }
            vx *= PEARL_DRAG;
            vy *= PEARL_DRAG;
            vy -= PEARL_GRAVITY;
        }
        return Double.NEGATIVE_INFINITY;
    }

    private boolean isSolid(double x, double y, double z) {
        class_2338 pos = class_2338.method_49637(x, y, z);
        return !this.mc.field_1687.method_8320(pos).method_45474();
    }

    private int findPearlSlot() {
        for (int i = 0; i < 9; i++) {
            class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
            if (stack.method_7909() == class_1802.field_8634) {
                return i;
            }
        }
        return -1;
    }
}
