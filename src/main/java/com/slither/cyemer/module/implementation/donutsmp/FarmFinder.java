package com.slither.cyemer.module.implementation.donutsmp;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.event.BlockUpdateListener;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ColorSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.module.StringSetting;
import com.slither.cyemer.module.implementation.Notifications;
import com.slither.cyemer.util.AutoRtp;
import com.slither.cyemer.util.render.RenderUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1311;
import net.minecraft.class_1321;
import net.minecraft.class_2586;
import net.minecraft.class_2591;
import net.minecraft.class_2636;
import net.minecraft.class_2680;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_3417;
import net.minecraft.class_3486;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4597.class_4598;
import net.minecraft.class_634;
import net.minecraft.class_638;
import net.minecraft.class_746;
import net.minecraft.class_2818;
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
 * block change in a loaded chunk, so this channel reaches as far as the
 * client can see. Observers make no sound at all in 1.21.11, but a pulse
 * flips their powered state and that change is synced to every client
 * watching the chunk; likewise a furnace lighting, a dropper firing, a hopper
 * locking, a comparator reading a chest, dust carrying power. Only a state
 * change on a block that was already that block counts, so placing,
 * breaking, chunk loading or an anti-xray reveal never trips it.
 *
 * The stash scan reads the block entities the server sends with every chunk
 * (chests, shulkers, hoppers, spawners...) across all loaded chunks, so it
 * finds a base whether or not anything in it is running. Dense spots alert
 * once each, scored so natural loot stays quiet: a shulker box never
 * generates and alerts on its own, spawners count 3 and hoppers 2 toward the
 * threshold, everything else 1 - a dungeon (spawner and two chests) is 5.
 * Flag Spawners goes further and calls out every mob spawner by itself,
 * with the mob it spawns, past the threshold and the density filter.
 *
 * The density filter holds back any find - activity or stash - with fewer
 * than Filter Min Blocks distinct detected blocks within Filter Radius chunks
 * of it, counting both recent activity and the scanned block entities, so a
 * lone furnace or a village chest stays quiet while a real farm, which keeps
 * firing from block after block, gets through once enough of it has shown.
 *
 * Every block that fires is highlighted through walls for a while, and the
 * first find becomes a route target: FarmRoute plans a walking path to beside
 * it over the loaded chunks, mining through only where it has to, and the
 * path, the blocks to break and the target are drawn in the world.
 */
@Environment(EnvType.CLIENT)
public class FarmFinder extends Module implements BlockUpdateListener {
    /** Floor between any two alerts, so flying over a mega-farm cannot flood chat. */
    private static final long GLOBAL_GAP_MS = 300L;
    /** Clusters unheard for this long are forgotten. */
    private static final long CLUSTER_TTL_MS = 5L * 60L * 1000L;
    private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    private static final int MAX_MARKS = 2000;
    /** Eye to target centre: close enough to see and reach it. */
    private static final double ARRIVE_DISTANCE = 4.0;
    /** This much further from the target than when it was found means we teleported away. */
    private static final double TARGET_DROP_SLACK = 128.0;
    private static final long STASH_SCAN_MS = 2500L;
    /** Chunks either side of us to look at; anything loaded is inside this. */
    private static final int STASH_SCAN_RADIUS = 32;
    /** Stashes announced individually per scan; the rest are summed up in one line. */
    private static final int STASH_ANNOUNCE_MAX = 3;
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
        COMPOSTER(new Color(180, 130, 70)),
        HOPPER(new Color(130, 140, 165)),
        CHEST(new Color(215, 150, 60)),
        SHULKER(new Color(220, 100, 255)),
        SPAWNER(new Color(255, 50, 170)),
        BASE(new Color(60, 255, 200)),
        CROP(new Color(150, 230, 70)),
        VILLAGER(new Color(90, 200, 120)),
        ANIMAL(new Color(240, 200, 150)),
        MOB(new Color(200, 60, 60)),
        ITEM(new Color(255, 255, 140)),
        BASE_SIGN(new Color(120, 220, 255));

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

    /** Blocks whose state flipping in place means a machine ran. Built on first use, after Blocks exists. */
    private static Map<class_2248, Source> blockSources;
    /** Block entity types the stash scan counts. */
    private static Map<class_2591<?>, Source> stashSources;

    private static Map<class_2248, Source> blockSources() {
        if (blockSources == null) {
            Map<class_2248, Source> m = new HashMap<>();
            m.put(class_2246.field_10282, new Source(Family.OBSERVER, "Observer"));
            Source piston = new Source(Family.PISTON, "Piston");
            m.put(class_2246.field_10560, piston);
            m.put(class_2246.field_10615, piston);
            m.put(class_2246.field_10181, new Source(Family.SMELTER, "Furnace"));
            m.put(class_2246.field_16334, new Source(Family.SMELTER, "Smoker"));
            m.put(class_2246.field_16333, new Source(Family.SMELTER, "Blast Furnace"));
            m.put(class_2246.field_10200, new Source(Family.DISPENSER, "Dispenser"));
            m.put(class_2246.field_10228, new Source(Family.DISPENSER, "Dropper"));
            m.put(class_2246.field_46797, new Source(Family.CRAFTER, "Crafter"));
            m.put(class_2246.field_10312, new Source(Family.HOPPER, "Hopper"));
            m.put(class_2246.field_17563, new Source(Family.COMPOSTER, "Composter"));
            m.put(class_2246.field_10091, new Source(Family.REDSTONE, "Redstone Dust"));
            Source torch = new Source(Family.REDSTONE, "Redstone Torch");
            m.put(class_2246.field_10523, torch);
            m.put(class_2246.field_10301, torch);
            m.put(class_2246.field_10450, new Source(Family.REDSTONE, "Repeater"));
            m.put(class_2246.field_10377, new Source(Family.REDSTONE, "Comparator"));
            m.put(class_2246.field_10524, new Source(Family.REDSTONE, "Redstone Lamp"));
            m.put(class_2246.field_10179, new Source(Family.REDSTONE, "Note Block"));
            m.put(class_2246.field_22422, new Source(Family.REDSTONE, "Target Block"));
            m.put(class_2246.field_10425, new Source(Family.REDSTONE, "Powered Rail"));
            m.put(class_2246.field_10025, new Source(Family.REDSTONE, "Detector Rail"));
            m.put(class_2246.field_10546, new Source(Family.REDSTONE, "Activator Rail"));
            Source bulb = new Source(Family.REDSTONE, "Copper Bulb");
            for (class_2248 b : new class_2248[]{
                    class_2246.field_47072, class_2246.field_47073, class_2246.field_47074, class_2246.field_47075,
                    class_2246.field_47076, class_2246.field_47077, class_2246.field_47078, class_2246.field_47079}) {
                m.put(b, bulb);
            }
            // Crops whose age the server sends as an in-place state change.
            m.put(class_2246.field_10293, new Source(Family.CROP, "Wheat"));
            m.put(class_2246.field_10609, new Source(Family.CROP, "Carrots"));
            m.put(class_2246.field_10247, new Source(Family.CROP, "Potatoes"));
            m.put(class_2246.field_10341, new Source(Family.CROP, "Beetroots"));
            m.put(class_2246.field_9974, new Source(Family.CROP, "Nether Wart"));
            m.put(class_2246.field_10302, new Source(Family.CROP, "Cocoa"));
            m.put(class_2246.field_16999, new Source(Family.CROP, "Sweet Berries"));
            Source sculk = new Source(Family.REDSTONE, "Sculk Sensor");
            m.put(class_2246.field_28108, sculk);
            m.put(class_2246.field_43231, sculk);
            blockSources = m;
        }
        return blockSources;
    }

