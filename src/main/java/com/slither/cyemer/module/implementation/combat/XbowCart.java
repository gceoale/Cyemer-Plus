package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.mixin.MinecraftClientAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.PlaceValidator;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1764;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;

@Environment(EnvType.CLIENT)
public class XbowCart extends Module {
    private final BooleanSetting swapBack = new BooleanSetting("Swap Back", true);
    private final SliderSetting railDelayMin = new SliderSetting("Rail Delay Min", 50.0, 0.0, 1000.0, 0);
    private final SliderSetting railDelayMax = new SliderSetting("Rail Delay Max", 250.0, 0.0, 1000.0, 0);
    private final SliderSetting cartDelayMin = new SliderSetting("Cart Delay Min", 50.0, 0.0, 1000.0, 0);
    private final SliderSetting cartDelayMax = new SliderSetting("Cart Delay Max", 250.0, 0.0, 1000.0, 0);
    private final SliderSetting fireDelayMin = new SliderSetting("Fire Delay Min", 100.0, 0.0, 1000.0, 0);
    private final SliderSetting fireDelayMax = new SliderSetting("Fire Delay Max", 300.0, 0.0, 1000.0, 0);
    private final SliderSetting loadDelayMin = new SliderSetting("Load Delay Min", 100.0, 0.0, 1000.0, 0);
    private final SliderSetting loadDelayMax = new SliderSetting("Load Delay Max", 300.0, 0.0, 1000.0, 0);
    private final SliderSetting shootDelayMin = new SliderSetting("Shoot Delay Min", 50.0, 0.0, 1000.0, 0);
    private final SliderSetting shootDelayMax = new SliderSetting("Shoot Delay Max", 250.0, 0.0, 1000.0, 0);
    private final Random random = new Random();
    private int stage = 0;
    private long lastTime = 0L;
    private int originalSlot = -1;
    private float originalPitch = 0.0F;
    private float originalYaw = 0.0F;
    private long randomizedDelay = 0L;
    private boolean loadingCrossbow = false;
    private float initialAimPitch = 0.0F;

    public XbowCart() {
        super("XbowCart", "Crossbow carting macro", Category.COMBAT);
        this.addSetting(this.swapBack);
        this.addSetting(this.railDelayMin);
        this.addSetting(this.railDelayMax);
        this.addSetting(this.cartDelayMin);
        this.addSetting(this.cartDelayMax);
        this.addSetting(this.fireDelayMin);
        this.addSetting(this.fireDelayMax);
        this.addSetting(this.loadDelayMin);
        this.addSetting(this.loadDelayMax);
        this.addSetting(this.shootDelayMin);
        this.addSetting(this.shootDelayMax);
    }

