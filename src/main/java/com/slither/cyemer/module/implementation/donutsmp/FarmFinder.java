package com.slither.cyemer.module.implementation.donutsmp;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ColorSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.module.StringSetting;
import com.slither.cyemer.module.implementation.Notifications;
import com.slither.cyemer.util.render.RenderUtils;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_1144;
import net.minecraft.class_1145;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2561;
import net.minecraft.class_2680;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_3417;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4597.class_4598;
import net.minecraft.class_634;
import net.minecraft.class_638;
import net.minecraft.class_746;
import net.minecraft.class_9799;

/**
 * Tells you when the parts of a redstone farm go off nearby.
 *
 * Two channels, both purely passive - nothing is sent, only what the server
 * already hands the client is read.
 *
 * Sounds come from a SoundManager listener, which sees every sound the client
 * plays whatever its origin: server sound packets, world events, block events,
 * and the client-side random display ticks that make lit furnaces crackle. The
 * listener runs ahead of SoundSystem's "volume was zero" skip, so detection
 * keeps working with block sounds muted. Range is whatever the server sends:
 * about 16 blocks for a piston, 64 for dispensers and crafters, and roughly
 * 16 to 32 for smelters, which are sampled by display ticks around the player.
 *
 * Block updates come from ClientWorldMixin, which sees every server-driven
 * block change in a loaded chunk. This exists for observers, which make no
 * sound at all in 1.21.11 - but a pulse flips their powered state and that
 * change is synced to every client watching the chunk. The same channel picks
 * pistons up across the whole loaded area rather than just inside hearing
 * range. Only a state change on a block that was already that block counts,
 * so placing, breaking, chunk loading or an anti-xray reveal never trips it.
 *
 * Every block that fires is highlighted through walls for a while, and the
 * first find becomes a route target: FarmRoute plans a walking path to beside
 * it over the loaded chunks, mining through only where it has to, and the
 * path, the blocks to break and the target are drawn in the world.
 */
@Environment(EnvType.CLIENT)
public class FarmFinder extends Module {
    /** Floor between any two alerts, so flying over a mega-farm cannot flood chat. */
    private static final long GLOBAL_GAP_MS = 300L;
    /** Clusters unheard for this long are forgotten. */
    private static final long CLUSTER_TTL_MS = 5L * 60L * 1000L;
    private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    private static final int MAX_MARKS = 600;
    /** Eye to target centre: close enough to see and reach it. */
    private static final double ARRIVE_DISTANCE = 4.0;
    /** Further than this from the target means we teleported away from it. */
    private static final double TARGET_DROP_DISTANCE = 400.0;
    /** Planning time per tick; the search resumes next tick from where it stopped. */
    private static final long PLAN_BUDGET_NS = 3_000_000L;
    private static final Color MINE_COLOR = new Color(255, 70, 70);
    private static final Color PLACE_COLOR = new Color(255, 220, 60);

    private enum Family {
        PISTON(new Color(110, 255, 110)),
        OBSERVER(new Color(190, 190, 255)),
        SMELTER(new Color(255, 150, 40)),
        DISPENSER(new Color(200, 200, 200)),
        CRAFTER(new Color(255, 215, 90)),
        REDSTONE(new Color(255, 60, 60)),
        COMPOSTER(new Color(180, 130, 70));

        final Color color;

        Family(Color color) {
            this.color = color;
        }
    }

    private record Source(Family family, String label) {}

    /** Sound ids by path, every one checked against 1.21.11's SoundEvents. */
    private static final Map<String, Source> SOUNDS = new HashMap<>();

    static {
        put(Family.PISTON, "Piston", "block.piston.extend", "block.piston.contract");
        put(Family.SMELTER, "Furnace", "block.furnace.fire_crackle");
        put(Family.SMELTER, "Blast Furnace", "block.blastfurnace.fire_crackle");
        put(Family.SMELTER, "Smoker", "block.smoker.smoke");
        put(Family.DISPENSER, "Dispenser", "block.dispenser.dispense", "block.dispenser.fail", "block.dispenser.launch");
        put(Family.CRAFTER, "Crafter", "block.crafter.craft", "block.crafter.fail");
        put(Family.REDSTONE, "Torch Burnout", "block.redstone_torch.burnout");
        put(Family.REDSTONE, "Copper Bulb", "block.copper_bulb.turn_on", "block.copper_bulb.turn_off");
        put(Family.REDSTONE, "Sculk Sensor", "block.sculk_sensor.clicking", "block.sculk_sensor.clicking_stop");
        put(Family.COMPOSTER, "Composter",
                "block.composter.fill", "block.composter.fill_success", "block.composter.ready", "block.composter.empty");
    }

