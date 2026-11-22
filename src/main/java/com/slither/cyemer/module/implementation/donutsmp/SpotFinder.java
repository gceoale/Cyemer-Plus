package com.slither.cyemer.module.implementation.donutsmp;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.event.BlockUpdateListener;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ColorSetting;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.module.implementation.donutsmp.DigKit.Keys.Key;
import com.slither.cyemer.util.RotationManager;
import com.slither.cyemer.util.render.RenderUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1661;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2346;
import net.minecraft.class_2350;
import net.minecraft.class_239;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_2680;
import net.minecraft.class_2818;
import net.minecraft.class_2826;
import net.minecraft.class_332;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
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
 *
 * Auto Mine does the whole job with nobody at the keys: it digs its own
 * route down to the nearest spot's door, mines the room out, then walls the
 * door back up (from inside or outside) so the room is sealed again, with
 * an ETA on screen the whole way. It plays like a player: the camera turns,
 * forward, jump and attack are held, and attack only ever while the
 * crosshair is on the block it means to break. Vanilla sends every packet.
 */
@Environment(EnvType.CLIENT)
public class SpotFinder extends Module implements BlockUpdateListener {
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
    private final ModeSetting seal = new ModeSetting("Seal Entrance", "Inside", "Outside", "Off");
    private final SliderSetting minHealth = new SliderSetting("Stop Below HP", 8.0, 0.0, 19.0, 0);
    private final BooleanSetting etaHud = new BooleanSetting("ETA HUD", true);
    /** Shows what Auto Mine would dig, for digging it yourself. */
    private final BooleanSetting highlight = new BooleanSetting("Highlight Mode", false);

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

    private enum Phase { TRAVEL, MINE, LEAVE, SEAL }

    /** The room cell the tunnel comes in by, at floor level, and the way into the room from it. */
    private record Door(int x, int z, int dx, int dz) {}

    /** Where to click to put a block into an opening: a face of a block beside it. */
    private record Place(int x, int y, int z, class_2350 side, class_243 point) {}

    private Spot mining = null;
    private Phase phase = Phase.TRAVEL;
    private List<Door> doors = List.of();
    private Door door = null;
    private int doorTry = 0;
    private DigWalker walker = null;
    private final DigKit.Keys keys = new DigKit.Keys();
    private final LongOpenHashSet noMine = new LongOpenHashSet();
    private final Set<Spot> unreachable = new HashSet<>();
    private Aim mineTarget = null;
    private long targetSince = 0L;
    private final Map<Long, Long> skipped = new HashMap<>();
    private String lastHint = null;
    private boolean wasAuto = false;
    private long stepSince = 0L;
    private int lastLeaks = -1;
    private int descendTo = Integer.MIN_VALUE;
    private boolean doorSealed = false;
    private int doorOpen = -1;
    private long placingSince = 0L;
    private long sealRetryAt = 0L;
    private double lastPosX = Double.NaN;
    private long progressAt = 0L;
    private double progX = Double.NaN;
    private double progY = Double.NaN;
    private double progZ = Double.NaN;
    private double lastPosY = Double.NaN;
    private double lastPosZ = Double.NaN;

    // Highlight Mode
    private Spot guideSpot = null;
    private Door guideDoor = null;
    private FarmRoute guideSearch = null;
    private List<FarmRoute.Step> guidePath = List.of();
    private boolean guidePartial = false;
    private boolean guideFailed = false;
    private long guidePlannedAt = 0L;
    private int guideIndex = 0;
    private int[] guideNext = null;
    private List<int[]> guideRoom = List.of();
    private List<int[]> guideSeal = List.of();
    private int guideRouteLeft = 0;
    private int guideRoomLeft = 0;
    private int guideLayer = 0;
    private double guideEta = Double.NaN;

