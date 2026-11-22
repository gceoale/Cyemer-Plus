package com.slither.cyemer.module.implementation.donutsmp;

import com.slither.cyemer.module.implementation.donutsmp.DigKit.Keys.Key;
import com.slither.cyemer.util.RotationManager;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_638;
import net.minecraft.class_746;

/**
 * Walks a FarmRoute under dig rules with the player's own controls: turns
 * the camera, holds forward and jump, and holds attack on whatever the
 * route needs broken next, only while the crosshair is on that block.
 * Vanilla does the movement, the breaking and every packet.
 *
 * Before each block it checks the live world again (fluid beside it, sand
 * or gravel over it, unbreakable, inside the room's walls). Anything wrong
 * is banned and the rest is re-planned around it, as is getting knocked
 * off the route or stuck.
 */
@Environment(EnvType.CLIENT)
final class DigWalker {
    enum Result { WORKING, ARRIVED, FAILED }

    private static final long PLAN_BUDGET_NS = 6_000_000L; // the player stands still while it plans
    private static final int MAX_REPLANS = 40;
    private static final int MAX_TRIES_PER_BLOCK = 4;
    static final double WALK_SPEED = 4.3; // blocks a second, walking

    private final class_310 mc = class_310.method_1551();
    private final Object owner;
    private final DigKit.Keys keys;
    private final DoubleSupplier turnSpeed;
    private final BooleanSupplier bestTool;
    private final int gx;
    private final int gy;
    private final int gz;
    private final FarmRoute.Dig rules;
    private final class_2338.class_2339 probe = new class_2338.class_2339();
    private final Long2IntOpenHashMap tries = new Long2IntOpenHashMap();

    private FarmRoute search;
    private List<FarmRoute.Step> path = List.of();
    private boolean partial;
    private int idx;
    private int replans;
    private int version;
    private String failure;
    private boolean fatal;
    private String activity = "Planning a route";

    private long mineKey = Long.MIN_VALUE;
    private long mineSince;
    private int[] mineCell;
    private long blindSince;
    private double anchorX;
    private double anchorY;
    private double anchorZ;
    private long anchorAt;
    private int offPathTicks;

    DigWalker(Object owner, DigKit.Keys keys, DoubleSupplier turnSpeed, BooleanSupplier bestTool, int gx, int gy, int gz, FarmRoute.Dig rules) {
        this.owner = owner;
        this.keys = keys;
        this.turnSpeed = turnSpeed;
        this.bestTool = bestTool;
        this.gx = gx;
        this.gy = gy;
        this.gz = gz;
        this.rules = rules;
    }

    List<FarmRoute.Step> path() {
        return this.path;
    }

    int index() {
        return this.idx;
    }

    /** The block being mined, or null. */
    int[] target() {
        return this.mineCell;
    }

    String activity() {
        return this.activity;
    }

    /** Bumped whenever the route is replaced, so an ETA knows not to compare across plans. */
    int version() {
        return this.version;
    }

    String failure() {
        return this.failure;
    }

    /** Failed for a reason another door or spot will not fix, like having no pickaxe. */
    boolean fatal() {
        return this.fatal;
    }

