package com.slither.cyemer.module.implementation;

import com.slither.cyemer.mixin.KeyBindingAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.ModuleRandomDelay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10192;
import net.minecraft.class_1304;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_9334;

@Environment(EnvType.CLIENT)
public class ElytraSwap extends Module {
    private final BooleanSetting randomization = new BooleanSetting("Randomization", false);
    private final SliderSetting randomMinDelay = new SliderSetting("Random Min (ms)", 0.0, 0.0, 500.0, 0);
    private final SliderSetting randomMaxDelay = new SliderSetting("Random Max (ms)", 25.0, 0.0, 500.0, 0);
    private int originalSlot = -1;
    private int tickCounter = 0;
    private boolean wasWearingElytra = false;
    private static final int MAX_WAIT_TICKS = 10;

    public ElytraSwap() {
        super("ElytraSwap", "Swaps between Elytra and Chestplate.", Category.PLAYER);
        this.addSetting(this.randomization);
        this.addSetting(this.randomMinDelay);
        this.addSetting(this.randomMaxDelay);
    }

    @Override
    public void onEnable() {
        if (this.mc.field_1724 == null) {
            this.toggle();
        } else {
            class_1799 currentChest = this.mc.field_1724.method_6118(class_1304.field_6174);
            this.wasWearingElytra = currentChest.method_31574(class_1802.field_8833);
            int targetSlot = this.findSwapSlot();
            if (targetSlot != -1) {
                this.originalSlot = this.mc.field_1724.method_31548().method_67532();
                this.mc.field_1724.method_31548().method_61496(targetSlot);
                this.tickCounter = 0;
            } else {
                this.toggle();
            }
        }
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.originalSlot != -1) {
            if (this.tickCounter == 0
                && (
                    !this.randomization.isEnabled()
                        || ModuleRandomDelay.gateAction("player.use.elytraswap", this.getRandomMinDelay(), this.getRandomMaxDelay())
                )) {
                KeyBindingAccessor useKey = (KeyBindingAccessor)this.mc.field_1690.field_1904;
                useKey.setTimesPressed(useKey.getTimesPressed() + 1);
            }

            this.tickCounter++;
            class_1799 currentChest = this.mc.field_1724.method_6118(class_1304.field_6174);
            boolean isNowWearingElytra = currentChest.method_31574(class_1802.field_8833);
            boolean swapComplete = this.wasWearingElytra && !isNowWearingElytra && this.isChestplate(currentChest)
                || !this.wasWearingElytra && isNowWearingElytra;
            if (swapComplete || this.tickCounter >= 10) {
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
        this.tickCounter = 0;
        this.wasWearingElytra = false;
    }

    private int findSwapSlot() {
        class_1799 currentChest = this.mc.field_1724.method_6118(class_1304.field_6174);
        boolean wearingElytra = currentChest.method_31574(class_1802.field_8833);

        for (int i = 0; i < 9; i++) {
            class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
            if (!stack.method_7960()) {
                if (wearingElytra) {
                    if (this.isChestplate(stack)) {
                        return i;
                    }
                } else if (stack.method_31574(class_1802.field_8833)) {
                    return i;
                }
            }
        }

        return -1;
    }

    private boolean isChestplate(class_1799 stack) {
        if (stack.method_7960()) {
            return false;
        } else if (stack.method_31574(class_1802.field_8833)) {
            return false;
        } else {
            class_10192 equippable = (class_10192)stack.method_58694(class_9334.field_54196);
            return equippable != null && equippable.comp_3174() == class_1304.field_6174;
        }
    }

    private long getRandomMinDelay() {
        return (long)this.randomMinDelay.getValue();
    }

    private long getRandomMaxDelay() {
        return (long)this.randomMaxDelay.getValue();
    }
}
