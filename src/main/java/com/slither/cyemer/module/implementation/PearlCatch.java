package com.slither.cyemer.module.implementation;

import com.slither.cyemer.mixin.MinecraftClientAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.RotationManager;
import com.slither.cyemer.util.render.RenderUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1684;
import net.minecraft.class_1776;
import net.minecraft.class_1792;
import net.minecraft.class_1802;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_265;
import net.minecraft.class_3486;
import net.minecraft.class_3532;
import net.minecraft.class_3610;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_746;
import net.minecraft.class_9239;
import net.minecraft.class_9799;
import net.minecraft.class_4597.class_4598;

/**
 * Throws a pearl, then a wind charge timed so its burst shoves the pearl farther.
 *
 * A wind charge can't touch a pearl (pearls aren't in #redirectable_projectile), it only bursts on a
 * block or a mob. So instead of aiming at the pearl we look for a block the pearl flies close to, right
 * after the throw near our feet if possible, otherwise anywhere along the path, and land the wind charge
 * on it the tick the pearl goes past. Both throws are ours and lag the same, so ticks since the pearl
 * throw line the two up exactly without guessing ping.
 */
@Environment(EnvType.CLIENT)
public class PearlCatch extends Module {
    private final SliderSetting rotationStrength = new SliderSetting("Rotation Speed", 15.0, 1.0, 20.0, 1);
    private final ModeSetting rotPattern = new ModeSetting("Pattern", "Sine", "Smooth", "Linear", "Instant");
    private final SliderSetting rotRandom = new SliderSetting("Randomness", 0.0, 0.0, 1.0, 2);
    private final BooleanSetting silentRotation = new BooleanSetting("Silent Aim", true);
    private final SliderSetting minGain = new SliderSetting("Min Gain", 2.0, 0.5, 15.0, 1);
    private final BooleanSetting showTrajectory = new BooleanSetting("Show Trajectory", true);
    // class_1682.method_7490
    private static final double PEARL_GRAVITY = 0.03;
    // class_1682.method_63673 multiplies by these floats, keep the float widening so it matches bit for bit
    private static final double PEARL_DRAG = 0.99F;
    private static final double PEARL_WATER_DRAG = 0.8F;
    // class_3857 owner ctor spawns the pearl at eyeY - 0.1F
    private static final double PEARL_SPAWN_DROP = 0.1F;
    private static final double PEARL_HALF_WIDTH = 0.125;
    private static final double PEARL_HEIGHT = 0.25;
    // 0.25 tall entity, default eye height is 0.85 of that. Explosions push toward the eye
    private static final double PEARL_EYE = 0.25F * 0.85F;
    // class_8956: power 1.2F, class_9892.method_61741 reaches power * 2 and scales by the 1.22F knockback modifier
    private static final double BURST_RADIUS = 1.2F * 2.0F;
    private static final double BURST_KNOCKBACK = 1.22F;
    // class_9236.method_24920 bursts a quarter block off the face it hit
    private static final double BURST_FACE_OFFSET = 0.25;
    private static final float FALLBACK_POWER = 1.5F;
    private static final int MAX_PATH_TICKS = 400;
    private static final int MAX_WIND_TICKS = 40;
    private static final int PLAN_WINDOW = 24;
    private static final int MAX_CASTS = 500;
    private static final int GIVE_UP_TICKS = 60;
    private PearlCatch.State state = PearlCatch.State.IDLE;
    private int originalSlot = -1;
    private int pearlThrowTick = 0;
    private int stateTicks = 0;
    private final Set<UUID> ignoredPearls = new HashSet<>();
    private UUID trackedPearl = null;
    // path index 0 is the pearl's state this many ticks after it spawned
    private int pathBase = 0;
    private final List<class_243> pathPos = new ArrayList<>();
    private final List<class_243> pathVel = new ArrayList<>();
    private final List<Boolean> pathWater = new ArrayList<>();
    private boolean pathLanded = false;
    private PearlCatch.Plan plan = null;

    public PearlCatch() {
        super("PearlCatch", "BETA ASF.", Category.MOVEMENT);
        this.addSetting(this.rotationStrength);
        this.addSetting(this.rotPattern);
        this.addSetting(this.rotRandom);
        this.addSetting(this.silentRotation);
        this.addSetting(this.minGain);
        this.addSetting(this.showTrajectory);
    }

