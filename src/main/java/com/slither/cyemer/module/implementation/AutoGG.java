package com.slither.cyemer.module.implementation;

import com.slither.cyemer.event.EventBus;
import com.slither.cyemer.event.EventTarget;
import com.slither.cyemer.event.impl.AttackEvent;
import com.slither.cyemer.friend.FriendManager;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_3966;
import net.minecraft.class_239.class_240;

@Environment(EnvType.CLIENT)
public class AutoGG extends Module {
    private static final long TRACK_TTL_MS = 5000L;

    private final BooleanSetting alternate = new BooleanSetting("Alternate", true);
    private final BooleanSetting skipFriends = new BooleanSetting("Skip Friends", true);
    private final BooleanSetting playersOnly = new BooleanSetting("Players Only", true);
    private final SliderSetting delay = new SliderSetting("Delay (ms)", 200.0, 0.0, 5000.0, 0);

    private final List<TrackedHit> tracked = new ArrayList<>();
    private final Deque<Long> pending = new ArrayDeque<>();
    private boolean useGgs = false;
    private boolean registered = false;

    public AutoGG() {
        super("AutoGG", "Says gg (or ggs) in chat when you get a kill.", Category.MISC);
        this.addSetting(this.alternate);
        this.addSetting(this.skipFriends);
        this.addSetting(this.playersOnly);
        this.addSetting(this.delay);
    }

    @Override
    public void onEnable() {
        if (!this.registered) {
            EventBus.register(this);
            this.registered = true;
        }
        this.tracked.clear();
        this.pending.clear();
    }

    @Override
    public void onDisable() {
        this.tracked.clear();
        this.pending.clear();
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled() || this.mc.field_1724 == null || this.mc.field_1765 == null) {
            return;
        }
        if (this.mc.field_1765.method_17783() != class_240.field_1331) {
            return;
        }
        class_1297 target = ((class_3966) this.mc.field_1765).method_17782();
        if (target == this.mc.field_1724 || target == null) {
            return;
        }
        if (this.playersOnly.isEnabled() && !(target instanceof class_1657)) {
            return;
        }
        if (this.skipFriends.isEnabled()
                && target instanceof class_1657 player
                && FriendManager.getInstance().isFriend(player.method_5667())) {
            return;
        }
        this.tracked.add(new TrackedHit(target, System.currentTimeMillis()));
    }

    @Override
    public void onTick() {
        long now = System.currentTimeMillis();

        Iterator<TrackedHit> it = this.tracked.iterator();
        while (it.hasNext()) {
            TrackedHit hit = it.next();
            if (now - hit.timestamp > TRACK_TTL_MS) {
                it.remove();
                continue;
            }
            boolean dead = hit.entity.method_31481()
                    || (hit.entity instanceof class_1309 living && living.method_6032() <= 0.0F);
            if (dead) {
                this.pending.add(now + (long) this.delay.getValue());
                it.remove();
            }
        }

        while (!this.pending.isEmpty() && this.pending.peekFirst() <= now) {
            this.pending.pollFirst();
            this.sendGg();
        }
    }

    private void sendGg() {
        if (this.mc.field_1724 == null || this.mc.field_1724.field_3944 == null) {
            return;
        }
        String message = this.alternate.isEnabled() && this.useGgs ? "ggs" : "gg";
        this.mc.field_1724.field_3944.method_45729(message);
        if (this.alternate.isEnabled()) {
            this.useGgs = !this.useGgs;
        }
    }

    private static final class TrackedHit {
        final class_1297 entity;
        final long timestamp;

        TrackedHit(class_1297 entity, long timestamp) {
            this.entity = entity;
            this.timestamp = timestamp;
        }
    }
}