    private static void put(Family family, String label, String... paths) {
        Source source = new Source(family, label);
        for (String path : paths) {
            SOUNDS.put(path, source);
        }
    }

    private static final Source OBSERVER_SOURCE = new Source(Family.OBSERVER, "Observer");
    private static final Source PISTON_SOURCE = new Source(Family.PISTON, "Piston");

    private static FarmFinder instance;

    private final BooleanSetting pistons = new BooleanSetting("Pistons", true);
    private final BooleanSetting observers = new BooleanSetting("Observers", true);
    private final BooleanSetting smelters = new BooleanSetting("Smelters", true);
    private final BooleanSetting dispensers = new BooleanSetting("Dispensers", true);
    private final BooleanSetting crafters = new BooleanSetting("Crafters", true);
    private final BooleanSetting redstone = new BooleanSetting("Redstone", true);
    private final BooleanSetting composters = new BooleanSetting("Composters", true);
    private final BooleanSetting blockUpdates = new BooleanSetting("Block Updates", true);
    private final SliderSetting cooldown = new SliderSetting("Cooldown (s)", 15.0, 1.0, 120.0, 0);
    private final SliderSetting clusterSize = new SliderSetting("Cluster Size", 16.0, 4.0, 64.0, 0);
    private final SliderSetting minDistance = new SliderSetting("Min Distance", 0.0, 0.0, 64.0, 0);
    private final BooleanSetting chat = new BooleanSetting("Chat", true);
    private final BooleanSetting toast = new BooleanSetting("Toast", true);
    private final BooleanSetting ping = new BooleanSetting("Ping", true);
    private final BooleanSetting highlight = new BooleanSetting("Highlight", true);
    private final SliderSetting highlightTime = new SliderSetting("Highlight Time (s)", 60.0, 5.0, 600.0, 0);
    private final BooleanSetting route = new BooleanSetting("Route", true);
    private final BooleanSetting routeMining = new BooleanSetting("Route Mining", true);
    private final BooleanSetting routeHud = new BooleanSetting("Route HUD", true);
    private final ColorSetting routeColor = new ColorSetting("Route Color", new Color(0, 200, 255));
    private final BooleanSetting autoRtp = new BooleanSetting("AutoRTP", false);
    private final SliderSetting rtpInterval = new SliderSetting("RTP Interval (s)", 0.5, 0.5, 30.0, 1);
    private final StringSetting rtpCommand = new StringSetting("RTP Command", "/rtp");

    private final class_1145 soundListener = (sound, soundSet, range) -> this.onSound(sound);
    private class_1144 registeredWith = null;

    private final Map<String, Cluster> clusters = new HashMap<>();
    private long lastAnyAlertAt = 0L;
    private long lastPruneAt = 0L;
    private class_1937 lastWorld = null;
    private long lastRtpAt = 0L;
    private class_634 lastConnection = null;

    private final Map<Long, Mark> marks = new HashMap<>();
    private long lastMarkPruneAt = 0L;
    private Target target = null;
    private FarmRoute search = null;
    private List<FarmRoute.Step> path = List.of();
    private boolean pathPartial = false;
    private boolean failNotified = false;
    private boolean routeAnnounced = false;
    private int cursor = 0;
    private int toMine = 0;
    private int toPlace = 0;
    private long lastPlanAt = 0L;
    private long offRouteSince = 0L;
    /** Where we last arrived, so the same farm firing again does not start a new route on top of us. */
    private int[] arrivedAt = null;
    private final class_2338.class_2339 probe = new class_2338.class_2339();

    private static final class Cluster {
        long lastAlertAt = 0L;
        long lastSeenAt = 0L;
        int suppressed = 0;
    }

