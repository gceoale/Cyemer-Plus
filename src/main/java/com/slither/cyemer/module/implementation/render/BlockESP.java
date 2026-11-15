package com.slither.cyemer.module.implementation.render;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.module.BlockListSetting;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ColorSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.render.RenderUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1937;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
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
 * Highlights every block of the picked types in the loaded world, through
 * walls. Respawn anchors by default; any block can be picked from the menu
 * in its settings.
 *
 * Finding them reads only the chunk data the server already sent. Each
 * 16x16x16 section's palette is checked first, so a section without any
 * picked block is skipped without touching a single block, and only
 * sections that have one are read in full. Chunks are scanned nearest
 * first, a couple of milliseconds per tick, and rescanned when they reload;
 * single block changes arrive through ClientWorldMixin, so an anchor that
 * is placed, charged or blown up updates at once.
 */
@Environment(EnvType.CLIENT)
public class BlockESP extends Module {
    private static final int SCAN_RADIUS = 32;           // chunks; anything loaded is inside this
    private static final long SCAN_BUDGET_NS = 2_000_000L;
    private static final int PER_CHUNK_CAP = 4096;        // picking stone should not eat all memory
    private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private static BlockESP instance;

    private final BlockListSetting blocks = new BlockListSetting("Blocks", "minecraft:respawn_anchor");
    private final ColorSetting color = new ColorSetting("Color", new Color(170, 60, 255));
    private final SliderSetting opacity = new SliderSetting("Opacity", 0.45, 0.1, 1.0, 2);
    private final BooleanSetting tracers = new BooleanSetting("Tracers", false);
    private final BooleanSetting notify = new BooleanSetting("Notify", false);
    private final SliderSetting maxShown = new SliderSetting("Max Shown", 512.0, 16.0, 4096.0, 0);

    /** Matching positions per chunk, both as packed longs. */
    private final Map<Long, LongOpenHashSet> found = new HashMap<>();
    /** The chunk object each entry was read from, so a reloaded chunk is noticed and reread. */
    private final Map<Long, class_2818> scanned = new HashMap<>();
    private final ArrayDeque<long[]> queue = new ArrayDeque<>();
    private long[] shown = new long[0];
    private class_1937 lastWorld = null;
    private int seenVersion = -1;
    private int ticks = 0;

    // New finds waiting to be announced together.
    private int pendingNew = 0;
    private long pendingNearest = 0L;
    private double pendingNearestSq = Double.MAX_VALUE;
    private final Map<String, Integer> pendingKinds = new HashMap<>();
    private long lastNotifyAt = 0L;
    private boolean initialPass = true;
    private boolean scanning = false;

    public BlockESP() {
        super("BlockESP", "Highlights chosen blocks through walls across your render distance.", Category.RENDER);
        this.addSetting(this.blocks);
        this.addSetting(this.color);
        this.addSetting(this.opacity);
        this.addSetting(this.tracers);
        this.addSetting(this.notify);
        this.addSetting(this.maxShown);
        instance = this;
    }

    public static BlockESP getInstance() {
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
        this.found.clear();
        this.scanned.clear();
        this.queue.clear();
        this.shown = new long[0];
        this.pendingNew = 0;
        this.pendingKinds.clear();
        this.pendingNearestSq = Double.MAX_VALUE;
        this.initialPass = true;
        this.scanning = false;
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
        int version = this.blocks.getVersion();
        if (version != this.seenVersion) {
            this.seenVersion = version;
            this.reset();
        }
        Set<class_2248> targets = this.blocks.getBlocks();
        if (targets.isEmpty()) {
            return;
        }
        this.ticks++;

        if (this.queue.isEmpty() && this.ticks % 10 == 0) {
            this.refillQueue(world, self);
            this.scanning = !this.queue.isEmpty();
        }
        if (this.ticks % 20 == 0) {
            this.dropUnloaded(world);
        }

        long deadline = System.nanoTime() + SCAN_BUDGET_NS;
        Predicate<class_2680> match = state -> targets.contains(state.method_26204());
        while (!this.queue.isEmpty() && System.nanoTime() < deadline) {
            long[] next = this.queue.poll();
            int cx = (int) next[0];
            int cz = (int) next[1];
            if (world.method_8393(cx, cz)) {
                this.scanChunk(world.method_8497(cx, cz), cx, cz, match, self);
            }
        }
        // What was already around when we joined (or changed the picks) is not
        // news; after the first full pass, new finds are.
        if (this.scanning && this.queue.isEmpty()) {
            this.scanning = false;
            this.initialPass = false;
        }

        if (this.ticks % 5 == 0) {
            this.rebuildShown(self);
        }
        this.flushNotify(self);
    }

