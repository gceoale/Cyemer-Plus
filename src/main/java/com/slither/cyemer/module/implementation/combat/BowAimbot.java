package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.friend.FriendManager;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.RotationManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1753;
import net.minecraft.class_1764;
import net.minecraft.class_1799;
import net.minecraft.class_243;
import net.minecraft.class_3532;

@Environment(EnvType.CLIENT)
public class BowAimbot extends Module {
    private static final double ARROW_GRAVITY = 0.05;
    private static final double ARROW_DRAG = 0.99;
    private static final double ENTITY_GRAVITY = 0.08;
    private static final double ENTITY_DRAG = 0.98;
    private static final int MAX_SIM_TICKS = 200;
    private static final int LEAD_ITERATIONS = 3;

    private final SliderSetting range = new SliderSetting("Range", 60.0, 10.0, 100.0, 0);
    private final SliderSetting fov = new SliderSetting("FOV", 90.0, 10.0, 360.0, 0);
    private final ModeSetting targetPart = new ModeSetting("Aim At", "Auto", "Chest", "Head", "Legs");
    private final SliderSetting autoHeightThreshold = new SliderSetting("Auto Height Diff", 3.0, 0.0, 50.0, 0);
    private final SliderSetting autoDistanceThreshold = new SliderSetting("Auto Distance", 12.0, 0.0, 50.0, 0);
    private final BooleanSetting visibleOnly = new BooleanSetting("Visible Only", true);
    private final BooleanSetting ignoreFriends = new BooleanSetting("Ignore Friends", true);
    private final BooleanSetting predict = new BooleanSetting("Lead Target", true);
    private final BooleanSetting predictGravity = new BooleanSetting("Predict Gravity", true);
    private final ModeSetting rotationMode = new ModeSetting("Rotation", "Linear", "Smooth", "Sine", "FPS", "Instant");
    private final SliderSetting rotationSpeed = new SliderSetting("Rotation Speed", 18.0, 1.0, 30.0, 1);
    private final SliderSetting randomness = new SliderSetting("Randomness", 0.0, 0.0, 1.0, 2);
    private final BooleanSetting silent = new BooleanSetting("Silent", false);
    private final BooleanSetting autoShoot = new BooleanSetting("Auto Shoot", false);
    private final SliderSetting autoShootAngle = new SliderSetting("Shoot Angle", 2.0, 0.5, 10.0, 1);
    private final SliderSetting autoShootMinCharge = new SliderSetting("Min Charge", 0.85, 0.1, 1.0, 2);

    private class_1297 currentTarget;
    private float lockedPitch = 0.0F;
    private float lockedYaw = 0.0F;
    private boolean hasLockedRotation = false;

    public BowAimbot() {
        super("BowAimbot", "Automatically aims bows for you.", Category.COMBAT);
        this.addSetting(this.range);
        this.addSetting(this.fov);
        this.addSetting(this.targetPart);
        this.addSetting(this.autoHeightThreshold);
        this.addSetting(this.autoDistanceThreshold);
        this.addSetting(this.visibleOnly);
        this.addSetting(this.ignoreFriends);
        this.addSetting(this.predict);
        this.addSetting(this.predictGravity);
        this.addSetting(this.rotationMode);
        this.addSetting(this.rotationSpeed);
        this.addSetting(this.randomness);
        this.addSetting(this.silent);
        this.addSetting(this.autoShoot);
        this.addSetting(this.autoShootAngle);
        this.addSetting(this.autoShootMinCharge);
    }

    @Override
    public void onDisable() {
        RotationManager.stop(this);
        this.currentTarget = null;
        this.hasLockedRotation = false;
    }

    @Override
    public void onTick() {
        if (!this.isEnabled() || this.mc.field_1724 == null || this.mc.field_1687 == null) {
            RotationManager.stop(this);
            this.hasLockedRotation = false;
            return;
        }
        class_1799 mainHand = this.mc.field_1724.method_6047();
        boolean isBow = mainHand.method_7909() instanceof class_1753;
        boolean isCrossbow = mainHand.method_7909() instanceof class_1764;
        if (!isBow && !isCrossbow) {
            RotationManager.stop(this);
            this.hasLockedRotation = false;
            return;
        }

        boolean isAimingBow = isBow && this.mc.field_1724.method_6115() && this.mc.field_1724.method_6030() == mainHand;
        boolean isChargedCrossbow = isCrossbow && class_1764.method_7781(mainHand);
        if (!isAimingBow && !isChargedCrossbow) {
            RotationManager.stop(this);
            this.hasLockedRotation = false;
            return;
        }

        this.findTarget();
        if (this.currentTarget == null) {
            RotationManager.stop(this);
            this.hasLockedRotation = false;
            return;
        }

        this.performAiming(isBow);
        if (this.autoShoot.isEnabled()) {
            this.tryAutoShoot(isBow, isChargedCrossbow, mainHand);
        }
    }

