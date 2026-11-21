package com.slither.cyemer.module.implementation.donutsmp;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ColorSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
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
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2346;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_2680;
import net.minecraft.class_2818;
import net.minecraft.class_2826;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4597.class_4598;
import net.minecraft.class_638;
import net.minecraft.class_746;
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
        if (shown.isEmpty() || self == null || Cyemer.selfDestructed) {
            return;
        }
        class_243 cam = this.mc.field_1773.method_19418().method_71156();
        double cx = cam.field_1352;
        double cy = cam.field_1351;
        double cz = cam.field_1350;
        Color c = this.color.getValue();
        try (class_9799 allocator = new class_9799(8192)) {
            class_4598 consumers = class_4597.method_22991(allocator);
            for (int i = 0; i < shown.size(); i++) {
                Spot s = shown.get(i);
                float strength = i == 0 ? 1.0F : 0.55F; // the nearest stands out
                double x0 = s.x() - cx;
                double y0 = s.y() - cy;
                double z0 = s.z() - cz;
                double x1 = x0 + s.lx();
                double y1 = y0 + s.h();
                double z1 = z0 + s.lz();
                RenderUtils.drawBoxEsp(matrices, consumers, new class_238(x0, y0, z0, x1, y1, z1), c, 0.12F * strength, true);
                edges(matrices, consumers, x0, y0, z0, x1, y1, z1, c, 0.9F * strength);
            }
            if (this.tracer.isEnabled()) {
                Spot s = shown.get(0);
                class_243 look = self.method_5828(tickDelta);
                RenderUtils.drawLine(matrices, consumers, new class_243(look.field_1352 * 0.6, look.field_1351 * 0.6, look.field_1350 * 0.6),
                        new class_243(s.centerX() - cx, s.y() + s.h() / 2.0 - cy, s.centerZ() - cz), c, 0.8F, true);
            }
            consumers.method_22993();
        }
    }

    /** The twelve edges of a box, drawn through terrain. */
    private static void edges(class_4587 m, class_4598 v, double x0, double y0, double z0, double x1, double y1, double z1, Color c, float a) {
        double[][] corners = {{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}, {x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}};
        int[][] pairs = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] p : pairs) {
            double[] a0 = corners[p[0]];
            double[] b0 = corners[p[1]];
            RenderUtils.drawLine(m, v, new class_243(a0[0], a0[1], a0[2]), new class_243(b0[0], b0[1], b0[2]), c, a, true);
        }
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