    /** Queues loaded chunks that are new or have been reloaded since they were read, nearest first. */
    private void refillQueue(class_638 world, class_746 self) {
        int pcx = (int) Math.floor(self.method_23317()) >> 4;
        int pcz = (int) Math.floor(self.method_23321()) >> 4;
        List<long[]> todo = new ArrayList<>();
        for (int cx = pcx - SCAN_RADIUS; cx <= pcx + SCAN_RADIUS; cx++) {
            for (int cz = pcz - SCAN_RADIUS; cz <= pcz + SCAN_RADIUS; cz++) {
                if (!world.method_8393(cx, cz)) {
                    continue;
                }
                if (this.scanned.get(chunkKey(cx, cz)) != world.method_8497(cx, cz)) {
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
        Iterator<Map.Entry<Long, class_2818>> it = this.scanned.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, class_2818> entry = it.next();
            long key = entry.getKey();
            int cx = (int) (key >> 32);
            int cz = (int) key;
            if (!world.method_8393(cx, cz) || world.method_8497(cx, cz) != entry.getValue()) {
                it.remove();
                this.found.remove(key);
            }
        }
    }

    private void scanChunk(class_2818 chunk, int cx, int cz, Predicate<class_2680> match, class_746 self) {
        long key = chunkKey(cx, cz);
        LongOpenHashSet before = this.found.get(key);
        LongOpenHashSet hits = new LongOpenHashSet();
        class_2826[] sections = chunk.method_12006();
        outer:
        for (int i = 0; i < sections.length; i++) {
            class_2826 section = sections[i];
            // Palette check: skips every section without a picked block at once.
            if (section == null || section.method_38292() || !section.method_19523(match)) {
                continue;
            }
            int baseX = cx << 4;
            int baseY = chunk.method_31604(i) << 4;
            int baseZ = cz << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (match.test(section.method_12254(x, y, z))) {
                            long pos = pack(baseX + x, baseY + y, baseZ + z);
                            hits.add(pos);
                            if (before == null || !before.contains(pos)) {
                                this.noteNew(pos, section.method_12254(x, y, z).method_26204(), self);
                            }
                            if (hits.size() >= PER_CHUNK_CAP) {
                                break outer;
                            }
                        }
                    }
                }
            }
        }
        if (hits.isEmpty()) {
            this.found.remove(key);
        } else {
            this.found.put(key, hits);
        }
        this.scanned.put(key, chunk);
    }

    /** From ClientWorldMixin: a block in a loaded chunk changed. */
    public void onBlockUpdate(class_2338 pos, class_2680 state) {
        if (!this.isEnabled() || Cyemer.selfDestructed) {
            return;
        }
        int x = pos.method_10263();
        int y = pos.method_10264();
        int z = pos.method_10260();
        long key = chunkKey(x >> 4, z >> 4);
        if (!this.scanned.containsKey(key)) {
            return; // not read yet; the scan will see the new state
        }
        long packed = pack(x, y, z);
        if (this.blocks.contains(state.method_26204())) {
            LongOpenHashSet set = this.found.computeIfAbsent(key, k -> new LongOpenHashSet());
            if (set.size() < PER_CHUNK_CAP && set.add(packed) && this.mc.field_1724 != null) {
                this.noteNew(packed, state.method_26204(), this.mc.field_1724);
            }
        } else {
            LongOpenHashSet set = this.found.get(key);
            if (set != null && set.remove(packed) && set.isEmpty()) {
                this.found.remove(key);
            }
        }
    }

    /** Keeps the nearest Max Shown matches for rendering; recomputed a few times a second, not every frame. */
    private void rebuildShown(class_746 self) {
        int limit = (int) this.maxShown.getValue();
        double px = self.method_23317();
        double py = self.method_23318();
        double pz = self.method_23321();
        // Max-heap on distance, so the farthest of the kept ones is the one evicted.
        PriorityQueue<Near> heap = new PriorityQueue<>((a, b) -> Double.compare(b.distSq(), a.distSq()));
        for (LongOpenHashSet set : this.found.values()) {
            for (long pos : set) {
                double dx = unpackX(pos) + 0.5 - px;
                double dy = unpackY(pos) + 0.5 - py;
                double dz = unpackZ(pos) + 0.5 - pz;
                double d = dx * dx + dy * dy + dz * dz;
                if (heap.size() < limit) {
                    heap.add(new Near(pos, d));
                } else if (d < heap.peek().distSq()) {
                    heap.poll();
                    heap.add(new Near(pos, d));
                }
            }
        }
        long[] next = new long[heap.size()];
        int i = 0;
        for (Near entry : heap) {
            next[i++] = entry.pos();
        }
        this.shown = next;
    }

    private void noteNew(long pos, class_2248 block, class_746 self) {
        if (!this.notify.isEnabled() || this.initialPass) {
            return; // what was already there when we joined is not news
        }
        this.pendingNew++;
        this.pendingKinds.merge(block.method_9518().getString(), 1, Integer::sum);
        double dx = unpackX(pos) + 0.5 - self.method_23317();
        double dy = unpackY(pos) + 0.5 - self.method_23318();
        double dz = unpackZ(pos) + 0.5 - self.method_23321();
        double d = dx * dx + dy * dy + dz * dz;
        if (d < this.pendingNearestSq) {
            this.pendingNearestSq = d;
            this.pendingNearest = pos;
        }
    }

    /** At most one line a second, summing everything new since the last one. */
    private void flushNotify(class_746 self) {
        long now = System.currentTimeMillis();
        if (this.pendingNew == 0 || now - this.lastNotifyAt < 1000L) {
            return;
        }
        this.lastNotifyAt = now;
        StringBuilder kinds = new StringBuilder();
        for (Map.Entry<String, Integer> e : this.pendingKinds.entrySet()) {
            if (kinds.length() > 0) {
                kinds.append(", ");
            }
            kinds.append(e.getValue()).append(' ').append(e.getKey());
        }
        int x = unpackX(this.pendingNearest);
        int y = unpackY(this.pendingNearest);
        int z = unpackZ(this.pendingNearest);
        double dx = x + 0.5 - self.method_23317();
        double dz = z + 0.5 - self.method_23321();
        self.method_7353(class_2561.method_43470("§5[BlockESP]§r §e" + kinds + "§r §7nearest "
                + (int) Math.round(Math.sqrt(this.pendingNearestSq)) + "m " + compass(dx, dz)
                + " §8(" + x + ", " + y + ", " + z + ")"), false);
        this.pendingNew = 0;
        this.pendingKinds.clear();
        this.pendingNearestSq = Double.MAX_VALUE;
    }

    @Override
    public void onWorldRender(class_4587 matrices, float tickDelta) {
        long[] list = this.shown;
        class_746 self = this.mc.field_1724;
        if (list.length == 0 || self == null || Cyemer.selfDestructed) {
            return;
        }
        class_243 cam = this.mc.field_1773.method_19418().method_71156();
        double cx = cam.field_1352;
        double cy = cam.field_1351;
        double cz = cam.field_1350;
        Color c = this.color.getValue();
        float alpha = (float) this.opacity.getValue();
        boolean lines = this.tracers.isEnabled();
        class_243 look = self.method_5828(tickDelta);
        class_243 start = new class_243(look.field_1352 * 0.6, look.field_1351 * 0.6, look.field_1350 * 0.6);

        try (class_9799 allocator = new class_9799(16384)) {
            class_4598 consumers = class_4597.method_22991(allocator);
            for (long pos : list) {
                // Camera-relative in double before the cast to float vertices.
                double x = unpackX(pos) - cx;
                double y = unpackY(pos) - cy;
                double z = unpackZ(pos) - cz;
                RenderUtils.drawBoxEsp(matrices, consumers,
                        new class_238(x - 0.002, y - 0.002, z - 0.002, x + 1.002, y + 1.002, z + 1.002), c, alpha, true);
                if (lines) {
                    RenderUtils.drawLine(matrices, consumers, start, new class_243(x + 0.5, y + 0.5, z + 0.5), c, 0.8F, true);
                }
            }
            consumers.method_22993();
        }
    }

    private record Near(long pos, double distSq) {}

    private static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    /** x in the top 26 bits, z in the next 26, y in the low 12; all signed. */
    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    private static int unpackX(long k) {
        return (int) (k >> 38);
    }

    private static int unpackZ(long k) {
        return (int) ((k << 26) >> 38);
    }

    private static int unpackY(long k) {
        return (int) ((k << 52) >> 52);
    }

    private static String compass(double dx, double dz) {
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        if (angle < 0.0) {
            angle += 360.0;
        }
        return COMPASS[(int) Math.round(angle / 45.0) % 8];
    }
}
