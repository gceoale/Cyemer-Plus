package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import java.util.ArrayDeque;
import java.util.Deque;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2596;
import net.minecraft.class_2827;
import net.minecraft.class_6374;

/**
 * Extra reach via inflated ping.
 *
 * Holds outgoing CommonPongC2SPacket (class_6374) and KeepAliveC2SPacket
 * (class_2827) for `Delay (ms)` before releasing. The server measures
 * RTT off these; a later reply looks like real latency. Grim uses its
 * ping window to rewind target positions during reach checks, so an
 * inflated ping means the check runs against the target's older
 * position - if the target has since moved further, a hit that would
 * normally exceed reach still lands. Position packets are untouched,
 * so Simulation / Timer / PacketOrder stay clean.
 */
@Environment(EnvType.CLIENT)
public class LagReach extends Module {
    private final SliderSetting delay = new SliderSetting("Delay (ms)", 100.0, 20.0, 250.0, 0);
    private final Deque<Queued> queue = new ArrayDeque<>();
    private boolean flushing = false;

    public LagReach() {
        super("LagReach", "Delays pong/keepalive replies to inflate perceived ping so reach checks rewind targets further.", Category.PLAYER);
        this.addSetting(this.delay);
    }

    @Override
    public void onDisable() {
        this.flushAll();
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 == null || this.mc.method_1562() == null) {
            this.flushAll();
            return;
        }
        long cutoff = System.currentTimeMillis() - (long) this.delay.getValue();
        this.flushing = true;
        try {
            while (!this.queue.isEmpty() && this.queue.peekFirst().enqueuedAt <= cutoff) {
                Queued q = this.queue.pollFirst();
                this.mc.method_1562().method_52787(q.packet);
            }
        } finally {
            this.flushing = false;
        }
    }

    public boolean handleOutgoingPacket(class_2596<?> packet) {
        if (this.flushing) return false;
        if (this.mc.field_1724 == null || this.mc.method_1562() == null) {
            this.flushAll();
            return false;
        }
        if (packet instanceof class_6374 || packet instanceof class_2827) {
            this.queue.addLast(new Queued(packet, System.currentTimeMillis()));
            return true;
        }
        return false;
    }

    private void flushAll() {
        if (this.mc.method_1562() == null) {
            this.queue.clear();
            return;
        }
        this.flushing = true;
        try {
            while (!this.queue.isEmpty()) {
                this.mc.method_1562().method_52787(this.queue.pollFirst().packet);
            }
        } finally {
            this.flushing = false;
        }
    }

    private record Queued(class_2596<?> packet, long enqueuedAt) {}
}