    private static final class Mark {
        final int x;
        final int y;
        final int z;
        Family family;
        long at;

        Mark(int x, int y, int z, Family family, long at) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.family = family;
            this.at = at;
        }
    }

    private record Target(String label, int x, int y, int z) {}

    public FarmFinder() {
        super("FarmFinder", "Alerts when redstone farm parts fire nearby: pistons, observers, smelters and more.", Category.DONUT_SMP);
        this.addSetting(this.pistons);
        this.addSetting(this.observers);
        this.addSetting(this.smelters);
        this.addSetting(this.dispensers);
        this.addSetting(this.crafters);
        this.addSetting(this.redstone);
        this.addSetting(this.composters);
        this.addSetting(this.blockUpdates);
        this.addSetting(this.cooldown);
        this.addSetting(this.clusterSize);
        this.addSetting(this.minDistance);
        this.addSetting(this.chat);
        this.addSetting(this.toast);
        this.addSetting(this.ping);
        this.addSetting(this.highlight);
        this.addSetting(this.highlightTime);
        this.addSetting(this.route);
        this.addSetting(this.routeMining);
        this.addSetting(this.routeHud);
        this.addSetting(this.routeColor);
        this.addSetting(this.autoRtp);
        this.addSetting(this.rtpInterval);
        this.addSetting(this.rtpCommand);
        instance = this;
    }

    public static FarmFinder getInstance() {
        return instance;
    }

    @Override
    public void onDisable() {
        this.unregister();
        this.clusters.clear();
        this.marks.clear();
        this.clearTarget();
        this.arrivedAt = null;
    }

    @Override
    public void onTick() {
        // Registered lazily from the tick rather than onEnable, because a module
        // can be enabled from saved config before the SoundManager exists.
        class_1144 manager = this.mc.method_1483();
        if (manager != null && this.registeredWith != manager) {
            this.unregister();
            manager.method_4878(this.soundListener);
            this.registeredWith = manager;
        }

        // Coordinates mean nothing across a world or server switch.
        if (this.mc.field_1687 != this.lastWorld) {
            this.lastWorld = this.mc.field_1687;
            this.clusters.clear();
            this.marks.clear();
            this.clearTarget();
            this.arrivedAt = null;
        }

        // A fresh connection means a rejoin - most likely after a kick, which is
        // exactly when resuming the command spam would get us kicked again. A
        // dimension change keeps the same connection, so RTPing into another
        // dimension does not trip this.
        class_634 connection = this.mc.method_1562();
        if (connection != this.lastConnection) {
            if (this.lastConnection != null && this.autoRtp.isEnabled()) {
                this.autoRtp.setEnabled(false);
                this.say("§6[FarmFinder]§r §cAutoRTP off§r §7- reconnected");
            }
            this.lastConnection = connection;
        }

        long now = System.currentTimeMillis();
        if (this.autoRtp.isEnabled() && now - this.lastRtpAt >= (long) (this.rtpInterval.getValue() * 1000.0)) {
            this.lastRtpAt = now;
            this.sendRtp();
        }

        if (now - this.lastPruneAt > 30_000L) {
            this.lastPruneAt = now;
            this.clusters.values().removeIf(c -> now - c.lastSeenAt > CLUSTER_TTL_MS);
        }

        if (now - this.lastMarkPruneAt > 1000L) {
            this.lastMarkPruneAt = now;
            long ttl = (long) (this.highlightTime.getValue() * 1000.0);
            this.marks.values().removeIf(m -> now - m.at > ttl);
        }

        this.tickRoute(now);
    }

    private void unregister() {
        if (this.registeredWith != null) {
            this.registeredWith.method_4866(this.soundListener);
            this.registeredWith = null;
        }
    }

    private void onSound(class_1113 sound) {
        if (!this.isEnabled() || Cyemer.selfDestructed) {
            return;
        }
        class_2960 id = sound.method_4775();
        if (id == null) {
            return;
        }
        Source source = SOUNDS.get(id.method_12832());
        if (source == null || !this.familyEnabled(source.family())) {
            return;
        }
        this.report(source, sound.method_4784(), sound.method_4779(), sound.method_4778());
    }

    /**
     * Called from ClientWorldMixin with the state before and after a
     * server-driven block change. The position may be a mutable instance the
     * caller reuses, so its coordinates are read here and never kept.
     */
    public void onBlockUpdate(class_2338 pos, class_2680 before, class_2680 after) {
        if (!this.isEnabled() || !this.blockUpdates.isEnabled() || before == after) {
            return;
        }
        class_2248 block = after.method_26204();
        if (before.method_26204() != block) {
            return;
        }

        Source source;
        if (block == class_2246.field_10282) {
            source = OBSERVER_SOURCE;
        } else if (block == class_2246.field_10560 || block == class_2246.field_10615) {
            source = PISTON_SOURCE;
        } else {
            return;
        }
        if (!this.familyEnabled(source.family())) {
            return;
        }
        this.report(source, pos.method_10263() + 0.5, pos.method_10264() + 0.5, pos.method_10260() + 0.5);
    }

    private boolean familyEnabled(Family family) {
        return switch (family) {
            case PISTON -> this.pistons.isEnabled();
            case OBSERVER -> this.observers.isEnabled();
            case SMELTER -> this.smelters.isEnabled();
            case DISPENSER -> this.dispensers.isEnabled();
            case CRAFTER -> this.crafters.isEnabled();
            case REDSTONE -> this.redstone.isEnabled();
            case COMPOSTER -> this.composters.isEnabled();
        };
    }

    /**
     * One alert per family per area per cooldown. Repeats inside the cooldown
     * are counted rather than dropped, so the next alert can say how busy the
     * spot has been - a farm running continuously reads very differently from
     * one furnace crackling once.
     */
    private void report(Source source, double x, double y, double z) {
        class_746 self = this.mc.field_1724;
        if (self == null) {
            return;
        }

        double dx = x - self.method_23317();
        double dy = y - self.method_23318();
        double dz = z - self.method_23321();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < this.minDistance.getValue()) {
            return;
        }

        // Stop on detection itself, ahead of the cooldown and throttle below.
        // Those only decide whether to announce it again; if they gated this, a
        // spot that already alerted would let the next RTP carry us away from it.
        if (this.autoRtp.isEnabled()) {
            this.autoRtp.setEnabled(false);
            this.say("§6[FarmFinder]§r §cAutoRTP stopped§r §7- " + source.label() + " "
                    + (int) Math.round(distance) + "m " + compass(dx, dz)
                    + " §8(" + (int) Math.floor(x) + ", " + (int) Math.floor(y) + ", " + (int) Math.floor(z) + ")");
            if (Cyemer.getInstance().getModuleManager().getModule("Notifications") instanceof Notifications notifications) {
                notifications.warn("AutoRTP stopped", source.label() + " detected");
            }
        }

        // Highlighting and routing see every event, including the ones the
        // cooldown below keeps out of chat.
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);
        this.mark(source.family(), bx, by, bz);
        this.offerTarget(source, bx, by, bz, distance);

        int size = Math.max(1, (int) this.clusterSize.getValue());
        String key = source.family().name() + ":"
                + Math.floorDiv((int) Math.floor(x), size) + ":"
                + Math.floorDiv((int) Math.floor(y), size) + ":"
                + Math.floorDiv((int) Math.floor(z), size);

        long now = System.currentTimeMillis();
        Cluster cluster = this.clusters.computeIfAbsent(key, k -> new Cluster());
        cluster.lastSeenAt = now;
        if (now - cluster.lastAlertAt < (long) (this.cooldown.getValue() * 1000.0)) {
            cluster.suppressed++;
            return;
        }
        // Throttled globally: leave this cluster's cooldown untouched so its
        // next event is still free to alert.
        if (now - this.lastAnyAlertAt < GLOBAL_GAP_MS) {
            cluster.suppressed++;
            return;
        }

        int extra = cluster.suppressed;
        cluster.suppressed = 0;
        cluster.lastAlertAt = now;
        this.lastAnyAlertAt = now;

        this.emit(source.label(), (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z),
                (int) Math.round(distance), compass(dx, dz), extra);
    }

    private void emit(String label, int x, int y, int z, int distance, String direction, int extra) {
        String more = extra > 0 ? " §8(+" + extra + " since last)" : "";

        if (this.chat.isEnabled() && this.mc.field_1724 != null) {
            this.mc.field_1724.method_7353(class_2561.method_43470(
                    "§6[FarmFinder]§r §e" + label + "§r §7" + distance + "m " + direction
                            + "§r §8(" + x + ", " + y + ", " + z + ")" + more), false);
        }

        if (this.toast.isEnabled()
                && Cyemer.getInstance().getModuleManager().getModule("Notifications") instanceof Notifications notifications) {
            notifications.info("Farm Finder", label + " · " + distance + "m " + direction);
        }

        if (this.ping.isEnabled()) {
            class_1144 manager = this.mc.method_1483();
            if (manager != null) {
                manager.method_4873(class_1109.method_47978(class_3417.field_14622, 1.6F));
            }
        }
    }

    private void mark(Family family, int x, int y, int z) {
        long now = System.currentTimeMillis();
        long key = FarmRoute.key(x, y, z);
        Mark mark = this.marks.get(key);
        if (mark != null) {
            mark.at = now;
            mark.family = family;
            return;
        }
        if (this.marks.size() >= MAX_MARKS) {
            Long oldest = null;
            long oldestAt = Long.MAX_VALUE;
            for (Map.Entry<Long, Mark> entry : this.marks.entrySet()) {
                if (entry.getValue().at < oldestAt) {
                    oldestAt = entry.getValue().at;
                    oldest = entry.getKey();
                }
            }
            this.marks.remove(oldest);
        }
        this.marks.put(key, new Mark(x, y, z, family, now));
    }

    /**
     * The first find becomes the route target and keeps it until we arrive,
     * so a busy farm firing all over cannot keep re-planning under us. A
     * target left far behind (we RTP'd on) gives way to the new find.
     */
    private void offerTarget(Source source, int x, int y, int z, double distance) {
        if (!this.route.isEnabled() || distance < ARRIVE_DISTANCE + 4.0) {
            return;
        }
        if (this.arrivedAt != null && distSq(this.arrivedAt[0], this.arrivedAt[1], this.arrivedAt[2], x, y, z) < 48.0 * 48.0) {
            return;
        }
        class_746 self = this.mc.field_1724;
        if (this.target != null && self != null
                && distSq(this.target.x(), this.target.y(), this.target.z(), self.method_23317(), self.method_23318(), self.method_23321()) < 192.0 * 192.0) {
            return;
        }
        this.clearTarget();
        this.target = new Target(source.label(), x, y, z);
        this.plan();
    }

    private void clearTarget() {
        this.target = null;
        this.search = null;
        this.path = List.of();
        this.pathPartial = false;
        this.failNotified = false;
        this.routeAnnounced = false;
        this.cursor = 0;
        this.toMine = 0;
        this.toPlace = 0;
        this.offRouteSince = 0L;
    }

    /** Starts a search from where we stand; any current route stays drawn until the new one is ready. */
    private void plan() {
        class_746 self = this.mc.field_1724;
        class_638 world = this.mc.field_1687;
        if (self == null || world == null || this.target == null) {
            return;
        }
        this.lastPlanAt = System.currentTimeMillis();
        this.offRouteSince = 0L;
        // +0.5 so standing on a slab or soul sand counts as standing on top of it.
        int sx = (int) Math.floor(self.method_23317());
        int sy = (int) Math.floor(self.method_23318() + 0.5);
        int sz = (int) Math.floor(self.method_23321());
        this.search = new FarmRoute(world, sx, sy, sz, this.target.x(), this.target.y(), this.target.z(), this.routeMining.isEnabled());
    }

    private void tickRoute(long now) {
        class_746 self = this.mc.field_1724;
        if (self == null) {
            return;
        }
        if (this.arrivedAt != null
                && distSq(this.arrivedAt[0], this.arrivedAt[1], this.arrivedAt[2], self.method_23317(), self.method_23318(), self.method_23321()) > 96.0 * 96.0) {
            this.arrivedAt = null;
        }
        Target t = this.target;
        if (t == null) {
            return;
        }
        if (!this.route.isEnabled()) {
            this.clearTarget();
            return;
        }

        class_243 eye = self.method_33571();
        double distance = Math.sqrt(distSq(t.x(), t.y(), t.z(), eye.field_1352 - 0.5, eye.field_1351 - 0.5, eye.field_1350 - 0.5));
        if (distance > TARGET_DROP_DISTANCE) {
            this.clearTarget();
            return;
        }
        if (distance <= ARRIVE_DISTANCE) {
            this.say("§6[FarmFinder]§r §aArrived§r §7at the " + t.label() + " §8(" + t.x() + ", " + t.y() + ", " + t.z() + ")");
            this.arrivedAt = new int[]{t.x(), t.y(), t.z()};
            this.clearTarget();
            return;
        }

        if (this.search != null) {
            FarmRoute.Status status = this.search.step(PLAN_BUDGET_NS);
            if (status == FarmRoute.Status.SEARCHING) {
                return;
            }
            this.adopt(this.search, status);
            this.search = null;
        }

        if (this.path.isEmpty()) {
            if (now - this.lastPlanAt > 4000L) {
                this.plan(); // nothing found yet: try again as we move and chunks load
            }
            return;
        }
        this.follow(self, now);
    }

    private void adopt(FarmRoute done, FarmRoute.Status status) {
        if (status == FarmRoute.Status.FAILED) {
            if (this.path.isEmpty() && !this.failNotified) {
                this.failNotified = true;
                this.say("§6[FarmFinder]§r §7No route from here yet - follow the line, it re-plans as you move");
            }
            return; // an older route, if there is one, is still the best guide
        }
        this.path = done.path();
        this.pathPartial = status == FarmRoute.Status.PARTIAL;
        this.cursor = 0;
        this.recount();
        if (!this.routeAnnounced) {
            this.routeAnnounced = true;
            String extras = (this.toMine > 0 ? ", " + this.toMine + " to mine" : "")
                    + (this.toPlace > 0 ? ", " + this.toPlace + " to place" : "")
                    + (this.pathPartial ? " §8(partial - the rest is not loaded yet)" : "");
            this.say("§6[FarmFinder]§r §bRoute§r §7to the " + this.target.label() + ": "
                    + (this.path.size() - 1) + " steps" + extras);
        }
    }

    /**
     * Tracks how far along the route we are, and re-plans when we have
     * clearly left it or have walked to the end of a partial one.
     */
    private void follow(class_746 self, long now) {
        double px = self.method_23317();
        double py = self.method_23318();
        double pz = self.method_23321();
        int bestIndex = this.cursor;
        double bestSq = Double.MAX_VALUE;
        int from = Math.max(0, this.cursor - 4);
        int to = Math.min(this.path.size() - 1, this.cursor + 24);
        for (int i = from; i <= to; i++) {
            FarmRoute.Step step = this.path.get(i);
            double d = distSq(step.x() + 0.5, step.y(), step.z() + 0.5, px, py, pz);
            if (d < bestSq) {
                bestSq = d;
                bestIndex = i;
            }
        }
        this.cursor = bestIndex;
        this.recount();

        if (bestSq > 3.5 * 3.5) {
            if (this.offRouteSince == 0L) {
                this.offRouteSince = now;
            } else if (now - this.offRouteSince > 1200L && now - this.lastPlanAt > 2500L && this.search == null) {
                this.plan();
            }
        } else {
            this.offRouteSince = 0L;
        }
        if (this.pathPartial && this.cursor >= this.path.size() - 4 && now - this.lastPlanAt > 2500L && this.search == null) {
            this.plan();
        }
    }

    /** Blocks still to break and place ahead of us; ones already mined out are not counted. */
    private void recount() {
        int mine = 0;
        int place = 0;
        class_638 world = this.mc.field_1687;
        for (int i = this.cursor + 1; i < this.path.size(); i++) {
            FarmRoute.Step step = this.path.get(i);
            if (step.placed()) {
                place++;
            }
            int[] mined = step.mined();
            if (mined != null) {
                for (int j = 0; j < mined.length; j += 3) {
                    if (world == null || !this.isAir(world, mined[j], mined[j + 1], mined[j + 2])) {
                        mine++;
                    }
                }
            }
        }
        this.toMine = mine;
        this.toPlace = place;
    }

    private boolean isAir(class_638 world, int x, int y, int z) {
        return world.method_8320(this.probe.method_10103(x, y, z)).method_26215();
    }

    @Override
    public void onWorldRender(class_4587 matrices, float tickDelta) {
        class_746 self = this.mc.field_1724;
        class_638 world = this.mc.field_1687;
        if (self == null || world == null || Cyemer.selfDestructed) {
            return;
        }
        boolean drawMarks = this.highlight.isEnabled() && !this.marks.isEmpty();
        Target t = this.target;
        boolean drawRoute = this.route.isEnabled() && t != null;
        if (!drawMarks && !drawRoute) {
            return;
        }

        // Everything is placed relative to the camera in double precision
        // before it becomes float vertices, so lines stay steady at the large
        // coordinates RTP lands on.
        class_243 cam = this.mc.field_1773.method_19418().method_71156();
        double cx = cam.field_1352;
        double cy = cam.field_1351;
        double cz = cam.field_1350;
        long now = System.currentTimeMillis();

        try (class_9799 allocator = new class_9799(16384)) {
            class_4598 consumers = class_4597.method_22991(allocator);

            if (drawMarks) {
                float ttl = (float) (this.highlightTime.getValue() * 1000.0);
                for (Mark m : this.marks.values()) {
                    float age = now - m.at;
                    if (age > ttl) {
                        continue;
                    }
                    // Bright when it just fired, fading over the highlight time.
                    float alpha = 0.16F + 0.44F * (1.0F - age / ttl);
                    RenderUtils.drawBoxEsp(matrices, consumers, box(m.x, m.y, m.z, cx, cy, cz, 0.004), m.family.color, alpha, true);
                }
            }

            if (drawRoute) {
                Color color = this.routeColor.getValue();
                class_243 targetCenter = new class_243(t.x() + 0.5 - cx, t.y() + 0.5 - cy, t.z() + 0.5 - cz);
                List<FarmRoute.Step> shown = this.path;

                if (!shown.isEmpty()) {
                    int from = Math.max(0, Math.min(this.cursor, shown.size() - 1));
                    for (int i = from; i + 1 < shown.size(); i++) {
                        RenderUtils.drawLine(matrices, consumers, point(shown.get(i), cx, cy, cz), point(shown.get(i + 1), cx, cy, cz), color, 0.9F, true);
                    }
                    for (int i = from + 1; i < shown.size(); i++) {
                        FarmRoute.Step step = shown.get(i);
                        int[] mined = step.mined();
                        if (mined != null) {
                            for (int j = 0; j < mined.length; j += 3) {
                                if (!this.isAir(world, mined[j], mined[j + 1], mined[j + 2])) {
                                    RenderUtils.drawBoxEsp(matrices, consumers, box(mined[j], mined[j + 1], mined[j + 2], cx, cy, cz, 0.003), MINE_COLOR, 0.32F, true);
                                }
                            }
                        }
                        if (step.placed()) {
                            RenderUtils.drawBoxEsp(matrices, consumers, box(step.x(), step.y() - 1, step.z(), cx, cy, cz, -0.1), PLACE_COLOR, 0.3F, true);
                        }
                    }
                    if (this.pathPartial) {
                        // The rest is not loaded yet: point from the end of what is planned.
                        RenderUtils.drawLine(matrices, consumers, point(shown.get(shown.size() - 1), cx, cy, cz), targetCenter, color, 0.35F, true);
                    }
                } else {
                    // No route yet: a tracer from the crosshair.
                    class_243 look = self.method_5828(tickDelta);
                    class_243 start = new class_243(look.field_1352 * 0.6, look.field_1351 * 0.6, look.field_1350 * 0.6);
                    RenderUtils.drawLine(matrices, consumers, start, targetCenter, color, 0.8F, true);
                }

                float pulse = 0.35F + 0.2F * (float) Math.sin(now / 180.0);
                RenderUtils.drawBoxEsp(matrices, consumers, box(t.x(), t.y(), t.z(), cx, cy, cz, 0.03), color, pulse, true);
                // A beam straight up, findable from anywhere around it.
                RenderUtils.drawLine(matrices, consumers,
                        new class_243(targetCenter.field_1352, targetCenter.field_1351 + 0.5, targetCenter.field_1350),
                        new class_243(targetCenter.field_1352, targetCenter.field_1351 + 64.0, targetCenter.field_1350), color, 0.55F, true);
            }

            consumers.method_22993();
        }
    }

    @Override
    public void onRender(class_332 context, float tickDelta) {
        Target t = this.target;
        class_746 self = this.mc.field_1724;
        if (!this.routeHud.isEnabled() || !this.route.isEnabled() || t == null || self == null || Cyemer.selfDestructed) {
            return;
        }
        double dx = t.x() + 0.5 - self.method_23317();
        double dz = t.z() + 0.5 - self.method_23321();
        int dy = t.y() - (int) Math.floor(self.method_23318());
        int horizontal = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        String vertical = dy > 0 ? dy + " up" : (dy < 0 ? -dy + " down" : "level");
        String line1 = t.label() + "  " + horizontal + "m " + compass(dx, dz) + "  " + vertical;

        String line2;
        if (this.path.isEmpty()) {
            line2 = this.search != null ? "planning route..." : "no route yet - follow the line";
        } else {
            line2 = Math.max(0, this.path.size() - 1 - this.cursor) + " steps"
                    + (this.toMine > 0 ? " · " + this.toMine + " to mine" : "")
                    + (this.toPlace > 0 ? " · " + this.toPlace + " to place" : "")
                    + (this.pathPartial ? " · partial" : "");
        }

        int centerX = context.method_51421() / 2;
        int y = context.method_51443() / 2 + 22;
        int color = this.routeColor.getValue().getRGB() | 0xFF000000;
        this.centered(context, line1, centerX, y, color);
        this.centered(context, line2, centerX, y + 10, 0xFFAAAAAA);
    }

    private void centered(class_332 context, String text, int centerX, int y, int color) {
        int x = centerX - this.mc.field_1772.method_1727(text) / 2;
        context.method_25303(this.mc.field_1772, text, x + 1, y + 1, 0x80000000);
        context.method_25303(this.mc.field_1772, text, x, y, color);
    }

    /** The line runs just above the floor through the middle of each block. */
    private static class_243 point(FarmRoute.Step step, double cx, double cy, double cz) {
        return new class_243(step.x() + 0.5 - cx, step.y() + 0.08 - cy, step.z() + 0.5 - cz);
    }

    /** A block's box relative to the camera, grown (or shrunk, when negative) by pad. */
    private static class_238 box(int x, int y, int z, double cx, double cy, double cz, double pad) {
        return new class_238(x - pad - cx, y - pad - cy, z - pad - cz, x + 1 + pad - cx, y + 1 + pad - cy, z + 1 + pad - cz);
    }

    private static double distSq(double ax, double ay, double az, double bx, double by, double bz) {
        double dx = ax - bx;
        double dy = ay - by;
        double dz = az - bz;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Sent as a command, not chat. A message starting with a slash that goes
     * through the chat path is posted to chat as literal text; the chat screen
     * strips the slash and routes commands through method_45730 instead, so
     * this does the same. Accepts the setting with or without its slash.
     */
    private void sendRtp() {
        if (this.mc.field_1724 == null || this.mc.field_1724.field_3944 == null) {
            return;
        }
        String command = this.rtpCommand.getValue() == null ? "" : this.rtpCommand.getValue().trim();
        while (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (command.isEmpty()) {
            return;
        }
        this.mc.field_1724.field_3944.method_45730(command);
    }

    private void say(String message) {
        if (this.mc.field_1724 != null) {
            this.mc.field_1724.method_7353(class_2561.method_43470(message), false);
        }
    }

    /** Eight-point heading from us to the source; north is -Z in Minecraft. */
    private static String compass(double dx, double dz) {
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        if (angle < 0.0) {
            angle += 360.0;
        }
        return COMPASS[(int) Math.round(angle / 45.0) % 8];
    }
}