    private void performAiming(boolean isBow) {
        RotationManager.RotationMode mode;
        try {
            mode = RotationManager.RotationMode.valueOf(this.rotationMode.getCurrentMode().toUpperCase());
        } catch (Exception e) {
            mode = RotationManager.RotationMode.LINEAR;
        }

        // Live supplier: recomputed every RotationManager update tick so lead
        // tracks the target's current position rather than a snapshot from
        // when performAiming last ran.
        RotationManager.setRotationSupplier(
            this,
            RotationManager.Priority.HIGH,
            this::computeAimPoint,
            this.rotationSpeed.getValue(),
            mode,
            this.randomness.getValue(),
            this.silent.isEnabled(),
            false
        );

        class_243 aimPoint = this.computeAimPoint();
        if (aimPoint != null) {
            float[] rot = RotationManager.calculateRotationsToPos(aimPoint, RotationManager.getFinalYaw());
            this.lockedYaw = rot[0];
            this.lockedPitch = rot[1];
            this.hasLockedRotation = true;
        } else {
            this.hasLockedRotation = false;
        }
    }

    private class_243 computeAimPoint() {
        if (this.currentTarget == null || !this.currentTarget.method_5805() || this.mc.field_1724 == null) return null;
        float velocity = this.getWeaponVelocity();
        class_243 eyePos = this.mc.field_1724.method_33571();
        class_243 leadPos = this.getLeadPosition(this.currentTarget, velocity);
        double aimYOffset = this.getYOffset(this.currentTarget);
        double dx = leadPos.field_1352 - eyePos.field_1352;
        double dz = leadPos.field_1350 - eyePos.field_1350;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        double dy = leadPos.field_1351 + aimYOffset - eyePos.field_1351;
        float pitch = this.solvePitch(velocity, horizontalDist, dy);
        double yOffset = Math.tan(Math.toRadians(-pitch)) * horizontalDist;
        return new class_243(leadPos.field_1352, eyePos.field_1351 + yOffset, leadPos.field_1350);
    }

    private void tryAutoShoot(boolean isBow, boolean isChargedCrossbow, class_1799 mainHand) {
        if (!this.hasLockedRotation) return;
        float charge = this.getCurrentCharge(isBow, mainHand);
        if (isBow && charge < this.autoShootMinCharge.getValue()) return;
        if (!isBow && !isChargedCrossbow) return;

        float currentYaw = RotationManager.getFinalYaw();
        float currentPitch = RotationManager.getFinalPitch();
        float yawDiff = Math.abs(class_3532.method_15393(currentYaw - this.lockedYaw));
        float pitchDiff = Math.abs(class_3532.method_15393(currentPitch - this.lockedPitch));
        double tol = this.autoShootAngle.getValue();
        if (yawDiff > tol || pitchDiff > tol) return;

        if (this.mc.field_1761 != null && isBow) {
            this.mc.field_1761.method_2897(this.mc.field_1724);
        }
        this.mc.field_1690.field_1904.method_23481(false);
    }

    private float getCurrentCharge(boolean isBow, class_1799 stack) {
        if (!isBow || !this.mc.field_1724.method_6115()) return 1.0F;
        int useTicks = this.mc.field_1724.method_6048();
        float charge = useTicks / 20.0F;
        charge = (charge * charge + charge * 2.0F) / 3.0F;
        return Math.min(charge, 1.0F);
    }

    private float getWeaponVelocity() {
        class_1799 stack = this.mc.field_1724.method_6047();
        if (stack.method_7909() instanceof class_1764 && class_1764.method_7781(stack)) {
            return 3.15F;
        }
        if (stack.method_7909() instanceof class_1753 && this.mc.field_1724.method_6115()) {
            int useTicks = this.mc.field_1724.method_6048();
            float charge = useTicks / 20.0F;
            charge = (charge * charge + charge * 2.0F) / 3.0F;
            charge = Math.min(charge, 1.0F);
            return Math.max(charge * 3.0F, 0.1F);
        }
        return 3.0F;
    }