    /** Plants that grow upward; each group shares one Source so a block can be matched to what is under it. */
    private static Map<class_2248, Source> growthSources;

    private static Map<class_2248, Source> growthSources() {
        if (growthSources == null) {
            Map<class_2248, Source> m = new HashMap<>();
            m.put(class_2246.field_10424, new Source(Family.CROP, "Sugar Cane"));
            m.put(class_2246.field_10029, new Source(Family.CROP, "Cactus"));
            m.put(class_2246.field_10211, new Source(Family.CROP, "Bamboo"));
            Source kelp = new Source(Family.CROP, "Kelp");
            m.put(class_2246.field_9993, kelp);
            m.put(class_2246.field_10463, kelp);
            growthSources = m;
        }
        return growthSources;
    }

    private static Map<class_2591<?>, Source> stashSources() {
        if (stashSources == null) {
            Map<class_2591<?>, Source> m = new HashMap<>();
            m.put(class_2591.field_11914, new Source(Family.CHEST, "Chest"));
            m.put(class_2591.field_11891, new Source(Family.CHEST, "Trapped Chest"));
            m.put(class_2591.field_16411, new Source(Family.CHEST, "Barrel"));
            m.put(class_2591.field_11901, new Source(Family.CHEST, "Ender Chest"));
            m.put(class_2591.field_11896, new Source(Family.SHULKER, "Shulker"));
            m.put(class_2591.field_11888, new Source(Family.HOPPER, "Hopper"));
            m.put(class_2591.field_11889, new Source(Family.SPAWNER, "Spawner"));
            m.put(class_2591.field_11903, new Source(Family.SMELTER, "Furnace"));
            m.put(class_2591.field_16415, new Source(Family.SMELTER, "Blast Furnace"));
            m.put(class_2591.field_16414, new Source(Family.SMELTER, "Smoker"));
            m.put(class_2591.field_11887, new Source(Family.DISPENSER, "Dispenser"));
            m.put(class_2591.field_11899, new Source(Family.DISPENSER, "Dropper"));
            m.put(class_2591.field_46808, new Source(Family.CRAFTER, "Crafter"));
            m.put(class_2591.field_11894, new Source(Family.BASE, "Brewing Stand"));
            m.put(class_2591.field_11890, new Source(Family.BASE, "Beacon"));
            m.put(class_2591.field_11912, new Source(Family.BASE, "Enchanting Table"));
            stashSources = m;
        }
        return stashSources;
    }

    /** How strongly a block says "somebody lives here". Shulkers never generate, so one is enough. */
    private static int stashWeight(Family family) {
        return switch (family) {
            case SHULKER -> 1000;
            case SPAWNER -> 3;
            case HOPPER -> 2;
            default -> 1;
        };
    }

    private static FarmFinder instance;

    private final BooleanSetting pistons = new BooleanSetting("Pistons", true);
    private final BooleanSetting observers = new BooleanSetting("Observers", true);
    private final BooleanSetting smelters = new BooleanSetting("Smelters", true);
    private final BooleanSetting dispensers = new BooleanSetting("Dispensers", true);
    private final BooleanSetting crafters = new BooleanSetting("Crafters", true);
    private final BooleanSetting redstone = new BooleanSetting("Redstone", true);
    private final BooleanSetting composters = new BooleanSetting("Composters", true);
    private final BooleanSetting hoppers = new BooleanSetting("Hoppers", true);
    private final BooleanSetting chests = new BooleanSetting("Chests", true);
    private final BooleanSetting shulkers = new BooleanSetting("Shulkers", true);
    private final BooleanSetting spawners = new BooleanSetting("Spawners", true);
    private final BooleanSetting flagSpawners = new BooleanSetting("Flag Spawners", true);
    private final BooleanSetting baseBlocks = new BooleanSetting("Base Blocks", true);
    private final BooleanSetting crops = new BooleanSetting("Crops", true);
    private final BooleanSetting villagerFarms = new BooleanSetting("Villager Farms", true);
    private final BooleanSetting animalFarms = new BooleanSetting("Animal Farms", true);
    private final BooleanSetting mobFarms = new BooleanSetting("Mob Farms", true);
    private final BooleanSetting itemStreams = new BooleanSetting("Item Streams", true);
    private final BooleanSetting baseSigns = new BooleanSetting("Base Signs", true);
    private final BooleanSetting blockUpdates = new BooleanSetting("Block Updates", true);
    private final BooleanSetting stashScan = new BooleanSetting("Stash Scan", true);
    private final SliderSetting stashThreshold = new SliderSetting("Stash Threshold", 6.0, 1.0, 40.0, 0);
    private final BooleanSetting densityFilter = new BooleanSetting("Density Filter", true);
    private final SliderSetting densityRadius = new SliderSetting("Filter Radius (chunks)", 4.0, 1.0, 8.0, 0);
    private final SliderSetting densityMin = new SliderSetting("Filter Min Blocks", 5.0, 2.0, 100.0, 0);
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

