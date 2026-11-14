package com.slither.cyemer.module.implementation.donutsmp;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.module.StringSetting;
import com.slither.cyemer.module.implementation.Notifications;
import java.util.HashMap;
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
import net.minecraft.class_2960;
import net.minecraft.class_3417;
import net.minecraft.class_634;
import net.minecraft.class_746;

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
 */
@Environment(EnvType.CLIENT)
public class FarmFinder extends Module {
    /** Floor between any two alerts, so flying over a mega-farm cannot flood chat. */
    private static final long GLOBAL_GAP_MS = 300L;
    /** Clusters unheard for this long are forgotten. */
    private static final long CLUSTER_TTL_MS = 5L * 60L * 1000L;
    private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private enum Family { PISTON, OBSERVER, SMELTER, DISPENSER, CRAFTER, REDSTONE, COMPOSTER }

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

    private static final class Cluster {
        long lastAlertAt = 0L;
        long lastSeenAt = 0L;
        int suppressed = 0;
    }

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
