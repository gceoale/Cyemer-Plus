package com.slither.cyemer.module.implementation;

import com.slither.cyemer.mixin.KeyBindingAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1799;
import net.minecraft.class_1802;

@Environment(EnvType.CLIENT)
public class KeyPearl extends Module {
    private final BooleanSetting randomization = new BooleanSetting("Randomization", false);
    private final SliderSetting randomMinDelay = new SliderSetting("Random Min (ms)", 0.0, 0.0, 500.0, 0);
    private final SliderSetting randomMaxDelay = new SliderSetting("Random Max (ms)", 25.0, 0.0, 500.0, 0);
    private boolean hasThrown = false;
    private int originalSlot = -1;
    private long throwReadyAtMs = 0L;
    private int waitCounter = 0;
    private static final int DELAY_TICKS = 2;

    public KeyPearl() {
        super("KeyPearl", "Swaps to and throws a pearl, then swaps back.", Category.COMBAT);
        this.addSetting(this.randomization);
        this.addSetting(this.randomMinDelay);
        this.addSetting(this.randomMaxDelay);
    }

    @Override
    public void onEnable() {
        this.hasThrown = false;
        this.originalSlot = -1;
        this.waitCounter = 0;
        this.throwReadyAtMs = 0L;
        if (this.mc.field_1724 == null) {
            this.toggle();
        } else {
            int pearlSlot = this.findPearlSlot();
            if (pearlSlot != -1) {
                this.originalSlot = this.mc.field_1724.method_31548().method_67532();
                this.mc.field_1724.method_31548().method_61496(pearlSlot);
                this.throwReadyAtMs = this.randomization.isEnabled() ? System.currentTimeMillis() + this.sampleRandomDelayMs() : 0L;
            } else {
                this.toggle();
            }
        }
    }

    @Override
    public void onDisable() {
        if (this.mc.field_1690 != null) {
            this.mc.field_1690.field_1904.method_23481(false);
        }

        if (this.originalSlot != -1 && this.mc.field_1724 != null) {
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
        }

        this.originalSlot = -1;
        this.hasThrown = false;
        this.waitCounter = 0;
        this.throwReadyAtMs = 0L;
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1690 != null && this.originalSlot != -1) {
            if (!this.hasThrown) {
                if (this.mc.field_1724.method_6047().method_7909() == class_1802.field_8634) {
                    if (this.throwReadyAtMs > 0L && System.currentTimeMillis() < this.throwReadyAtMs) {
                        return;
                    }

                    this.mc.field_1690.field_1904.method_23481(true);
                    if (this.mc.field_1690.field_1904 instanceof KeyBindingAccessor accessor) {
                        accessor.setTimesPressed(accessor.getTimesPressed() + 1);
                    }

                    this.hasThrown = true;
                } else {
                    this.toggle();
                }
            } else {
                this.waitCounter++;
                if (this.waitCounter >= 2) {
                    this.mc.field_1690.field_1904.method_23481(false);
                    this.toggle();
                }
            }
        } else {
            this.toggle();
        }
    }

    private int findPearlSlot() {
        for (int i = 0; i < 9; i++) {
            class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
            if (stack.method_7909() == class_1802.field_8634) {
                return i;
            }
        }

        return -1;
    }

    private long getRandomMinDelay() {
        return (long)this.randomMinDelay.getValue();
    }

    private long getRandomMaxDelay() {
        return (long)this.randomMaxDelay.getValue();
    }

    private long sampleRandomDelayMs() {
        long min = Math.max(0L, this.getRandomMinDelay());
        long max = Math.max(0L, this.getRandomMaxDelay());
        if (max < min) {
            long tmp = min;
            min = max;
            max = tmp;
        }

        return max == min ? min : ThreadLocalRandom.current().nextLong(min, max + 1L);
    }
}
