package com.slither.cyemer.module.implementation.donutsmp;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ColorSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.RotationManager;
import com.slither.cyemer.util.render.RenderUtils;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1661;
import net.minecraft.class_1799;
import net.minecraft.class_1887;
import net.minecraft.class_1890;
import net.minecraft.class_1893;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2346;
import net.minecraft.class_239;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_2680;
import net.minecraft.class_2818;
import net.minecraft.class_2826;
import net.minecraft.class_304;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4597.class_4598;
import net.minecraft.class_638;
import net.minecraft.class_6880;
import net.minecraft.class_746;
import net.minecraft.class_7924;
import net.minecraft.class_9799;

/**
 * Finds places to dig a sealed room: a Length x Width x Height box whose
 * floor is at Bottom Y (-59 by default, the first layer clear of bedrock)
 * that, once mined out, touches nothing but solid blocks.
 *
 * Around the box, a shell Wall Thickness blocks deep on every side, above
 * and below must be solid full blocks: no air, water, lava, or see-through
 * blocks like glass or dripstone. Inside the box, no fluid (cave pockets
 * this deep are lava). Air sealed inside the box is fine; it cannot connect
 * out. With No Falling Ceiling, nothing that falls (sand, gravel, concrete
 * powder) sits directly over the room.
 *
 * Each column is graded once per chunk into two flags, "can be wall" and
 * "can be inside", so a box check is two 2D prefix-sum lookups. Chunks
 * whose sections hold nothing risky in the layers that matter skip the
 * per-block read entirely. Only what the client is sent is known: on a
 * server whose anti-xray hides caves themselves, a hidden cave would look
 * solid.
 *
 * Auto Mine digs out the nearest spot: it looks at each block and holds
 * attack like a player, only ever while the crosshair is on a block inside
 * the room, and walks within the opened room to reach the rest.
 */
@Environment(EnvType.CLIENT)
public class SpotFinder extends Module {
    private static final int BAD_WALL = 1;
    private static final int BAD_INSIDE = 2;
    private static final long SCAN_BUDGET_NS = 2_000_000L;
    private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private static SpotFinder instance;

    private final SliderSetting length = new SliderSetting("Length", 5.0, 1.0, 48.0, 0);
    private final SliderSetting width = new SliderSetting("Width", 5.0, 1.0, 48.0, 0);
    private final SliderSetting height = new SliderSetting("Height", 3.0, 1.0, 24.0, 0);
    private final SliderSetting bottomY = new SliderSetting("Bottom Y", -59.0, -63.0, 300.0, 0);
    private final SliderSetting wall = new SliderSetting("Wall Thickness", 1.0, 1.0, 4.0, 0);
    private final BooleanSetting rotate = new BooleanSetting("Rotate", true);
    private final BooleanSetting noFalling = new BooleanSetting("No Falling Ceiling", true);
    private final SliderSetting range = new SliderSetting("Range (chunks)", 8.0, 1.0, 32.0, 0);
    private final SliderSetting maxSpots = new SliderSetting("Max Spots", 5.0, 1.0, 20.0, 0);
    private final ColorSetting color = new ColorSetting("Color", new Color(80, 220, 255));
    private final BooleanSetting tracer = new BooleanSetting("Tracer", true);
    /** A button: turning it on prints the spots and turns itself back off. */
    private final BooleanSetting listSpots = new BooleanSetting("List Spots", false);
    private final BooleanSetting autoMine = new BooleanSetting("Auto Mine", false);
    private final SliderSetting turnSpeed = new SliderSetting("Turn Speed", 22.0, 1.0, 35.0, 1);
    private final BooleanSetting autoWalk = new BooleanSetting("Auto Walk", true);
    private final BooleanSetting bestTool = new BooleanSetting("Best Tool", true);

    private static final class ChunkInfo {
        final class_2818 chunk;
        final byte[] flags;

        ChunkInfo(class_2818 chunk, byte[] flags) {
            this.chunk = chunk;
            this.flags = flags;
        }
    }

    /** A room's interior: min corner, footprint and floor Y. */
    private record Spot(int x, int y, int z, int lx, int lz, int h) {
        double centerX() {
            return this.x + this.lx / 2.0;
        }

        double centerZ() {
            return this.z + this.lz / 2.0;
        }
    }

