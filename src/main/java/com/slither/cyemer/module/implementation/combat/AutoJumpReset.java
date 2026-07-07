package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.mixin.KeyBindingAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class AutoJumpReset extends Module {
    private final SliderSetting missChance = new SliderSetting("Miss Chance %", 0.0, 0.0, 100.0, 0);
    private final BooleanSetting randomization = new BooleanSetting("Randomization", false);
    private final SliderSetting randomMinDelay = new SliderSetting("Min Random (ms)", 0.0, 0.0, 500.0, 0);
    private final SliderSetting randomMaxDelay = new SliderSetting("Max Random (ms)", 25.0, 0.0, 500.0, 0);
    private boolean jumpQueued = false;
    private long queuedJumpAtMs = 0L;

    public AutoJumpReset() {
        super("AutoJumpReset", "Jumps after being hit to reset momentum.", Category.COMBAT);
        this.addSetting(this.missChance);
        this.addSetting(this.randomization);
        this.addSetting(this.randomMinDelay);
        this.addSetting(this.randomMaxDelay);
    }

    @Override
    public void onTick() {
        if (this.isEnabled() && this.mc.field_1724 != null && this.mc.field_1687 != null) {
            if (this.jumpQueued) {
                if (System.currentTimeMillis() >= this.queuedJumpAtMs) {
                    this.jumpQueued = false;
                    this.simulateJumpKeyPress();
                }
            }
        }
    }

    public void onVelocityPacket() {
        if (this.isEnabled() && this.mc.field_1724 != null && this.mc.field_1687 != null) {
            if (!this.shouldMiss()) {
                long delayMs = this.getDelayMs();
                if (delayMs <= 0L) {
                    this.simulateJumpKeyPress();
                } else {
                    this.queuedJumpAtMs = System.currentTimeMillis() + delayMs;
                    this.jumpQueued = true;
                }
            }
        }
    }

    private boolean shouldMiss() {
        double missPercent = this.missChance.getValue();
        return missPercent <= 0.0 ? false : ThreadLocalRandom.current().nextDouble(0.0, 100.0) < missPercent;
    }

    private long getDelayMs() {
        if (!this.randomization.isEnabled()) {
            return 0L;
        } else {
            long minDelay = (long)this.randomMinDelay.getValue();
            long maxDelay = (long)this.randomMaxDelay.getValue();
            if (minDelay > maxDelay) {
                long tmp = minDelay;
                minDelay = maxDelay;
                maxDelay = tmp;
            }

            return ThreadLocalRandom.current().nextLong(minDelay, maxDelay + 1L);
        }
    }

    private void simulateJumpKeyPress() {
        if (this.mc.field_1724 != null && this.mc.field_1690 != null && this.mc.field_1755 == null) {
            KeyBindingAccessor jumpKey = (KeyBindingAccessor)this.mc.field_1690.field_1903;
            jumpKey.setTimesPressed(jumpKey.getTimesPressed() + 1);
        }
    }

    @Override
    public void onDisable() {
        this.jumpQueued = false;
        this.queuedJumpAtMs = 0L;
    }
}