    /**
     * Iterative fixed-point lead: guess flight time from current distance,
     * project target that far along its velocity vector, recompute pitch and
     * flight time, repeat. Converges in 2-3 iterations for realistic targets.
     * If Predict Gravity is on, apply per-tick gravity + drag to Y velocity
     * so falling / bouncing targets stay in the crosshair.
     */
    private class_243 getLeadPosition(class_1297 target, float velocity) {
        class_243 currentPos = this.getInterpolatedPos(target);
        if (!this.predict.isEnabled()) return currentPos;

        class_243 vel = target.method_18798();
        class_243 eye = this.mc.field_1724.method_33571();
        double dx = currentPos.field_1352 - eye.field_1352;
        double dz = currentPos.field_1350 - eye.field_1350;
        double horiz = Math.sqrt(dx * dx + dz * dz);

        double leadX = currentPos.field_1352;
        double leadY = currentPos.field_1351;
        double leadZ = currentPos.field_1350;

        for (int i = 0; i < LEAD_ITERATIONS; i++) {
            int flightTicks = this.estimateFlightTicks(velocity, horiz);
            leadX = currentPos.field_1352 + vel.field_1352 * flightTicks;
            leadZ = currentPos.field_1350 + vel.field_1350 * flightTicks;
            if (this.predictGravity.isEnabled() && !target.method_24828()) {
                double vy = vel.field_1351;
                double y = currentPos.field_1351;
                for (int t = 0; t < flightTicks; t++) {
                    y += vy;
                    vy = (vy - ENTITY_GRAVITY) * ENTITY_DRAG;
                }
                leadY = y;
            } else {
                leadY = currentPos.field_1351;
            }
            double ndx = leadX - eye.field_1352;
            double ndz = leadZ - eye.field_1350;
            horiz = Math.sqrt(ndx * ndx + ndz * ndz);
        }
        return new class_243(leadX, leadY, leadZ);
    }

    private int estimateFlightTicks(float velocity, double horizontalDist) {
        // With drag 0.99, terminal horizontal distance over t ticks:
        //   sum_{i=0}^{t-1} v * 0.99^i * cos(pitch)
        // Approximate with cos(pitch)=1 for shallow shots. Solve geometric
        // series inverse: t ~= log(1 - horiz*(1-drag)/v) / log(drag)
        double v = velocity;
        if (v < 0.1) return MAX_SIM_TICKS;
        double x = 1.0 - horizontalDist * (1.0 - ARROW_DRAG) / v;
        if (x <= 0.0) return MAX_SIM_TICKS;
        int ticks = (int) Math.ceil(Math.log(x) / Math.log(ARROW_DRAG));
        return class_3532.method_15340(ticks, 1, MAX_SIM_TICKS);
    }

    private class_243 getInterpolatedPos(class_1297 target) {
        double t = this.mc.method_61966().method_60637(false);
        double x = class_3532.method_16436(t, target.field_6038, target.method_23317());
        double y = class_3532.method_16436(t, target.field_5971, target.method_23318());
        double z = class_3532.method_16436(t, target.field_5989, target.method_23321());
        return new class_243(x, y, z);
    }

    /**
     * Solves for the low-arc pitch that lands an arrow at dy at horizontal dx.
     * Splits the search at 45° because hitY(pitch) is parabolic: increasing
     * for pitch in [-89, 45], then decreasing. The old code bisected across
     * the full range and got the wrong side of the maximum for far targets.
     */
    private float solvePitch(float velocity, double horizontalDist, double targetDy) {
        // Try low arc first (fast arrival, less time for target to dodge).
        BisectResult low = this.bisectPitch(velocity, horizontalDist, targetDy, -89.0F, 45.0F, true);
        // If low arc missed by more than 0.5 blocks, try high arc and pick better.
        if (low.error > 0.5) {
            BisectResult high = this.bisectPitch(velocity, horizontalDist, targetDy, 45.0F, 89.0F, false);
            if (high.error < low.error) return high.pitch;
        }
        return low.pitch;
    }

    private BisectResult bisectPitch(float velocity, double horizontalDist, double targetDy, float lo, float hi, boolean risingArc) {
        float best = (lo + hi) / 2.0F;
        double bestErr = Double.MAX_VALUE;
        for (int i = 0; i < 30; i++) {
            float mid = (lo + hi) / 2.0F;
            double hitY = this.simulateArrow(velocity, mid, horizontalDist);
            if (Double.isInfinite(hitY)) {
                // Arrow can't reach horizontalDist within MAX_SIM_TICKS.
                // Low arc: need more pitch to add reach via arc; high arc: less pitch.
                if (risingArc) lo = mid;
                else hi = mid;
                continue;
            }
            double err = hitY - targetDy;
            if (Math.abs(err) < bestErr) {
                bestErr = Math.abs(err);
                best = mid;
            }
            if (Math.abs(err) < 0.02) return new BisectResult(mid, Math.abs(err));
            // Rising arc: hitY grows with pitch; falling arc: shrinks with pitch.
            boolean tooLow = err < 0;
            if (risingArc == tooLow) lo = mid;
            else hi = mid;
        }
        return new BisectResult(best, bestErr);
    }

