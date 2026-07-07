package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.ModuleRandomDelay;
import com.slither.cyemer.util.SystemInputSimulator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1799;
import net.minecraft.class_1802;

@Environment(EnvType.CLIENT)
public class WindChargeKey extends Module {
    private final BooleanSetting randomization = new BooleanSetting("Randomization", false);
    private final SliderSetting randomMinDelay = new SliderSetting("Random Min (ms)", 0.0, 0.0, 500.0, 0);
    private final SliderSetting randomMaxDelay = new SliderSetting("Random Max (ms)", 25.0, 0.0, 500.0, 0);
    private boolean hasThrown = false;
    private int originalSlot = -1;
    private int waitCounter = 0;
    private static final int DELAY_TICKS = 2;

    public WindChargeKey() {
        super("WindChargeKey", "Swaps to and throws a wind charge, then swaps back.", Category.COMBAT);
        this.addSetting(this.randomization);
        this.addSetting(this.randomMinDelay);
        this.addSetting(this.randomMaxDelay);
    }

    @Override
    public void onEnable() {
        this.hasThrown = false;
        this.originalSlot = -1;
        this.waitCounter = 0;
        if (this.mc.field_1724 == null) {
            this.toggle();
        } else {
            int pearlSlot = this.findChargeSlot();
            if (pearlSlot != -1) {
                this.originalSlot = this.mc.field_1724.method_31548().method_67532();
                this.mc.field_1724.method_31548().method_61496(pearlSlot);
            } else {
                this.toggle();
            }
        }
    }

    @Override
    public void onDisable() {
        if (this.originalSlot != -1 && this.mc.field_1724 != null) {
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
        }

        this.originalSlot = -1;
        this.hasThrown = false;
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.originalSlot != -1) {
            if (!this.hasThrown) {
                if (this.mc.field_1724.method_6047().method_7909() == class_1802.field_49098) {
                    if (!this.randomization.isEnabled()
                        || ModuleRandomDelay.gateAction("combat.use.windchargekey.throw", this.getRandomMinDelay(), this.getRandomMaxDelay())) {
                        SystemInputSimulator.pressUse();
                        this.hasThrown = true;
                    }
                } else {
                    this.toggle();
                }
            } else {
                this.waitCounter++;
                if (this.waitCounter >= 2) {
                    SystemInputSimulator.releaseUse();
                    this.toggle();
                }
            }
        }
    }

    private int findChargeSlot() {
        for (int i = 0; i < 9; i++) {
            class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
            if (stack.method_7909() == class_1802.field_49098) {
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
}
