package com.slither.cyemer.module.implementation;

import com.slither.cyemer.mixin.MinecraftClientAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.render.RenderUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1776;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_2561;
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
 * Throws a pearl and catches it mid-air with a wind charge, teleporting you to that spot.
 *
 * Wind charges are in #redirectable_projectile, so a pearl can hit one (class_1676.method_26958) and lands
 * on it. The pearl was thrown first so it moves before the wind charge every server tick, tracing its centre
 * from last tick's spot to this tick's and hitting if that line enters the wind charge's box grown by the
 * pearl's age tolerance (class_1675.method_37226 / method_71624, class_238.method_992). The wind charge is aimed
 * so that line runs through the middle of its box. Both throws go out through useItem carrying their own
 * yaw/pitch in the use packet, so the camera never moves.
 *
 * Same Tick sends both in one tick, they can't get split up by lag. Next Tick sends the wind charge a tick
 * later, which catches about twice as high and far. Only upward throws can be caught: thrown flat or down the
 * pearl runs ahead of anything that comes after it at the same speed.
 */
@Environment(EnvType.CLIENT)
public class PearlCatch extends Module {
    private final ModeSetting timing = new ModeSetting("Timing", "Same Tick", "Next Tick");
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
    // class_9236.method_65341: 0.3125 cube that starts 0.15F below the wind charge's position
    private static final double WIND_HALF = 0.3125F / 2.0F;
    private static final double WIND_BELOW = 0.15F;
    private static final double WIND_HEIGHT = 0.3125F;
    private static final double WIND_CENTRE = WIND_HEIGHT / 2.0 - WIND_BELOW;
    // nextTriangular(0, 0.0172275) per axis times 1.5 power, the standard deviation of what each throw drifts per tick
    private static final double SPREAD_PER_TICK = 1.5 * 0.0172275 / Math.sqrt(6.0);
    private static final float FALLBACK_POWER = 1.5F;
    private static final int MAX_PATH_TICKS = 400;
    private static final int MAX_WIND_TICKS = 60;
    private static final int SHOW_TICKS = 40;
    private PearlCatch.State state = PearlCatch.State.IDLE;
    private int originalSlot = -1;
    private int stateTicks = 0;
    private final List<class_243> pathPos = new ArrayList<>();
    private final List<class_243> pathVel = new ArrayList<>();
    private final List<Boolean> pathWater = new ArrayList<>();
    private PearlCatch.Catch plan = null;

    public PearlCatch() {
        super("PearlCatch", "Throws a pearl and catches it mid-air with a wind charge.", Category.MOVEMENT);
        this.addSetting(this.timing);
        this.addSetting(this.showTrajectory);
    }

    @Override
    public void onEnable() {
        if (this.mc.field_1724 == null || this.mc.field_1687 == null) {
            this.toggle();
        } else if (this.hasItem(class_1802.field_8634) && this.hasItem(class_1802.field_49098)) {
            this.state = PearlCatch.State.THROWING;
            this.originalSlot = this.mc.field_1724.method_31548().method_67532();
            this.stateTicks = 0;
            this.plan = null;
            this.pathPos.clear();
        } else {
            this.tell("Need ender pearls and wind charges in your hotbar");
            this.toggle();
        }
    }

    @Override
    public void onDisable() {
        if (this.mc.field_1724 != null && this.originalSlot != -1) {
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
        }

        this.state = PearlCatch.State.IDLE;
        this.originalSlot = -1;
        this.plan = null;
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 == null || this.mc.field_1687 == null) {
            return;
        }