    private record BisectResult(float pitch, double error) {}

    /**
     * Simulate arrow trajectory. Returns Y at the moment X crosses target dist,
     * or NEGATIVE_INFINITY if the arrow never reaches the target within MAX_SIM_TICKS.
     */
    private double simulateArrow(float speed, float pitch, double targetDist) {
        double radPitch = Math.toRadians(pitch);
        double vx = speed * Math.cos(radPitch);
        double vy = speed * Math.sin(radPitch);
        double x = 0.0;
        double y = 0.0;
        for (int tick = 0; tick < MAX_SIM_TICKS; tick++) {
            double prevX = x;
            double prevY = y;
            x += vx;
            y += vy;
            if (x >= targetDist) {
                double delta = x - prevX;
                if (Math.abs(delta) < 1.0E-6) return y;
                double ratio = (targetDist - prevX) / delta;
                return prevY + (y - prevY) * ratio;
            }
            vx *= ARROW_DRAG;
            vy *= ARROW_DRAG;
            vy -= ARROW_GRAVITY;
        }
        return Double.NEGATIVE_INFINITY;
    }

    private void findTarget() {
        class_1297 bestEntity = null;
        double bestDistance = this.range.getValue();
        double maxAngle = this.fov.getValue() / 2.0;

        for (class_1297 entity : this.mc.field_1687.method_18112()) {
            if (!this.isValidTarget(entity)) continue;
            double distance = this.mc.field_1724.method_5739(entity);
            if (distance > this.range.getValue()) continue;
            if (this.visibleOnly.isEnabled() && !this.mc.field_1724.method_6057(entity)) continue;
            double angle = this.getAngleToEntity(entity);
            if (angle > maxAngle) continue;
            if (distance < bestDistance) {
                bestDistance = distance;
                bestEntity = entity;
            }
        }
        this.currentTarget = bestEntity;
    }

    private boolean isValidTarget(class_1297 entity) {
        if (!(entity instanceof class_1309) || entity == this.mc.field_1724 || !entity.method_5805()) return false;
        if (!this.ignoreFriends.isEnabled()) return true;
        if (entity instanceof class_1657 player) {
            return !FriendManager.getInstance().isFriend(player.method_5667());
        }
        return true;
    }

    private double getYOffset(class_1297 target) {
        String part = this.targetPart.getCurrentMode();
        if (part.equals("Auto")) {
            part = this.determinePart(target);
        }
        double height = target.method_17682();
        return switch (part) {
            case "Head" -> height * 0.85;
            case "Legs" -> height * 0.1;
            default -> height * 0.5;
        };
    }

    /**
     * Auto part selection based on relative position:
     *   - Target far below us: aim Head (arrow drop already handles it, head hitbox is highest)
     *   - Target far above us: aim Legs (arrow apex tends to overshoot, legs = wider tolerance)
     *   - Target close: aim Head (fast + clean shot)
     *   - Target moving fast or at mid range: aim Chest (biggest hitbox, most forgiving)
     */
    private String determinePart(class_1297 target) {
        if (this.mc.field_1724 == null) return "Chest";
        double heightDiff = target.method_23318() - this.mc.field_1724.method_23318();
        double distance = this.mc.field_1724.method_5739(target);
        double hThresh = this.autoHeightThreshold.getValue();
        double dThresh = this.autoDistanceThreshold.getValue();
        if (heightDiff < -hThresh) return "Head";
        if (heightDiff > hThresh) return "Legs";
        if (distance < dThresh) return "Head";
        return "Chest";
    }

    private double getAngleToEntity(class_1297 entity) {
        class_243 playerPos = this.mc.field_1724.method_33571();
        class_243 entityPos = entity.method_5829().method_1005();
        double deltaX = entityPos.field_1352 - playerPos.field_1352;
        double deltaZ = entityPos.field_1350 - playerPos.field_1350;
        float targetYaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
        return Math.abs(class_3532.method_15393(this.mc.field_1724.method_36454() - targetYaw));
    }
}
