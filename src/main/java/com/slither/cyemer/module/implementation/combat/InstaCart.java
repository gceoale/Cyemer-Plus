package com.slither.cyemer.module.implementation.combat;

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
public class InstaCart extends Module {
    private final SliderSetting railDelayMin = new SliderSetting("Rail Delay Min", 50.0, 0.0, 500.0, 0);
    private final SliderSetting railDelayMax = new SliderSetting("Rail Delay Max", 150.0, 0.0, 500.0, 0);
    private final SliderSetting cooldownMin = new SliderSetting("Min Cooldown", 5.0, 0.0, 100.0, 0);
    private final SliderSetting cooldownMax = new SliderSetting("Max Cooldown", 15.0, 0.0, 100.0, 0);
    private final SliderSetting cartDelayMin = new SliderSetting("Cart Delay Min", 50.0, 0.0, 500.0, 0);
    private final SliderSetting cartDelayMax = new SliderSetting("Cart Delay Max", 150.0, 0.0, 500.0, 0);
    private final BooleanSetting swapBack = new BooleanSetting("Swap Back", true);
    private final Random random = new Random();
    private int stage = 0;
    private long lastTime = 0L;
    private int originalSlot = -1;
    private long randomizedDelay = 0L;

    public InstaCart() {
        super("InstaCart", "Places a rail, shoots a bow fast, and places a TNT minecart.", Category.COMBAT);
        this.addSetting(this.railDelayMin);
        this.addSetting(this.railDelayMax);
        this.addSetting(this.cooldownMin);
        this.addSetting(this.cooldownMax);
        this.addSetting(this.cartDelayMin);
        this.addSetting(this.cartDelayMax);
        this.addSetting(this.swapBack);
    }

    @Override
    public void onEnable() {
        if (this.mc.field_1724 == null) {
            this.toggle();
        } else {
            this.lastTime = System.currentTimeMillis();
            this.originalSlot = this.mc.field_1724.method_31548().method_67532();
            this.randomizedDelay = 0L;
            this.stage = 0;
        }
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - this.lastTime >= this.randomizedDelay) {
                switch (this.stage) {
                    case 0:
                        int bowSlot = this.findItemSlot(class_1802.field_8102);
                        if (bowSlot == -1) {
                            this.finish();
                            return;
                        }

                        if (this.mc.field_1724.method_31548().method_67532() != bowSlot) {
                            this.mc.field_1724.method_31548().method_61496(bowSlot);
                        }

                        this.mc.field_1690.field_1904.method_23481(true);
                        this.doClick();
                        this.lastTime = currentTime;
                        this.randomizedDelay = this.getDelay(this.cooldownMin.getValue() * 10.0, this.cooldownMax.getValue() * 10.0);
                        this.stage = 1;
                        break;
                    case 1:
                        this.mc.field_1690.field_1904.method_23481(false);
                        this.lastTime = currentTime;
                        this.randomizedDelay = this.getDelay(this.railDelayMin.getValue(), this.railDelayMax.getValue());
                        this.stage = 2;
                        break;
                    case 2:
                        int railSlot = this.findRailSlot();
                        if (railSlot == -1) {
                            this.finish();
                            return;
                        }

                        if (this.mc.field_1724.method_31548().method_67532() != railSlot) {
                            this.mc.field_1724.method_31548().method_61496(railSlot);
                        }

                        this.doClick();
                        this.lastTime = currentTime;
                        this.randomizedDelay = this.getDelay(this.cartDelayMin.getValue(), this.cartDelayMax.getValue());
                        this.stage = 3;
                        break;
                    case 3:
                        int cartSlot = this.findItemSlot(class_1802.field_8069);
                        if (cartSlot == -1) {
                            this.finish();
                            return;
                        }

                        if (this.mc.field_1724.method_31548().method_67532() != cartSlot) {
                            this.mc.field_1724.method_31548().method_61496(cartSlot);
                        }

                        this.doClick();
                        this.lastTime = currentTime;
                        this.randomizedDelay = 100L;
                        this.stage = 4;
                        break;
                    case 4:
                        this.finish();
                }
            }
        } else {
            this.toggle();
        }
    }

    private void finish() {
        this.mc.field_1690.field_1904.method_23481(false);
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

    private int findRailSlot() {
        for (int i = 0; i < 9; i++) {
            class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
            if (stack != null && !stack.method_7960()) {
                class_1792 item = stack.method_7909();
                if (item == class_1802.field_8129 || item == class_1802.field_8848) {
                    return i;
                }
            }
        }

        return -1;
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
