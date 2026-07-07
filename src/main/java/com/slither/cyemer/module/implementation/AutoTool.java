package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1661;
import net.minecraft.class_1799;
import net.minecraft.class_1887;
import net.minecraft.class_1890;
import net.minecraft.class_1893;
import net.minecraft.class_239;
import net.minecraft.class_2680;
import net.minecraft.class_3965;
import net.minecraft.class_6880;
import net.minecraft.class_7924;

@Environment(EnvType.CLIENT)
public class AutoTool extends Module {
    private final SliderSetting swapDelay = new SliderSetting("Swap Delay", 0.0, 0.0, 20.0, 0);
    private final SliderSetting swapBackDelay = new SliderSetting("Swap Back Delay", 1.0, 0.0, 40.0, 0);
    private AutoTool.State currentState = AutoTool.State.IDLE;
    private int originalSlot = -1;
    private int waitCounter = 0;

    public AutoTool() {
        super("AutoTool", "Automatically swaps to the best tool for mining.", Category.PLAYER);
        this.addSetting(this.swapDelay);
        this.addSetting(this.swapBackDelay);
    }

    @Override
    public void onDisable() {
        if (this.originalSlot != -1 && this.mc.field_1724 != null && this.currentState == AutoTool.State.TOOL_SWAPPED) {
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
        }

        this.reset();
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null && this.mc.field_1761 != null) {
            switch (this.currentState) {
                case IDLE:
                    if (this.mc.field_1761.method_2923()) {
                        class_239 target = this.mc.field_1765;
                        if (target instanceof class_3965) {
                            class_2680 blockState = this.mc.field_1687.method_8320(((class_3965)target).method_17777());
                            int bestToolSlot = this.findBestTool(blockState);
                            if (bestToolSlot != -1 && this.mc.field_1724.method_31548().method_67532() != bestToolSlot) {
                                this.startSwap();
                            }
                        }
                    }
                    break;
                case WAITING_TO_SWAP:
                    if (this.waitCounter > 0) {
                        this.waitCounter--;
                    } else {
                        this.swapToBestTool();
                        this.currentState = AutoTool.State.TOOL_SWAPPED;
                    }
                    break;
                case TOOL_SWAPPED:
                    if (!this.mc.field_1761.method_2923()) {
                        this.currentState = AutoTool.State.WAITING_FOR_SWAP_BACK;
                        this.waitCounter = (int)this.swapBackDelay.getValue();
                    }
                    break;
                case WAITING_FOR_SWAP_BACK:
                    if (this.mc.field_1761.method_2923()) {
                        this.currentState = AutoTool.State.TOOL_SWAPPED;
                    } else if (this.waitCounter > 0) {
                        this.waitCounter--;
                    } else {
                        this.swapBack();
                    }
            }
        }
    }

    private int findBestTool(class_2680 blockState) {
        class_1661 inventory = this.mc.field_1724.method_31548();
        int bestSlot = -1;
        float bestSpeed = 1.0F;
        class_6880<class_1887> efficiencyEntry = this.mc.field_1687.method_30349().method_30530(class_7924.field_41265).method_46747(class_1893.field_9131);

        for (int i = 0; i < 9; i++) {
            class_1799 stack = inventory.method_5438(i);
            if (!stack.method_7960()) {
                float speed = stack.method_7924(blockState);
                int efficiencyLevel = class_1890.method_8225(efficiencyEntry, stack);
                if (efficiencyLevel > 0) {
                    speed += efficiencyLevel * efficiencyLevel + 1;
                }

                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
        }

        return bestSlot;
    }

    private void startSwap() {
        this.originalSlot = this.mc.field_1724.method_31548().method_67532();
        this.waitCounter = (int)this.swapDelay.getValue();
        this.currentState = AutoTool.State.WAITING_TO_SWAP;
    }

    private void swapToBestTool() {
        if (this.mc.field_1765 instanceof class_3965) {
            class_2680 blockState = this.mc.field_1687.method_8320(((class_3965)this.mc.field_1765).method_17777());
            int bestSlot = this.findBestTool(blockState);
            if (bestSlot != -1) {
                this.mc.field_1724.method_31548().method_61496(bestSlot);
            }
        }
    }

    private void swapBack() {
        if (this.originalSlot != -1) {
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
        }

        this.reset();
    }

    private void reset() {
        this.currentState = AutoTool.State.IDLE;
        this.originalSlot = -1;
        this.waitCounter = 0;
    }

    @Environment(EnvType.CLIENT)
    private static enum State {
        IDLE,
        WAITING_TO_SWAP,
        TOOL_SWAPPED,
        WAITING_FOR_SWAP_BACK;
    }
}