    private final Map<Long, ChunkInfo> chunks = new HashMap<>();
    private final ArrayDeque<long[]> queue = new ArrayDeque<>();
    private List<Spot> spots = List.of();
    private class_1937 lastWorld = null;
    private String seenConfig = null;
    private boolean dirty = true;
    private int ticks = 0;
    private double solvedAtX = Double.NaN;
    private double solvedAtZ = Double.NaN;
    private boolean announced = false;
    private int[] wallSums = new int[0];
    private int[] insideSums = new int[0];
    private final class_2338.class_2339 probe = new class_2338.class_2339();

    /** A block to mine and the point on it to look at. */
    private record Aim(int x, int y, int z, class_243 point) {}

    private Spot mining = null;
    private Aim mineTarget = null;
    private long targetSince = 0L;
    private final Map<Long, Long> skipped = new HashMap<>();
    private boolean holdingAttack = false;
    private boolean holdingForward = false;
    private String lastHint = null;

    public SpotFinder() {
        super("SpotFinder", "Finds sealed spots to dig a hidden room: solid on every side once mined out.", Category.DONUT_SMP);
        this.addSetting(this.length);
        this.addSetting(this.width);
        this.addSetting(this.height);
        this.addSetting(this.bottomY);
        this.addSetting(this.wall);
        this.addSetting(this.rotate);
        this.addSetting(this.noFalling);
        this.addSetting(this.range);
        this.addSetting(this.maxSpots);
        this.addSetting(this.color);
        this.addSetting(this.tracer);
        this.addSetting(this.listSpots);
        this.addSetting(this.autoMine);
        this.addSetting(this.turnSpeed);
        this.addSetting(this.autoWalk);
        this.addSetting(this.bestTool);
        instance = this;
    }