    Result tick(class_638 world, class_746 self) {
        if (this.failure != null) {
            return Result.FAILED;
        }
        long now = System.currentTimeMillis();
        if (this.search != null) {
            this.idle();
            FarmRoute.Status status = this.search.step(PLAN_BUDGET_NS);
            if (status == FarmRoute.Status.SEARCHING) {
                return Result.WORKING;
            }
            FarmRoute done = this.search;
            this.search = null;
            if (status == FarmRoute.Status.FAILED) {
                return this.fail("no safe way to dig to it from here", false);
            }
            this.path = done.path();
            this.partial = status == FarmRoute.Status.PARTIAL;
            this.idx = 1;
            this.version++;
            this.offPathTicks = 0;
            this.anchor(self, now);
        }
        if (this.path.isEmpty()) {
            this.idle();
            this.plan(world, self);
            return Result.WORKING;
        }

        int fx = (int) Math.floor(self.method_23317());
        int fy = (int) Math.floor(self.method_23318() + 0.01);
        int fz = (int) Math.floor(self.method_23321());
        if (self.method_24828()) {
            int found = -1;
            for (int j = Math.min(this.path.size() - 1, this.idx + 4); j >= Math.max(0, this.idx - 1); j--) {
                FarmRoute.Step s = this.path.get(j);
                if (s.x() == fx && s.y() == fy && s.z() == fz) {
                    found = j;
                    break;
                }
            }
            if (found < 0) {
                if (++this.offPathTicks > 10) {
                    return this.replan(world, self, "knocked off the route");
                }
            } else {
                this.offPathTicks = 0;
                if (found > this.idx || (found == this.idx && horizontal(self, this.path.get(found)) < 0.45)) {
                    this.idx = found + 1;
                }
            }
        }
        if (this.idx >= this.path.size()) {
            if (!this.partial) {
                this.idle();
                return Result.ARRIVED;
            }
            return this.replan(world, self, null); // the end of what could be planned last time: plan the next leg
        }

        FarmRoute.Step target = this.path.get(this.idx);
        FarmRoute.Step from = this.path.get(this.idx - 1);
        boolean digging = target.mined() != null;
        this.activity = !digging ? "Walking to the room"
                : (target.y() < from.y() ? "Digging down" : (target.y() > from.y() ? "Digging up" : "Tunnelling"));

        if (DigKit.fluidAt(world, this.probe, target.x(), target.y(), target.z())
                || DigKit.fluidAt(world, this.probe, target.x(), target.y() + 1, target.z())) {
            return this.replan(world, self, "fluid on the route");
        }
        List<int[]> blocked = this.blockedCells(world, target, fx, fy, fz);
        if (!blocked.isEmpty()) {
            this.anchor(self, now);
            return this.mine(world, self, blocked, fx, fy, fz, now);
        }
        this.mineKey = Long.MIN_VALUE;
        this.mineCell = null;
        this.blindSince = 0L;
        return this.move(world, self, target, fy, now);
    }

    /** What still has to be broken before stepping into the target cell. */
    private List<int[]> blockedCells(class_638 world, FarmRoute.Step t, int fx, int fy, int fz) {
        List<int[]> out = new ArrayList<>(4);
        int[] mined = t.mined();
        if (mined != null) {
            for (int i = 0; i + 2 < mined.length; i += 3) {
                this.addIfBlocked(out, world, mined[i], mined[i + 1], mined[i + 2]);
            }
        }
        this.addIfBlocked(out, world, t.x(), t.y(), t.z());
        this.addIfBlocked(out, world, t.x(), t.y() + 1, t.z());
        this.addIfBlocked(out, world, fx, fy + 1, fz); // something fell onto our head
        return out;
    }

    private void addIfBlocked(List<int[]> out, class_638 world, int x, int y, int z) {
        for (int[] c : out) {
            if (c[0] == x && c[1] == y && c[2] == z) {
                return;
            }
        }
        if (DigKit.blocking(world, this.probe, x, y, z)) {
            out.add(new int[]{x, y, z});
        }
    }

