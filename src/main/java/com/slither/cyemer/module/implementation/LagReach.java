package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ColorSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2596;
import net.minecraft.class_2827;
import net.minecraft.class_332;
import net.minecraft.class_6374;
import net.minecraft.class_9779;

/**
 * Extra reach via inflated ping.
 *
 * Holds outgoing pong (class_6374) and keepalive (class_2827) replies
 * for `Delay` ms. Server measures ping off round-trip; Grim's reach
 * check rewinds targets by ping so late replies widen the rewind
 * window and moving targets can be hit from further. Position stream
 * is untouched so Simulation / Timer / PacketOrder don't flag.
 *
 * Client-side reach bonus is added via PlayerEntityMixin injecting
 * into class_1657.method_55755 (getEntityInteractionRange) so the
 * client raycast can actually pick up targets at the extended range.
 *
 * Combat-aware: only inflates ping while combat is active (attack
 * within Combat Window ms), so out-of-combat traffic looks normal.
 * Safety cap: hold time never exceeds MAX_HOLD_MS regardless of
 * setting - long holds risk keepalive-timeout disconnects.
 */
@Environment(EnvType.CLIENT)
public class LagReach extends Module {
    private static final long MAX_HOLD_MS = 400L;
    private static LagReach instance;

    private final SliderSetting delay = new SliderSetting("Delay (ms)", 60.0, 20.0, 150.0, 0);
    private final SliderSetting reachBonus = new SliderSetting("Reach (blocks)", 0.05, 0.0, 0.1, 2);
    private final BooleanSetting combatOnly = new BooleanSetting("Combat Only", true);
    private final SliderSetting combatWindow = new SliderSetting("Combat Window (ms)", 1500.0, 500.0, 5000.0, 0);
    private final BooleanSetting showHud = new BooleanSetting("Show HUD", true);
    private final ColorSetting hudColor = new ColorSetting("HUD Color", new Color(120, 220, 120, 220));

    private final Deque<Queued> queue = new ArrayDeque<>();
    private boolean flushing = false;
    private long lastAttackAt = 0L;

    public LagReach() {
        super("LagReach", "Delays pong/keepalive replies and extends client reach so hits land at longer range.", Category.PLAYER);
        this.addSetting(this.delay);
        this.addSetting(this.reachBonus);
        this.addSetting(this.combatOnly);
        this.addSetting(this.combatWindow);
        this.addSetting(this.showHud);
        this.addSetting(this.hudColor);
        instance = this;
    }

    public static LagReach getInstance() {
        return instance;
    }

    public void notifyAttack() {
        this.lastAttackAt = System.currentTimeMillis();
    }

    public double getReachBonus() {
        return this.reachBonus.getValue();
    }

    private long effectiveHoldMs() {
        return Math.min(MAX_HOLD_MS, (long) this.delay.getValue());
    }

    private boolean shouldHold() {
        if (!this.combatOnly.isEnabled()) return true;
        return System.currentTimeMillis() - this.lastAttackAt <= (long) this.combatWindow.getValue();
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
        if (!this.shouldHold()) {
            this.flushAll();
            return;
        }
        long cutoff = System.currentTimeMillis() - this.effectiveHoldMs();
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
        if (!this.shouldHold()) return false;
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

    public void onHudRender(class_332 context, class_9779 tickDelta) {
        if (!this.showHud.isEnabled() || this.mc.field_1724 == null) return;
        long addedPing = this.queue.isEmpty() ? 0L : this.effectiveHoldMs();
        boolean active = this.shouldHold();
        double reach = 3.0 + this.reachBonus.getValue();

        String line1 = String.format("LagReach %s", active ? "ACTIVE" : "idle");
        String line2 = String.format("+%d ms  |  %.2f blocks", addedPing, reach);

        int screenWidth = context.method_51421();
        int screenHeight = context.method_51443();
        int x = screenWidth / 2 + 12;
        int y = screenHeight / 2 + 12;

        Color c = this.hudColor.getValue();
        int rgb = active ? c.getRGB() : (c.getRGB() & 0x00FFFFFF) | 0x60000000;
        int shadow = 0x80000000;

        context.method_25303(this.mc.field_1772, line1, x + 1, y + 1, shadow);
        context.method_25303(this.mc.field_1772, line1, x, y, rgb);
        context.method_25303(this.mc.field_1772, line2, x + 1, y + 11, shadow);
        context.method_25303(this.mc.field_1772, line2, x, y + 10, rgb);
    }

    private record Queued(class_2596<?> packet, long enqueuedAt) {}
}