    public static SpotFinder getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        this.reset();
    }

    @Override
    public void onDisable() {
        this.stopMining();
        this.reset();
    }

    private void reset() {
        this.chunks.clear();
        this.queue.clear();
        this.spots = List.of();
        this.dirty = true;
        this.announced = false;
        this.solvedAtX = Double.NaN;
    }

    private int floorY() {
        return (int) this.bottomY.getValue();
    }

    private int roomHeight() {
        return (int) this.height.getValue();
    }

    private int thickness() {
        return (int) this.wall.getValue();
    }

    @Override
    public void onTick() {
        class_638 world = this.mc.field_1687;
        class_746 self = this.mc.field_1724;
        if (world == null || self == null) {
            return;
        }
        if (world != this.lastWorld) {
            this.lastWorld = world;
            this.stopMining(); // a spot in another world means nothing here
            this.reset();
        }
        // The column grades depend on the vertical settings; the footprint
        // only matters when solving, so changing it just re-solves.
        String config = this.floorY() + ":" + this.roomHeight() + ":" + this.thickness() + ":" + this.noFalling.isEnabled();
        if (!config.equals(this.seenConfig)) {
            this.seenConfig = config;
            this.reset();
        }
        this.ticks++;

        if (this.queue.isEmpty() && this.ticks % 10 == 0) {
            this.refillQueue(world, self);
        }
        if (this.ticks % 20 == 0) {
            this.dropUnloaded(world);
        }
        long deadline = System.nanoTime() + SCAN_BUDGET_NS;
        while (!this.queue.isEmpty() && System.nanoTime() < deadline) {
            long[] next = this.queue.poll();
            int cx = (int) next[0];
            int cz = (int) next[1];
            if (world.method_8393(cx, cz)) {
                this.chunks.put(chunkKey(cx, cz), new ChunkInfo(world.method_8497(cx, cz), this.grade(world, world.method_8497(cx, cz), cx, cz)));
                this.dirty = true;
            }
        }

        boolean moved = Double.isNaN(this.solvedAtX)
                || Math.abs(self.method_23317() - this.solvedAtX) > 8.0 || Math.abs(self.method_23321() - this.solvedAtZ) > 8.0;
        if (this.ticks % 20 == 0 && (this.dirty || moved)) {
            this.dirty = false;
            this.solvedAtX = self.method_23317();
            this.solvedAtZ = self.method_23321();
            this.solve(self);
            if (!this.announced && !this.spots.isEmpty()) {
                this.announced = true;
                this.say("§7Nearest: " + this.describe(this.spots.get(0), self));
            }
        }

        if (this.autoMine.isEnabled()) {
            this.tickMine(world, self);
        } else if (this.mining != null || this.holdingAttack || this.holdingForward) {
            this.stopMining();
            this.lastHint = null;
        }

        if (this.listSpots.isEnabled()) {
            this.listSpots.setEnabled(false);
            if (this.spots.isEmpty()) {
                this.say("§7No sealed spot found in range yet" + (this.queue.isEmpty() ? "" : " (still scanning)"));
            } else {
                for (int i = 0; i < this.spots.size(); i++) {
                    this.say("§f" + (i + 1) + ".§r §7" + this.describe(this.spots.get(i), self));
                }
            }
        }
    }

    private void refillQueue(class_638 world, class_746 self) {
        int pcx = (int) Math.floor(self.method_23317()) >> 4;
        int pcz = (int) Math.floor(self.method_23321()) >> 4;
        int r = (int) this.range.getValue() + 1; // one extra so walls at the edge can be graded
        List<long[]> todo = new ArrayList<>();
        for (int cx = pcx - r; cx <= pcx + r; cx++) {
            for (int cz = pcz - r; cz <= pcz + r; cz++) {
                if (!world.method_8393(cx, cz)) {
                    continue;
                }
                ChunkInfo info = this.chunks.get(chunkKey(cx, cz));
                if (info == null || info.chunk != world.method_8497(cx, cz)) {
                    int dx = cx - pcx;
                    int dz = cz - pcz;
                    todo.add(new long[]{cx, cz, (long) dx * dx + (long) dz * dz});
                }
            }
        }
        todo.sort((a, b) -> Long.compare(a[2], b[2]));
        this.queue.addAll(todo);
    }

    private void dropUnloaded(class_638 world) {
        Iterator<Map.Entry<Long, ChunkInfo>> it = this.chunks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, ChunkInfo> entry = it.next();
            long key = entry.getKey();
            int cx = (int) (key >> 32);
            int cz = (int) key;
            if (!world.method_8393(cx, cz) || world.method_8497(cx, cz) != entry.getValue().chunk) {
                it.remove();
                this.dirty = true;
            }
        }
    }

    /** Solid full block: what a wall, floor or ceiling of a sealed room has to be. */
    private boolean solid(class_2818 chunk, class_2680 state, class_2338 pos) {
        return !state.method_26215() && state.method_26227().method_15769() && state.method_26212(chunk, pos);
    }

    /** Grades the 256 columns of one chunk. */
    private byte[] grade(class_638 world, class_2818 chunk, int cx, int cz) {
        byte[] flags = new byte[256];
        int floor = this.floorY();
        int h = this.roomHeight();
        int t = this.thickness();
        int y0 = floor - t;
        int y1 = floor + h - 1 + t;
        int worldBottom = world.method_31607();
        boolean falling = this.noFalling.isEnabled();

        // Fast path: if no section in the layers that matter holds anything
        // that is not solid (or that falls), every column passes.
        Predicate<class_2680> risky = state -> !this.solid(chunk, state, class_2338.field_10980)
                || (falling && state.method_26204() instanceof class_2346);
        boolean anyRisky = false;
        class_2826[] sections = chunk.method_12006();
        for (int y = Math.max(y0, worldBottom) & ~15; y <= y1 && !anyRisky; y += 16) {
            int index = chunk.method_31602(y);
            if (index < 0 || index >= sections.length) {
                anyRisky = y >= worldBottom; // above the world is air
                continue;
            }
            class_2826 section = sections[index];
            anyRisky = section == null || section.method_38292() || section.method_19523(risky);
        }
        if (!anyRisky) {
            return flags;
        }

        int baseX = cx << 4;
        int baseZ = cz << 4;
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int f = 0;
                for (int y = y0; y <= y1; y++) {
                    boolean ok;
                    boolean fluid;
                    class_2680 state = null;
                    if (y < worldBottom) {
                        ok = true; // under the world: nothing can get in from there
                        fluid = false;
                    } else {
                        this.probe.method_10103(baseX + lx, y, baseZ + lz);
                        state = chunk.method_8320(this.probe);
                        ok = this.solid(chunk, state, this.probe);
                        fluid = !state.method_26227().method_15769();
                    }
                    if (!ok) {
                        f |= BAD_WALL;
                    }
                    boolean inRoom = y >= floor && y < floor + h;
                    if (inRoom ? fluid : !ok) {
                        f |= BAD_INSIDE; // fluid in the room, or a hole in its floor or ceiling
                    }
                    if (falling && y == floor + h && state != null && state.method_26204() instanceof class_2346) {
                        f |= BAD_INSIDE; // would drop in once the room under it is dug
                    }
                }
                flags[(lz << 4) | lx] = (byte) f;
            }
        }
        return flags;
    }

    // ---- Auto Mine ----

    /**
     * Mines the locked spot the way a player does: turns the camera onto a
     * block and holds attack, so vanilla does the breaking, the progress and
     * the packets. Attack is only held while the crosshair is actually on a
     * block inside the room, so a turn that sweeps across a wall can never
     * chip it; the walls are what keeps the room sealed.
     */
    private void tickMine(class_638 world, class_746 self) {
        if (this.mc.field_1755 != null) {
            this.releaseKeys(); // a screen is open: hands off, camera included
            RotationManager.stop(this);
            return;
        }
        if (this.mining == null) {
            if (this.spots.isEmpty()) {
                this.hint("§7Auto Mine is waiting for a spot to be found");
                return;
            }
            this.mining = this.spots.get(0);
            this.skipped.clear();
            this.say("§aMining§r §7" + this.describe(this.mining, self));
        }
        Spot s = this.mining;
        long now = System.currentTimeMillis();
        this.skipped.values().removeIf(until -> until < now);

        class_243 eye = self.method_33571();
        double reach = self.method_55754() - 0.25;
        Aim best = null;
        double bestD = Double.MAX_VALUE;
        int ex = (int) Math.floor(eye.field_1352);
        int ey = (int) Math.floor(eye.field_1351);
        int ez = (int) Math.floor(eye.field_1350);
        int r = (int) Math.ceil(reach) + 1;
        for (int x = Math.max(s.x(), ex - r); x <= Math.min(s.x() + s.lx() - 1, ex + r); x++) {
            for (int y = Math.max(s.y(), ey - r); y <= Math.min(s.y() + s.h() - 1, ey + r); y++) {
                for (int z = Math.max(s.z(), ez - r); z <= Math.min(s.z() + s.lz() - 1, ez + r); z++) {
                    if (!this.minable(world, x, y, z) || this.skipped.containsKey(pack(x, y, z))) {
                        continue;
                    }
                    class_243 point = this.visiblePoint(world, self, eye, x, y, z, reach);
                    if (point == null) {
                        continue;
                    }
                    double d = point.method_1025(eye);
                    if (d < bestD) {
                        bestD = d;
                        best = new Aim(x, y, z, point);
                    }
                }
            }
        }

        if (best == null) {
            this.mineTarget = null;
            this.setKey(this.mc.field_1690.field_1886, false, true);
            if (this.ticks % 10 == 0 && this.remaining(world, s) == 0) {
                this.say("§aRoom mined out§r §7- " + s.lx() + "x" + s.lz() + "x" + s.h() + " at (" + s.x() + ", " + s.y() + ", " + s.z() + ")");
                this.autoMine.setEnabled(false);
                this.stopMining();
                return;
            }
            this.walkIntoRoom(world, self, s);
            return;
        }
        this.setKey(this.mc.field_1690.field_1894, false, false); // close enough: stop walking

        // The spot was sealed when found; make sure it still is before opening this block.
        if (this.fluidBeside(world, best.x(), best.y(), best.z())) {
            this.say("§cAuto Mine stopped§r §7- fluid beside (" + best.x() + ", " + best.y() + ", " + best.z() + "), so nothing floods in");
            this.autoMine.setEnabled(false);
            this.stopMining();
            return;
        }

        if (this.mineTarget == null || this.mineTarget.x() != best.x() || this.mineTarget.y() != best.y() || this.mineTarget.z() != best.z()) {
            this.mineTarget = best;
            this.targetSince = now;
        } else if (now - this.targetSince > 6000L) {
            // Five seconds on one block without breaking it: come back to it later.
            this.skipped.put(pack(best.x(), best.y(), best.z()), now + 10_000L);
            this.mineTarget = null;
            return;
        }
        if (this.bestTool.isEnabled()) {
            this.selectBestTool(world.method_8320(this.probe.method_10103(best.x(), best.y(), best.z())));
        }

        class_243 aimPoint = best.point();
        RotationManager.setRotationSupplier(this, RotationManager.Priority.NORMAL, () -> aimPoint, this.turnSpeed.getValue(),
                RotationManager.RotationMode.LINEAR, 0.0, false, false);
        this.setKey(this.mc.field_1690.field_1886, this.crosshairInRoom(world, s), true);
    }

    /** A block the crosshair is on, if it is a minable block inside the room. */
    private boolean crosshairInRoom(class_638 world, Spot s) {
        if (!(this.mc.field_1765 instanceof class_3965 hit) || hit.method_17783() != class_239.class_240.field_1332) {
            return false;
        }
        class_2338 pos = hit.method_17777();
        int x = pos.method_10263();
        int y = pos.method_10264();
        int z = pos.method_10260();
        return x >= s.x() && x < s.x() + s.lx() && y >= s.y() && y < s.y() + s.h() && z >= s.z() && z < s.z() + s.lz()
                && this.minable(world, x, y, z);
    }

    private boolean minable(class_638 world, int x, int y, int z) {
        class_2680 state = world.method_8320(this.probe.method_10103(x, y, z));
        return !state.method_26215() && state.method_26227().method_15769() && state.method_26214(world, this.probe) >= 0.0F;
    }

    /**
     * A point on the block the eye can see within reach: its centre or the
     * middle of a face, checked with the same outline raycast the crosshair
     * uses, so aiming there really puts the crosshair on this block.
     */
    private class_243 visiblePoint(class_638 world, class_746 self, class_243 eye, int x, int y, int z, double reach) {
        double reach2 = reach * reach;
        double[][] offsets = {{0.5, 0.5, 0.5}, {0.5, 0.95, 0.5}, {0.5, 0.05, 0.5}, {0.05, 0.5, 0.5}, {0.95, 0.5, 0.5}, {0.5, 0.5, 0.05}, {0.5, 0.5, 0.95}};
        for (double[] o : offsets) {
            class_243 point = new class_243(x + o[0], y + o[1], z + o[2]);
            if (point.method_1025(eye) > reach2) {
                continue;
            }
            class_3965 hit = world.method_17742(new class_3959(eye, point, class_3959.class_3960.field_17559, class_3959.class_242.field_1348, self));
            if (hit != null && hit.method_17783() == class_239.class_240.field_1332) {
                class_2338 pos = hit.method_17777();
                if (pos.method_10263() == x && pos.method_10264() == y && pos.method_10260() == z) {
                    return point;
                }
            }
        }
        return null;
    }

    private boolean fluidBeside(class_638 world, int x, int y, int z) {
        int[][] around = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (int[] o : around) {
            if (!world.method_8320(this.probe.method_10103(x + o[0], y + o[1], z + o[2])).method_26227().method_15769()) {
                return true;
            }
        }
        return false;
    }

    private int remaining(class_638 world, Spot s) {
        int count = 0;
        for (int x = s.x(); x < s.x() + s.lx(); x++) {
            for (int y = s.y(); y < s.y() + s.h(); y++) {
                for (int z = s.z(); z < s.z() + s.lz(); z++) {
                    if (this.minable(world, x, y, z)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Nothing in reach: if we are already standing inside the opened room,
     * walk toward the nearest block still to mine. The room is flat and
     * sealed, so walking in it is safe; outside it we never move the player
     * (that is where caves and drops are), we just say where to go.
     */
    private void walkIntoRoom(class_638 world, class_746 self, Spot s) {
        int fx = (int) Math.floor(self.method_23317());
        int fy = (int) Math.floor(self.method_23318() + 0.2);
        int fz = (int) Math.floor(self.method_23321());
        boolean inside = fx >= s.x() && fx < s.x() + s.lx() && fz >= s.z() && fz < s.z() + s.lz() && fy >= s.y() && fy < s.y() + s.h();
        if (!this.autoWalk.isEnabled() || !inside || s.h() < 2) {
            this.setKey(this.mc.field_1690.field_1894, false, false);
            RotationManager.stop(this);
            this.hint("§7Get within reach of the room to start: dig down to (" + s.x() + ", " + s.y() + ", " + s.z() + ")");
            return;
        }
        class_243 goal = null;
        double bestD = Double.MAX_VALUE;
        for (int x = s.x(); x < s.x() + s.lx(); x++) {
            for (int y = s.y(); y < s.y() + s.h(); y++) {
                for (int z = s.z(); z < s.z() + s.lz(); z++) {
                    if (!this.minable(world, x, y, z) || this.skipped.containsKey(pack(x, y, z))) {
                        continue;
                    }
                    double dx = x + 0.5 - self.method_23317();
                    double dz = z + 0.5 - self.method_23321();
                    double d = dx * dx + dz * dz;
                    if (d < bestD) {
                        bestD = d;
                        goal = new class_243(x + 0.5, self.method_33571().field_1351, z + 0.5);
                    }
                }
            }
        }
        if (goal == null) {
            this.setKey(this.mc.field_1690.field_1894, false, false);
            return;
        }
        class_243 walkTo = goal;
        RotationManager.setRotationSupplier(this, RotationManager.Priority.NORMAL, () -> walkTo, this.turnSpeed.getValue(),
                RotationManager.RotationMode.LINEAR, 0.0, false, true);
        this.setKey(this.mc.field_1690.field_1894, RotationManager.isRotationComplete(25.0F), false);
    }

    private void selectBestTool(class_2680 state) {
        class_1661 inventory = this.mc.field_1724.method_31548();
        class_6880<class_1887> efficiency = this.mc.field_1687.method_30349().method_30530(class_7924.field_41265).method_46747(class_1893.field_9131);
        int bestSlot = -1;
        float bestSpeed = 1.0F;
        for (int i = 0; i < 9; i++) {
            class_1799 stack = inventory.method_5438(i);
            if (stack.method_7960()) {
                continue;
            }
            float speed = stack.method_7924(state);
            int level = class_1890.method_8225(efficiency, stack);
            if (level > 0 && speed > 1.0F) {
                speed += level * level + 1;
            }
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        if (bestSlot != -1 && inventory.method_67532() != bestSlot) {
            inventory.method_61496(bestSlot);
        }
    }

    /** Presses or releases a key, remembering which ones we hold so we only ever let go of our own. */
    private void setKey(class_304 key, boolean down, boolean attack) {
        boolean held = attack ? this.holdingAttack : this.holdingForward;
        if (down == held) {
            return;
        }
        key.method_23481(down);
        if (attack) {
            this.holdingAttack = down;
        } else {
            this.holdingForward = down;
        }
    }

    private void releaseKeys() {
        this.setKey(this.mc.field_1690.field_1886, false, true);
        this.setKey(this.mc.field_1690.field_1894, false, false);
    }

    private void stopMining() {
        this.releaseKeys();
        RotationManager.stop(this);
        this.mining = null;
        this.mineTarget = null;
    }

    /** Says each distinct hint once, not every tick. */
    private void hint(String message) {
        if (!message.equals(this.lastHint)) {
            this.lastHint = message;
            this.say(message);
        }
    }

    /** From ClientWorldMixin: a changed block in the graded layers regrades its chunk. */
    public void onBlockUpdate(class_2338 pos) {
        if (!this.isEnabled() || Cyemer.selfDestructed) {
            return;
        }
        int y = pos.method_10264();
        int floor = this.floorY();
        int t = this.thickness();
        if (y < floor - t || y > floor + this.roomHeight() - 1 + t) {
            return;
        }
        if (this.chunks.remove(chunkKey(pos.method_10263() >> 4, pos.method_10260() >> 4)) != null) {
            this.dirty = true;
        }
    }

    /**
     * Valid boxes, nearest first, none overlapping another's walls. Boxes
     * are tried outward from us in rings so the search stops as soon as
     * Max Spots are taken instead of scoring every position in range.
     */
    private void solve(class_746 self) {
        int pcx = (int) Math.floor(self.method_23317()) >> 4;
        int pcz = (int) Math.floor(self.method_23321()) >> 4;
        int r = (int) this.range.getValue();
        int n = (2 * r + 1) << 4;
        int minX = (pcx - r) << 4;
        int minZ = (pcz - r) << 4;
        int stride = n + 1;
        if (this.wallSums.length != stride * stride) {
            this.wallSums = new int[stride * stride];
            this.insideSums = new int[stride * stride];
        }
        int[] ws = this.wallSums;
        int[] is = this.insideSums;
        for (int i = 0; i < n; i++) {
            int x = minX + i;
            for (int j = 0; j < n; j++) {
                int z = minZ + j;
                ChunkInfo info = this.chunks.get(chunkKey(x >> 4, z >> 4));
                int f = info == null ? (BAD_WALL | BAD_INSIDE) : info.flags[((z & 15) << 4) | (x & 15)];
                int idx = (i + 1) * stride + (j + 1);
                ws[idx] = ((f & BAD_WALL) != 0 ? 1 : 0) + ws[idx - stride] + ws[idx - 1] - ws[idx - stride - 1];
                is[idx] = ((f & BAD_INSIDE) != 0 ? 1 : 0) + is[idx - stride] + is[idx - 1] - is[idx - stride - 1];
            }
        }

        int len = (int) this.length.getValue();
        int wid = (int) this.width.getValue();
        int t = this.thickness();
        int floor = this.floorY();
        int h = this.roomHeight();
        int limit = (int) this.maxSpots.getValue();
        int[][] shapes = this.rotate.isEnabled() && len != wid ? new int[][]{{len, wid}, {wid, len}} : new int[][]{{len, wid}};
        double px = self.method_23317();
        double pz = self.method_23321();
        int ox = (int) Math.floor(px) - minX;
        int oz = (int) Math.floor(pz) - minZ;

        List<Spot> picked = new ArrayList<>();
        List<Spot> ring = new ArrayList<>();
        for (int k = 0; k < n && picked.size() < limit; k++) {
            ring.clear();
            for (int[] shape : shapes) {
                int lx = shape[0];
                int lz = shape[1];
                // Just the perimeter of ring k: 8k cells, not the (2k+1)^2 square.
                int cells = k == 0 ? 1 : 8 * k;
                for (int c = 0; c < cells; c++) {
                    int dx;
                    int dz;
                    if (k == 0) {
                        dx = 0;
                        dz = 0;
                    } else if (c < 2 * k + 1) {
                        dx = c - k;
                        dz = -k;
                    } else if (c < 4 * k + 2) {
                        dx = c - (2 * k + 1) - k;
                        dz = k;
                    } else if (c < 6 * k + 1) {
                        dx = -k;
                        dz = c - (4 * k + 2) - k + 1;
                    } else {
                        dx = k;
                        dz = c - (6 * k + 1) - k + 1;
                    }
                    int i0 = ox + dx - lx / 2;
                    int j0 = oz + dz - lz / 2;
                    if (i0 - t < 0 || j0 - t < 0 || i0 + lx + t > n || j0 + lz + t > n) {
                        continue;
                    }
                    if (sum(is, stride, i0, j0, lx, lz) != 0) {
                        continue;
                    }
                    int ringBad = sum(ws, stride, i0 - t, j0 - t, lx + 2 * t, lz + 2 * t) - sum(ws, stride, i0, j0, lx, lz);
                    if (ringBad == 0) {
                        ring.add(new Spot(minX + i0, floor, minZ + j0, lx, lz, h));
                    }
                }
            }
            ring.sort((a, b) -> Double.compare(dist2(a, px, pz), dist2(b, px, pz)));
            for (Spot s : ring) {
                if (picked.size() >= limit) {
                    break;
                }
                if (!this.clashes(s, picked, t)) {
                    picked.add(s);
                }
            }
        }
        this.spots = picked;
    }

    /** Count of flagged columns in an lx x lz rectangle whose min corner is (i, j) in grid space. */
    private static int sum(int[] p, int stride, int i, int j, int lx, int lz) {
        int a = (i + lx) * stride + (j + lz);
        int b = i * stride + (j + lz);
        int c = (i + lx) * stride + j;
        int d = i * stride + j;
        return p[a] - p[b] - p[c] + p[d];
    }

    /** Two rooms clash when one's walls would reach into the other. */
    private boolean clashes(Spot s, List<Spot> picked, int t) {
        for (Spot o : picked) {
            boolean apartX = s.x() + s.lx() + t <= o.x() || o.x() + o.lx() + t <= s.x();
            boolean apartZ = s.z() + s.lz() + t <= o.z() || o.z() + o.lz() + t <= s.z();
            if (!apartX && !apartZ) {
                return true;
            }
        }
        return false;
    }

    private static double dist2(Spot s, double px, double pz) {
        double dx = s.centerX() - px;
        double dz = s.centerZ() - pz;
        return dx * dx + dz * dz;
    }

    private String describe(Spot s, class_746 self) {
        double dx = s.centerX() - self.method_23317();
        double dz = s.centerZ() - self.method_23321();
        double dy = s.y() - self.method_23318();
        int distance = (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
        return "§f(" + s.x() + ", " + s.y() + ", " + s.z() + ")§7 to §f(" + (s.x() + s.lx() - 1) + ", " + (s.y() + s.h() - 1) + ", "
                + (s.z() + s.lz() - 1) + ")§7 · " + distance + "m " + compass(dx, dz) + " · " + s.lx() + "x" + s.lz() + "x" + s.h();
    }

    @Override
    public void onWorldRender(class_4587 matrices, float tickDelta) {
        List<Spot> shown = this.spots;
        class_746 self = this.mc.field_1724;
        if ((shown.isEmpty() && this.mining == null) || self == null || Cyemer.selfDestructed) {
            return;
        }
        class_243 cam = this.mc.field_1773.method_19418().method_71156();
        double cx = cam.field_1352;
        double cy = cam.field_1351;
        double cz = cam.field_1350;
        Color c = this.color.getValue();
        try (class_9799 allocator = new class_9799(8192)) {
            class_4598 consumers = class_4597.method_22991(allocator);
            Spot locked = this.mining;
            List<Spot> all = new ArrayList<>(shown);
            if (locked != null && !all.contains(locked)) {
                all.add(0, locked);
            }
            for (int i = 0; i < all.size(); i++) {
                Spot s = all.get(i);
                boolean primary = locked != null ? s.equals(locked) : i == 0;
                float strength = primary ? 1.0F : 0.5F; // the one being mined, or the nearest, stands out
                double x0 = s.x() - cx;
                double y0 = s.y() - cy;
                double z0 = s.z() - cz;
                double x1 = x0 + s.lx();
                double y1 = y0 + s.h();
                double z1 = z0 + s.lz();
                // Bars thicken with distance so the outline reads at any range.
                double far = Math.sqrt((x0 + x1) * (x0 + x1) + (y0 + y1) * (y0 + y1) + (z0 + z1) * (z0 + z1)) / 2.0;
                double w = Math.max(0.03, Math.min(0.3, 0.02 + far * 0.004));
                RenderUtils.drawBoxEsp(matrices, consumers, new class_238(x0, y0, z0, x1, y1, z1), c, 0.07F * strength, true);
                RenderUtils.drawBoxEsp(matrices, consumers, new class_238(x0, y0, z0, x1, y0 + 0.02, z1), c, 0.22F * strength, true);
                outline(matrices, consumers, x0, y0, z0, x1, y1, z1, w, c, 0.95F * strength);
            }
            Aim target = this.mineTarget;
            if (target != null) {
                double tx = target.x() - cx;
                double ty = target.y() - cy;
                double tz = target.z() - cz;
                RenderUtils.drawBoxEsp(matrices, consumers, new class_238(tx + 0.02, ty + 0.02, tz + 0.02, tx + 0.98, ty + 0.98, tz + 0.98),
                        Color.WHITE, 0.35F, true);
            }
            if (this.tracer.isEnabled()) {
                Spot s = this.mining != null ? this.mining : shown.get(0);
                class_243 look = self.method_5828(tickDelta);
                RenderUtils.drawLine(matrices, consumers, new class_243(look.field_1352 * 0.6, look.field_1351 * 0.6, look.field_1350 * 0.6),
                        new class_243(s.centerX() - cx, s.y() + s.h() / 2.0 - cy, s.centerZ() - cz), c, 0.8F, true);
            }
            consumers.method_22993();
        }
    }

    /**
     * The twelve edges of a box as solid bars w thick, drawn through
     * terrain. Lines in this pipeline are a single pixel, which disappears
     * underground; bars stay clear.
     */
    private static void outline(class_4587 m, class_4598 v, double x0, double y0, double z0, double x1, double y1, double z1,
                                double w, Color c, float a) {
        for (double y : new double[]{y0, y1}) {
            for (double z : new double[]{z0, z1}) {
                RenderUtils.drawBoxEsp(m, v, new class_238(x0 - w, y - w, z - w, x1 + w, y + w, z + w), c, a, true);
            }
            for (double x : new double[]{x0, x1}) {
                RenderUtils.drawBoxEsp(m, v, new class_238(x - w, y - w, z0 - w, x + w, y + w, z1 + w), c, a, true);
            }
        }
        for (double x : new double[]{x0, x1}) {
            for (double z : new double[]{z0, z1}) {
                RenderUtils.drawBoxEsp(m, v, new class_238(x - w, y0 - w, z - w, x + w, y1 + w, z + w), c, a, true);
            }
        }
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    private void say(String message) {
        if (this.mc.field_1724 != null) {
            this.mc.field_1724.method_7353(class_2561.method_43470("§b[SpotFinder]§r " + message), false);
        }
    }

    private static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    private static String compass(double dx, double dz) {
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        if (angle < 0.0) {
            angle += 360.0;
        }
        return COMPASS[(int) Math.round(angle / 45.0) % 8];
    }
}