    /** Access-ordered, so when it is full the mark refreshed longest ago is the one dropped. */
    private final Map<Long, Mark> marks = new LinkedHashMap<>(256, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Mark> eldest) {
            return this.size() > MAX_MARKS;
        }
    };
    /** Spawners already flagged this session, as FarmRoute keys. */
    private final Set<Long> flaggedSpawners = new HashSet<>();
    /** Centres of stashes already announced this session. */
    private final List<int[]> stashAlerts = new ArrayList<>();
    private long lastStashScanAt = 0L;
    private long lastStashSignature = 0L;
    private int lastStashCount = -1;
    private long lastStashScoreAt = 0L;
    /** Block entities the last scan counted, as FarmRoute keys. */
    private Set<Long> stashPositions = new HashSet<>();
    /** Distinct detected blocks per chunk (FarmRoute key of cx, 0, cz), for the density filter. */
    private final Map<Long, Integer> chunkCounts = new HashMap<>();
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

    private record Target(String label, int x, int y, int z, double foundAt) {}

    private record Found(int x, int y, int z, Source source) {}

    private record Stash(int x, int y, int z, String summary, int total, double distance) {}

    /** mob is null when the server has not told the client what it spawns. */
    private record SpawnerHit(int x, int y, int z, String mob, double distance) {}

    private record Body(double x, double y, double z, boolean still) {}

    private record Cluster2(Body center, List<Body> members) {}

    private record EntityHit(String group, Family family, String label, String detail, Cluster2 cluster) {}

    private record EntityAlert(String group, double x, double y, double z) {}

    /** Where each tracked villager has been standing, and since when. */
    private static final class Track {
        double x;
        double y;
        double z;
        long since;
        long seenAt;
    }

    private final Map<Integer, Track> tracks = new HashMap<>();
    private final List<EntityAlert> entityAlerts = new ArrayList<>();

    public FarmFinder() {
        super("FarmFinder", "Alerts when redstone farm parts fire nearby: pistons, observers, smelters and more.", Category.DONUT_SMP);
        this.addSetting(this.pistons);
        this.addSetting(this.observers);
        this.addSetting(this.smelters);
        this.addSetting(this.dispensers);
        this.addSetting(this.crafters);
        this.addSetting(this.redstone);
        this.addSetting(this.composters);
        this.addSetting(this.hoppers);
        this.addSetting(this.chests);
        this.addSetting(this.shulkers);
        this.addSetting(this.spawners);
        this.addSetting(this.flagSpawners);
        this.addSetting(this.baseBlocks);
        this.addSetting(this.crops);
        this.addSetting(this.villagerFarms);
        this.addSetting(this.animalFarms);
        this.addSetting(this.mobFarms);
        this.addSetting(this.itemStreams);
        this.addSetting(this.baseSigns);
        this.addSetting(this.blockUpdates);
        this.addSetting(this.stashScan);
        this.addSetting(this.stashThreshold);
        this.addSetting(this.densityFilter);
        this.addSetting(this.densityRadius);
        this.addSetting(this.densityMin);
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
        this.resetStashes();
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
            this.resetStashes();
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
            this.rebuildDensity();
        }

        if (now - this.lastStashScanAt >= STASH_SCAN_MS) {
            this.lastStashScanAt = now;
            if (this.stashScan.isEnabled() || this.flagSpawners.isEnabled()) {
                this.scanStashes();
            }
            this.scanEntities();
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
            Source grew = this.growth(pos, before, block);
            if (grew != null && this.familyEnabled(grew.family())) {
                this.report(grew, pos.method_10263() + 0.5, pos.method_10264() + 0.5, pos.method_10260() + 0.5);
            }
            return;
        }

        Source source = blockSources().get(block);
        if (source == null || !this.familyEnabled(source.family())) {
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
            case HOPPER -> this.hoppers.isEnabled();
            case CHEST -> this.chests.isEnabled();
            case SHULKER -> this.shulkers.isEnabled();
            case SPAWNER -> this.spawners.isEnabled();
            case BASE -> this.baseBlocks.isEnabled();
            case CROP -> this.crops.isEnabled();
            case VILLAGER -> this.villagerFarms.isEnabled();
            case ANIMAL -> this.animalFarms.isEnabled();
            case MOB -> this.mobFarms.isEnabled();
            case ITEM -> this.itemStreams.isEnabled();
            case BASE_SIGN -> this.baseSigns.isEnabled();
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

        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);

        // Highlighted whatever happens next: it also counts toward the density
        // of every later find around it, including its own.
        this.mark(source.family(), bx, by, bz);
        if (!this.passesDensity(bx, bz)) {
            return;
        }

        // Stop on detection itself, ahead of the cooldown and throttle below.
        // Those only decide whether to announce it again; if they gated this, a
        // spot that already alerted would let the next RTP carry us away from it.
        this.stopAutoRtp(source.label(), bx, by, bz, distance, dx, dz);
        // Routing sees every event that passes, including the ones the
        // cooldown below keeps out of chat.
        this.offerTarget(source.label(), bx, by, bz, distance);

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

    /**
     * Farms that are mostly mobs, read off the entities the server tracks
     * around us (about 48 blocks for mobs, 32 for items). Every rule needs a
     * crowd far denser than anything that spawns naturally: a village keeps
     * its villagers walking and spread out, a herd spawns 2 to 4 together.
     */
    private void scanEntities() {
        class_746 self = this.mc.field_1724;
        class_638 world = this.mc.field_1687;
        if (self == null || world == null) {
            return;
        }
        boolean villagers = this.villagerFarms.isEnabled();
        boolean animals = this.animalFarms.isEnabled();
        boolean mobs = this.mobFarms.isEnabled();
        boolean items = this.itemStreams.isEnabled();
        boolean signs = this.baseSigns.isEnabled();
        if (!villagers && !animals && !mobs && !items && !signs) {
            return;
        }
        long now = System.currentTimeMillis();
        List<Body> villagerList = new ArrayList<>();
        List<Body> golems = new ArrayList<>();
        List<Body> monsters = new ArrayList<>();
        List<Body> itemList = new ArrayList<>();
        List<Body> carts = new ArrayList<>();
        List<Body> decor = new ArrayList<>();
        List<Body> pets = new ArrayList<>();
        Map<String, List<Body>> herds = new HashMap<>();

        for (class_1297 entity : world.method_18112()) {
            if (entity == self || !entity.method_5805()) {
                continue;
            }
            class_1299<?> type = entity.method_5864();
            if (type == class_1299.field_6097) {
                continue; // players are not farms
            }
            double x = entity.method_23317();
            double y = entity.method_23318();
            double z = entity.method_23321();
            if (type == class_1299.field_6077) {
                if (villagers) {
                    villagerList.add(new Body(x, y, z, this.still(entity.method_5628(), x, y, z, now)));
                }
            } else if (type == class_1299.field_6147) {
                if (villagers) {
                    golems.add(new Body(x, y, z, false));
                }
            } else if (type == class_1299.field_6052) {
                // Our own drops and pickups are not a farm.
                if (items && distSq(x, y, z, self.method_23317(), self.method_23318(), self.method_23321()) > 36.0) {
                    itemList.add(new Body(x, y, z, false));
                }
            } else if (type == class_1299.field_6058 || type == class_1299.field_6126) {
                if (items) {
                    carts.add(new Body(x, y, z, false));
                }
            } else if (type == class_1299.field_6043 || type == class_1299.field_28401 || type == class_1299.field_6131) {
                if (signs) {
                    decor.add(new Body(x, y, z, false)); // item frames and armor stands never generate
                }
            } else if (signs && isPet(entity, self)) {
                pets.add(new Body(x, y, z, false));
            } else if (mobs && type.method_5891() == class_1311.field_6302) {
                monsters.add(new Body(x, y, z, false));
            } else if (animals && type.method_5891() == class_1311.field_6294) {
                herds.computeIfAbsent(type.method_5897().getString(), k -> new ArrayList<>()).add(new Body(x, y, z, false));
            }
        }
        this.tracks.values().removeIf(t -> now - t.seenAt > 30_000L);

        List<EntityHit> hits = new ArrayList<>();
        if (villagers) {
            this.villagerClusters(villagerList, golems, hits);
        }
        if (animals) {
            for (Map.Entry<String, List<Body>> herd : herds.entrySet()) {
                for (Cluster2 c : clusters(herd.getValue(), 7.0, 12)) {
                    hits.add(new EntityHit("animal:" + herd.getKey(), Family.ANIMAL, herd.getKey() + " farm",
                            c.members().size() + " " + herd.getKey().toLowerCase() + "s packed together", c));
                }
            }
        }
        if (mobs) {
            for (Cluster2 c : clusters(monsters, 6.0, 12)) {
                hits.add(new EntityHit("mobs", Family.MOB, "Mob farm", c.members().size() + " hostile mobs packed together", c));
            }
        }
        if (items) {
            for (Cluster2 c : clusters(itemList, 5.0, 25)) {
                hits.add(new EntityHit("items", Family.ITEM, "Items piling up", c.members().size() + " dropped items in one spot", c));
            }
            for (Cluster2 c : clusters(carts, 10.0, 3)) {
                hits.add(new EntityHit("carts", Family.ITEM, "Minecart system", c.members().size() + " hopper/chest minecarts", c));
            }
        }
        if (signs) {
            for (Cluster2 c : clusters(decor, 10.0, 4)) {
                hits.add(new EntityHit("decor", Family.BASE_SIGN, "Base decor", c.members().size() + " item frames / armor stands", c));
            }
            for (Cluster2 c : clusters(pets, 16.0, 2)) {
                hits.add(new EntityHit("pets", Family.BASE_SIGN, "Pets", c.members().size() + " tamed or named mobs", c));
            }
        }
        this.announceEntityHits(self, hits);
    }

    /**
     * Villagers packed together and stuck in place are a trading hall or a
     * breeder; with iron golems among them, an iron farm. Golems alone in a
     * crowd are an iron farm's kill chamber.
     */
    private void villagerClusters(List<Body> villagerList, List<Body> golems, List<EntityHit> hits) {
        List<Cluster2> found = clusters(villagerList, 6.0, 6);
        for (Cluster2 c : found) {
            int stuck = 0;
            for (Body b : c.members()) {
                if (b.still()) {
                    stuck++;
                }
            }
            if (stuck < 4) {
                continue; // walking around: a village, not a farm
            }
            int golemsNear = 0;
            for (Body g : golems) {
                if (distSq(g.x(), g.y(), g.z(), c.center().x(), c.center().y(), c.center().z()) <= 100.0) {
                    golemsNear++;
                }
            }
            if (golemsNear > 0) {
                hits.add(new EntityHit("villagers", Family.VILLAGER, "Iron farm",
                        c.members().size() + " villagers (" + stuck + " stuck), " + golemsNear + " iron golem" + (golemsNear == 1 ? "" : "s"), c));
            } else {
                hits.add(new EntityHit("villagers", Family.VILLAGER, "Villager hall",
                        c.members().size() + " villagers, " + stuck + " stuck in place", c));
            }
        }
        for (Cluster2 c : clusters(golems, 8.0, 3)) {
            hits.add(new EntityHit("golems", Family.VILLAGER, "Iron farm", c.members().size() + " iron golems together", c));
        }
    }

    /** Whether an entity has stayed within 1.5 blocks for the last 8 seconds. */
    private boolean still(int id, double x, double y, double z, long now) {
        Track t = this.tracks.get(id);
        if (t == null || distSq(x, y, z, t.x, t.y, t.z) > 2.25) {
            t = new Track();
            t.x = x;
            t.y = y;
            t.z = z;
            t.since = now;
            this.tracks.put(id, t);
        }
        t.seenAt = now;
        return now - t.since >= 8_000L;
    }

    /** Tamed by someone else, or given a name: a player put it there. */
    private static boolean isPet(class_1297 entity, class_746 self) {
        if (entity instanceof class_1321 tame && tame.method_6181()) {
            return !tame.method_6171(self);
        }
        return entity.method_16914();
    }

    /**
     * Densest spots first: each body's neighbours within radius, best
     * counts taken first, and nothing within two radii of a spot already
     * taken. Entity counts are small, so the square scan is fine.
     */
    private static List<Cluster2> clusters(List<Body> bodies, double radius, int min) {
        List<Cluster2> out = new ArrayList<>();
        if (bodies.size() < min) {
            return out;
        }
        double r2 = radius * radius;
        List<Cluster2> candidates = new ArrayList<>();
        for (Body b : bodies) {
            List<Body> members = new ArrayList<>();
            for (Body o : bodies) {
                if (distSq(b.x(), b.y(), b.z(), o.x(), o.y(), o.z()) <= r2) {
                    members.add(o);
                }
            }
            if (members.size() >= min) {
                candidates.add(new Cluster2(b, members));
            }
        }
        candidates.sort((a, c) -> Integer.compare(c.members().size(), a.members().size()));
        double apart = 4.0 * r2;
        for (Cluster2 c : candidates) {
            boolean overlaps = false;
            for (Cluster2 taken : out) {
                if (distSq(c.center().x(), c.center().y(), c.center().z(), taken.center().x(), taken.center().y(), taken.center().z()) < apart) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                out.add(c);
            }
        }
        return out;
    }

    private void announceEntityHits(class_746 self, List<EntityHit> hits) {
        double px = self.method_23317();
        double py = self.method_23318();
        double pz = self.method_23321();
        List<EntityHit> fresh = new ArrayList<>();
        for (EntityHit hit : hits) {
            Body c = hit.cluster().center();
            // Mobs drift, so anything of the same kind within 24 blocks of an
            // earlier announcement is that same farm.
            boolean known = false;
            for (EntityAlert a : this.entityAlerts) {
                if (a.group().equals(hit.group()) && distSq(a.x(), a.y(), a.z(), c.x(), c.y(), c.z()) < 24.0 * 24.0) {
                    known = true;
                    break;
                }
            }
            if (known || Math.sqrt(distSq(c.x(), c.y(), c.z(), px, py, pz)) < this.minDistance.getValue()) {
                continue;
            }
            if (this.entityAlerts.size() >= 300) {
                this.entityAlerts.remove(0);
            }
            this.entityAlerts.add(new EntityAlert(hit.group(), c.x(), c.y(), c.z()));
            for (Body b : hit.cluster().members()) {
                this.mark(hit.family(), (int) Math.floor(b.x()), (int) Math.floor(b.y()), (int) Math.floor(b.z()));
            }
            fresh.add(hit);
        }
        if (fresh.isEmpty()) {
            return;
        }
        fresh.sort(Comparator.comparingDouble(h -> distSq(h.cluster().center().x(), h.cluster().center().y(), h.cluster().center().z(), px, py, pz)));
        for (int i = 0; i < fresh.size(); i++) {
            EntityHit hit = fresh.get(i);
            Body c = hit.cluster().center();
            int bx = (int) Math.floor(c.x());
            int by = (int) Math.floor(c.y());
            int bz = (int) Math.floor(c.z());
            double dx = c.x() - px;
            double dz = c.z() - pz;
            double distance = Math.sqrt(distSq(c.x(), c.y(), c.z(), px, py, pz));
            if (i == 0) {
                this.stopAutoRtp(hit.label(), bx, by, bz, distance, dx, dz);
                this.offerTarget(hit.label(), bx, by, bz, distance);
            }
            if (i >= STASH_ANNOUNCE_MAX) {
                continue;
            }
            int shown = (int) Math.round(distance);
            String direction = compass(dx, dz);
            if (this.chat.isEnabled()) {
                this.say("§6[FarmFinder]§r §e" + hit.label() + "§r §7" + shown + "m " + direction
                        + " §8(" + bx + ", " + by + ", " + bz + ")§r §7- " + hit.detail());
            }
            if (this.toast.isEnabled()
                    && Cyemer.getInstance().getModuleManager().getModule("Notifications") instanceof Notifications notifications) {
                notifications.info("Farm Finder", hit.label() + " · " + shown + "m " + direction);
            }
            if (i == 0 && this.ping.isEnabled()) {
                class_1144 manager = this.mc.method_1483();
                if (manager != null) {
                    manager.method_4873(class_1109.method_47978(class_3417.field_14622, 1.6F));
                }
            }
        }
        if (fresh.size() > STASH_ANNOUNCE_MAX) {
            this.say("§6[FarmFinder]§r §e+" + (fresh.size() - STASH_ANNOUNCE_MAX) + " more mob farms§r §7in range, highlighted");
        }
    }

    /**
     * Sugar cane, cactus, bamboo and kelp grow by adding a block on top of
     * themselves, which arrives as a new block rather than a state change.
     * The server never sends their age ticking up, only that new block.
     */
    private Source growth(class_2338 pos, class_2680 before, class_2248 grown) {
        Source source = growthSources().get(grown);
        class_638 world = this.mc.field_1687;
        if (source == null || world == null) {
            return null;
        }
        boolean wasOpen = before.method_26215() || before.method_26227().method_15767(class_3486.field_15517);
        if (!wasOpen) {
            return null;
        }
        class_2248 below = world.method_8320(this.probe.method_10103(pos.method_10263(), pos.method_10264() - 1, pos.method_10260())).method_26204();
        return growthSources().get(below) == source ? source : null;
    }

    private void resetStashes() {
        this.stashAlerts.clear();
        this.flaggedSpawners.clear();
        this.entityAlerts.clear();
        this.tracks.clear();
        this.lastStashSignature = 0L;
        this.lastStashCount = -1;
        this.stashPositions = new HashSet<>();
        this.chunkCounts.clear();
    }

    /**
     * Recounts detected blocks per chunk from scratch: recent activity marks
     * and the last scan's block entities, each position once, so a hopper
     * that both ticked and was scanned is not counted twice.
     */
    private void rebuildDensity() {
        Set<Long> positions = new HashSet<>(this.stashPositions);
        positions.addAll(this.marks.keySet());
        this.chunkCounts.clear();
        for (long pos : positions) {
            this.chunkCounts.merge(FarmRoute.key(FarmRoute.unpackX(pos) >> 4, 0, FarmRoute.unpackZ(pos) >> 4), 1, Integer::sum);
        }
    }

    /** Whether enough detected blocks lie within the filter radius of this one to count it. */
    private boolean passesDensity(int x, int z) {
        if (!this.densityFilter.isEnabled()) {
            return true;
        }
        int radius = (int) this.densityRadius.getValue();
        int needed = (int) this.densityMin.getValue();
        int cx = x >> 4;
        int cz = z >> 4;
        int total = 0;
        for (int ox = -radius; ox <= radius; ox++) {
            for (int oz = -radius; oz <= radius; oz++) {
                Integer count = this.chunkCounts.get(FarmRoute.key(cx + ox, 0, cz + oz));
                if (count != null && (total += count) >= needed) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Reads every counted block entity in the loaded chunks, scores each one
     * by the weighted blocks around it, and announces the densest spots not
     * already announced. Scoring is skipped while the set of block entities
     * is unchanged since the last pass, which is most passes.
     */
    private void scanStashes() {
        class_746 self = this.mc.field_1724;
        class_638 world = this.mc.field_1687;
        if (self == null || world == null) {
            return;
        }
        Map<class_2591<?>, Source> sources = stashSources();
        boolean stash = this.stashScan.isEnabled();
        boolean flag = this.flagSpawners.isEnabled();
        double px = self.method_23317();
        double py = self.method_23318();
        double pz = self.method_23321();
        List<Found> found = new ArrayList<>();
        List<SpawnerHit> freshSpawners = new ArrayList<>();
        long signature = 0L;
        int pcx = (int) Math.floor(self.method_23317()) >> 4;
        int pcz = (int) Math.floor(self.method_23321()) >> 4;
        for (int cx = pcx - STASH_SCAN_RADIUS; cx <= pcx + STASH_SCAN_RADIUS; cx++) {
            for (int cz = pcz - STASH_SCAN_RADIUS; cz <= pcz + STASH_SCAN_RADIUS; cz++) {
                if (!world.method_8393(cx, cz)) {
                    continue;
                }
                class_2818 chunk = world.method_8497(cx, cz);
                for (class_2586 entity : chunk.method_12214().values()) {
                    if (flag && entity instanceof class_2636 spawner) {
                        class_2338 pos = entity.method_11016();
                        int x = pos.method_10263();
                        int y = pos.method_10264();
                        int z = pos.method_10260();
                        this.mark(Family.SPAWNER, x, y, z); // always highlighted while loaded
                        long key = FarmRoute.key(x, y, z);
                        double distance = Math.sqrt(distSq(x + 0.5, y + 0.5, z + 0.5, px, py, pz));
                        if (!this.flaggedSpawners.contains(key) && distance >= this.minDistance.getValue()) {
                            freshSpawners.add(new SpawnerHit(x, y, z, mobName(world, spawner, pos), distance));
                        }
                    }
                    if (!stash) {
                        continue;
                    }
                    Source source = sources.get(entity.method_11017());
                    if (source == null || !this.familyEnabled(source.family())) {
                        continue;
                    }
                    class_2338 pos = entity.method_11016();
                    found.add(new Found(pos.method_10263(), pos.method_10264(), pos.method_10260(), source));
                    // Order-independent, so iteration order cannot fake a change.
                    signature += FarmRoute.key(pos.method_10263(), pos.method_10264(), pos.method_10260()) * 0x9E3779B97F4A7C15L
                            + source.family().ordinal();
                }
            }
        }

        if (!freshSpawners.isEmpty()) {
            this.flagSpawners(freshSpawners);
        }

        Set<Long> positions = new HashSet<>();
        for (Found f : found) {
            positions.add(FarmRoute.key(f.x(), f.y(), f.z()));
        }
        this.stashPositions = positions;
        this.rebuildDensity();
        if (!stash) {
            return;
        }

        // Rescore when the block entities change, and every 10 s regardless:
        // activity alone can lift a spot over the density filter.
        long now = System.currentTimeMillis();
        if (signature != this.lastStashSignature || found.size() != this.lastStashCount
                || (this.densityFilter.isEnabled() && now - this.lastStashScoreAt > 10_000L)) {
            this.lastStashSignature = signature;
            this.lastStashCount = found.size();
            this.lastStashScoreAt = now;
            this.scoreStashes(self, found);
        }

        // Members of announced stashes stay highlighted while they are loaded.
        int reach = this.stashRadius() * 2;
        for (Found f : found) {
            for (int[] centre : this.stashAlerts) {
                if (Math.abs(f.x() - centre[0]) <= reach && Math.abs(f.y() - centre[1]) <= reach && Math.abs(f.z() - centre[2]) <= reach) {
                    this.mark(f.source().family(), f.x(), f.y(), f.z());
                    break;
                }
            }
        }
    }

    /**
     * Every spawner is news on its own, so these skip the stash threshold and
     * the density filter. Spawners within a cluster of each other are one
     * message ("12 spawners (8 skeleton, 4 zombie)") rather than twelve.
     */
    private void flagSpawners(List<SpawnerHit> fresh) {
        int reach = this.stashRadius() * 2;
        boolean[] used = new boolean[fresh.size()];
        List<List<SpawnerHit>> groups = new ArrayList<>();
        for (int i = 0; i < fresh.size(); i++) {
            if (used[i]) {
                continue;
            }
            SpawnerHit anchor = fresh.get(i);
            List<SpawnerHit> group = new ArrayList<>();
            for (int j = i; j < fresh.size(); j++) {
                SpawnerHit s = fresh.get(j);
                if (!used[j] && Math.abs(s.x() - anchor.x()) <= reach && Math.abs(s.y() - anchor.y()) <= reach
                        && Math.abs(s.z() - anchor.z()) <= reach) {
                    used[j] = true;
                    group.add(s);
                    this.flaggedSpawners.add(FarmRoute.key(s.x(), s.y(), s.z()));
                }
            }
            group.sort(Comparator.comparingDouble(SpawnerHit::distance));
            groups.add(group);
        }
        groups.sort(Comparator.comparingDouble(g -> g.get(0).distance()));

        class_746 self = this.mc.field_1724;
        double px = self == null ? 0.0 : self.method_23317();
        double pz = self == null ? 0.0 : self.method_23321();
        for (int i = 0; i < groups.size(); i++) {
            List<SpawnerHit> group = groups.get(i);
            SpawnerHit nearest = group.get(0);
            Map<String, Integer> mobs = new HashMap<>();
            for (SpawnerHit s : group) {
                mobs.merge(s.mob() == null ? "Unknown" : s.mob(), 1, Integer::sum);
            }
            String label = mobs.size() == 1 && nearest.mob() != null ? nearest.mob() + " Spawner" : "Spawner";
            double dx = nearest.x() + 0.5 - px;
            double dz = nearest.z() + 0.5 - pz;
            if (i == 0) {
                this.stopAutoRtp(label, nearest.x(), nearest.y(), nearest.z(), nearest.distance(), dx, dz);
                this.offerTarget(label, nearest.x(), nearest.y(), nearest.z(), nearest.distance());
            }
            if (i >= STASH_ANNOUNCE_MAX) {
                continue;
            }
            int distance = (int) Math.round(nearest.distance());
            String direction = compass(dx, dz);
            String head = group.size() == 1 ? "Spawner" : group.size() + " Spawners";
            String kinds = group.size() == 1 ? (nearest.mob() == null ? "type unknown" : nearest.mob()) : summarize(mobs);
            if (this.chat.isEnabled()) {
                this.say("§6[FarmFinder]§r §d" + head + "§r §f(" + kinds + ")§r §7" + distance + "m " + direction
                        + " §8(" + nearest.x() + ", " + nearest.y() + ", " + nearest.z() + ")");
            }
            if (this.toast.isEnabled()
                    && Cyemer.getInstance().getModuleManager().getModule("Notifications") instanceof Notifications notifications) {
                notifications.info("Farm Finder", head + " · " + kinds + " · " + distance + "m " + direction);
            }
            if (i == 0 && this.ping.isEnabled()) {
                class_1144 manager = this.mc.method_1483();
                if (manager != null) {
                    manager.method_4873(class_1109.method_47978(class_3417.field_14622, 1.6F));
                }
            }
        }
        if (groups.size() > STASH_ANNOUNCE_MAX) {
            this.say("§6[FarmFinder]§r §d+" + (groups.size() - STASH_ANNOUNCE_MAX) + " more spawner spots§r §7in range, highlighted");
        }
    }

    /**
     * The mob a spawner shows spinning in its cage, which is what it spawns.
     * Same call the vanilla spawner renderer makes every frame; null when the
     * server sent no spawn data.
     */
    private static String mobName(class_638 world, class_2636 spawner, class_2338 pos) {
        try {
            class_1297 shown = spawner.method_11390().method_8283(world, pos);
            if (shown != null) {
                return shown.method_5864().method_5897().getString();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** Half the cluster size: blocks this close to each other belong to one stash. */
    private int stashRadius() {
        return Math.max(4, (int) Math.round(this.clusterSize.getValue() / 2.0));
    }

    private void scoreStashes(class_746 self, List<Found> found) {
        int r = this.stashRadius();
        Map<Long, List<Found>> grid = new HashMap<>();
        for (Found f : found) {
            grid.computeIfAbsent(FarmRoute.key(Math.floorDiv(f.x(), r), Math.floorDiv(f.y(), r), Math.floorDiv(f.z(), r)),
                    k -> new ArrayList<>()).add(f);
        }

        int n = found.size();
        int[] scores = new int[n];
        for (int i = 0; i < n; i++) {
            scores[i] = this.neighbourhood(found.get(i), grid, r, null);
        }
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        java.util.Arrays.sort(order, (a, b) -> Integer.compare(scores[b], scores[a]));

        int threshold = (int) this.stashThreshold.getValue();
        double px = self.method_23317();
        double py = self.method_23318();
        double pz = self.method_23321();
        List<Stash> fresh = new ArrayList<>();
        for (int idx : order) {
            if (scores[idx] < threshold) {
                break;
            }
            Found c = found.get(idx);
            if (this.nearAnnounced(c.x(), c.y(), c.z(), r * 2)) {
                continue;
            }
            double distance = Math.sqrt(distSq(c.x() + 0.5, c.y() + 0.5, c.z() + 0.5, px, py, pz));
            if (distance < this.minDistance.getValue() || !this.passesDensity(c.x(), c.z())) {
                continue; // not marked announced, so it can still qualify later
            }
            Map<String, Integer> counts = new HashMap<>();
            int total = this.neighbourhood(c, grid, r, counts);
            if (this.stashAlerts.size() >= 300) {
                this.stashAlerts.remove(0);
            }
            this.stashAlerts.add(new int[]{c.x(), c.y(), c.z()});
            fresh.add(new Stash(c.x(), c.y(), c.z(), summarize(counts), total, distance));
        }
        if (fresh.isEmpty()) {
            return;
        }

        fresh.sort(Comparator.comparingDouble(Stash::distance));
        Stash nearest = fresh.get(0);
        double ndx = nearest.x() + 0.5 - px;
        double ndz = nearest.z() + 0.5 - pz;
        this.stopAutoRtp("Stash", nearest.x(), nearest.y(), nearest.z(), nearest.distance(), ndx, ndz);
        this.offerTarget("Stash", nearest.x(), nearest.y(), nearest.z(), nearest.distance());
        for (int i = 0; i < Math.min(STASH_ANNOUNCE_MAX, fresh.size()); i++) {
            this.emitStash(fresh.get(i), px, pz, i == 0);
        }
        if (fresh.size() > STASH_ANNOUNCE_MAX) {
            this.say("§6[FarmFinder]§r §d+" + (fresh.size() - STASH_ANNOUNCE_MAX) + " more stashes§r §7in range, highlighted");
        }
    }

    /**
     * Weighted count of counted blocks within r of f (a cube, f included).
     * With counts given, also tallies each kind and returns the plain count.
     */
    private int neighbourhood(Found f, Map<Long, List<Found>> grid, int r, Map<String, Integer> counts) {
        int gx = Math.floorDiv(f.x(), r);
        int gy = Math.floorDiv(f.y(), r);
        int gz = Math.floorDiv(f.z(), r);
        int sum = 0;
        for (int ox = -1; ox <= 1; ox++) {
            for (int oy = -1; oy <= 1; oy++) {
                for (int oz = -1; oz <= 1; oz++) {
                    List<Found> cell = grid.get(FarmRoute.key(gx + ox, gy + oy, gz + oz));
                    if (cell == null) {
                        continue;
                    }
                    for (Found g : cell) {
                        if (Math.abs(g.x() - f.x()) <= r && Math.abs(g.y() - f.y()) <= r && Math.abs(g.z() - f.z()) <= r) {
                            if (counts != null) {
                                counts.merge(g.source().label(), 1, Integer::sum);
                                sum++;
                            } else {
                                sum += stashWeight(g.source().family());
                            }
                        }
                    }
                }
            }
        }
        return sum;
    }

    private boolean nearAnnounced(int x, int y, int z, int within) {
        for (int[] c : this.stashAlerts) {
            if (Math.abs(x - c[0]) <= within && Math.abs(y - c[1]) <= within && Math.abs(z - c[2]) <= within) {
                return true;
            }
        }
        return false;
    }

    /** "23 chests, 4 shulkers, 1 spawner", most numerous first. */
    private static String summarize(Map<String, Integer> counts) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, Integer> e : entries) {
            if (out.length() > 0) {
                out.append(", ");
            }
            String name = e.getKey().toLowerCase();
            out.append(e.getValue()).append(' ').append(e.getValue() == 1 ? name : name + "s");
        }
        return out.toString();
    }

    private void emitStash(Stash stash, double px, double pz, boolean withPing) {
        double dx = stash.x() + 0.5 - px;
        double dz = stash.z() + 0.5 - pz;
        int distance = (int) Math.round(stash.distance());
        String direction = compass(dx, dz);
        if (this.chat.isEnabled()) {
            this.say("§6[FarmFinder]§r §dStash§r §7" + distance + "m " + direction
                    + "§r §8(" + stash.x() + ", " + stash.y() + ", " + stash.z() + ")§r §7- " + stash.summary());
        }
        if (this.toast.isEnabled()
                && Cyemer.getInstance().getModuleManager().getModule("Notifications") instanceof Notifications notifications) {
            notifications.info("Farm Finder", "Stash · " + stash.total() + " blocks · " + distance + "m " + direction);
        }
        if (withPing && this.ping.isEnabled()) {
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
        this.marks.put(key, new Mark(x, y, z, family, now));
        if (!this.stashPositions.contains(key)) {
            this.chunkCounts.merge(FarmRoute.key(x >> 4, 0, z >> 4), 1, Integer::sum);
        }
    }

    /**
     * The first find becomes the route target and keeps it until we arrive,
     * so a busy farm firing all over cannot keep re-planning under us. Only a
     * find at under half the distance takes over, so with detection reaching
     * the whole render distance the route still ends up on the nearest base
     * after a few switches at most.
     */
    private void offerTarget(String label, int x, int y, int z, double distance) {
        if (!this.route.isEnabled() || distance < ARRIVE_DISTANCE + 4.0) {
            return;
        }
        if (this.arrivedAt != null && distSq(this.arrivedAt[0], this.arrivedAt[1], this.arrivedAt[2], x, y, z) < 48.0 * 48.0) {
            return;
        }
        class_746 self = this.mc.field_1724;
        if (this.target != null && self != null) {
            double current = Math.sqrt(distSq(this.target.x(), this.target.y(), this.target.z(),
                    self.method_23317(), self.method_23318(), self.method_23321()));
            if (distance >= current * 0.5) {
                return;
            }
        }
        this.clearTarget();
        this.target = new Target(label, x, y, z, distance);
        this.plan();
    }

    private void stopAutoRtp(String label, int x, int y, int z, double distance, double dx, double dz) {
        if (!this.autoRtp.isEnabled()) {
            return;
        }
        AutoRtp.stopAll();
        this.say("§6[FarmFinder]§r §cAutoRTP stopped§r §7- " + label + " "
                + (int) Math.round(distance) + "m " + compass(dx, dz) + " §8(" + x + ", " + y + ", " + z + ")");
        if (Cyemer.getInstance().getModuleManager().getModule("Notifications") instanceof Notifications notifications) {
            notifications.warn("AutoRTP stopped", label + " detected");
        }
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
        if (distance > t.foundAt() + TARGET_DROP_SLACK) {
            this.clearTarget(); // teleported away from it
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

    /** Sent as a command, not chat; see AutoRtp.send. */
    private void sendRtp() {
        AutoRtp.send(this.rtpCommand.getValue());
    }

    /** Quietly turns AutoRTP off; called when another module's find stops every AutoRTP. */
    public void cancelAutoRtp() {
        if (this.autoRtp.isEnabled()) {
            this.autoRtp.setEnabled(false);
        }
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