    // ETA: predicted seconds left, scaled by how fast the work has really gone so far.
    private long jobStarted = 0L;
    private double eta = Double.NaN;
    private long etaAt = 0L;
    private double lastPredicted = Double.NaN;
    private long lastEtaAt = 0L;
    private int lastEtaVersion = -1;
    private double predDone = 0.0;
    private double actualDone = 0.0;
    private int blocksLeft = 0;
    private boolean etaAnnounced = false;

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
        this.addSetting(this.highlight);
        this.addSetting(this.turnSpeed);
        this.addSetting(this.autoWalk);
        this.addSetting(this.bestTool);
        this.addSetting(this.seal);
        this.addSetting(this.minHealth);
        this.addSetting(this.etaHud);
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
        this.forgetJob();
        this.clearGuide();
        this.wasAuto = false;
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
            this.forgetJob();
            this.clearGuide();
            this.unreachable.clear();
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
            if (!this.wasAuto) {
                this.wasAuto = true;
                this.stepSince = System.currentTimeMillis();
                this.lastEtaAt = 0L;
                this.lastPosX = Double.NaN;
                this.descendTo = Integer.MIN_VALUE;
                this.progX = Double.NaN;
            }
            try {
                this.tickAuto(world, self);
            } catch (RuntimeException e) {
                // Module ticks are not guarded: a bug here must not take the game down.
                this.abort("§cAuto Mine stopped§r §7- internal error: " + e);
            }
        } else if (this.wasAuto) {
            this.wasAuto = false;
            this.stopMining();
            this.lastHint = null;
            if (this.mining != null) {
                this.say("§7Auto Mine paused. Turn it back on to carry on with this spot (turn SpotFinder off to forget it)");
            }
        }

        if (this.highlight.isEnabled() && !this.autoMine.isEnabled()) {
            try {
                this.tickGuide(world, self);
            } catch (RuntimeException e) {
                this.clearGuide();
            }
        } else if (this.guideSpot != null) {
            this.clearGuide();
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
     * One step of the job: dig to the door (TRAVEL), mine the room out
     * (MINE), walk out into the tunnel when sealing from outside (LEAVE),
     * then wall the door up (SEAL).
     */
    private void tickAuto(class_638 world, class_746 self) {
        if (this.mc.field_1755 != null) {
            // A screen is open (chat, inventory, the pause menu): hands off, camera included.
            this.keys.releaseAll();
            RotationManager.stop(this);
            this.lastEtaAt = 0L;
            this.lastPosX = Double.NaN;
            return;
        }
        String danger = this.danger(self);
        if (danger != null) {
            this.abort("§cAuto Mine stopped§r §7- " + danger);
            return;
        }
        double px = self.method_23317();
        double py = self.method_23318();
        double pz = self.method_23321();
        boolean jumped = !Double.isNaN(this.lastPosX)
                && (px - this.lastPosX) * (px - this.lastPosX) + (py - this.lastPosY) * (py - this.lastPosY) + (pz - this.lastPosZ) * (pz - this.lastPosZ) > 24.0 * 24.0;
        this.lastPosX = px;
        this.lastPosY = py;
        this.lastPosZ = pz;
        if (jumped && this.mining != null) {
            this.abort("§cAuto Mine stopped§r §7- the player was teleported away from the room");
            return;
        }
        if (this.mining == null && !this.startJob(self)) {
            return;
        }
        if (this.phase == Phase.TRAVEL && this.walker == null) {
            this.beginTravel(self);
            if (this.mining == null) {
                return;
            }
        }
        Spot s = this.mining;
        switch (this.phase) {
            case TRAVEL -> this.tickTravel(world, self);
            case MINE -> this.tickMine(world, self, s);
            case LEAVE -> this.tickLeave(self, s);
            case SEAL -> this.tickSeal(world, self, s);
        }
        if (this.mining != null && this.ticks % 10 == 0) {
            this.updateEta(world);
        }
    }

    private String danger(class_746 self) {
        float hp = self.method_6032();
        if (this.minHealth.getValue() > 0.0 && hp <= this.minHealth.getValue()) {
            return "health is down to " + Math.round(hp) + " HP";
        }
        if (self.method_5771()) {
            return "in lava";
        }
        if (self.method_5809()) {
            return "on fire";
        }
        return null;
    }

    private boolean startJob(class_746 self) {
        Spot pick = null;
        for (Spot s : this.spots) {
            if (!this.unreachable.contains(s)) {
                pick = s;
                break;
            }
        }
        if (pick == null) {
            this.keys.releaseAll();
            this.hint(this.spots.isEmpty() ? "§7Auto Mine is waiting for a spot to be found"
                    : "§7Auto Mine: no spot in range can be reached, try somewhere else");
            return false;
        }
        this.mining = pick;
        this.phase = Phase.TRAVEL;
        this.doors = this.doorsFor(pick, self.method_23317(), self.method_23321());
        this.doorTry = 0;
        this.door = null;
        this.walker = null;
        this.noMine.clear();
        this.skipped.clear();
        this.jobStarted = System.currentTimeMillis();
        this.stepSince = this.jobStarted;
        this.eta = Double.NaN;
        this.lastPredicted = Double.NaN;
        this.lastEtaAt = 0L;
        this.lastEtaVersion = -1;
        this.predDone = 0.0;
        this.actualDone = 0.0;
        this.etaAnnounced = false;
        this.descendTo = Integer.MIN_VALUE;
        this.doorSealed = false;
        this.doorOpen = -1;
        this.placingSince = 0L;
        this.sealRetryAt = 0L;
        this.progX = Double.NaN;
        this.say("§aAuto Mine§r §7- " + this.describe(pick, self));
        if ("Outside".equals(this.seal.getCurrentMode()) && pick.h() > 3) {
            this.say("§7Sealing from outside needs a room 3 or less tall to walk back out of, so this one is sealed from inside");
        }
        return true;
    }

    /**
     * Sets up the dig to the current door. The room and its walls are off
     * limits to the route except the door's own cells: a straight tunnel
     * one wide and two tall through the wall at the top layer's level, so
     * the only opening the job ever makes is the one it walls up.
     */
    private void beginTravel(class_746 self) {
        Spot s = this.mining;
        if (this.insideRoom(self, s) || !this.autoWalk.isEnabled()) {
            this.phase = Phase.MINE;
            this.walker = null;
            return;
        }
        if (this.doorTry >= this.doors.size()) {
            this.say("§cCan't dig into this spot from any side§r §7- trying the next one");
            this.unreachable.add(s);
            this.stopMining();
            this.forgetJob();
            return;
        }
        Door d = this.doors.get(this.doorTry);
        this.door = d;
        int[] goal = this.goalFor(s, d);
        this.walker = new DigWalker(this, this.keys, this.turnSpeed::getValue, this.bestTool::isEnabled, goal[0], goal[1], goal[2],
                this.rulesFor(s, d, this.noMine));
        this.phase = Phase.TRAVEL;
    }

    /** The room and its walls are off limits to a route, except the door: one wide, two tall, through the wall. */
    private FarmRoute.Dig rulesFor(Spot s, Door d, LongOpenHashSet banned) {
        int t = this.thickness();
        int floor = s.y();
        int level = doorLevel(s);
        LongOpenHashSet allowed = new LongOpenHashSet();
        for (int k = 1; k <= t; k++) {
            allowed.add(FarmRoute.key(d.x() - k * d.dx(), level, d.z() - k * d.dz()));
            allowed.add(FarmRoute.key(d.x() - k * d.dx(), level + 1, d.z() - k * d.dz()));
        }
        if (s.h() >= 2) {
            allowed.add(FarmRoute.key(d.x(), level, d.z()));
            allowed.add(FarmRoute.key(d.x(), level + 1, d.z()));
        }
        return new FarmRoute.Dig(s.x() - t, floor - t, s.z() - t,
                s.x() + s.lx() - 1 + t, floor + s.h() - 1 + t, s.z() + s.lz() - 1 + t, allowed, banned);
    }

    /** Where the route ends: in the room when it is tall enough to stand in, else in the doorway. */
    private int[] goalFor(Spot s, Door d) {
        boolean walkIn = s.h() >= 2;
        return new int[]{walkIn ? d.x() : d.x() - d.dx(), doorLevel(s), walkIn ? d.z() : d.z() - d.dz()};
    }

    /** A door on each side, at the point of that side nearest us; nearest first. */
    private List<Door> doorsFor(Spot s, double px, double pz) {
        int x0 = s.x();
        int x1 = s.x() + s.lx() - 1;
        int z0 = s.z();
        int z1 = s.z() + s.lz() - 1;
        int ix = Math.max(x0, Math.min(x1, (int) Math.floor(px)));
        int iz = Math.max(z0, Math.min(z1, (int) Math.floor(pz)));
        List<Door> out = new ArrayList<>(List.of(
            new Door(x0, iz, 1, 0),
            new Door(x1, iz, -1, 0),
            new Door(ix, z0, 0, 1),
            new Door(ix, z1, 0, -1)));
        out.sort(Comparator.comparingDouble(d -> {
            double dx = d.x() - 2 * d.dx() + 0.5 - px;
            double dz = d.z() - 2 * d.dz() + 0.5 - pz;
            return dx * dx + dz * dz;
        }));
        return out;
    }

    private void tickTravel(class_638 world, class_746 self) {
        DigWalker w = this.walker;
        DigWalker.Result r = w.tick(world, self);
        if (r == DigWalker.Result.ARRIVED) {
            this.walker = null;
            this.phase = Phase.MINE;
            this.progX = Double.NaN;
            int n = layers(this.mining).length;
            this.say("§7At the room, mining it out" + (n > 1 ? " from the top down in " + n + " layers" : ""));
        } else if (r == DigWalker.Result.FAILED) {
            if (w.fatal()) {
                this.abort("§cAuto Mine stopped§r §7- " + w.failure());
                return;
            }
            this.walker = null;
            this.doorTry++;
            if (this.doorTry < this.doors.size()) {
                this.say("§7Can't get in on that side (" + w.failure() + "), trying another");
            }
            this.beginTravel(self);
        }
    }

    /** Feet heights the room is mined from, top down: 3-block layers, each mined standing on the one below. */
    private static int[] layers(Spot s) {
        int floor = s.y();
        int f = doorLevel(s);
        List<Integer> out = new ArrayList<>();
        out.add(f);
        while (f > floor) {
            f = Math.max(floor, f - 3);
            out.add(f);
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    /** The door comes in on the top layer, so the whole room is mined from the top down. */
    private static int doorLevel(Spot s) {
        return Math.max(s.y(), s.y() + s.h() - 3);
    }

    /** The next layer below feet height y, or MIN_VALUE. */
    private static int layerBelow(Spot s, int y) {
        for (int f : layers(s)) {
            if (f < y) {
                return f;
            }
        }
        return Integer.MIN_VALUE;
    }

    private boolean sealsOutside(Spot s) {
        return "Outside".equals(this.seal.getCurrentMode()) && s.h() <= 3;
    }

    /** Inside sealing walls the door up right after we get in, so the room stays sealed while it is mined. */
    private boolean sealsEarly(Spot s) {
        return !"Off".equals(this.seal.getCurrentMode()) && !this.sealsOutside(s) && s.h() >= 2;
    }

    /**
     * Mines the room from the top down in 3-block layers. On each layer it
     * mines everything at or above its feet it can reach, walks (over the
     * layer below, round any holes) to where there is more, and once the
     * layer is done digs straight down to the next. It never breaks a block
     * below its feet except to go down, and attack is only held while the
     * crosshair is on the block it means to break, so walls and floor stay
     * whole.
     */
    private void tickMine(class_638 world, class_746 self, Spot s) {
        long now = System.currentTimeMillis();
        this.skipped.values().removeIf(until -> until < now);
        int fx = (int) Math.floor(self.method_23317());
        int fy = (int) Math.floor(self.method_23318() + 0.2);
        int fz = (int) Math.floor(self.method_23321());

        // Watchdog: moving or starting on a new block is progress; nothing for long means stuck.
        double mx = self.method_23317() - this.progX;
        double my = self.method_23318() - this.progY;
        double mz = self.method_23321() - this.progZ;
        if (Double.isNaN(this.progX) || mx * mx + my * my + mz * mz > 0.25) {
            this.progX = self.method_23317();
            this.progY = self.method_23318();
            this.progZ = self.method_23321();
            this.progressAt = now;
        } else if (now - this.progressAt > 30_000L) {
            this.abort("§cAuto Mine stopped§r §7- no progress for 30s at (" + fx + ", " + fy + ", " + fz + "). Turn it back on to retry");
            return;
        }

        if (!this.insideRoom(self, s)) {
            // In the doorway of a 1-tall room, or not in yet.
            Aim a = this.pickTarget(world, self, s, Integer.MIN_VALUE);
            if (a != null) {
                this.keys.releaseMovement();
                this.mineAt(world, self, a, now);
                return;
            }
            this.mineTarget = null;
            this.keys.set(Key.ATTACK, false);
            this.keys.releaseMovement();
            int left = this.remaining(world, s);
            if (left == 0) {
                this.roomDone(self, s, 0);
            } else if (!this.autoWalk.isEnabled()) {
                RotationManager.stop(this);
                this.hint("§7Get within reach of the room to start: dig down to (" + s.x() + ", " + doorLevel(s) + ", " + s.z() + ")");
            } else if (s.h() >= 2) {
                this.beginTravel(self);
            } else if (this.skipped.isEmpty()) {
                this.roomDone(self, s, left);
            }
            return;
        }

        if (this.sealsEarly(s) && !this.doorSealed && now >= this.sealRetryAt && this.sealDoor(world, self, s, now)) {
            return;
        }
        if (this.descendTo != Integer.MIN_VALUE) {
            if (fy <= this.descendTo) {
                this.descendTo = Integer.MIN_VALUE;
            } else {
                if (self.method_24828()) {
                    this.descend(world, self, s, fx, fy, fz, now);
                }
                return;
            }
        }

        Aim a = this.pickTarget(world, self, s, fy);
        if (a != null) {
            this.keys.releaseMovement();
            this.mineAt(world, self, a, now);
            return;
        }
        this.mineTarget = null;
        this.keys.set(Key.ATTACK, false);
        if (!self.method_24828()) {
            return;
        }
        if (this.autoWalk.isEnabled() && this.walkToWork(world, self, s, fx, fy, fz)) {
            return;
        }
        this.keys.releaseMovement();
        int below = layerBelow(s, fy);
        if (below != Integer.MIN_VALUE && this.anyBelow(world, s, fy)) {
            if (this.autoWalk.isEnabled()) {
                this.descendTo = below;
            } else {
                this.hint("§7This layer is done: dig down to the next one at Y " + below);
            }
            return;
        }
        int left = this.remaining(world, s);
        if (left == 0) {
            this.roomDone(self, s, 0);
        } else if (this.skipped.isEmpty()) {
            this.roomDone(self, s, left); // what is left cannot be reached from anywhere we can stand
        }
    }

    /**
     * The block to mine next: one we can see and reach at or above minY.
     * The current one is kept while it is still there, and a new one is the
     * one closest to where the camera already points (then the nearest),
     * so the view sweeps across the face instead of flicking about.
     */
    private Aim pickTarget(class_638 world, class_746 self, Spot s, int minY) {
        class_243 eye = self.method_33571();
        double reach = self.method_55754() - 0.25;
        Aim cur = this.mineTarget;
        if (cur != null && cur.y() >= minY && this.minable(world, cur.x(), cur.y(), cur.z()) && !this.skipped.containsKey(pack(cur.x(), cur.y(), cur.z()))) {
            class_243 p = DigKit.visiblePoint(world, self, eye, cur.x(), cur.y(), cur.z(), reach);
            if (p != null) {
                return new Aim(cur.x(), cur.y(), cur.z(), p);
            }
        }
        Aim best = null;
        double bestScore = Double.MAX_VALUE;
        int ex = (int) Math.floor(eye.field_1352);
        int ey = (int) Math.floor(eye.field_1351);
        int ez = (int) Math.floor(eye.field_1350);
        int r = (int) Math.ceil(reach) + 1;
        for (int x = Math.max(s.x(), ex - r); x <= Math.min(s.x() + s.lx() - 1, ex + r); x++) {
            for (int y = Math.max(Math.max(s.y(), minY), ey - r); y <= Math.min(s.y() + s.h() - 1, ey + r); y++) {
                for (int z = Math.max(s.z(), ez - r); z <= Math.min(s.z() + s.lz() - 1, ez + r); z++) {
                    if (!this.minable(world, x, y, z) || this.skipped.containsKey(pack(x, y, z)) || !DigKit.exposed(world, this.probe, x, y, z)) {
                        continue;
                    }
                    class_243 point = DigKit.visiblePoint(world, self, eye, x, y, z, reach);
                    if (point == null) {
                        continue;
                    }
                    double score = DigKit.angleTo(self, eye, point) + 12.0 * Math.sqrt(point.method_1025(eye));
                    if (score < bestScore) {
                        bestScore = score;
                        best = new Aim(x, y, z, point);
                    }
                }
            }
        }
        return best;
    }

    /** Looks at the block and holds attack while the crosshair is on it, after the safety checks. */
    private void mineAt(class_638 world, class_746 self, Aim best, long now) {
        // The spot was sealed when found; make sure it still is before opening this block.
        if (DigKit.fluidBeside(world, this.probe, best.x(), best.y(), best.z())) {
            this.abort("§cAuto Mine stopped§r §7- fluid beside (" + best.x() + ", " + best.y() + ", " + best.z() + "), so nothing floods in");
            return;
        }
        class_2680 state = world.method_8320(this.probe.method_10103(best.x(), best.y(), best.z()));
        if (!DigKit.canHarvest(this.mc, state)) {
            this.abort("§cAuto Mine stopped§r §7- no working pickaxe left in the hotbar");
            return;
        }
        if (this.mineTarget == null || this.mineTarget.x() != best.x() || this.mineTarget.y() != best.y() || this.mineTarget.z() != best.z()) {
            this.targetSince = now;
            this.progressAt = now;
        } else if (now - this.targetSince > Math.max(6000L, DigKit.breakTicks(this.mc, world, state, this.probe) * 150L)) {
            // Far longer than it should take: leave it and come back to it later.
            this.skipped.put(pack(best.x(), best.y(), best.z()), now + 10_000L);
            this.mineTarget = null;
            this.keys.set(Key.ATTACK, false);
            return;
        }
        this.mineTarget = best;
        if (this.bestTool.isEnabled()) {
            DigKit.selectBestTool(this.mc, state);
        }
        DigKit.look(this, best.point(), this.turnSpeed.getValue(), false);
        this.keys.set(Key.ATTACK, DigKit.crosshairOn(this.mc, best.x(), best.y(), best.z()));
    }

    /** Any room block still to mine below feet height y. */
    private boolean anyBelow(class_638 world, Spot s, int y) {
        for (int x = s.x(); x < s.x() + s.lx(); x++) {
            for (int yy = s.y(); yy < Math.min(y, s.y() + s.h()); yy++) {
                for (int z = s.z(); z < s.z() + s.lz(); z++) {
                    if (this.minable(world, x, yy, z)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Digs straight down, centred over the hole, until we stand on the next layer. */
    private void descend(class_638 world, class_746 self, Spot s, int fx, int fy, int fz, long now) {
        if (!this.safeDrop(world, s, fx, fy, fz)) {
            int[] step = this.bfsStep(world, s, fx, fy, fz, (x, z) -> this.safeDrop(world, s, x, fy, z), true);
            if (step == null) {
                this.descendTo = Integer.MIN_VALUE;
                this.roomDone(self, s, this.remaining(world, s)); // no safe way down from this layer
                return;
            }
            this.keys.set(Key.ATTACK, false);
            this.walkTo(self, step[0] + 0.5, step[1] + 0.5, step[2] == 1);
            return;
        }
        if (!this.center(self, fx + 0.5, fz + 0.5)) {
            this.keys.set(Key.ATTACK, false);
            return;
        }
        int by = fy - 1;
        class_243 p = DigKit.visiblePoint(world, self, self.method_33571(), fx, by, fz, self.method_55754() - 0.25);
        this.mineAt(world, self, new Aim(fx, by, fz, p != null ? p : new class_243(fx + 0.5, by + 0.95, fz + 0.5)), now);
    }

    /** Breaking the block under (x, y, z) drops us at most 3 blocks, onto something solid, into nothing wet, and stays in the room. */
    private boolean safeDrop(class_638 world, Spot s, int x, int y, int z) {
        if (y - 1 < s.y() || !this.minable(world, x, y - 1, z) || DigKit.fluidBeside(world, this.probe, x, y - 1, z)) {
            return false;
        }
        for (int j = 2; j <= 4; j++) {
            if (DigKit.fluidAt(world, this.probe, x, y - j, z)) {
                return false;
            }
            if (DigKit.blocking(world, this.probe, x, y - j, z)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Walks over this layer to the nearest place next to blocks still to
     * mine within reach of it. Returns false when there is nowhere to go.
     */
    private boolean walkToWork(class_638 world, class_746 self, Spot s, int fx, int fy, int fz) {
        int lx = s.lx();
        int lz = s.lz();
        int lo = Math.max(s.y(), fy);
        int hi = Math.min(s.y() + s.h() - 1, fy + 4);
        boolean[] work = new boolean[lx * lz];
        boolean any = false;
        for (int i = 0; i < lx; i++) {
            for (int j = 0; j < lz; j++) {
                for (int y = lo; y <= hi; y++) {
                    int x = s.x() + i;
                    int z = s.z() + j;
                    if (this.minable(world, x, y, z) && !this.skipped.containsKey(pack(x, y, z))) {
                        work[i * lz + j] = true;
                        any = true;
                        break;
                    }
                }
            }
        }
        if (!any) {
            return false;
        }
        int[] step = this.bfsStep(world, s, fx, fy, fz, (x, z) -> {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int i = x + dx - s.x();
                    int j = z + dz - s.z();
                    if (i >= 0 && i < lx && j >= 0 && j < lz && work[i * lz + j]) {
                        return true;
                    }
                }
            }
            return false;
        }, true);
        if (step == null) {
            return false;
        }
        this.walkTo(self, step[0] + 0.5, step[1] + 0.5, step[2] == 1);
        return true;
    }

    private interface CellTest {
        boolean test(int x, int z);
    }

    /** Can stand at (x, y, z): room to stand, something solid underneath, nothing wet. */
    private boolean standable(class_638 world, int x, int y, int z) {
        return !DigKit.blocking(world, this.probe, x, y, z) && !DigKit.blocking(world, this.probe, x, y + 1, z)
                && DigKit.blocking(world, this.probe, x, y - 1, z) && !DigKit.fluidAt(world, this.probe, x, y, z);
    }

    /**
     * Breadth-first search over the cells we can stand in on this layer,
     * inside the room, to the nearest one that passes the test. Returns the
     * point to walk toward (a couple of cells along the way, so walking is
     * one smooth line rather than a stop in every block) and whether that
     * point is the goal itself, or null if no such cell can be reached.
     */
    private int[] bfsStep(class_638 world, Spot s, int fx, int fy, int fz, CellTest goal, boolean skipStart) {
        int lx = s.lx();
        int lz = s.lz();
        int si = fx - s.x();
        int sj = fz - s.z();
        if (si < 0 || si >= lx || sj < 0 || sj >= lz) {
            return null;
        }
        int[] prev = new int[lx * lz];
        java.util.Arrays.fill(prev, -1);
        int start = si * lz + sj;
        prev[start] = start;
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(start);
        int found = -1;
        int[][] moves = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        while (!queue.isEmpty()) {
            int c = queue.poll();
            int ci = c / lz;
            int cj = c % lz;
            if ((c != start || !skipStart) && goal.test(s.x() + ci, s.z() + cj)) {
                found = c;
                break;
            }
            for (int[] m : moves) {
                int ni = ci + m[0];
                int nj = cj + m[1];
                if (ni < 0 || ni >= lx || nj < 0 || nj >= lz || prev[ni * lz + nj] != -1) {
                    continue;
                }
                if (!this.standable(world, s.x() + ni, fy, s.z() + nj)) {
                    continue;
                }
                if (m[0] != 0 && m[1] != 0 && (!this.standable(world, s.x() + ni, fy, s.z() + cj) || !this.standable(world, s.x() + ci, fy, s.z() + nj))) {
                    continue; // only cut a corner when both sides are open
                }
                prev[ni * lz + nj] = c;
                queue.add(ni * lz + nj);
            }
        }
        if (found < 0) {
            return null;
        }
        List<Integer> route = new ArrayList<>();
        for (int c = found; c != start; c = prev[c]) {
            route.add(0, c);
        }
        if (route.isEmpty()) {
            return new int[]{fx, fz, 1};
        }
        int k = Math.min(2, route.size()) - 1;
        int c = route.get(k);
        return new int[]{s.x() + c / lz, s.z() + c % lz, k == route.size() - 1 ? 1 : 0};
    }

    /**
     * Walls the door up from inside as soon as we are clear of it. True
     * while it is busy placing, so mining waits; if it cannot manage it
     * soon, mining carries on and it tries again later.
     */
    private boolean sealDoor(class_638 world, class_746 self, Spot s, long now) {
        Door d = this.door;
        if (d == null) {
            this.doorSealed = true;
            return false;
        }
        int y0 = doorLevel(s);
        int x = d.x() - d.dx();
        int z = d.z() - d.dz();
        List<int[]> open = new ArrayList<>();
        for (int y = y0; y <= Math.min(y0 + 1, s.y() + s.h() - 1); y++) {
            if (!DigKit.solid(world, this.probe, x, y, z)) {
                open.add(new int[]{x, y, z});
            }
        }
        if (open.isEmpty()) {
            this.doorSealed = true;
            this.releasePlacing();
            this.say("§7Door walled up behind you, so the room stays sealed while it is mined");
            return false;
        }
        if (open.size() != this.doorOpen) {
            this.doorOpen = open.size();
            this.placingSince = 0L;
        }
        int slot = DigKit.fillerSlot(this.mc);
        if (slot < 0) {
            this.releasePlacing();
            return false;
        }
        Place place = this.findPlacement(world, self, open);
        if (place == null) {
            this.releasePlacing();
            this.placingSince = 0L;
            return false;
        }
        if (this.placingSince == 0L) {
            this.placingSince = now;
        } else if (now - this.placingSince > 3000L) {
            this.placingSince = 0L;
            this.sealRetryAt = now + 15_000L;
            this.releasePlacing();
            return false;
        }
        this.keys.set(Key.ATTACK, false);
        this.place(self, slot, place);
        return true;
    }

    /** The first opening (lowest first) that a block can be placed into from here, and how. */
    private Place findPlacement(class_638 world, class_746 self, List<int[]> openings) {
        class_243 eye = self.method_33571();
        double reach = self.method_55754() - 0.25;
        class_238 me = self.method_5829();
        for (int[] leak : openings) {
            if (me.method_1003(leak[0], leak[1], leak[2], leak[0] + 1, leak[1] + 1, leak[2] + 1)) {
                continue; // vanilla will not place a block where we stand
            }
            Place place = this.placement(world, self, eye, reach, leak);
            if (place != null) {
                return place;
            }
        }
        return null;
    }

    /** Holds sneak and use with the block selected while the crosshair is on the right face. */
    private void place(class_746 self, int slot, Place place) {
        this.keys.set(Key.FORWARD, false);
        this.keys.set(Key.JUMP, false);
        this.keys.set(Key.SNEAK, true); // so a chest or crafting table there takes the block instead of opening
        class_1661 inventory = self.method_31548();
        if (inventory.method_67532() != slot) {
            inventory.method_61496(slot);
        }
        DigKit.look(this, place.point(), this.turnSpeed.getValue(), false);
        boolean on = this.mc.field_1765 instanceof class_3965 hit && hit.method_17783() == class_239.class_240.field_1332
                && hit.method_17777().method_10263() == place.x() && hit.method_17777().method_10264() == place.y()
                && hit.method_17777().method_10260() == place.z() && hit.method_17780() == place.side();
        this.keys.set(Key.USE, on);
    }

    private void releasePlacing() {
        this.keys.set(Key.USE, false);
        this.keys.set(Key.SNEAK, false);
    }

    private boolean minable(class_638 world, int x, int y, int z) {
        return DigKit.minable(world, this.probe, x, y, z);
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

    private boolean insideRoom(class_746 self, Spot s) {
        int fx = (int) Math.floor(self.method_23317());
        int fy = (int) Math.floor(self.method_23318() + 0.2);
        int fz = (int) Math.floor(self.method_23321());
        return fx >= s.x() && fx < s.x() + s.lx() && fz >= s.z() && fz < s.z() + s.lz() && fy >= s.y() && fy < s.y() + s.h();
    }

    private boolean walkTo(class_746 self, double x, double z) {
        return this.walkTo(self, x, z, true);
    }

    /** Turns toward a point and walks there; with stop, lets go once within 0.3 and says so. */
    private boolean walkTo(class_746 self, double x, double z, boolean stop) {
        double dx = x - self.method_23317();
        double dz = z - self.method_23321();
        this.keys.set(Key.SNEAK, false);
        if (stop && dx * dx + dz * dz < 0.09) {
            this.keys.set(Key.FORWARD, false);
            return true;
        }
        DigKit.look(this, new class_243(x, self.method_33571().field_1351, z), this.turnSpeed.getValue(), true);
        this.keys.set(Key.FORWARD, RotationManager.isRotationComplete(25.0F));
        return false;
    }

    /** Creeps (sneaking, so it cannot overshoot or slip off) to within 0.18 of a point; true once there. */
    private boolean center(class_746 self, double x, double z) {
        double dx = x - self.method_23317();
        double dz = z - self.method_23321();
        if (dx * dx + dz * dz < 0.18 * 0.18) {
            this.keys.set(Key.FORWARD, false);
            this.keys.set(Key.SNEAK, false);
            return true;
        }
        DigKit.look(this, new class_243(x, self.method_33571().field_1351, z), this.turnSpeed.getValue(), true);
        this.keys.set(Key.SNEAK, true);
        this.keys.set(Key.FORWARD, RotationManager.isRotationComplete(20.0F));
        return false;
    }

    private void roomDone(class_746 self, Spot s, int leftover) {
        String what = leftover > 0 ? "Room mined out except " + leftover + " blocks it can't reach" : "Room mined out";
        String mode = this.seal.getCurrentMode();
        if ("Off".equals(mode)) {
            this.finish("§a" + what + "§r §7- " + s.lx() + "x" + s.lz() + "x" + s.h() + " at (" + s.x() + ", " + s.y() + ", " + s.z() + ")");
            return;
        }
        // Inside needs a room tall enough to stand in; without a door of our own there is no tunnel to stand in.
        boolean inside = this.door == null || (!this.sealsOutside(s) && s.h() >= 2 && this.insideRoom(self, s));
        this.keys.releaseAll();
        this.phase = inside ? Phase.SEAL : Phase.LEAVE;
        this.stepSince = System.currentTimeMillis();
        this.lastLeaks = -1;
        this.say("§7" + what + ", walling up the entrance " + (inside ? "with you inside" : "from the tunnel"));
    }

    /** Back out to the tunnel: line up on the door, then walk out through the wall. */
    private void tickLeave(class_746 self, Spot s) {
        Door d = this.door;
        long now = System.currentTimeMillis();
        int ox = d.x() - 2 * d.dx(); // just past the innermost wall block, in the tunnel
        int oz = d.z() - 2 * d.dz();
        int fx = (int) Math.floor(self.method_23317());
        int fz = (int) Math.floor(self.method_23321());
        if (fx == ox && fz == oz) {
            double dx = ox + 0.5 - self.method_23317();
            double dz = oz + 0.5 - self.method_23321();
            if (dx * dx + dz * dz < 0.35 * 0.35) {
                this.keys.releaseMovement();
                this.phase = Phase.SEAL;
                this.stepSince = now;
                this.lastLeaks = -1;
                return;
            }
        }
        if (this.insideRoom(self, s) && !(fx == d.x() && fz == d.z())) {
            this.walkTo(self, d.x() + 0.5, d.z() + 0.5);
        } else {
            this.walkTo(self, ox + 0.5, oz + 0.5);
        }
        if (now - this.stepSince > 15_000L) {
            this.finish("§eRoom mined out, but couldn't get back out to the tunnel to seal it");
        }
    }

    /**
     * Puts a plain block from the hotbar into every opening in the walls,
     * lowest first so each one placed holds up the next. Placing is the use
     * key held while the crosshair is on the right face of a neighbouring
     * block, with sneak held so a chest or crafting table there takes the
     * block instead of opening.
     */
    private void tickSeal(class_638 world, class_746 self, Spot s) {
        long now = System.currentTimeMillis();
        List<int[]> leaks = this.leaks(world, s);
        if (leaks.size() != this.lastLeaks) {
            this.lastLeaks = leaks.size();
            this.stepSince = now;
        }
        if (leaks.isEmpty()) {
            this.finish("§aRoom done and sealed§r §7- " + s.lx() + "x" + s.lz() + "x" + s.h() + " at (" + s.x() + ", " + s.y() + ", " + s.z() + ")"
                    + (this.insideRoom(self, s) ? ", you're inside it" : ""));
            return;
        }
        int[] first = leaks.get(0);
        if (now - this.stepSince > 12_000L) {
            this.finish("§eRoom mined out, but " + leaks.size() + " opening(s) are left§r §7- first at (" + first[0] + ", " + first[1] + ", " + first[2] + ")");
            return;
        }
        int slot = DigKit.fillerSlot(this.mc);
        if (slot < 0) {
            this.finish("§eRoom mined out, but there are no plain blocks (cobblestone, deepslate...) in the hotbar to seal it§r §7- "
                    + leaks.size() + " opening(s) at (" + first[0] + ", " + first[1] + ", " + first[2] + ")");
            return;
        }

        class_238 me = self.method_5829();
        boolean inTheWay = false;
        for (int[] leak : leaks) {
            if (me.method_1003(leak[0], leak[1], leak[2], leak[0] + 1, leak[1] + 1, leak[2] + 1)) {
                inTheWay = true;
            }
        }
        Place place = this.findPlacement(world, self, leaks);
        if (place == null) {
            this.releasePlacing();
            if (this.insideRoom(self, s)) {
                if (inTheWay) {
                    this.walkTo(self, s.centerX(), s.centerZ());
                } else {
                    // Get next to the lowest opening from inside the room.
                    double tx = Math.max(s.x(), Math.min(s.x() + s.lx() - 1, first[0])) + 0.5;
                    double tz = Math.max(s.z(), Math.min(s.z() + s.lz() - 1, first[2])) + 0.5;
                    this.walkTo(self, tx, tz);
                }
            } else {
                this.keys.releaseMovement();
            }
            return;
        }
        this.place(self, slot, place);
    }

    /** A face of a solid neighbour of the opening that we can see and reach, so clicking it fills the opening. */
    private Place placement(class_638 world, class_746 self, class_243 eye, double reach, int[] leak) {
        for (int[] o : DigKit.SIDES) {
            int sx = leak[0] + o[0];
            int sy = leak[1] + o[1];
            int sz = leak[2] + o[2];
            class_2680 state = world.method_8320(this.probe.method_10103(sx, sy, sz));
            if (state.method_26215() || !state.method_26227().method_15769() || state.method_26220(world, this.probe).method_1110()) {
                continue;
            }
            double fx = leak[0] + 0.5 + o[0] * 0.5;
            double fy = leak[1] + 0.5 + o[1] * 0.5;
            double fz = leak[2] + 0.5 + o[2] * 0.5;
            class_243 face = new class_243(fx, fy, fz);
            if (face.method_1025(eye) > reach * reach) {
                continue;
            }
            class_2350 side = facing(-o[0], -o[1], -o[2]);
            class_243 past = new class_243(fx + o[0] * 0.1, fy + o[1] * 0.1, fz + o[2] * 0.1);
            class_3965 hit = world.method_17742(new class_3959(eye, past, class_3959.class_3960.field_17559, class_3959.class_242.field_1348, self));
            if (hit != null && hit.method_17783() == class_239.class_240.field_1332 && hit.method_17780() == side
                    && hit.method_17777().method_10263() == sx && hit.method_17777().method_10264() == sy && hit.method_17777().method_10260() == sz) {
                return new Place(sx, sy, sz, side, new class_243(fx + o[0] * 0.02, fy + o[1] * 0.02, fz + o[2] * 0.02));
            }
        }
        return null;
    }

    private static class_2350 facing(int dx, int dy, int dz) {
        if (dy > 0) {
            return class_2350.field_11036;
        }
        if (dy < 0) {
            return class_2350.field_11033;
        }
        if (dx > 0) {
            return class_2350.field_11034;
        }
        if (dx < 0) {
            return class_2350.field_11039;
        }
        return dz > 0 ? class_2350.field_11035 : class_2350.field_11043;
    }

    /** Openings in the room's walls: anything not solid right against the room. Lowest first. */
    private List<int[]> leaks(class_638 world, Spot s) {
        List<int[]> out = new ArrayList<>();
        LongOpenHashSet seen = new LongOpenHashSet();
        int x0 = s.x();
        int x1 = s.x() + s.lx() - 1;
        int y0 = s.y();
        int y1 = s.y() + s.h() - 1;
        int z0 = s.z();
        int z1 = s.z() + s.lz() - 1;
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    if (x != x0 && x != x1 && y != y0 && y != y1 && z != z0 && z != z1) {
                        continue; // only cells on the room's surface touch its walls
                    }
                    for (int[] o : DigKit.SIDES) {
                        int nx = x + o[0];
                        int ny = y + o[1];
                        int nz = z + o[2];
                        if (nx >= x0 && nx <= x1 && ny >= y0 && ny <= y1 && nz >= z0 && nz <= z1) {
                            continue;
                        }
                        if (seen.add(FarmRoute.key(nx, ny, nz)) && !DigKit.solid(world, this.probe, nx, ny, nz)) {
                            out.add(new int[]{nx, ny, nz});
                        }
                    }
                }
            }
        }
        out.sort(Comparator.comparingInt(c -> c[1]));
        return out;
    }

    // ---- Highlight Mode ----

    /**
     * Highlight Mode: the plan Auto Mine would follow, shown instead of done.
     * Outside the room, the dig route from here to the nearest spot's door,
     * every block on it still to break outlined and the next one in white.
     * In the room, the top layer that still has blocks (the room is dug top
     * down, like Auto Mine does it). Then, in green, where a block goes to
     * wall the door up. You do the digging; it re-plans if you leave the
     * route.
     */
    private void tickGuide(class_638 world, class_746 self) {
        Spot s = this.mining != null ? this.mining : (this.spots.isEmpty() ? null : this.spots.get(0));
        if (s == null) {
            this.clearGuide();
            return;
        }
        long now = System.currentTimeMillis();
        if (!s.equals(this.guideSpot)) {
            this.clearGuide();
            this.guideSpot = s;
            this.guideDoor = this.door != null && s.equals(this.mining) ? this.door : this.doorsFor(s, self.method_23317(), self.method_23321()).get(0);
            this.say("§7Highlight Mode: dig the outlined blocks (white first) down to " + this.describe(s, self));
        }
        Door d = this.guideDoor;
        if (this.insideRoom(self, s)) {
            this.guidePath = List.of();
            this.guideSearch = null;
        } else if (this.guideSearch != null) {
            FarmRoute.Status status = this.guideSearch.step(3_000_000L);
            if (status != FarmRoute.Status.SEARCHING) {
                this.guidePath = status == FarmRoute.Status.FAILED ? List.of() : this.guideSearch.path();
                this.guidePartial = status == FarmRoute.Status.PARTIAL;
                this.guideFailed = status == FarmRoute.Status.FAILED;
                this.guideSearch = null;
                this.guidePlannedAt = now;
                this.guideIndex = 0;
            }
        } else if (self.method_24828() && this.guideNeedsPlan(self, now)) {
            int[] g = this.goalFor(s, d);
            this.guideSearch = new FarmRoute(world, (int) Math.floor(self.method_23317()), (int) Math.floor(self.method_23318() + 0.01),
                    (int) Math.floor(self.method_23321()), g[0], g[1], g[2], this.rulesFor(s, d, new LongOpenHashSet()));
        }
        if (this.ticks % 10 == 0) {
            this.refreshGuide(world, self, s, d);
        }
    }

    private boolean guideNeedsPlan(class_746 self, long now) {
        if (this.guidePath.isEmpty()) {
            return now - this.guidePlannedAt > (this.guideFailed ? 10_000L : 0L);
        }
        if (now - this.guidePlannedAt > 20_000L) {
            return true; // the world changed under it (or you dug your own way): plan again from here
        }
        double best = Double.MAX_VALUE;
        for (FarmRoute.Step step : this.guidePath) {
            double dx = step.x() + 0.5 - self.method_23317();
            double dy = step.y() - self.method_23318();
            double dz = step.z() + 0.5 - self.method_23321();
            best = Math.min(best, dx * dx + dy * dy + dz * dz);
        }
        if (best > 9.0) {
            return true; // left the route
        }
        FarmRoute.Step end = this.guidePath.get(this.guidePath.size() - 1);
        double ex = end.x() + 0.5 - self.method_23317();
        double ez = end.z() + 0.5 - self.method_23321();
        return this.guidePartial && ex * ex + ez * ez < 9.0 && Math.abs(end.y() - self.method_23318()) < 3.0;
    }

    /** Counts, the room layer to show, the opening to seal, and the time it should all take by hand. */
    private void refreshGuide(class_638 world, class_746 self, Spot s, Door d) {
        DigKit.Clock clock = new DigKit.Clock(this.mc, world);
        double seconds = 0.0;
        int routeLeft = 0;
        double nearest = Double.MAX_VALUE;
        for (int i = 0; i < this.guidePath.size(); i++) {
            FarmRoute.Step step = this.guidePath.get(i);
            double dx = step.x() + 0.5 - self.method_23317();
            double dy = step.y() - self.method_23318();
            double dz = step.z() + 0.5 - self.method_23321();
            double dd = dx * dx + dy * dy + dz * dz;
            if (dd < nearest) {
                nearest = dd;
                this.guideIndex = i;
            }
        }
        int[] next = null;
        for (int i = this.guideIndex; i < this.guidePath.size(); i++) {
            int[] mined = this.guidePath.get(i).mined();
            if (mined == null) {
                continue;
            }
            for (int k = 0; k + 2 < mined.length; k += 3) {
                if (DigKit.blocking(world, this.probe, mined[k], mined[k + 1], mined[k + 2])) {
                    routeLeft++;
                    seconds += clock.seconds(world.method_8320(this.probe), this.probe);
                    if (next == null) {
                        next = new int[]{mined[k], mined[k + 1], mined[k + 2]};
                    }
                }
            }
        }
        this.guideNext = next;

        int[] levels = layers(s);
        int top = s.y() + s.h() - 1;
        List<int[]> show = new ArrayList<>();
        int roomLeft = 0;
        this.guideLayer = 0;
        for (int i = 0; i < levels.length; i++) {
            int lo = levels[i];
            int hi = i == 0 ? top : levels[i - 1] - 1;
            List<int[]> here = new ArrayList<>();
            for (int x = s.x(); x < s.x() + s.lx(); x++) {
                for (int y = lo; y <= hi; y++) {
                    for (int z = s.z(); z < s.z() + s.lz(); z++) {
                        if (this.minable(world, x, y, z)) {
                            here.add(new int[]{x, y, z});
                            seconds += clock.seconds(world.method_8320(this.probe), this.probe);
                        }
                    }
                }
            }
            roomLeft += here.size();
            if (show.isEmpty() && !here.isEmpty()) {
                show = here;
                this.guideLayer = i + 1;
            }
        }
        double px = self.method_23317();
        double py = self.method_23318();
        double pz = self.method_23321();
        show.sort(Comparator.comparingDouble(c -> (c[0] + 0.5 - px) * (c[0] + 0.5 - px) + (c[1] - py) * (c[1] - py) + (c[2] + 0.5 - pz) * (c[2] + 0.5 - pz)));
        this.guideRoom = show.size() > 2000 ? new ArrayList<>(show.subList(0, 2000)) : show;

        List<int[]> seal = new ArrayList<>();
        boolean inside = this.insideRoom(self, s);
        if (d != null && !"Off".equals(this.seal.getCurrentMode()) && (inside || roomLeft == 0)) {
            int level = doorLevel(s);
            for (int y = level; y <= Math.min(level + 1, top); y++) {
                int x = d.x() - d.dx();
                int z = d.z() - d.dz();
                if (!DigKit.solid(world, this.probe, x, y, z)) {
                    seal.add(new int[]{x, y, z});
                }
            }
        }
        this.guideSeal = seal;
        this.guideRouteLeft = inside ? 0 : routeLeft;
        this.guideRoomLeft = roomLeft;
        this.guideEta = seconds;
    }

    private void clearGuide() {
        this.guideSpot = null;
        this.guideDoor = null;
        this.guideSearch = null;
        this.guidePath = List.of();
        this.guideRoom = List.of();
        this.guideSeal = List.of();
        this.guideNext = null;
        this.guideFailed = false;
        this.guidePlannedAt = 0L;
        this.guideRouteLeft = 0;
        this.guideRoomLeft = 0;
        this.guideEta = Double.NaN;
    }

    private boolean guiding() {
        return this.highlight.isEnabled() && !this.autoMine.isEnabled() && this.guideSpot != null;
    }

    private void drawGuide(class_4587 matrices, class_4598 consumers, double cx, double cy, double cz, Color c) {
        if (!this.guidePath.isEmpty()) {
            this.drawRoute(matrices, consumers, this.guidePath, this.guideIndex + 1, this.guideNext, 1000, cx, cy, cz, c);
        }
        Color room = new Color(255, 170, 60);
        for (int[] cell : this.guideRoom) {
            double x = cell[0] - cx;
            double y = cell[1] - cy;
            double z = cell[2] - cz;
            RenderUtils.drawBoxEsp(matrices, consumers, new class_238(x + 0.04, y + 0.04, z + 0.04, x + 0.96, y + 0.96, z + 0.96), room, 0.16F, true);
        }
        Color green = new Color(70, 230, 90);
        for (int[] cell : this.guideSeal) {
            double x = cell[0] - cx;
            double y = cell[1] - cy;
            double z = cell[2] - cz;
            RenderUtils.drawBoxEsp(matrices, consumers, new class_238(x + 0.02, y + 0.02, z + 0.02, x + 0.98, y + 0.98, z + 0.98), green, 0.4F, true);
        }
    }

    private void renderGuideHud(class_332 context) {
        String what;
        if (this.guideRoomLeft == 0) {
            what = this.guideSeal.isEmpty() ? "Room done" : "Room done: put a block in the green opening";
        } else if (this.guideRouteLeft > 0 || (!this.guidePath.isEmpty() && this.mc.field_1724 != null && !this.insideRoom(this.mc.field_1724, this.guideSpot))) {
            what = "Dig the outlined route to the room";
        } else if (this.guideSearch != null) {
            what = "Planning the route...";
        } else if (this.guideFailed && this.mc.field_1724 != null && !this.insideRoom(this.mc.field_1724, this.guideSpot)) {
            what = "No safe route from here, move and it tries again";
        } else {
            int layers = layers(this.guideSpot).length;
            what = layers > 1 ? "Mine the orange blocks, layer " + this.guideLayer + " of " + layers : "Mine the orange blocks";
        }
        String line1 = "Highlight · " + what + (this.guideSeal.isEmpty() || this.guideRoomLeft == 0 ? "" : " · then seal the green opening");
        String line2 = this.guideRouteLeft + " on the route · " + this.guideRoomLeft + " in the room"
                + (Double.isNaN(this.guideEta) || this.guideEta <= 0.0 ? "" : " · ~" + DigKit.time(this.guideEta) + " with your best tool");
        int centerX = context.method_51421() / 2;
        int y = context.method_51443() / 2 + 46;
        this.centered(context, line1, centerX, y, this.color.getValue().getRGB() | 0xFF000000);
        this.centered(context, line2, centerX, y + 10, 0xFFAAAAAA);
    }

    // ---- ETA ----

    /**
     * Every half second: predict the seconds left from what is actually
     * left (each block's break time with the best hotbar tool plus vanilla's
     * pause between blocks, walking at walking speed), then scale it by how
     * the real time spent so far compares to what was predicted for it.
     * That catches what the model cannot know: lag, Haste, re-routes.
     */
    private void updateEta(class_638 world) {
        long now = System.currentTimeMillis();
        DigKit.Clock clock = new DigKit.Clock(this.mc, world);
        double r = this.predictRemaining(world, clock);
        int version = this.phase.ordinal() * 1_000_000 + (this.walker != null ? this.walker.version() : 0);
        if (!Double.isNaN(r) && !Double.isNaN(this.lastPredicted) && this.lastEtaAt > 0L && version == this.lastEtaVersion) {
            double dt = (now - this.lastEtaAt) / 1000.0;
            if (dt > 0.0 && dt < 3.0) {
                this.actualDone += dt;
                this.predDone += Math.max(0.0, this.lastPredicted - r);
            }
        }
        this.lastPredicted = r;
        this.lastEtaAt = now;
        this.lastEtaVersion = version;
        if (Double.isNaN(r)) {
            this.eta = Double.NaN;
            return;
        }
        // Eased in: the first ~20 seconds lean on the prediction alone.
        double factor = Math.max(0.6, Math.min(4.0, (this.actualDone + 20.0) / (this.predDone + 20.0)));
        this.eta = r * factor;
        this.etaAt = now;
        if (!this.etaAnnounced) {
            this.etaAnnounced = true;
            this.say("§7ETA about §f" + DigKit.time(this.eta) + "§7 with " + this.blocksLeft + " blocks to dig");
        }
    }

    private double predictRemaining(class_638 world, DigKit.Clock clock) {
        Spot s = this.mining;
        double t = 0.0;
        int blocks = 0;
        if (this.phase == Phase.TRAVEL) {
            if (this.walker == null) {
                return Double.NaN;
            }
            double travel = this.walker.etaSeconds(world, clock);
            if (Double.isNaN(travel)) {
                return Double.NaN;
            }
            t += travel;
            blocks += this.walker.blocksLeft(world);
        }
        if (this.phase == Phase.TRAVEL || this.phase == Phase.MINE) {
            for (int x = s.x(); x < s.x() + s.lx(); x++) {
                for (int y = s.y(); y < s.y() + s.h(); y++) {
                    for (int z = s.z(); z < s.z() + s.lz(); z++) {
                        if (this.minable(world, x, y, z)) {
                            t += clock.seconds(world.method_8320(this.probe), this.probe) + 0.1;
                            blocks++;
                        }
                    }
                }
            }
        }
        String mode = this.seal.getCurrentMode();
        if (!"Off".equals(mode)) {
            int openings = this.phase == Phase.SEAL || this.phase == Phase.LEAVE ? this.leaks(world, s).size() : (s.h() >= 2 ? 2 : 1);
            t += openings * 1.5;
            if (this.phase != Phase.SEAL && ("Outside".equals(mode) || s.h() < 2)) {
                t += 3.0; // walking back out to the tunnel
            }
        }
        this.blocksLeft = blocks;
        return t;
    }

    @Override
    public void onRender(class_332 context, float tickDelta) {
        if (!this.etaHud.isEnabled() || this.mc.field_1724 == null || Cyemer.selfDestructed) {
            return;
        }
        if (this.guiding()) {
            this.renderGuideHud(context);
            return;
        }
        if (!this.autoMine.isEnabled() || this.mining == null) {
            return;
        }
        String what = switch (this.phase) {
            case TRAVEL -> this.walker != null ? this.walker.activity() : "Heading to the room";
            case MINE -> this.layerLabel(this.mining);
            case LEAVE -> "Heading out to seal it";
            case SEAL -> "Sealing the entrance";
        };
        long now = System.currentTimeMillis();
        double elapsed = (now - this.jobStarted) / 1000.0;
        double left = Double.isNaN(this.eta) ? Double.NaN : Math.max(0.0, this.eta - (now - this.etaAt) / 1000.0);
        String line1 = "Auto Mine · " + what + " · " + (Double.isNaN(left) ? "working out the ETA" : "~" + DigKit.time(left) + " left");
        String event = this.phase == Phase.TRAVEL && this.walker != null ? this.walker.recentEvent() : null;
        String line2 = (Double.isNaN(left) ? "" : Math.round(100.0 * elapsed / Math.max(1.0, elapsed + left)) + "% · ")
                + this.blocksLeft + " blocks to dig · " + DigKit.time(elapsed) + " so far" + (event != null ? " · " + event : "");
        int centerX = context.method_51421() / 2;
        int y = context.method_51443() / 2 + 46;
        this.centered(context, line1, centerX, y, this.color.getValue().getRGB() | 0xFF000000);
        this.centered(context, line2, centerX, y + 10, 0xFFAAAAAA);
    }

    private String layerLabel(Spot s) {
        int[] levels = layers(s);
        if (levels.length <= 1) {
            return "Mining the room";
        }
        int fy = (int) Math.floor(this.mc.field_1724.method_23318() + 0.2);
        int layer = levels.length;
        for (int i = 0; i < levels.length; i++) {
            if (fy >= levels[i]) {
                layer = i + 1;
                break;
            }
        }
        return (this.descendTo != Integer.MIN_VALUE ? "Digging down to layer " : "Mining layer ") + layer + " of " + levels.length;
    }

    /** Auto Mine is on a job: other modules' AutoRTP must not carry the player off. */
    public static boolean isDigging() {
        SpotFinder f = instance;
        return f != null && f.isEnabled() && f.autoMine.isEnabled() && f.mining != null;
    }

    private void centered(class_332 context, String text, int centerX, int y, int color) {
        int x = centerX - this.mc.field_1772.method_1727(text) / 2;
        context.method_25303(this.mc.field_1772, text, x + 1, y + 1, 0x80000000);
        context.method_25303(this.mc.field_1772, text, x, y, color);
    }

    /** Lets go of everything Auto Mine holds. The spot stays locked, so turning Auto Mine back on carries on. */
    private void stopMining() {
        this.keys.releaseAll();
        RotationManager.stop(this);
        this.walker = null;
        this.mineTarget = null;
        this.lastEtaAt = 0L;
    }

    private void forgetJob() {
        this.mining = null;
        this.phase = Phase.TRAVEL;
        this.door = null;
        this.doors = List.of();
        this.walker = null;
        this.eta = Double.NaN;
    }

    private void finish(String message) {
        double took = (System.currentTimeMillis() - this.jobStarted) / 1000.0;
        this.say(message + " §7(took " + DigKit.time(took) + ")");
        this.wasAuto = false;
        this.autoMine.setEnabled(false);
        this.stopMining();
        this.forgetJob();
    }

    private void abort(String message) {
        this.say(message);
        this.wasAuto = false;
        this.autoMine.setEnabled(false);
        this.stopMining();
    }

    /** Says each distinct hint once, not every tick. */
    private void hint(String message) {
        if (!message.equals(this.lastHint)) {
            this.lastHint = message;
            this.say(message);
        }
    }

    /** From ClientWorldMixin: a changed block in the graded layers regrades its chunk. */
    @Override
    public void onBlockUpdate(class_2338 pos, class_2680 before, class_2680 after) {
        this.onBlockUpdate(pos);
    }

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
        if ((shown.isEmpty() && this.mining == null && this.guideSpot == null) || self == null || Cyemer.selfDestructed) {
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
            DigWalker w = this.walker;
            if (w != null && this.phase == Phase.TRAVEL) {
                this.drawRoute(matrices, consumers, w.path(), w.index(), w.target(), 24, cx, cy, cz, c);
            }
            if (this.guiding()) {
                this.drawGuide(matrices, consumers, cx, cy, cz, c);
            }
            Aim target = this.mineTarget;
            if (target != null) {
                double tx = target.x() - cx;
                double ty = target.y() - cy;
                double tz = target.z() - cz;
                RenderUtils.drawBoxEsp(matrices, consumers, new class_238(tx + 0.02, ty + 0.02, tz + 0.02, tx + 0.98, ty + 0.98, tz + 0.98),
                        Color.WHITE, 0.35F, true);
            }
            if (this.tracer.isEnabled() && (this.mining != null || !shown.isEmpty())) {
                Spot s = this.mining != null ? this.mining : shown.get(0);
                class_243 look = self.method_5828(tickDelta);
                RenderUtils.drawLine(matrices, consumers, new class_243(look.field_1352 * 0.6, look.field_1351 * 0.6, look.field_1350 * 0.6),
                        new class_243(s.centerX() - cx, s.y() + s.h() / 2.0 - cy, s.centerZ() - cz), c, 0.8F, true);
            }
            consumers.method_22993();
        }
    }

    /**
     * The dig route ahead as a line through terrain, with the blocks the
     * next stretch still has to break outlined and the one being broken
     * now in white.
     */
    private void drawRoute(class_4587 matrices, class_4598 consumers, List<FarmRoute.Step> path, int index, int[] t, int ahead,
                           double cx, double cy, double cz, Color c) {
        class_638 world = this.mc.field_1687;
        if (path.isEmpty() || world == null) {
            return;
        }
        int from = Math.max(0, index - 1);
        int to = Math.min(path.size() - 1, from + 400);
        for (int i = from; i < to; i++) {
            RenderUtils.drawLine(matrices, consumers, stepPoint(path.get(i), cx, cy, cz), stepPoint(path.get(i + 1), cx, cy, cz), c, 0.9F, true);
        }
        for (int i = Math.max(1, index); i < path.size() && i < index + ahead; i++) {
            int[] mined = path.get(i).mined();
            if (mined == null) {
                continue;
            }
            for (int k = 0; k + 2 < mined.length; k += 3) {
                if (!DigKit.blocking(world, this.probe, mined[k], mined[k + 1], mined[k + 2])) {
                    continue;
                }
                double bx = mined[k] - cx;
                double by = mined[k + 1] - cy;
                double bz = mined[k + 2] - cz;
                RenderUtils.drawBoxEsp(matrices, consumers, new class_238(bx + 0.05, by + 0.05, bz + 0.05, bx + 0.95, by + 0.95, bz + 0.95), c, 0.12F, true);
            }
        }
        if (t != null) {
            double tx = t[0] - cx;
            double ty = t[1] - cy;
            double tz = t[2] - cz;
            RenderUtils.drawBoxEsp(matrices, consumers, new class_238(tx + 0.02, ty + 0.02, tz + 0.02, tx + 0.98, ty + 0.98, tz + 0.98), Color.WHITE, 0.35F, true);
        }
    }

    /** The route line runs just above the floor through the middle of each block. */
    private static class_243 stepPoint(FarmRoute.Step step, double cx, double cy, double cz) {
        return new class_243(step.x() + 0.5 - cx, step.y() + 0.1 - cy, step.z() + 0.5 - cz);
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
