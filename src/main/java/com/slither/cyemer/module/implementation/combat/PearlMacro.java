package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.event.EventBus;
import com.slither.cyemer.event.impl.PearlThrowEvent;
import com.slither.cyemer.event.impl.ShieldDrainEvent;
import com.slither.cyemer.mixin.MinecraftClientAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;

@Environment(EnvType.CLIENT)
public class PearlMacro extends Module {
    private final SliderSetting switchDelayMin = new SliderSetting("Switch Delay Min", 5.0, 0.0, 500.0, 2);
    private final SliderSetting switchDelayMax = new SliderSetting("Switch Delay Max", 20.0, 0.0, 500.0, 2);
    private final SliderSetting windDelayMin = new SliderSetting("Wind Delay Min", 5.0, 0.0, 200.0, 2);
    private final SliderSetting windDelayMax = new SliderSetting("Wind Delay Max", 20.0, 0.0, 200.0, 2);
    private final BooleanSetting swapBack = new BooleanSetting("Swap Back", true);
    private final Random random = new Random();
    private int stage = 0;
    private long lastTime = 0L;
    private int originalSlot = -1;
    private long randomizedSwitchDelay = 0L;
    private long randomizedWindDelay = 0L;

    public PearlMacro() {
        super("PearlMacro", "Throws a Pearl followed by a Wind Charge", Category.COMBAT);
        this.addSetting(this.switchDelayMin);
        this.addSetting(this.switchDelayMax);
        this.addSetting(this.windDelayMin);
        this.addSetting(this.windDelayMax);
        this.addSetting(this.swapBack);
    }

    @Override
    public void onEnable() {
        if (this.mc.field_1724 == null) {
            this.toggle();
        } else if (this.mc.field_1724.method_6115()) {
            this.toggle();
        } else {
            this.stage = 0;
            this.lastTime = System.currentTimeMillis();
            this.originalSlot = -1;
            this.randomizedSwitchDelay = this.getDelay(this.switchDelayMin.getValue(), this.switchDelayMax.getValue());
            this.randomizedWindDelay = this.getDelay(this.windDelayMin.getValue(), this.windDelayMax.getValue());
        }
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            ShieldDrainEvent drainEvent = new ShieldDrainEvent();
            EventBus.post(drainEvent);
            if (!drainEvent.isActive()) {
                if (this.stage == 0) {
                    if (this.mc.field_1724.method_6115()) {
                        this.toggle();
                        return;
                    }

                    if (this.originalSlot == -1) {
                        this.originalSlot = this.mc.field_1724.method_31548().method_67532();
                    }

                    int pearlSlot = this.findItemSlot(class_1802.field_8634);
                    if (pearlSlot == -1) {
                        this.toggle();
                        return;
                    }

                    if (this.mc.field_1724.method_31548().method_67532() != pearlSlot) {
                        this.mc.field_1724.method_31548().method_61496(pearlSlot);
                    }

                    this.stage = 1;
                }

                if (this.stage == 1) {
                    EventBus.post(new PearlThrowEvent());
                    this.doClick();
                    this.lastTime = System.currentTimeMillis();
                    this.stage = 2;
                }

                if (this.stage == 2) {
                    if (System.currentTimeMillis() - this.lastTime < this.randomizedSwitchDelay) {
                        return;
                    }

                    int windSlot = this.findItemSlot(class_1802.field_49098);
                    if (windSlot == -1) {
                        this.finish();
                        return;
                    }

                    if (this.mc.field_1724.method_31548().method_67532() != windSlot) {
                        this.mc.field_1724.method_31548().method_61496(windSlot);
                    }

                    this.stage = 3;
                }

                if (this.stage == 3) {
                    this.doClick();
                    this.lastTime = System.currentTimeMillis();
                    this.stage = 4;
                }

                if (this.stage == 4 && System.currentTimeMillis() - this.lastTime >= this.randomizedWindDelay) {
                    this.finish();
                }
            }
        } else {
            this.toggle();
        }
    }

    private void finish() {
        if (this.swapBack.isEnabled() && this.originalSlot != -1 && this.mc.field_1724.method_31548().method_67532() != this.originalSlot) {
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
        }

        this.originalSlot = -1;
        this.stage = 0;
        this.toggle();
    }

    private void doClick() {
        ((MinecraftClientAccessor)this.mc).useItem();
    }

    private int findItemSlot(class_1792 item) {
        for (int i = 0; i < 9; i++) {
            class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
            if (stack != null && !stack.method_7960() && stack.method_7909() == item) {
                return i;
            }
        }

        return -1;
    }

    private long getDelay(double min, double max) {
        return min >= max ? (long)min : (long)(min + this.random.nextDouble() * (max - min));
    }
}