    private Result mine(class_638 world, class_746 self, List<int[]> blocked, int fx, int fy, int fz, long now) {
        this.keys.releaseMovement();
        // The live world, not the plan, decides what is safe to open.
        for (int[] c : blocked) {
            class_2680 state = world.method_8320(this.probe.method_10103(c[0], c[1], c[2]));
            if (state.method_26214(world, this.probe) < 0.0F) {
                return this.ban(world, self, c, "unbreakable block");
            }
            if (this.rules.keptOut(c[0], c[1], c[2])) {
                return this.ban(world, self, c, "that is the room's wall");
            }
            if (DigKit.fluidBeside(world, this.probe, c[0], c[1], c[2])) {
                return this.ban(world, self, c, "fluid beside a block");
            }
            boolean ours = c[0] == fx && c[2] == fz;
            if (!ours && DigKit.fallingAt(world, this.probe, c[0], c[1] + 1, c[2]) && !listed(blocked, c[0], c[1] + 1, c[2])) {
                return this.ban(world, self, c, "sand or gravel over a block");
            }
        }

        class_243 eye = self.method_33571();
        double reach = self.method_55754() - 0.25;
        int[] pick = null;
        class_243 point = null;
        double best = Double.MAX_VALUE;
        for (int[] c : blocked) {
            class_243 p = DigKit.visiblePoint(world, self, eye, c[0], c[1], c[2], reach);
            if (p != null && p.method_1025(eye) < best) {
                best = p.method_1025(eye);
                pick = c;
                point = p;
            }
        }
        if (pick == null) {
            int[] c = blocked.get(0);
            if (this.blindSince == 0L) {
                this.blindSince = now;
            } else if (now - this.blindSince > 2500L) {
                this.blindSince = 0L;
                return this.ban(world, self, c, "could not see a block");
            }
            this.keys.set(Key.ATTACK, false);
            this.rotate(new class_243(c[0] + 0.5, c[1] + 0.5, c[2] + 0.5), false);
            return Result.WORKING;
        }
        this.blindSince = 0L;

        class_2680 state = world.method_8320(this.probe.method_10103(pick[0], pick[1], pick[2]));
        if (!DigKit.canHarvest(this.mc, state)) {
            return this.fail("no working pickaxe left in the hotbar", true);
        }
        long key = FarmRoute.key(pick[0], pick[1], pick[2]);
        if (key != this.mineKey) {
            this.mineKey = key;
            this.mineSince = now;
            this.mineCell = pick;
            // A block that keeps coming back (a protected area) is not worth fighting.
            if (this.tries.addTo(key, 1) + 1 > MAX_TRIES_PER_BLOCK) {
                return this.ban(world, self, pick, "a block kept coming back");
            }
        } else {
            long expected = DigKit.breakTicks(this.mc, world, state, this.probe) * 50L;
            if (now - this.mineSince > Math.max(6000L, expected * 3L)) {
                return this.ban(world, self, pick, "a block would not break");
            }
        }
        if (this.bestTool.getAsBoolean()) {
            DigKit.selectBestTool(this.mc, state);
        }
        this.rotate(point, false);
        this.keys.set(Key.ATTACK, DigKit.crosshairOn(this.mc, pick[0], pick[1], pick[2]));
        return Result.WORKING;
    }

    private Result move(class_638 world, class_746 self, FarmRoute.Step t, int fy, long now) {
        this.keys.set(Key.ATTACK, false);
        double dx = t.x() + 0.5 - self.method_23317();
        double dz = t.z() + 0.5 - self.method_23321();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        boolean up = t.y() > fy;
        boolean grounded = self.method_24828();
        if (horiz < 0.15) {
            // Right over a hole we just dug, or under the next block up: no walking needed.
            this.keys.set(Key.FORWARD, false);
            this.keys.set(Key.JUMP, up && grounded);
        } else {
            this.rotate(new class_243(t.x() + 0.5, self.method_33571().field_1351, t.z() + 0.5), true);
            boolean aligned = RotationManager.isRotationComplete(30.0F);
            this.keys.set(Key.FORWARD, aligned);
            this.keys.set(Key.JUMP, aligned && grounded && up && horiz < 1.4);
        }
        double mx = self.method_23317() - this.anchorX;
        double my = self.method_23318() - this.anchorY;
        double mz = self.method_23321() - this.anchorZ;
        if (mx * mx + my * my + mz * mz > 0.01) {
            this.anchor(self, now);
        } else if (now - this.anchorAt > 5000L) {
            return this.replan(world, self, "stuck");
        } else if (now - this.anchorAt > 2000L && grounded) {
            this.keys.set(Key.JUMP, true); // a lip or a slab in the way
        }
        return Result.WORKING;
    }

    private void rotate(class_243 point, boolean yawOnly) {
        RotationManager.setRotationSupplier(this.owner, RotationManager.Priority.NORMAL, () -> point, this.turnSpeed.getAsDouble(),
                RotationManager.RotationMode.LINEAR, 0.0, false, yawOnly);
    }

    private Result ban(class_638 world, class_746 self, int[] c, String why) {
        this.rules.noMine.add(FarmRoute.key(c[0], c[1], c[2]));
        return this.replan(world, self, why);
    }

    private Result replan(class_638 world, class_746 self, String why) {
        this.idle();
        this.path = List.of();
        this.search = null;
        this.mineCell = null;
        this.mineKey = Long.MIN_VALUE;
        if (++this.replans > MAX_REPLANS) {
            return this.fail("gave up after " + MAX_REPLANS + " re-routes" + (why != null ? ", last one: " + why : ""), false);
        }
        this.plan(world, self);
        return Result.WORKING;
    }

    private void plan(class_638 world, class_746 self) {
        if (!self.method_24828() && !self.method_5799()) {
            return; // mid-fall or mid-jump: plan from where we land
        }
        int sx = (int) Math.floor(self.method_23317());
        int sy = (int) Math.floor(self.method_23318() + 0.01);
        int sz = (int) Math.floor(self.method_23321());
        this.search = new FarmRoute(world, sx, sy, sz, this.gx, this.gy, this.gz, this.rules);
        this.activity = "Planning a route";
    }

