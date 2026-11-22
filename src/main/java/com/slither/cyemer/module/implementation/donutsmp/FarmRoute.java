package com.slither.cyemer.module.implementation.donutsmp;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2346;
import net.minecraft.class_2680;
import net.minecraft.class_3486;
import net.minecraft.class_3610;
import net.minecraft.class_638;

/**
 * A walking route from where the player stands to beside a detected block,
 * planned over the chunks the client has loaded.
 *
 * Moves are walking (straight and diagonal), stepping up a block, safe drops,
 * swimming, tunnelling, digging a staircase down, digging straight down and
 * pillaring up. Each is priced so the route walks wherever it can and only
 * mines when it has to, which is what reaching a base buried underground
 * takes: walk to above it, then dig. Breaking anything next to lava, and
 * standing on magma, cactus or dripstone, is never planned.
 *
 * The world is only safe to read on the client thread, so the search runs
 * there a slice at a time via step() rather than on a worker.
 *
 * With Dig rules (SpotFinder's Auto Mine, which walks the route itself) the
 * goal is an exact cell, a box around it may only be entered through the
 * cells it allows, mining is priced near walking, and safety is stricter:
 * no water at all, nothing occupied beside lava, nothing mined under sand
 * or gravel, and no pillaring.
 */
@Environment(EnvType.CLIENT)
final class FarmRoute {
    enum Status { SEARCHING, FOUND, PARTIAL, FAILED }

    /** Where the feet stand at this point, the blocks to break to get there, and whether a block is placed to stand on. */
    record Step(int x, int y, int z, int[] mined, boolean placed) {}

    private static final int CLEAR = 1;        // nothing to collide with, safe to occupy
    private static final int SOLID = 2;        // can be stood on
    private static final int WATER = 4;
    private static final int LAVA = 8;
    private static final int BREAKABLE = 16;   // can be mined out of the way
    private static final int BAD_FLOOR = 32;   // hurts to stand on
    private static final int DANGER = 64;      // never occupy
    private static final int UNLOADED = 128;   // outside loaded chunks: unknown, treated as a wall
    private static final int FALLING = 256;    // sand, gravel: drops into a hole mined under it
    private static final int COST_SHIFT = 9;   // mining cost * 4, above the flag bits