        switch (this.state) {
            case THROWING:
                this.handleThrow();
                break;
            case WAITING:
                this.handleWait();
                break;
            case SHOWING:
                if (++this.stateTicks > SHOW_TICKS) {
                    this.toggle();
                }
                break;
            default:
                break;
        }
    }

    private void handleThrow() {
        class_746 player = this.mc.field_1724;
        int pearlSlot = this.findItemSlot(class_1802.field_8634);
        int windSlot = this.findItemSlot(class_1802.field_49098);
        if (pearlSlot == -1 || windSlot == -1) {
            this.toggle();
            return;
        }

        if (this.coolingDown(pearlSlot) || this.coolingDown(windSlot)) {
            this.tell("Pearl or wind charge is on cooldown");
            this.toggle();
            return;
        }

        this.predictThrow();
        boolean sameTick = "Same Tick".equals(this.timing.getCurrentMode());
        PearlCatch.Catch catchPlan = this.planFor(sameTick ? 0 : 1);
        if (catchPlan == null) {
            // the other timing sometimes reaches an angle this one can't
            sameTick = !sameTick;
            catchPlan = this.planFor(sameTick ? 0 : 1);
        }

        if (catchPlan == null) {
            this.tell("Can't catch that throw, look higher");
            this.toggle();
            return;
        }

        this.plan = catchPlan;
        player.method_31548().method_61496(pearlSlot);
        ((MinecraftClientAccessor)this.mc).useItem();
        if (sameTick) {
            this.throwWind(catchPlan);
            this.state = PearlCatch.State.SHOWING;
        } else {
            this.state = PearlCatch.State.WAITING;
        }

        this.stateTicks = 0;
    }

    private void handleWait() {
        // solve again from exactly where we are now, the throw-time plan had to guess this tick's movement
        PearlCatch.Catch catchPlan = this.solveCatch(1, this.mc.field_1724.method_33571(), this.lastMovement(this.mc.field_1724));
        if (catchPlan == null) {
            this.tell("Lost the catch, the pearl flies normally");
            this.state = PearlCatch.State.SHOWING;
            return;
        }

        this.plan = catchPlan;
        this.throwWind(catchPlan);
        this.state = PearlCatch.State.SHOWING;
        this.stateTicks = 0;
    }

    /** Plan for a wind charge thrown delta ticks after the pearl, from where we'll be standing then. */
    private PearlCatch.Catch planFor(int delta) {
        class_746 player = this.mc.field_1724;
        if (delta == 0) {
            return this.solveCatch(0, player.method_33571(), this.lastMovement(player));
        }

        // next tick we'll have moved by about our velocity, handleWait redoes this with the real numbers
        class_243 velocity = player.method_18798();
        class_243 windStart = player.method_33571().method_1019(velocity);
        class_243 inherited = new class_243(velocity.field_1352, player.method_24828() ? 0.0 : velocity.field_1351, velocity.field_1350);
        return this.solveCatch(delta, windStart, inherited);
    }

    /** Wind charge goes out with its own rotation in the use packet, then the real rotation comes straight back. */
    private void throwWind(PearlCatch.Catch catchPlan) {
        class_746 player = this.mc.field_1724;
        int windSlot = this.findItemSlot(class_1802.field_49098);
        if (windSlot == -1) {
            return;
        }

        float yaw = player.method_36454();
        float pitch = player.method_36455();
        player.method_31548().method_61496(windSlot);
        player.method_36456(catchPlan.yaw());
        player.method_36457(catchPlan.pitch());
        ((MinecraftClientAccessor)this.mc).useItem();
        player.method_36456(yaw);
        player.method_36457(pitch);
    }

    /**
     * For every pearl tick that could be the catch, finds where the wind charge has to be for the box centre
     * to sit on the pearl's line that tick, which pins the aim down exactly (|u - inherited| = power for the
     * flight vector u). Each candidate then gets the full vanilla hit test with the rotation the server will
     * really use, and the one with the most room for the random spread on both throws wins.
     */
    private PearlCatch.Catch solveCatch(int delta, class_243 windStart, class_243 inherited) {
        double speed = this.power(class_9239.field_55047);
        PearlCatch.Catch best = null;
        double bestScore = 0.0;

        for (int a = delta + 2; a < this.pathPos.size(); a++) {
            int b = a - 1 - delta;
            // drop the pearl's line so the box centre, not the wind charge's position, lands on it
            class_243 from = this.pathPos.get(a - 1).method_1023(0.0, WIND_CENTRE, 0.0);
            class_243 to = this.pathPos.get(a).method_1023(0.0, WIND_CENTRE, 0.0);
            class_243 seg = to.method_1020(from);
            class_243 rel = from.method_1020(windStart.method_1019(inherited.method_1021(b)));
            double reach = speed * b;
            double qa = seg.method_1026(seg);
            double qb = 2.0 * rel.method_1026(seg);
            double qc = rel.method_1026(rel) - reach * reach;
            double disc = qb * qb - 4.0 * qa * qc;
            if (qa < 1.0E-9 || disc < 0.0) {
                continue;
            }

            double root = Math.sqrt(disc);
            for (double t : new double[]{(-qb - root) / (2.0 * qa), (-qb + root) / (2.0 * qa)}) {
                if (t < 0.0 || t > 1.0) {
                    continue;
                }

                class_243 spot = from.method_1019(seg.method_1021(t));
                class_243 aim = spot.method_1020(windStart).method_1021(1.0 / b).method_1020(inherited);
                float yaw = (float)Math.toDegrees(Math.atan2(-aim.field_1352, aim.field_1350));
                float pitch = (float)Math.toDegrees(-Math.asin(class_3532.method_15350(aim.field_1351 / speed, -1.0, 1.0)));
                // the server builds the velocity through the MathHelper tables, test what it will actually launch
                class_243 velocity = this.lookVector(yaw, pitch).method_1021(speed).method_1019(inherited);
                PearlCatch.Catch hit = this.firstCatch(windStart, velocity, delta, yaw, pitch);
                if (hit != null) {
                    double score = this.catchScore(hit);
                    if (best == null || score > bestScore) {
                        best = hit;
                        bestScore = score;
                    }
                }
            }
        }

        return best;
    }

    /**
     * The first tick the pearl runs into the wind charge, exactly as the server checks it: pearl tick a traces
     * from its last spot to this one against the wind charge where it ended tick a - 1 - delta, box grown by
     * max(0, min(0.3, (age - 2) / 20)). Null if the pearl lands or the wind charge bursts on a block first.
     */
    private PearlCatch.Catch firstCatch(class_243 windStart, class_243 velocity, int delta, float yaw, float pitch) {
        double speed = Math.sqrt(velocity.method_1026(velocity));
        if (speed < 1.0E-4) {
            return null;
        }

        int windLife = MAX_WIND_TICKS;
        class_3965 block = this.raycast(windStart, windStart.method_1019(velocity.method_1021(MAX_WIND_TICKS)), this.mc.field_1724);
        if (block.method_17783() != class_239.class_240.field_1333) {
            windLife = Math.max(1, (int)Math.ceil(Math.sqrt(block.method_17784().method_1025(windStart)) / speed));
        }

        for (int a = 1; a < this.pathPos.size(); a++) {
            int b = a - 1 - delta;
            if (b < 0) {
                continue;
            }

            if (b >= windLife) {
                return null;
            }

            class_243 wind = windStart.method_1019(velocity.method_1021(b));
            double m = Math.max(0.0F, Math.min(0.3F, (float)(a - 2) / 20.0F));
            double minX = wind.field_1352 - WIND_HALF - m;
            double minY = wind.field_1351 - WIND_BELOW - m;
            double minZ = wind.field_1350 - WIND_HALF - m;
            double maxX = wind.field_1352 + WIND_HALF + m;
            double maxY = wind.field_1351 - WIND_BELOW + WIND_HEIGHT + m;
            double maxZ = wind.field_1350 + WIND_HALF + m;
            class_243 from = this.pathPos.get(a - 1);
            class_243 to = this.pathPos.get(a);
            double t = boxRaycast(minX, minY, minZ, maxX, maxY, maxZ, from, to);
            if (t >= 0.0) {
                class_243 hitPos = from.method_1019(to.method_1020(from).method_1021(t));
                double offCentre = lineDistance(wind.method_1031(0.0, WIND_CENTRE, 0.0), from, to);
                double startClear = Math.sqrt(boxDistanceSq(minX, minY, minZ, maxX, maxY, maxZ, from));
                return new PearlCatch.Catch(yaw, pitch, windStart, wind, hitPos, a, b, offCentre, startClear);
            }
        }

        return null;
    }

    /**
     * How safely a catch survives the random spread on both throws: room to slide sideways before the pearl's
     * line misses the box, or backwards before the line starts inside it (vanilla doesn't count that), against
     * how far each throw has drifted by the catch.
     */
    private double catchScore(PearlCatch.Catch hit) {
        double m = Math.max(0.0F, Math.min(0.3F, (float)(hit.pearlTick() - 2) / 20.0F));
        double sideways = WIND_HALF + m - hit.offCentre();
        double room = Math.min(sideways, hit.startClear());
        double drift = SPREAD_PER_TICK * Math.sqrt(hit.pearlTick() * hit.pearlTick() + hit.windTick() * hit.windTick()) + 0.02;
        return room / drift;
    }

    /** class_238.method_992: only faces turned toward the ray count, 0 < t < 1, a start inside is a miss. -1 if none. */
    private static double boxRaycast(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, class_243 from, class_243 to) {
        double dx = to.field_1352 - from.field_1352;
        double dy = to.field_1351 - from.field_1351;
        double dz = to.field_1350 - from.field_1350;
        double best = 1.0;
        boolean hit = false;
        if (Math.abs(dx) > 1.0E-7) {
            double t = ((dx > 0.0 ? minX : maxX) - from.field_1352) / dx;
            if (t > 0.0 && t < best && inside(from.field_1351 + t * dy, minY, maxY) && inside(from.field_1350 + t * dz, minZ, maxZ)) {
                best = t;
                hit = true;
            }
        }

        if (Math.abs(dy) > 1.0E-7) {
            double t = ((dy > 0.0 ? minY : maxY) - from.field_1351) / dy;
            if (t > 0.0 && t < best && inside(from.field_1352 + t * dx, minX, maxX) && inside(from.field_1350 + t * dz, minZ, maxZ)) {
                best = t;
                hit = true;
            }
        }

        if (Math.abs(dz) > 1.0E-7) {
            double t = ((dz > 0.0 ? minZ : maxZ) - from.field_1350) / dz;
            if (t > 0.0 && t < best && inside(from.field_1352 + t * dx, minX, maxX) && inside(from.field_1351 + t * dy, minY, maxY)) {
                best = t;
                hit = true;
            }
        }

        return hit ? best : -1.0;
    }

    private static boolean inside(double v, double min, double max) {
        return min - 1.0E-7 < v && v < max + 1.0E-7;
    }

    private static double lineDistance(class_243 point, class_243 from, class_243 to) {
        class_243 dir = to.method_1020(from);
        double len = dir.method_1026(dir);
        if (len < 1.0E-12) {
            return Math.sqrt(point.method_1025(from));
        }

        double t = point.method_1020(from).method_1026(dir) / len;
        return Math.sqrt(point.method_1025(from.method_1019(dir.method_1021(t))));
    }

    private static double boxDistanceSq(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, class_243 p) {
        double dx = Math.max(0.0, Math.max(minX - p.field_1352, p.field_1352 - maxX));
        double dy = Math.max(0.0, Math.max(minY - p.field_1351, p.field_1351 - maxY));
        double dz = Math.max(0.0, Math.max(minZ - p.field_1350, p.field_1350 - maxZ));
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * The pearl we're about to throw, built the way the server builds it: class_3857 spawns it at eyeY - 0.1F,
     * class_1676.method_24919 aims it from yaw/pitch through the MathHelper tables and adds our last tick of
     * movement (no vertical part on the ground). The server's small random spread is left out, the catch score
     * budgets for it.
     */
    private void predictThrow() {
        class_746 player = this.mc.field_1724;
        class_243 velocity = this.lookVector(player.method_36454(), player.method_36455())
            .method_1021(this.power(class_1776.field_55033))
            .method_1019(this.lastMovement(player));
        class_243 start = new class_243(player.method_23317(), player.method_23320() - PEARL_SPAWN_DROP, player.method_23321());
        this.simulate(start, velocity, false, player);
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
     * class_1682.method_5773 one tick at a time: gravity, then drag (0.8F in water, 0.99F in air, from the water
     * state at the end of the previous tick), then a COLLIDER raycast from where the pearl is to where it's going.
     * A block in the way ends the flight on the hit point. Index i is the pearl after i ticks.
     */
    private void simulate(class_243 start, class_243 velocity, boolean inWater, class_1297 shapeContext) {
        this.pathPos.clear();
        this.pathVel.clear();
        this.pathWater.clear();
        this.pathPos.add(start);
        this.pathVel.add(velocity);
        this.pathWater.add(inWater);
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
                this.pathPos.add(hit.method_17784());
                this.pathVel.add(new class_243(vx, vy, vz));
                this.pathWater.add(water);
                return;
            }

            x = to.field_1352;
            y = to.field_1351;
            z = to.field_1350;
            water = this.touchingWater(x, y, z);
            this.pathPos.add(to);
            this.pathVel.add(new class_243(vx, vy, vz));
            this.pathWater.add(water);
            if (y < floor) {
                return;
            }
        }
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

    private class_3965 raycast(class_243 from, class_243 to, class_1297 shapeContext) {
        return this.mc.field_1687.method_17742(new class_3959(from, to, class_3959.class_3960.field_17558, class_3959.class_242.field_1348, shapeContext));
    }

    private boolean coolingDown(int slot) {
        class_1799 stack = this.mc.field_1724.method_31548().method_5438(slot);
        return this.mc.field_1724.method_7357().method_7904(stack);
    }

    private void tell(String message) {
        if (this.mc.field_1724 != null) {
            this.mc.field_1724.method_7353(class_2561.method_43470("PearlCatch: " + message), true);
        }
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
        PearlCatch.Catch shown = this.plan;
        if (this.showTrajectory.isEnabled() && shown != null && this.pathPos.size() > 1 && this.mc.field_1724 != null) {
            class_4184 camera = this.mc.field_1773.method_19418();
            class_243 cameraPos = camera.method_71156();
            class_9799 allocator = new class_9799(2048);

            try {
                class_4598 vertexConsumers = class_4597.method_22991(allocator);
                matrices.method_22903();
                matrices.method_22904(-cameraPos.field_1352, -cameraPos.field_1351, -cameraPos.field_1350);
                int end = Math.min(shown.pearlTick(), this.pathPos.size() - 1);

                for (int i = 0; i < end - 1; i++) {
                    RenderUtils.drawLine(matrices, vertexConsumers, this.pathPos.get(i), this.pathPos.get(i + 1), new Color(0, 160, 255), 0.78431374F);
                }

                RenderUtils.drawLine(matrices, vertexConsumers, this.pathPos.get(Math.max(0, end - 1)), shown.hitPos(), new Color(0, 160, 255), 0.78431374F);
                RenderUtils.drawLine(matrices, vertexConsumers, shown.windStart(), shown.wind(), new Color(220, 220, 220), 0.5882353F);
                RenderUtils.drawBox(matrices, vertexConsumers, this.cube(shown.hitPos(), 0.2), new Color(0, 255, 90), 0.7058824F, true);
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
    private record Catch(
        float yaw, float pitch, class_243 windStart, class_243 wind, class_243 hitPos, int pearlTick, int windTick, double offCentre, double startClear
    ) {
    }

    @Environment(EnvType.CLIENT)
    private static enum State {
        IDLE,
        THROWING,
        WAITING,
        SHOWING;
    }
}
