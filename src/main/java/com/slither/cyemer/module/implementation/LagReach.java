package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import java.util.ArrayDeque;
import java.util.Deque;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2596;
import net.minecraft.class_2828;

/**
 * Extra melee reach via server-side position lag.
 *
 * How it works: outgoing PlayerMove (class_2828 + subclasses) packets are
 * held in a queue for `Delay (ms)` before being released. Meanwhile every
 * other packet - attacks, interacts, item use - passes straight through.
 * The server's copy of your position keeps trailing behind the client's
 * by the delay duration, so when you attack, the reach check runs from
 * that older server-side position. If a target has moved closer to that
 * older position (chasing / strafing in), the reach check accepts a
 * click that would normally be out of range.
 */
@Environment(EnvType.CLIENT)
public class LagReach extends Module {
    private final SliderSetting delay = new SliderSetting("Delay (ms)", 150.0, 20.0, 500.0, 0);
    private final Deque<Queued> queue = new ArrayDeque<>();
    private boolean flushing = false;

    public LagReach() {
        super("LagReach", "Delays position packets so server-side reach checks run from an older spot.", Category.PLAYER);
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

    /**
     * Called by ClientConnectionMixin for every outgoing packet. Returns
     * true to cancel the send (we'll send it ourselves later); false to
     * let the vanilla path continue.
     */
    public boolean handleOutgoingPacket(class_2596<?> packet) {
        if (this.flushing) return false;
        if (this.mc.field_1724 == null || this.mc.method_1562() == null) {
            this.flushAll();
            return false;
        }
        if (packet instanceof class_2828) {
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