    @Override
    public void onEnable() {
        if (this.mc.field_1724 == null || this.mc.field_1687 == null) {
            this.toggle();
        } else if (this.hasItem(class_1802.field_8634) && this.hasItem(class_1802.field_49098)) {
            this.state = PearlCatch.State.THROWING_PEARL;
            this.originalSlot = this.mc.field_1724.method_31548().method_67532();
            this.stateTicks = 0;
            this.trackedPearl = null;
            this.pathBase = 0;
            this.pathPos.clear();
            this.pathVel.clear();
            this.pathWater.clear();
            this.pathLanded = false;
            this.plan = null;
            // our pearls already in the air aren't the one we're about to throw
            this.ignoredPearls.clear();
            for (class_1297 entity : this.mc.field_1687.method_18112()) {
                if (entity instanceof class_1684 pearl && pearl.method_24921() == this.mc.field_1724) {
                    this.ignoredPearls.add(pearl.method_5667());
                }
            }
        } else {
            this.toggle();
        }
    }

    @Override
    public void onDisable() {
        if (this.mc.field_1724 != null && this.originalSlot != -1) {
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
        }

        if (this.mc.field_1690 != null) {
            this.mc.field_1690.field_1904.method_23481(false);
        }

        RotationManager.stop(this);
        this.state = PearlCatch.State.IDLE;
        this.originalSlot = -1;
        this.trackedPearl = null;
        this.plan = null;
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            switch (this.state) {
                case THROWING_PEARL:
                    this.handlePearlThrow();
                    break;
                case TRACKING:
                    this.handleTracking();
                    break;
                case EXECUTING_CATCH:
                    this.handleExecution();
                    break;
                case RESETTING:
                    this.handleReset();
                    break;
                default:
                    break;
            }
        }
    }

    private void handlePearlThrow() {
        int pearlSlot = this.findItemSlot(class_1802.field_8634);
        if (pearlSlot == -1) {
            this.toggle();
            return;
        }

        this.mc.field_1724.method_31548().method_61496(pearlSlot);
        this.predictThrow();
        // straight through doItemUse so it goes out this tick whatever the client's use cooldown says,
        // the tick count since this throw is what lines the wind charge up with the pearl
        ((MinecraftClientAccessor)this.mc).useItem();
        this.pearlThrowTick = this.mc.field_1724.field_6012;
        this.state = PearlCatch.State.TRACKING;
        this.stateTicks = 0;
    }

    private void handleTracking() {
        if (!this.refreshPath()) {
            this.finishCatch();
            return;
        }

        class_746 player = this.mc.field_1724;
        // the wind charge goes out next tick, from where we'll be standing by then
        class_243 velocity = player.method_18798();
        class_243 nextPos = player.method_73189().method_1019(velocity);
        class_243 windStart = nextPos.method_1031(0.0, player.method_5751(), 0.0);
        class_243 inherited = new class_243(velocity.field_1352, player.method_24828() ? 0.0 : velocity.field_1351, velocity.field_1350);
        this.plan = this.solvePlan(1, windStart, inherited);
        if (this.plan == null) {
            RotationManager.clearTarget(this);
            return;
        }

        class_243 fallbackAim = this.plan.aimPoint();
        RotationManager.setRotationSupplier(
            this,
            RotationManager.Priority.HIGHEST,
            () -> this.plan != null ? this.plan.aimPoint() : fallbackAim,
            this.rotationStrength.getValue(),
            this.getRotationMode(),
            this.rotRandom.getValue(),
            this.silentRotation.isEnabled(),
            false
        );
        if (RotationManager.isRotationComplete(1.5F) && this.findItemSlot(class_1802.field_49098) != -1) {
            this.state = PearlCatch.State.EXECUTING_CATCH;
        }
    }

    private void handleExecution() {
        if (!this.refreshPath()) {
            this.finishCatch();
            return;
        }

        int windSlot = this.findItemSlot(class_1802.field_49098);
        if (windSlot == -1) {
            this.finishCatch();
            return;
        }

        // last look using the rotation we're actually about to send, the plan was made for a guess of it
        PearlCatch.Plan check = this.checkThrow();
        if (check == null || check.gain() < this.minGain.getValue() * 0.75) {
            this.state = PearlCatch.State.TRACKING;
            return;
        }

        this.plan = check;
        this.mc.field_1724.method_31548().method_61496(windSlot);
        ((MinecraftClientAccessor)this.mc).useItem();
        RotationManager.clearTarget(this);
        this.state = PearlCatch.State.RESETTING;
        this.stateTicks = 0;
    }

    private void handleReset() {
        this.stateTicks++;
        if (!RotationManager.isActive() || this.stateTicks > 10) {
            RotationManager.stop(this);
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
            this.toggle();
        }
    }

    private void finishCatch() {
        RotationManager.clearTarget(this);
        this.state = PearlCatch.State.RESETTING;
        this.stateTicks = 0;
    }

    private RotationManager.RotationMode getRotationMode() {
        try {
            return RotationManager.RotationMode.valueOf(this.rotPattern.getCurrentMode().toUpperCase());
        } catch (Exception var2) {
            return RotationManager.RotationMode.SINE;
        }
    }

    private int ticksSinceThrow() {
        return this.mc.field_1724.field_6012 - this.pearlThrowTick;
    }

    /**
     * Rebuilds the path from our real pearl once its spawn packet is in, it already carries the server's
     * random spread. Until then the throw-time prediction stands in. False once the pearl is gone or if
     * it never showed up.
     */
    private boolean refreshPath() {
        if (this.ticksSinceThrow() > GIVE_UP_TICKS) {
            return false;
        }

        class_1684 pearl = this.findOurPearl();
        if (pearl != null) {
            this.trackedPearl = pearl.method_5667();
            this.pathBase = pearl.field_6012;
            this.pathLanded = this.simulate(
                pearl.method_73189(), pearl.method_18798(), pearl.method_5799(), pearl, this.pathPos, this.pathVel, this.pathWater
            );
            return true;
        }

        return this.trackedPearl == null && !this.pathPos.isEmpty();
    }

    /**
     * The pearl we're about to throw, built the way the server builds it: class_3857 spawns it at
     * eyeY - 0.1F, class_1676.method_24919 aims it from yaw/pitch through the MathHelper tables and adds
     * our last tick of movement (no vertical part on the ground). The server's random spread can't be
     * known ahead of time, it's tiny for the first few ticks which is all a launch boost needs.
     */
    private void predictThrow() {
        class_746 player = this.mc.field_1724;
        class_243 velocity = this.lookVector(player.method_36454(), player.method_36455())
            .method_1021(this.power(class_1776.field_55033))
            .method_1019(this.lastMovement(player));
        class_243 start = new class_243(player.method_23317(), player.method_23320() - PEARL_SPAWN_DROP, player.method_23321());
        this.pathBase = 0;
        this.pathLanded = this.simulate(start, velocity, false, player, this.pathPos, this.pathVel, this.pathWater);
    }

    /** class_1676.method_24919 without the random spread. */
    private class_243 lookVector(float yawDeg, float pitchDeg) {
        float yaw = class_3532.method_15393(yawDeg);
        float x = -class_3532.method_15374(yaw * 0.017453292F) * class_3532.method_15362(pitchDeg * 0.017453292F);
        float y = -class_3532.method_15374(pitchDeg * 0.017453292F);
        float z = class_3532.method_15362(yaw * 0.017453292F) * class_3532.method_15362(pitchDeg * 0.017453292F);
        return new class_243(x, y, z).method_1029();
    }

    /** What the server adds to a throw: our last tick of movement, minus the vertical part on the ground. */
    private class_243 lastMovement(class_746 player) {
        return new class_243(
            player.method_23317() - player.field_6014,
            player.method_24828() ? 0.0 : player.method_23318() - player.field_6036,
            player.method_23321() - player.field_5969
        );
    }

    /**
     * class_1682.method_5773 one tick at a time: gravity, then drag (0.8F in water, 0.99F in air, from the
     * water state at the end of the previous tick), then a COLLIDER raycast from where the pearl is to where
     * it's going. A block in the way ends the flight on the hit point, which is where the pearl lands you.
     * Index i holds the pearl's position, velocity and water state after i ticks. Returns true if it lands.
     */
    private boolean simulate(
        class_243 start,
        class_243 velocity,
        boolean inWater,
        class_1297 shapeContext,
        List<class_243> outPos,
        List<class_243> outVel,
        List<Boolean> outWater
    ) {
        outPos.clear();
        outVel.clear();
        outWater.clear();
        outPos.add(start);
        outVel.add(velocity);
        outWater.add(inWater);
        double x = start.field_1352;
        double y = start.field_1351;
        double z = start.field_1350;
        double vx = velocity.field_1352;
        double vy = velocity.field_1351;
        double vz = velocity.field_1350;
        boolean water = inWater;
        int floor = this.mc.field_1687.method_31607() - 64;

        for (int t = 0; t < MAX_PATH_TICKS; t++) {
            vy -= PEARL_GRAVITY;
            double drag = water ? PEARL_WATER_DRAG : PEARL_DRAG;
            vx *= drag;
            vy *= drag;
            vz *= drag;
            class_243 from = new class_243(x, y, z);
            class_243 to = new class_243(x + vx, y + vy, z + vz);
            class_3965 hit = this.raycast(from, to, shapeContext);
            if (hit.method_17783() != class_239.class_240.field_1333) {
                outPos.add(hit.method_17784());
                outVel.add(new class_243(vx, vy, vz));
                outWater.add(water);
                return true;
            }

            x = to.field_1352;
            y = to.field_1351;
            z = to.field_1350;
            water = this.touchingWater(x, y, z);
            outPos.add(to);
            outVel.add(new class_243(vx, vy, vz));
            outWater.add(water);
            if (y < floor) {
                return false;
            }
        }

        return false;
    }

    /**
     * Best wind charge for a throw throwDelay ticks from now. Looks at blocks within burst reach of where the
     * pearl will be, aims to land the wind charge on each one, works out which pearl tick the burst really
     * happens on, and keeps the one that pushes the landing spot out the furthest.
     */
    private PearlCatch.Plan solvePlan(int throwDelay, class_243 windStart, class_243 inherited) {
        int size = this.pathPos.size();
        if (size < 3) {
            return null;
        }

        // wind charge tick w meets pearl path index offset + w
        int offset = this.ticksSinceThrow() + throwDelay - this.pathBase;
        int landIdx = this.pathLanded ? size - 1 : size;
        double speed = this.power(class_9239.field_55047);
        class_243 origin = this.mc.field_1724.method_73189();
        double baseDist = this.horizontalDistance(this.pathPos.get(size - 1), origin);
        double reachSq = BURST_RADIUS * BURST_RADIUS;
        List<PearlCatch.Burst> bursts = new ArrayList<>();
        int casts = 0;
        int end = Math.min(landIdx, offset + 1 + PLAN_WINDOW);

        // every tick near the launch where boosts are strongest, every other tick after that
        for (int j = Math.max(offset + 1, 1); j < end && casts < MAX_CASTS; j += j - offset <= 8 ? 1 : 2) {
            class_243 pearl = this.pathPos.get(j);
            int minX = class_3532.method_15357(pearl.field_1352 - BURST_RADIUS);
            int maxX = class_3532.method_15357(pearl.field_1352 + BURST_RADIUS);
            int minY = class_3532.method_15357(pearl.field_1351 - BURST_RADIUS);
            int maxY = class_3532.method_15357(pearl.field_1351 + BURST_RADIUS);
            int minZ = class_3532.method_15357(pearl.field_1350 - BURST_RADIUS);
            int maxZ = class_3532.method_15357(pearl.field_1350 + BURST_RADIUS);

            for (int bx = minX; bx <= maxX && casts < MAX_CASTS; bx++) {
                for (int by = minY; by <= maxY && casts < MAX_CASTS; by++) {
                    for (int bz = minZ; bz <= maxZ && casts < MAX_CASTS; bz++) {
                        class_2338 pos = new class_2338(bx, by, bz);
                        class_265 shape = this.mc.field_1687.method_8320(pos).method_26220(this.mc.field_1687, pos);
                        if (shape.method_1110()) {
                            continue;
                        }

                        // closest point of this block to the pearl, that's the face we want the burst on
                        class_238 box = shape.method_1107();
                        class_243 target = new class_243(
                            class_3532.method_15350(pearl.field_1352, bx + box.field_1323, bx + box.field_1320),
                            class_3532.method_15350(pearl.field_1351, by + box.field_1322, by + box.field_1325),
                            class_3532.method_15350(pearl.field_1350, bz + box.field_1321, bz + box.field_1324)
                        );
                        if (target.method_1025(pearl) > reachSq) {
                            continue;
                        }

                        class_243 windVel = this.aimVelocity(windStart, target, inherited, speed);
                        if (windVel == null) {
                            continue;
                        }

                        casts++;
                        double toTarget = Math.sqrt(target.method_1025(windStart));
                        PearlCatch.Burst burst = this.castBurst(windStart, windVel, offset, landIdx, toTarget + 0.75);
                        if (burst != null) {
                            bursts.add(burst);
                        }
                    }
                }
            }
        }

        if (bursts.isEmpty()) {
            return null;
        }

        // cheap pass first, how hard the shove is along the way the pearl is already going,
        // then only the best few get the full flight simulated
        bursts.sort(Comparator.comparingDouble((PearlCatch.Burst b) -> -this.shoveScore(b)));
        PearlCatch.Plan best = null;

        for (int i = 0; i < Math.min(6, bursts.size()); i++) {
            PearlCatch.Burst burst = bursts.get(i);
            class_243 aim = burst.velocity().method_1020(inherited).method_1029();
            PearlCatch.Plan candidate = this.toPlan(burst, windStart.method_1019(aim.method_1021(100.0)), baseDist, origin);
            if (best == null || candidate.gain() > best.gain()) {
                best = candidate;
            }
        }

        return best != null && best.gain() >= this.minGain.getValue() ? best : null;
    }

    /** Same evaluation as the solver, but for the rotation we'll actually throw with this tick. */
    private PearlCatch.Plan checkThrow() {
        class_746 player = this.mc.field_1724;
        class_243 windStart = player.method_33571();
        class_243 velocity = this.lookVector(player.method_36454(), player.method_36455())
            .method_1021(this.power(class_9239.field_55047))
            .method_1019(this.lastMovement(player));
        int size = this.pathPos.size();
        if (size < 3) {
            return null;
        }

        int offset = this.ticksSinceThrow() - this.pathBase;
        int landIdx = this.pathLanded ? size - 1 : size;
        double reach = Math.sqrt(velocity.method_1025(class_243.field_1353)) * MAX_WIND_TICKS;
        PearlCatch.Burst burst = this.castBurst(windStart, velocity, offset, landIdx, reach);
        if (burst == null) {
            return null;
        }

        class_243 origin = player.method_73189();
        double baseDist = this.horizontalDistance(this.pathPos.get(size - 1), origin);
        return this.toPlan(burst, windStart.method_1019(this.lookVector(player.method_36454(), player.method_36455()).method_1021(100.0)), baseDist, origin);
    }

    /**
     * Wind velocity that flies straight through target once our inherited movement is added on top of the
     * aimed part (class_1676.method_24919). Solves |u * delta - inherited| = speed for the flight scale u.
     */
    private class_243 aimVelocity(class_243 start, class_243 target, class_243 inherited, double speed) {
        class_243 delta = target.method_1020(start);
        double dd = delta.method_1026(delta);
        if (dd < 1.0E-6) {
            return null;
        }

        double di = delta.method_1026(inherited);
        double disc = di * di - dd * (inherited.method_1026(inherited) - speed * speed);
        if (disc < 0.0) {
            return null;
        }

        double u = (di + Math.sqrt(disc)) / dd;
        return u > 0.0 ? delta.method_1021(u) : null;
    }

    /**
     * Where a wind charge with this velocity bursts and how hard that shoves the pearl. Wind charges hold
     * their velocity (drag 1.0, no acceleration or gravity, class_9236), so the first block along the line is
     * the burst. The push is class_9892.method_61741: toward the pearl's eye, scaled by 1 - distance/2.4 and
     * the 1.22 modifier. Null if it never bursts, the pearl's already down, it's out of reach or a block
     * shields it.
     */
    private PearlCatch.Burst castBurst(class_243 start, class_243 velocity, int offset, int landIdx, double maxTravel) {
        double speed = Math.sqrt(velocity.method_1026(velocity));
        if (speed < 1.0E-4) {
            return null;
        }

        class_243 end = start.method_1019(velocity.method_1021(maxTravel / speed));
        class_3965 hit = this.raycast(start, end, this.mc.field_1724);
        if (hit.method_17783() == class_239.class_240.field_1333) {
            return null;
        }

        double travel = Math.sqrt(hit.method_17784().method_1025(start));
        int windTick = Math.max(1, (int)Math.ceil(travel / speed));
        int idx = offset + windTick;
        if (windTick > MAX_WIND_TICKS || idx < 1 || idx >= landIdx) {
            return null;
        }

        class_243 burstPos = hit.method_17784().method_1019(class_243.method_24954(hit.method_17780().method_62675()).method_1021(BURST_FACE_OFFSET));
        class_243 pearl = this.pathPos.get(idx);
        double d = Math.sqrt(pearl.method_1025(burstPos)) / BURST_RADIUS;
        if (d > 1.0) {
            return null;
        }

        class_243 eye = pearl.method_1031(0.0, PEARL_EYE, 0.0);
        if (this.raycast(burstPos, eye, this.mc.field_1724).method_17783() != class_239.class_240.field_1333) {
            return null;
        }

        class_243 push = eye.method_1020(burstPos).method_1029().method_1021((1.0 - d) * BURST_KNOCKBACK);
        return new PearlCatch.Burst(start, velocity, burstPos, idx, push);
    }

    private double shoveScore(PearlCatch.Burst burst) {
        class_243 vel = this.pathVel.get(burst.pearlIdx());
        double horizontal = Math.sqrt(vel.field_1352 * vel.field_1352 + vel.field_1350 * vel.field_1350);
        class_243 push = burst.push();
        double forward = horizontal < 1.0E-4 ? 0.0 : (push.field_1352 * vel.field_1352 + push.field_1350 * vel.field_1350) / horizontal;
        return forward + Math.max(0.0, push.field_1351) * 0.5;
    }

    /** Flies the pearl on from the burst with the push added and scores how much further out it lands. */
    private PearlCatch.Plan toPlan(PearlCatch.Burst burst, class_243 aimPoint, double baseDist, class_243 origin) {
        int idx = burst.pearlIdx();
        List<class_243> pos = new ArrayList<>();
        this.simulate(
            this.pathPos.get(idx),
            this.pathVel.get(idx).method_1019(burst.push()),
            this.pathWater.get(idx),
            this.mc.field_1724,
            pos,
            new ArrayList<>(),
            new ArrayList<>()
        );
        double gain = this.horizontalDistance(pos.get(pos.size() - 1), origin) - baseDist;
        return new PearlCatch.Plan(aimPoint, burst.burst(), idx, gain, pos);
    }

    private class_3965 raycast(class_243 from, class_243 to, class_1297 shapeContext) {
        return this.mc.field_1687.method_17742(new class_3959(from, to, class_3959.class_3960.field_17558, class_3959.class_242.field_1348, shapeContext));
    }

    private double horizontalDistance(class_243 a, class_243 b) {
        double dx = a.field_1352 - b.field_1352;
        double dz = a.field_1350 - b.field_1350;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Entity.method_5692 for water on the pearl's 0.25 box, contracted by 0.001 like vanilla. */
    private boolean touchingWater(double x, double y, double z) {
        double minX = x - PEARL_HALF_WIDTH + 0.001;
        double maxX = x + PEARL_HALF_WIDTH - 0.001;
        double minY = y + 0.001;
        double maxY = y + PEARL_HEIGHT - 0.001;
        double minZ = z - PEARL_HALF_WIDTH + 0.001;
        double maxZ = z + PEARL_HALF_WIDTH - 0.001;

        for (int bx = class_3532.method_15357(minX); bx < class_3532.method_15384(maxX); bx++) {
            for (int by = class_3532.method_15357(minY); by < class_3532.method_15384(maxY); by++) {
                for (int bz = class_3532.method_15357(minZ); bz < class_3532.method_15384(maxZ); bz++) {
                    class_2338 pos = new class_2338(bx, by, bz);
                    class_3610 fluid = this.mc.field_1687.method_8316(pos);
                    if (fluid.method_15767(class_3486.field_15517) && (float)by + fluid.method_15763(this.mc.field_1687, pos) >= minY) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private class_1684 findOurPearl() {
        class_1684 newest = null;

        for (class_1297 entity : this.mc.field_1687.method_18112()) {
            if (entity instanceof class_1684 pearl && pearl.method_5805()) {
                if (this.trackedPearl != null) {
                    if (pearl.method_5667().equals(this.trackedPearl)) {
                        return pearl;
                    }
                } else if (pearl.method_24921() == this.mc.field_1724
                    && !this.ignoredPearls.contains(pearl.method_5667())
                    && (newest == null || pearl.field_6012 < newest.field_6012)) {
                    newest = pearl;
                }
            }
        }

        return newest;
    }

    private float power(float itemPower) {
        return itemPower > 0.0F ? itemPower : FALLBACK_POWER;
    }

    private int findItemSlot(class_1792 item) {
        for (int i = 0; i < 9; i++) {
            if (this.mc.field_1724.method_31548().method_5438(i).method_7909() == item) {
                return i;
            }
        }

        return -1;
    }

    private boolean hasItem(class_1792 item) {
        return this.findItemSlot(item) != -1;
    }

    @Override
    public void onWorldRender(class_4587 matrices, float tickDelta) {
        if (this.showTrajectory.isEnabled() && this.pathPos.size() > 1 && this.mc.field_1724 != null) {
            class_4184 camera = this.mc.field_1773.method_19418();
            class_243 cameraPos = camera.method_71156();
            class_9799 allocator = new class_9799(2048);

            try {
                class_4598 vertexConsumers = class_4597.method_22991(allocator);
                matrices.method_22903();
                matrices.method_22904(-cameraPos.field_1352, -cameraPos.field_1351, -cameraPos.field_1350);
                PearlCatch.Plan shown = this.plan;
                // with a plan, the plain path stops at the burst and the boosted flight takes over in green
                int plainEnd = shown != null ? Math.min(shown.pearlIdx(), this.pathPos.size() - 1) : this.pathPos.size() - 1;

                for (int i = 0; i < plainEnd; i++) {
                    float progress = (float)i / this.pathPos.size();
                    RenderUtils.drawLine(
                        matrices, vertexConsumers, this.pathPos.get(i), this.pathPos.get(i + 1), new Color(0, (int)(255.0F - progress * 100.0F), 255), 0.78431374F
                    );
                }

                if (shown != null) {
                    List<class_243> boosted = shown.boosted();
                    for (int i = 0; i < boosted.size() - 1; i++) {
                        RenderUtils.drawLine(matrices, vertexConsumers, boosted.get(i), boosted.get(i + 1), new Color(0, 255, 90), 0.78431374F);
                    }

                    RenderUtils.drawBox(matrices, vertexConsumers, this.cube(shown.burst(), 0.3), new Color(255, 255, 0), 0.7058824F, true);
                    RenderUtils.drawBox(matrices, vertexConsumers, this.cube(boosted.get(boosted.size() - 1), 0.15), new Color(0, 255, 90), 0.7058824F, true);
                } else if (this.pathLanded) {
                    RenderUtils.drawBox(
                        matrices, vertexConsumers, this.cube(this.pathPos.get(this.pathPos.size() - 1), 0.15), new Color(255, 0, 255), 0.7058824F, true
                    );
                }

                matrices.method_22909();
                vertexConsumers.method_22993();
            } catch (Throwable var15) {
                try {
                    allocator.close();
                } catch (Throwable var14) {
                    var15.addSuppressed(var14);
                }

                throw var15;
            }

            allocator.close();
        }
    }

    private class_238 cube(class_243 center, double half) {
        return new class_238(
            center.field_1352 - half, center.field_1351 - half, center.field_1350 - half, center.field_1352 + half, center.field_1351 + half, center.field_1350 + half
        );
    }

    @Environment(EnvType.CLIENT)
    private record Burst(class_243 windStart, class_243 velocity, class_243 burst, int pearlIdx, class_243 push) {
    }

    @Environment(EnvType.CLIENT)
    private record Plan(class_243 aimPoint, class_243 burst, int pearlIdx, double gain, List<class_243> boosted) {
    }

    @Environment(EnvType.CLIENT)
    private static enum State {
        IDLE,
        THROWING_PEARL,
        TRACKING,
        EXECUTING_CATCH,
        RESETTING;
    }
}
