package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.ModuleRandomDelay;
import com.slither.cyemer.util.SystemInputSimulator;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2338;

@Environment(EnvType.CLIENT)
public class WebBreaker extends Module {
    private final BooleanSetting pickupWater = new BooleanSetting("Pickup Water", true);
    private final BooleanSetting randomization = new BooleanSetting("Randomization", false);
    private final SliderSetting randomMinDelay = new SliderSetting("Random Min (ms)", 0.0, 0.0, 500.0, 0);
    private final SliderSetting randomMaxDelay = new SliderSetting("Random Max (ms)", 25.0, 0.0, 500.0, 0);
    private WebBreaker.State currentState = WebBreaker.State.IDLE;
    private int originalSlot = -1;
    private class_2338 webPos = null;
    private int timeoutTicks = 0;
    private long pickupDeadlineTime = 0L;
    private long nextPickupAttemptTime = 0L;
    private long nextActionTime = 0L;

    public WebBreaker() {
        super("WebBreaker", "Optimally breaks webs when looking down.", Category.PLAYER);
        this.addSetting(this.pickupWater);
        this.addSetting(this.randomization);
        this.addSetting(this.randomMinDelay);
        this.addSetting(this.randomMaxDelay);
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            switch (this.currentState) {
                case IDLE:
                    if (System.currentTimeMillis() < this.nextActionTime) {
                        return;
                    }

                    if (this.mc.field_1724.method_36455() < 55.0F) {
                        return;
                    }

                    class_2338 currentPos = this.mc.field_1724.method_24515();
                    if (this.mc.field_1687.method_8320(currentPos).method_26204() != class_2246.field_10343) {
                        return;
                    }

                    int waterBucketSlot = this.findWaterBucket();
                    if (waterBucketSlot != -1) {
                        if (this.randomization.isEnabled()
                            && !ModuleRandomDelay.gateAction("combat.use.webbreaker.place", this.getRandomMinDelay(), this.getRandomMaxDelay())) {
                            return;
                        }

                        this.originalSlot = this.mc.field_1724.method_31548().method_67532();
                        this.webPos = currentPos;
                        this.mc.field_1724.method_31548().method_61496(waterBucketSlot);
                        this.simulateRightClick();
                        this.currentState = WebBreaker.State.MONITORING;
                        this.timeoutTicks = 25;
                    }
                    break;
                case MONITORING:
                    boolean webIsGone = this.webPos != null && this.mc.field_1687.method_8320(this.webPos).method_26215();
                    boolean hasTimedOut = --this.timeoutTicks <= 0;
                    if (webIsGone || hasTimedOut) {
                        if (this.pickupWater.isEnabled() && this.mc.field_1724.method_6047().method_7909() == class_1802.field_8550) {
                            this.currentState = WebBreaker.State.PICKING_UP;
                            long now = System.currentTimeMillis();
                            this.pickupDeadlineTime = now + 1200L;
                            this.nextPickupAttemptTime = now + this.getPickupDelayMs();
                        } else {
                            this.currentState = WebBreaker.State.RESETTING;
                        }
                    }
                    break;
                case PICKING_UP:
                    if (!this.pickupWater.isEnabled()) {
                        this.currentState = WebBreaker.State.RESETTING;
                    } else if (this.mc.field_1724.method_6047().method_7909() != class_1802.field_8550) {
                        this.currentState = WebBreaker.State.RESETTING;
                    } else {
                        long now = System.currentTimeMillis();
                        if (now >= this.pickupDeadlineTime) {
                            this.currentState = WebBreaker.State.RESETTING;
                        } else if (now >= this.nextPickupAttemptTime) {
                            this.simulateRightClick();
                            this.nextPickupAttemptTime = now + this.getPickupDelayMs();
                        }
                    }
                    break;
                case RESETTING:
                    if (this.originalSlot != -1) {
                        this.mc.field_1724.method_31548().method_61496(this.originalSlot);
                    }

                    this.resetState();
                    this.nextActionTime = System.currentTimeMillis() + 75L;
            }
        }
    }

    private int findWaterBucket() {
        for (int i = 0; i < 9; i++) {
            if (this.mc.field_1724.method_31548().method_5438(i).method_7909() == class_1802.field_8705) {
                return i;
            }
        }

        return -1;
    }

    private void simulateRightClick() {
        SystemInputSimulator.pressUse();
        SystemInputSimulator.releaseUse();
    }

    private void resetState() {
        this.currentState = WebBreaker.State.IDLE;
        this.originalSlot = -1;
        this.webPos = null;
        this.timeoutTicks = 0;
        this.pickupDeadlineTime = 0L;
        this.nextPickupAttemptTime = 0L;
    }

    @Override
    public void onDisable() {
        this.resetState();
        this.nextActionTime = 0L;
    }

    private long getRandomMinDelay() {
        return (long)this.randomMinDelay.getValue();
    }

    private long getRandomMaxDelay() {
        return (long)this.randomMaxDelay.getValue();
    }

    private long getPickupDelayMs() {
        if (!this.randomization.isEnabled()) {
            return 0L;
        } else {
            long min = Math.max(0L, this.getRandomMinDelay());
            long max = Math.max(min, this.getRandomMaxDelay());
            return min == max ? min : ThreadLocalRandom.current().nextLong(min, max + 1L);
        }
    }

    @Environment(EnvType.CLIENT)
    private static enum State {
        IDLE,
        MONITORING,
        PICKING_UP,
        RESETTING;
    }
}
