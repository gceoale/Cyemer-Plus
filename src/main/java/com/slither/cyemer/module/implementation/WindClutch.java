package com.slither.cyemer.module.implementation;

import com.slither.cyemer.friend.FriendManager;
import com.slither.cyemer.mixin.ClientPlayerInteractionManagerMixin;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1268;
import net.minecraft.class_1294;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_2596;
import net.minecraft.class_2664;
import net.minecraft.class_2797;
import net.minecraft.class_2799;
import net.minecraft.class_2827;
import net.minecraft.class_3417;
import net.minecraft.class_3486;
import net.minecraft.class_3532;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_746;
import net.minecraft.class_9239;
import net.minecraft.class_9362;

/**
 * Fires a wind charge under you right before a big fall lands so it costs no health.
 *
 * A player wind charge that bursts within 2.4 blocks of you makes the server remember where you were
 * (class_3222.method_56918), and fall damage is then only counted from that spot down
 * (class_1657.method_5747: min(fallDistance, impactY - landingY)). Burst it while you're under 3 blocks up
 * and the fall is free.
 *
 * At real falling speeds you're only inside that 2.4 block zone for a tick, too tight to hit by timing alone.
 * So the wind charge goes out on the last tick before landing and everything we send after it is held until
 * the burst has happened: the server keeps seeing us parked in the zone, then gets the landing.
 */
@Environment(EnvType.CLIENT)
public class WindClutch extends Module {
    private final SliderSetting minFall = new SliderSetting("Min Fall", 6.0, 4.0, 40.0, 1);
    private final BooleanSetting skipNearPlayers = new BooleanSetting("Skip Near Players", true);
    private final SliderSetting playerRange = new SliderSetting("Player Range", 4.5, 1.0, 10.0, 1);
    // class_9892.method_61741: the burst reaches power * 2 from its centre to our feet
    private static final double BURST_RADIUS = 1.2F * 2.0F;
    // class_9236.method_24920 bursts a quarter block off the face it hit
    private static final double BURST_FACE_OFFSET = 0.25;
    private static final float FALLBACK_POWER = 1.5F;
    private final Queue<class_2596<?>> held = new ConcurrentLinkedQueue<>();
    private volatile boolean holding = false;
    private volatile boolean flushing = false;
    // 1 = a wind charge burst caught us, -1 = some other explosion did, set from the packet thread
    private volatile int pendingBurst = 0;
    private volatile int maceHitAge = -1000;
    private int holdUntilAge = 0;
    private boolean tracking = false;
    private double peakY = 0.0;
    private double lastX = 0.0;
    private double lastY = 0.0;
    private double lastZ = 0.0;
    // where the server will count our fall from, NaN when nothing has protected us
    private double protectedY = Double.NaN;

    public WindClutch() {
        super("WindClutch", "Fires a wind charge under you before a big fall lands, so it costs no health.", Category.MOVEMENT);
        this.addSetting(this.minFall);
        this.addSetting(this.skipNearPlayers);
        this.addSetting(this.playerRange);
    }

    @Override
    public void onEnable() {
        this.tracking = false;
        this.protectedY = Double.NaN;
        this.pendingBurst = 0;
    }

    @Override
    public void onDisable() {
        this.release();
        this.tracking = false;
    }