    private static final double INF = Double.POSITIVE_INFINITY;
    private static final int MAX_FALL = 3;              // more than 3 blocks starts costing hearts
    private static final int MAX_WATER_FALL = 24;
    private static final int MAX_EXPANSIONS = 40_000;
    private static final int MAX_DIG_EXPANSIONS = 60_000;
    private static final int MARGIN = 40;
    private static final int Y_MARGIN = 24;
    // Strongly greedy. Tuned on synthetic worlds: at 1.6 a base 45 blocks down
    // ran out of nodes; at 4.0 it takes ~10k, at the price of sometimes digging
    // through a small hill rather than walking round it.
    private static final double WEIGHT = 4.0;
    private static final double MAX_HARDNESS = 50.0;    // obsidian; bedrock (-1) and harder are walls
    // Mining priced in walking steps: stone is about 2.4 (roughly what a
    // pickaxe takes), obsidian about 31, so short detours still beat digging
    // but a 30-block dig down is not so dear that the search floods every
    // alternative tunnel before trying it.
    private static final double MINE_BASE = 1.0;
    private static final double MINE_PER_HARDNESS = 0.6;
    private static final double MINE_PENALTY = 0.5;
    private static final double PILLAR_COST = 5.0;
    // A dig to a room tunnels nearly the whole way, so mining is priced near
    // walking. At the prices above every block dug costs more than the
    // greedy heuristic gains back, and a 120-block dig floods the search.
    // Here a tunnel step (2 blocks) is ~3.5 and a stair step ~5.3, both under
    // WEIGHT times the distance they cover, so the search runs nearly straight.
    private static final double DIG_MINE_BASE = 0.5;
    private static final double DIG_MINE_PER_HARDNESS = 0.15;
    private static final double DIG_MINE_PENALTY = 0.25;
    private static final double SQRT2 = Math.sqrt(2.0);
    private static final int[][] CARDINALS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] DIAGONALS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    /** Neighbours a mined block can be flooded from: above and the four sides. */
    private static final int[][] AROUND = {{0, 1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};

    private final class_638 world;
    private final int gx;
    private final int gy;
    private final int gz;
    private final boolean allowMining;
    private final Dig dig;
    private final double mineBase;
    private final double minePerHardness;
    private final double minePenalty;
    private final int maxExpansions;
    private final int minX;
    private final int maxX;
    private final int minY;
    private final int maxY;
    private final int minZ;
    private final int maxZ;
    private final class_2338.class_2339 probe = new class_2338.class_2339();
    private final class_2338.class_2339 near = new class_2338.class_2339();
    private final Long2IntOpenHashMap cells = new Long2IntOpenHashMap();
    private final Long2ObjectOpenHashMap<Node> nodes = new Long2ObjectOpenHashMap<>();
    private final PriorityQueue<Entry> open = new PriorityQueue<>((a, b) -> Double.compare(a.f(), b.f()));
    private final Node start;
    private final long[] scratch = new long[3];
    private int scratchCount;
    private Node best;
    private int expansions;
    private Status status = Status.SEARCHING;
    private List<Step> path = List.of();

    private static final class Node {
        final int x;
        final int y;
        final int z;
        double g = INF;
        double h;
        double f;
        Node parent;
        long[] mined;
        boolean placed;
        boolean closed;

        Node(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private record Entry(Node node, double f) {}

    /**
     * What a dig to a room must respect. Cells inside the box (the room and
     * its walls) can be stood on but never mined or entered, except the
     * allowed ones: the door. noMine holds cells the walker found it could
     * not or should not break, so a re-plan goes round them.
     */
    static final class Dig {
        final int minX;
        final int minY;
        final int minZ;
        final int maxX;
        final int maxY;
        final int maxZ;
        final LongOpenHashSet allowed;
        final LongOpenHashSet noMine;

        Dig(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, LongOpenHashSet allowed, LongOpenHashSet noMine) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.allowed = allowed;
            this.noMine = noMine;
        }

        boolean keptOut(int x, int y, int z) {
            return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY && z >= this.minZ && z <= this.maxZ
                    && !this.allowed.contains(key(x, y, z));
        }
    }

    FarmRoute(class_638 world, int sx, int sy, int sz, int gx, int gy, int gz, boolean allowMining) {
        this(world, sx, sy, sz, gx, gy, gz, allowMining, null);
    }

    /** A route that ends with the feet exactly at (gx, gy, gz), under the dig rules. */
    FarmRoute(class_638 world, int sx, int sy, int sz, int gx, int gy, int gz, Dig dig) {
        this(world, sx, sy, sz, gx, gy, gz, true, dig);
    }

    private FarmRoute(class_638 world, int sx, int sy, int sz, int gx, int gy, int gz, boolean allowMining, Dig dig) {
        this.world = world;
        this.gx = gx;
        this.gy = gy;
        this.gz = gz;
        this.allowMining = allowMining;
        this.dig = dig;
        this.mineBase = dig != null ? DIG_MINE_BASE : MINE_BASE;
        this.minePerHardness = dig != null ? DIG_MINE_PER_HARDNESS : MINE_PER_HARDNESS;
        this.minePenalty = dig != null ? DIG_MINE_PENALTY : MINE_PENALTY;
        this.maxExpansions = dig != null ? MAX_DIG_EXPANSIONS : MAX_EXPANSIONS;
        this.minX = Math.min(sx, gx) - MARGIN;
        this.maxX = Math.max(sx, gx) + MARGIN;
        this.minZ = Math.min(sz, gz) - MARGIN;
        this.maxZ = Math.max(sz, gz) + MARGIN;
        this.minY = Math.min(sy, gy) - Y_MARGIN;
        this.maxY = Math.max(sy, gy) + Y_MARGIN;
        this.cells.defaultReturnValue(-1);

        this.start = new Node(sx, sy, sz);
        this.start.g = 0.0;
        this.start.h = this.heuristic(sx, sy, sz);
        this.start.f = WEIGHT * this.start.h;
        this.nodes.put(key(sx, sy, sz), this.start);
        this.open.add(new Entry(this.start, this.start.f));
    }

    Status status() {
        return this.status;
    }

    List<Step> path() {
        return this.path;
    }

    /** Advances the search for about budgetNanos and reports where it stands. */
    Status step(long budgetNanos) {
        if (this.status != Status.SEARCHING) {
            return this.status;
        }
        long deadline = System.nanoTime() + budgetNanos;
        int sinceClock = 0;
        while (!this.open.isEmpty()) {
            if (++sinceClock >= 32) {
                sinceClock = 0;
                if (System.nanoTime() > deadline) {
                    return this.status;
                }
            }
            Entry entry = this.open.poll();
            Node node = entry.node();
            if (node.closed || entry.f() != node.f) {
                continue; // superseded by a cheaper way in
            }
            node.closed = true;
            if (node.h == 0.0) {
                this.finish(node, Status.FOUND);
                return this.status;
            }
            if (this.best == null || node.h < this.best.h) {
                this.best = node;
            }
            if (++this.expansions > this.maxExpansions) {
                break;
            }
            this.expand(node);
        }

        // Nothing reaches it from what is loaded (a sealed vault, or the far
        // side is not sent yet): hand back the leg that gets closest, if it
        // gets meaningfully closer, so the player can walk it and re-plan.
        if (this.best != null && this.best != this.start && this.start.h - this.best.h >= 6.0) {
            this.finish(this.best, Status.PARTIAL);
        } else {
            this.status = Status.FAILED;
            this.path = List.of();
        }
        return this.status;
    }

    private void finish(Node end, Status result) {
        List<Step> steps = new ArrayList<>();
        for (Node n = end; n != null; n = n.parent) {
            int[] mined = null;
            if (n.mined != null) {
                mined = new int[n.mined.length * 3];
                for (int i = 0; i < n.mined.length; i++) {
                    mined[i * 3] = unpackX(n.mined[i]);
                    mined[i * 3 + 1] = unpackY(n.mined[i]);
                    mined[i * 3 + 2] = unpackZ(n.mined[i]);
                }
            }
            steps.add(new Step(n.x, n.y, n.z, mined, n.placed));
        }
        Collections.reverse(steps);
        this.path = steps;
        this.status = result;
        // The search state is large and no longer needed.
        this.nodes.clear();
        this.open.clear();
        this.cells.clear();
    }

    private void expand(Node n) {
        int x = n.x;
        int y = n.y;
        int z = n.z;
        boolean inWater = (this.cell(x, y, z) & WATER) != 0;

        for (int[] d : CARDINALS) {
            int tx = x + d[0];
            int tz = z + d[1];
            int ahead = this.cell(tx, y, tz);

            // Forward: walk, swim or tunnel into the next column, and drop if
            // there is no floor once we are in it.
            this.scratchCount = 0;
            double clear = this.need(tx, y, tz) + this.need(tx, y + 1, tz);
            clear += this.fallRisk(n);
            if (clear < INF) {
                double base = (ahead & WATER) != 0 ? 2.0 : 1.0;
                long[] mined = this.takeScratch();
                if (this.supported(tx, y, tz)) {
                    this.relax(n, tx, y, tz, base + clear, mined, false);
                } else {
                    this.drop(n, tx, y, tz, base + clear, mined);
                }
            }

            // Step up onto the block in front, mining headroom if needed.
            if ((ahead & SOLID) != 0 && (ahead & BAD_FLOOR) == 0) {
                this.scratchCount = 0;
                double up = this.need(x, y + 2, z) + this.need(tx, y + 1, tz) + this.need(tx, y + 2, tz);
                up += this.fallRisk(n);
                if (up < INF) {
                    this.relax(n, tx, y + 1, tz, 2.0 + up, this.takeScratch(), false);
                }
            }

            // Stair down: dig one step into the ground ahead. A plain one-block
            // drop with nothing to mine is the forward move above.
            this.scratchCount = 0;
            double stair = this.need(tx, y - 1, tz) + this.need(tx, y, tz) + this.need(tx, y + 1, tz);
            stair += this.fallRisk(n);
            if (stair > 0.0 && stair < INF && this.supported(tx, y - 1, tz)) {
                this.relax(n, tx, y - 1, tz, 1.5 + stair, this.takeScratch(), false);
            }
        }

        // Diagonals only through open air, so the line cuts corners on the
        // surface without ever planning to clip a block.
        for (int[] d : DIAGONALS) {
            int tx = x + d[0];
            int tz = z + d[1];
            if (this.open(tx, y, tz) && this.open(tx, y + 1, tz)
                    && this.open(tx, y, z) && this.open(tx, y + 1, z)
                    && this.open(x, y, tz) && this.open(x, y + 1, tz)
                    && (this.cell(tx, y, tz) & WATER) == 0
                    && this.supported(tx, y, tz)) {
                this.relax(n, tx, y, tz, SQRT2, null, false);
            }
        }

        // Down: swim down, or break the floor and fall into the hole.
        int below = this.cell(x, y - 1, z);
        if (inWater && (below & CLEAR) != 0) {
            this.relax(n, x, y - 1, z, 1.5, null, false);
        } else if ((below & SOLID) != 0) {
            this.scratchCount = 0;
            double down = this.need(x, y - 1, z);
            if (down < INF) {
                this.drop(n, x, y - 1, z, 1.0 + down, this.takeScratch());
            }
        }

        // Up: swim up, or jump and place a block underneath. Pillaring is only
        // considered while the target is still above us; offered everywhere,
        // every patch of open ground would grow a column of nodes to the top
        // of the search box and swamp it.
        boolean wantUp = this.dig == null && this.gy > y + 2;
        if (inWater || wantUp) {
            this.scratchCount = 0;
            double headroom = this.need(x, y + 2, z);
            if (headroom < INF) {
                if (inWater) {
                    this.relax(n, x, y + 1, z, 1.5 + headroom, this.takeScratch(), false);
                } else {
                    this.relax(n, x, y + 1, z, PILLAR_COST + headroom, this.takeScratch(), true);
                }
            }
        }
    }

    /**
     * Feet at (x, fy, z) with nothing solid under them: fall to the first
     * floor, but only if the whole drop from where the move started is
     * survivable, or ends in water.
     */
    private void drop(Node from, int x, int fy, int z, double cost, long[] mined) {
        for (int k = 0; k <= MAX_WATER_FALL; k++) {
            int y = fy - k;
            if (k > 0) {
                int c = this.cell(x, y, z);
                if ((c & CLEAR) == 0) {
                    return;
                }
                if ((c & WATER) != 0) {
                    this.relax(from, x, y, z, cost + (from.y - y) * 0.5, mined, false);
                    return;
                }
            }
            int floor = this.cell(x, y - 1, z);
            if ((floor & SOLID) != 0) {
                if (from.y - y <= MAX_FALL && (floor & BAD_FLOOR) == 0) {
                    this.relax(from, x, y, z, cost + (from.y - y) * 0.5, mined, false);
                }
                return;
            }
            if ((floor & CLEAR) == 0) {
                return; // lava, fire, cobweb or the edge of the loaded world
            }
        }
    }

    private void relax(Node from, int x, int y, int z, double cost, long[] mined, boolean placed) {
        if (x < this.minX || x > this.maxX || z < this.minZ || z > this.maxZ || y < this.minY || y > this.maxY) {
            return;
        }
        double g = from.g + cost;
        long k = key(x, y, z);
        Node node = this.nodes.get(k);
        if (node == null) {
            node = new Node(x, y, z);
            node.h = this.heuristic(x, y, z);
            this.nodes.put(k, node);
        } else if (node.closed || g >= node.g) {
            return;
        }
        node.g = g;
        node.f = g + WEIGHT * node.h;
        node.parent = from;
        node.mined = mined;
        node.placed = placed;
        this.open.add(new Entry(node, node.f));
    }

    /**
     * Distance from standing here to standing beside the target: within one
     * block sideways (diagonals count) and with the target somewhere between
     * the block under our feet and the one over our head. Zero means arrived.
     */
    private double heuristic(int x, int y, int z) {
        if (this.dig != null) {
            double dx = x - this.gx;
            double dy = y - this.gy;
            double dz = z - this.gz;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        double dx = Math.max(0, Math.abs(x - this.gx) - 1);
        double dz = Math.max(0, Math.abs(z - this.gz) - 1);
        double dy = this.gy > y + 2 ? this.gy - (y + 2) : (this.gy < y - 1 ? (y - 1) - this.gy : 0);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private boolean open(int x, int y, int z) {
        return (this.cell(x, y, z) & CLEAR) != 0;
    }

    /** Feet at (x, y, z) are held up by a floor, or are swimming. */
    private boolean supported(int x, int y, int z) {
        if ((this.cell(x, y, z) & WATER) != 0) {
            return true;
        }
        int floor = this.cell(x, y - 1, z);
        if ((floor & WATER) != 0 && (floor & CLEAR) != 0) {
            return true;
        }
        return (floor & SOLID) != 0 && (floor & BAD_FLOOR) == 0;
    }

    /** Cost to make a cell safe to stand in, recording it as mined when that costs anything. */
    private double need(int x, int y, int z) {
        double cost = this.clearCost(x, y, z);
        if (cost > 0.0 && cost < INF && this.scratchCount < this.scratch.length) {
            this.scratch[this.scratchCount++] = key(x, y, z);
        }
        return cost;
    }

    /**
     * On a dig, a move that mines a block with sand or gravel on top drops
     * it into the tunnel, onto the player's head if they are under it.
     * Blocks the move mines anyway, and the cells we are standing in (the
     * shaft we came down), are not a risk.
     */
    private double fallRisk(Node n) {
        if (this.dig == null) {
            return 0.0;
        }
        for (int i = 0; i < this.scratchCount; i++) {
            long k = this.scratch[i];
            int x = unpackX(k);
            int y = unpackY(k) + 1;
            int z = unpackZ(k);
            if (x == n.x && z == n.z && (y == n.y || y == n.y + 1)) {
                continue;
            }
            long above = key(x, y, z);
            boolean minedToo = false;
            for (int j = 0; j < this.scratchCount; j++) {
                if (this.scratch[j] == above) {
                    minedToo = true;
                    break;
                }
            }
            if (!minedToo && (this.cell(x, y, z) & FALLING) != 0) {
                return INF;
            }
        }
        return 0.0;
    }

    private long[] takeScratch() {
        return this.scratchCount == 0 ? null : Arrays.copyOf(this.scratch, this.scratchCount);
    }

    private double clearCost(int x, int y, int z) {
        int c = this.cell(x, y, z);
        if ((c & CLEAR) != 0) {
            return 0.0;
        }
        if (!this.allowMining || (c & (DANGER | UNLOADED)) != 0 || (c & BREAKABLE) == 0) {
            return INF;
        }
        double cost = (c >>> COST_SHIFT) / 4.0 + this.minePenalty;
        // Opening a block beside lava floods the tunnel with it; beside water
        // it is only a nuisance, unless we are digging the tunnel ourselves.
        for (int[] o : AROUND) {
            int n = this.cell(x + o[0], y + o[1], z + o[2]);
            if ((n & LAVA) != 0) {
                return INF;
            }
            if ((n & WATER) != 0) {
                if (this.dig != null) {
                    return INF;
                }
                if ((n & CLEAR) != 0) {
                    cost += 3.0;
                }
            }
        }
        return cost;
    }

    private int cell(int x, int y, int z) {
        long k = key(x, y, z);
        int c = this.cells.get(k);
        if (c == -1) {
            c = this.classify(x, y, z);
            this.cells.put(k, c);
        }
        return c;
    }

    private int classify(int x, int y, int z) {
        if (!this.world.method_8393(x >> 4, z >> 4)) {
            return UNLOADED;
        }
        this.probe.method_10103(x, y, z);
        class_2680 state = this.world.method_8320(this.probe);
        class_3610 fluid = state.method_26227();
        if (fluid.method_15767(class_3486.field_15518)) {
            return LAVA | DANGER;
        }
        class_2248 block = state.method_26204();
        if (block == class_2246.field_10036 || block == class_2246.field_22089) {
            return DANGER; // fire, soul fire
        }
        boolean water = fluid.method_15767(class_3486.field_15517);
        if (this.dig != null && water) {
            return WATER | DANGER; // no swimming on a dig: a flooded tunnel is no place to mine
        }
        boolean keptOut = this.dig != null && this.dig.keptOut(x, y, z);

        if (state.method_26220(this.world, this.probe).method_1110()) {
            // No collision, but slow or hurt to stand in: cobweb, berry bush,
            // powder snow, wither rose. Cheap to clear instead.
            if (block == class_2246.field_10343 || block == class_2246.field_16999
                    || block == class_2246.field_27879 || block == class_2246.field_10606) {
                return keptOut ? DANGER : this.bits(0.0F);
            }
            if (this.dig != null && (keptOut || this.lavaBeside(x, y, z))) {
                return DANGER; // never step into the room's walls, or stand next to lava
            }
            return CLEAR | (water ? WATER : 0);
        }

        int flags = SOLID | (water ? WATER : 0);
        if (block == class_2246.field_10092 || block == class_2246.field_10029 || block == class_2246.field_28048) {
            flags |= BAD_FLOOR; // magma, cactus, pointed dripstone
        }
        if (block instanceof class_2346) {
            flags |= FALLING;
        }
        if (keptOut || (this.dig != null && this.dig.noMine.contains(key(x, y, z)))) {
            return flags; // can be stood on, never mined
        }
        return flags | this.bits(state.method_26214(this.world, this.probe));
    }

    private boolean lavaBeside(int x, int y, int z) {
        for (int[] o : AROUND) {
            if (this.world.method_8320(this.near.method_10103(x + o[0], y + o[1], z + o[2])).method_26227().method_15767(class_3486.field_15518)) {
                return true;
            }
        }
        return this.world.method_8320(this.near.method_10103(x, y - 1, z)).method_26227().method_15767(class_3486.field_15518);
    }

    /** BREAKABLE plus the mining cost for a block of this hardness, or nothing if it cannot be mined. */
    private int bits(float hardness) {
        if (hardness < 0.0F || hardness > MAX_HARDNESS) {
            return 0;
        }
        return BREAKABLE | ((int) Math.round((this.mineBase + hardness * this.minePerHardness) * 4.0) << COST_SHIFT);
    }

    /** x in the top 26 bits, z in the next 26, y in the low 12; all signed. */
    static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    static int unpackX(long k) {
        return (int) (k >> 38);
    }

    static int unpackZ(long k) {
        return (int) ((k << 26) >> 38);
    }

    static int unpackY(long k) {
        return (int) ((k << 52) >> 52);
    }
}