    @Override
    public void onEnable() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            if (this.findRailSlot() != -1
                && this.findItemSlot(class_1802.field_8069) != -1
                && this.findItemSlot(class_1802.field_8884) != -1
                && this.findItemSlot(class_1802.field_8399) != -1) {
                this.originalSlot = this.mc.field_1724.method_31548().method_67532();
                this.originalPitch = this.mc.field_1724.method_36455();
                this.originalYaw = this.mc.field_1724.method_36454();
                this.initialAimPitch = this.mc.field_1724.method_36455();
                this.lastTime = System.currentTimeMillis();
                this.randomizedDelay = 0L;
                this.stage = 0;
                this.loadingCrossbow = false;
            } else {
                this.toggle();
            }
        } else {
            this.toggle();
        }
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - this.lastTime >= this.randomizedDelay) {
                switch (this.stage) {
                    case 0:
                        int railSlot = this.findRailSlot();
                        if (railSlot == -1) {
                            this.finish();
                            return;
                        }

                        this.switchTo(railSlot);
                        PlaceValidator.tryPlace(this.mc);
                        this.lastTime = currentTime;
                        this.randomizedDelay = 50L + this.getDelay(this.railDelayMin.getValue(), this.railDelayMax.getValue());
                        this.stage = 1;
                        break;
                    case 1:
                        int cartSlot = this.findItemSlot(class_1802.field_8069);
                        if (cartSlot == -1) {
                            this.finish();
                            return;
                        }

                        this.switchTo(cartSlot);
                        PlaceValidator.tryPlace(this.mc);
                        this.lastTime = currentTime;
                        this.randomizedDelay = 50L + this.getDelay(this.cartDelayMin.getValue(), this.cartDelayMax.getValue());
                        this.stage = 2;
                        break;
                    case 2:
                        this.mc.field_1724.method_36456(this.originalYaw);
                        this.mc.field_1724.method_36457(28.1F);
                        int flintSlot = this.findItemSlot(class_1802.field_8884);
                        if (flintSlot == -1) {
                            this.finish();
                            return;
                        }

                        this.switchTo(flintSlot);
                        this.lastTime = currentTime;
                        this.randomizedDelay = 50L;
                        this.stage = 21;
                        break;
                    case 4:
                        int crossbowSlot = this.findItemSlot(class_1802.field_8399);
                        if (crossbowSlot == -1) {
                            this.finish();
                            return;
                        }

                        this.switchTo(crossbowSlot);
                        class_1799 crossbowStack = this.mc.field_1724.method_31548().method_5438(crossbowSlot);
                        if (!class_1764.method_7781(crossbowStack)) {
                            if (this.loadingCrossbow) {
                                this.finish();
                                return;
                            }

                            this.mc.field_1690.field_1904.method_23481(true);
                            this.doClick();
                            this.loadingCrossbow = true;
                            this.lastTime = currentTime;
                            this.randomizedDelay = 1300L + this.getDelay(this.loadDelayMin.getValue(), this.loadDelayMax.getValue());
                        } else {
                            this.lastTime = currentTime;
                            this.randomizedDelay = 50L + this.getDelay(this.shootDelayMin.getValue(), this.shootDelayMax.getValue());
                            this.stage = 5;
                        }
                        break;
                    case 5:
                        if (this.loadingCrossbow) {
                            this.mc.field_1690.field_1904.method_23481(false);
                            this.loadingCrossbow = false;
                            this.lastTime = currentTime;
                            this.randomizedDelay = 100L;
                            this.stage = 6;
                            break;
                        } else {
                            this.stage = 6;
                        }
                    case 6:
                        this.mc.field_1724.method_36457(24.2F);
                        this.lastTime = currentTime;
                        this.randomizedDelay = 50L;
                        this.stage = 61;
                        break;
                    case 7:
                        this.finish();
                        break;
                    case 21:
                        if (!PlaceValidator.tryPlace(this.mc)) {
                            this.doClick();
                        }

                        this.lastTime = currentTime;
                        this.randomizedDelay = 50L + this.getDelay(this.fireDelayMin.getValue(), this.fireDelayMax.getValue());
                        this.stage = 4;
                        break;
                    case 61:
                        this.doClick();
                        this.lastTime = currentTime;
                        this.randomizedDelay = 150L;
                        this.stage = 7;
                }
            }
        } else {
            this.toggle();
        }
    }

    private void finish() {
        this.mc.field_1690.field_1904.method_23481(false);
        if (this.swapBack.isEnabled() && this.originalSlot != -1) {
            this.switchTo(this.originalSlot);
        }

        this.mc.field_1724.method_36457(this.originalPitch);
        this.mc.field_1724.method_36456(this.originalYaw);
        this.originalSlot = -1;
        this.stage = 0;
        this.toggle();
    }

    private void switchTo(int slot) {
        if (this.mc.field_1724.method_31548().method_67532() != slot) {
            this.mc.field_1724.method_31548().method_61496(slot);
        }
    }

    private void doClick() {
        ((MinecraftClientAccessor)this.mc).useItem();
    }

    private int findRailSlot() {
        return this.findItemSlot(class_1802.field_8129, class_1802.field_8848);
    }

    private int findItemSlot(class_1792... items) {
        for (int i = 0; i < 9; i++) {
            class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
            if (stack != null && !stack.method_7960()) {
                class_1792 stackItem = stack.method_7909();

                for (class_1792 target : items) {
                    if (stackItem == target) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    private long getDelay(double min, double max) {
        return min >= max ? (long)min : (long)(min + this.random.nextDouble() * (max - min));
    }
}