    private Result fail(String why, boolean fatal) {
        this.idle();
        this.failure = why;
        this.fatal = fatal;
        return Result.FAILED;
    }

    private void idle() {
        this.keys.releaseMovement();
        this.keys.set(Key.ATTACK, false);
    }

    private void anchor(class_746 self, long now) {
        this.anchorX = self.method_23317();
        this.anchorY = self.method_23318();
        this.anchorZ = self.method_23321();
        this.anchorAt = now;
    }

    private static boolean listed(List<int[]> cells, int x, int y, int z) {
        for (int[] c : cells) {
            if (c[0] == x && c[1] == y && c[2] == z) {
                return true;
            }
        }
        return false;
    }

    private static double horizontal(class_746 self, FarmRoute.Step s) {
        double dx = s.x() + 0.5 - self.method_23317();
        double dz = s.z() + 0.5 - self.method_23321();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Seconds of walking and digging left on the route, or NaN while there is no plan yet. */
    double etaSeconds(class_638 world, DigKit.Clock clock) {
        if (this.path.isEmpty()) {
            return Double.NaN;
        }
        double t = 0.0;
        for (int i = Math.max(1, this.idx); i < this.path.size(); i++) {
            FarmRoute.Step p = this.path.get(i - 1);
            FarmRoute.Step s = this.path.get(i);
            int dx = s.x() - p.x();
            int dz = s.z() - p.z();
            t += Math.sqrt(dx * dx + dz * dz) / WALK_SPEED;
            if (s.y() > p.y()) {
                t += 0.45;
            }
            t += this.digSeconds(world, s, clock);
        }
        if (this.partial) {
            t += this.beyondPlan() * (1.0 / WALK_SPEED + 1.5 * clock.typical(this.gy, this.probe.method_10103(this.gx, this.gy, this.gz)));
        }
        return t;
    }

    /** Blocks still to break on the route, plus a guess for the part not planned yet. */
    int blocksLeft(class_638 world) {
        int n = 0;
        for (int i = Math.max(1, this.idx); i < this.path.size(); i++) {
            n += this.digCount(world, this.path.get(i));
        }
        if (this.partial) {
            n += (int) Math.round(this.beyondPlan() * 1.5);
        }
        return n;
    }

    private double beyondPlan() {
        if (this.path.isEmpty()) {
            return 0.0;
        }
        FarmRoute.Step end = this.path.get(this.path.size() - 1);
        double dx = end.x() - this.gx;
        double dy = end.y() - this.gy;
        double dz = end.z() - this.gz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private double digSeconds(class_638 world, FarmRoute.Step s, DigKit.Clock clock) {
        double t = 0.0;
        int[] cells = cellsOf(s);
        for (int i = 0; i < cells.length; i += 3) {
            if (DigKit.blocking(world, this.probe, cells[i], cells[i + 1], cells[i + 2])) {
                t += clock.seconds(world.method_8320(this.probe), this.probe);
            }
        }
        return t;
    }

    private int digCount(class_638 world, FarmRoute.Step s) {
        int n = 0;
        int[] cells = cellsOf(s);
        for (int i = 0; i < cells.length; i += 3) {
            if (DigKit.blocking(world, this.probe, cells[i], cells[i + 1], cells[i + 2])) {
                n++;
            }
        }
        return n;
    }

    /** The cells a step mines, feet and head included, without repeats. */
    private static int[] cellsOf(FarmRoute.Step s) {
        int[] mined = s.mined() == null ? new int[0] : s.mined();
        int[] out = new int[mined.length + 6];
        int n = 0;
        for (int i = 0; i + 2 < mined.length; i += 3) {
            n = put(out, n, mined[i], mined[i + 1], mined[i + 2]);
        }
        n = put(out, n, s.x(), s.y(), s.z());
        n = put(out, n, s.x(), s.y() + 1, s.z());
        return java.util.Arrays.copyOf(out, n);
    }

    private static int put(int[] out, int n, int x, int y, int z) {
        for (int i = 0; i < n; i += 3) {
            if (out[i] == x && out[i + 1] == y && out[i + 2] == z) {
                return n;
            }
        }
        out[n] = x;
        out[n + 1] = y;
        out[n + 2] = z;
        return n + 3;
    }
}