    /** Called from ClientConnectionMixin for everything we send. True means it was queued instead. */
    public boolean handleOutgoingPacket(class_2596<?> packet) {
        if (!this.holding || this.flushing) {
            return false;
        } else if (!(packet instanceof class_2827) && !(packet instanceof class_2799) && !(packet instanceof class_2797)) {
            this.held.add(packet);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Called from ClientPlayNetworkHandlerMixin. The knockback is only in the packet when the server had us inside
     * the blast, the same condition under which it rewrites our fall state. Only a player wind charge (power 1.2,
     * the mace's Wind Burst uses the same sound at 3.5) turns the protection on, anything else turns it off.
     */
    public void onExplosion(class_2664 packet) {
        if (packet.comp_2884().isPresent()) {
            boolean windCharge = packet.comp_4594() < 2.0F && packet.comp_2886().comp_349().equals(class_3417.field_49044.comp_349());
            this.pendingBurst = windCharge ? 1 : -1;
        }
    }

    /** Called from ClientPlayerInteractionManagerMixin. A mace smash protects the fall the same way (class_9362.method_7873). */
    public void onAttack() {
        class_746 player = this.mc.field_1724;
        if (player != null && player.method_6047().method_7909() instanceof class_9362 && player.field_6017 > 1.5 && !player.method_6128()) {
            this.maceHitAge = player.field_6012;
            this.pendingBurst = 1;
        }
    }

    @Override
    public void onTick() {
        class_746 player = this.mc.field_1724;
        if (player == null || this.mc.field_1687 == null) {
            this.release();
            this.tracking = false;
            return;
        }

        if (this.holding && player.field_6012 >= this.holdUntilAge) {
            this.release();
        }

        double x = player.method_23317();
        double y = player.method_23318();
        double z = player.method_23321();
        int burst = this.pendingBurst;
        if (burst != 0) {
            this.pendingBurst = 0;
            if (burst > 0) {
                this.protectedY = y;
            } else if (player.field_6012 - this.maceHitAge > 20) {
                // not the blast from our own Wind Burst smash (the mace re-protects us right after that one)
                this.protectedY = Double.NaN;
            }
        }

        if (!this.tracking || this.fallCancelled(player)) {
            // nothing counts as falling here, the server's fall distance is back to zero
            this.tracking = true;
            this.peakY = y;
            if (player.method_24828()) {
                this.protectedY = Double.NaN;
            }
        } else {
            double dx = x - this.lastX;
            double dy = y - this.lastY;
            double dz = z - this.lastZ;
            if (dx * dx + dy * dy + dz * dz > 64.0) {
                // teleported (a pearl landing), which restarts the fall on the server
                this.peakY = y;
            }

            this.peakY = Math.max(this.peakY, y);
            if (!this.holding) {
                this.tryClutch(player);
            }
        }

        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
    }

    private boolean fallCancelled(class_746 player) {
        return player.method_24828()
            || player.method_5799()
            || player.method_5771()
            || player.method_6101()
            || player.method_6128()
            || player.method_5765()
            || player.method_7325()
            || player.method_31549().field_7478
            || player.method_31549().field_7479
            || player.method_6059(class_1294.field_5906)
            || player.method_6059(class_1294.field_5902);
    }

    private void tryClutch(class_746 player) {
        // only the last tick in the air matters, that's the closest we get to the ground before the server hears we landed
        double landY = this.landingY(player);
        if (Double.isNaN(landY)) {
            return;
        }

        double fall = this.peakY - landY;
        if (!Double.isNaN(this.protectedY)) {
            fall = Math.min(fall, this.protectedY - landY);
        }

        if (fall < this.minFall.getValue()) {
            return;
        }

        class_243 feet = player.method_73189();
        // water under us takes the fall on its own
        if (this.mc.field_1687.method_8316(class_2338.method_49637(feet.field_1352, landY + 0.05, feet.field_1350)).method_15767(class_3486.field_15517)) {
            return;
        }

        if (this.skipNearPlayers.isEnabled() && this.playerNear(feet.field_1352, landY, feet.field_1350)) {
            // someone's right under us, that's a mace smash and a burst would bounce us off it
            return;
        }

        int windSlot = this.findWindSlot();
        if (windSlot == -1 || player.method_7357().method_7904(player.method_31548().method_5438(windSlot))) {
            return;
        }

        class_243 ground = this.groundUnder(player, landY);
        if (ground == null) {
            return;
        }

        // aim so the wind charge drops straight onto that spot even with our own movement added to it
        class_243 eye = player.method_33571();
        class_243 inherited = new class_243(feet.field_1352 - player.field_6014, feet.field_1351 - player.field_6036, feet.field_1350 - player.field_5969);
        double power = class_9239.field_55047 > 0.0F ? class_9239.field_55047 : FALLBACK_POWER;
        class_243 wanted = this.aimVelocity(eye, ground, inherited, power);
        if (wanted == null) {
            return;
        }

        class_243 aim = wanted.method_1020(inherited);
        float yaw = (float)Math.toDegrees(Math.atan2(-aim.field_1352, aim.field_1350));
        float pitch = (float)Math.toDegrees(-Math.asin(class_3532.method_15350(aim.field_1351 / power, -1.0, 1.0)));
        // what the server will really launch, through the same MathHelper tables it uses
        class_243 velocity = this.lookVector(yaw, pitch).method_1021(power).method_1019(inherited);
        double speed = Math.sqrt(velocity.method_1026(velocity));
        class_3965 hit = this.raycast(eye, eye.method_1019(velocity.method_1021(8.0 / Math.max(speed, 0.01))), player);
        if (hit.method_17783() == class_239.class_240.field_1333) {
            return;
        }

        class_243 burstPos = hit.method_17784().method_1019(class_243.method_24954(hit.method_17780().method_62675()).method_1021(BURST_FACE_OFFSET));
        if (Math.sqrt(burstPos.method_1025(feet)) > BURST_RADIUS * 0.97) {
            // falling fast enough that even the last tick is out of reach, nothing a ground burst can do
            return;
        }

        int windTicks = Math.max(1, (int)Math.ceil(Math.sqrt(hit.method_17784().method_1025(eye)) / speed));
        int slot = player.method_31548().method_67532();
        float realYaw = player.method_36454();
        float realPitch = player.method_36455();
        player.method_31548().method_61496(windSlot);
        player.method_36456(yaw);
        player.method_36457(pitch);
        // straight to interactItem: slot sync, then the use packet carrying this rotation
        this.mc.field_1761.method_2919(player, class_1268.field_5808);
        player.method_36456(realYaw);
        player.method_36457(realPitch);
        player.method_31548().method_61496(slot);
        ((ClientPlayerInteractionManagerMixin.ClientPlayerInteractionManagerAccessor)this.mc.field_1761).invokeSyncSelectedSlot();
        // from here the server keeps us where we are until the burst has gone off, one tick of slack on top
        this.holdUntilAge = player.field_6012 + windTicks + 1;
        this.holding = true;
        this.protectedY = feet.field_1351;
    }

    /** Where our feet stop if this tick's move runs into something below us, NaN if we stay in the air. */
    private double landingY(class_746 player) {
        class_243 v = player.method_18798();
        if (v.field_1351 >= 0.0) {
            return Double.NaN;
        }

        class_238 box = player.method_5829();
        double dx = 0.0;
        double dz = 0.0;
        if (this.free(player, box.method_989(0.0, v.field_1351, 0.0))) {
            // straight down is clear, we only land if the full move drops us onto something ahead
            if (this.free(player, box.method_989(v.field_1352, v.field_1351, v.field_1350)) || !this.free(player, box.method_989(v.field_1352, 0.0, v.field_1350))) {
                return Double.NaN;
            }

            dx = v.field_1352;
            dz = v.field_1350;
        }

        double lo = 0.0;
        double hi = 1.0;

        for (int i = 0; i < 12; i++) {
            double mid = (lo + hi) / 2.0;
            if (this.free(player, box.method_989(dx * mid, v.field_1351 * mid, dz * mid))) {
                lo = mid;
            } else {
                hi = mid;
            }
        }

        return player.method_23318() + v.field_1351 * lo;
    }

    private boolean free(class_746 player, class_238 box) {
        return this.mc.field_1687.method_8587(player, box);
    }

    /** The surface we'll land on, straight below the middle of our hitbox or under a corner if the middle is over a gap. */
    private class_243 groundUnder(class_746 player, double landY) {
        double x = player.method_23317();
        double y = player.method_23318();
        double z = player.method_23321();
        double[][] offsets = new double[][]{{0.0, 0.0}, {0.29, 0.29}, {0.29, -0.29}, {-0.29, 0.29}, {-0.29, -0.29}};

        for (double[] offset : offsets) {
            class_243 from = new class_243(x + offset[0], y + 0.1, z + offset[1]);
            class_3965 hit = this.raycast(from, new class_243(x + offset[0], landY - 0.6, z + offset[1]), player);
            if (hit.method_17783() != class_239.class_240.field_1333) {
                return hit.method_17784();
            }
        }

        return null;
    }

    private boolean playerNear(double x, double y, double z) {
        double rangeSq = this.playerRange.getValue() * this.playerRange.getValue();

        for (class_1657 other : this.mc.field_1687.method_18456()) {
            if (other != this.mc.field_1724
                && other.method_5805()
                && !other.method_7325()
                && !FriendManager.getInstance().isFriend(other.method_5477().getString())
                && other.method_5649(x, y, z) <= rangeSq) {
                return true;
            }
        }

        return false;
    }

    /**
     * Wind velocity that flies straight at target once our inherited movement is added on top of the aimed part
     * (class_1676.method_24919). Solves |u * delta - inherited| = speed for the flight scale u.
     */
    private class_243 aimVelocity(class_243 start, class_243 target, class_243 inherited, double speed) {
        class_243 delta = target.method_1020(start);
        double dd = delta.method_1026(delta);
        if (dd < 1.0E-6) {
            return null;
        }

        double di = delta.method_1026(inherited);
        double disc = di * di - dd * (inherited.method_1026(inherited) - speed * speed);
        if (disc < 0.0) {
            return null;
        }

        double u = (di + Math.sqrt(disc)) / dd;
        return u > 0.0 ? delta.method_1021(u) : null;
    }

    /** class_1676.method_24919 without the random spread. */
    private class_243 lookVector(float yawDeg, float pitchDeg) {
        float yaw = class_3532.method_15393(yawDeg);
        float x = -class_3532.method_15374(yaw * 0.017453292F) * class_3532.method_15362(pitchDeg * 0.017453292F);
        float y = -class_3532.method_15374(pitchDeg * 0.017453292F);
        float z = class_3532.method_15362(yaw * 0.017453292F) * class_3532.method_15362(pitchDeg * 0.017453292F);
        return new class_243(x, y, z).method_1029();
    }

    private class_3965 raycast(class_243 from, class_243 to, class_746 player) {
        return this.mc.field_1687.method_17742(new class_3959(from, to, class_3959.class_3960.field_17558, class_3959.class_242.field_1348, player));
    }

    private int findWindSlot() {
        for (int i = 0; i < 9; i++) {
            class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
            if (stack.method_7909() == class_1802.field_49098) {
                return i;
            }
        }

        return -1;
    }

    /** Lets everything we held go out, in order. */
    private void release() {
        this.holding = false;
        if (this.mc.method_1562() == null) {
            this.held.clear();
            return;
        }

        this.flushing = true;

        try {
            class_2596<?> packet;
            while ((packet = this.held.poll()) != null) {
                this.mc.method_1562().method_52787(packet);
            }
        } finally {
            this.flushing = false;
        }
    }
}
