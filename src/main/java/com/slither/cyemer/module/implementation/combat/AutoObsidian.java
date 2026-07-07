package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.mixin.KeyBindingAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2199;
import net.minecraft.class_2248;
import net.minecraft.class_2281;
import net.minecraft.class_2304;
import net.minecraft.class_2331;
import net.minecraft.class_2338;
import net.minecraft.class_2480;
import net.minecraft.class_304;
import net.minecraft.class_3489;
import net.minecraft.class_3708;
import net.minecraft.class_3865;
import net.minecraft.class_3965;

@Environment(EnvType.CLIENT)
public class AutoObsidian extends Module {
    private final BooleanSetting onlySword = new BooleanSetting("Only Sword", true);
    private final BooleanSetting simClick = new BooleanSetting("SimClick", true);
    private final BooleanSetting swapBack = new BooleanSetting("Swap Back", true);
    private final SliderSetting delay = new SliderSetting("Delay (ms)", 100.0, 0.0, 500.0, 0);
    private long lastActionTime = 0L;
    private int originalSlot = -1;
    private boolean isWaitingForPlace = false;

    public AutoObsidian() {
        super("AutoObsidian", "Places obsidian when holding a sword.", Category.COMBAT);
        this.addSetting(this.onlySword);
        this.addSetting(this.simClick);
        this.addSetting(this.swapBack);
        this.addSetting(this.delay);
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null && this.mc.field_1761 != null) {
            if (this.isWaitingForPlace) {
                if (this.mc.field_1765 instanceof class_3965 hitResult) {
                    this.performPlaceAction(hitResult);
                    this.lastActionTime = System.currentTimeMillis();
                }

                if (this.swapBack.isEnabled() && this.originalSlot != -1) {
                    this.mc.field_1724.method_31548().method_61496(this.originalSlot);
                }

                this.isWaitingForPlace = false;
                this.originalSlot = -1;
                return;
            }

            if (this.mc.field_1690.field_1904.method_1434()
                && !this.mc.field_1724.method_7325()
                && System.currentTimeMillis() - this.lastActionTime >= this.delay.getValue()
                && (!this.onlySword.isEnabled() || this.mc.field_1724.method_6047().method_31573(class_3489.field_42611))
                && this.mc.field_1765 instanceof class_3965 hitResult) {
                class_2248 targetedBlock = this.mc.field_1687.method_8320(hitResult.method_17777()).method_26204();
                if (!this.isInteractableBlock(targetedBlock)) {
                    class_2338 placementPos = hitResult.method_17777().method_10093(hitResult.method_17780());
                    if (this.mc.field_1687.method_8320(placementPos).method_26215()) {
                        if (this.mc.field_1724.method_6047().method_7909() == class_1802.field_8281) {
                            this.performPlaceAction(hitResult);
                            this.lastActionTime = System.currentTimeMillis();
                        } else {
                            int obsidianSlot = this.findObsidianInHotbar();
                            if (obsidianSlot != -1) {
                                this.originalSlot = this.mc.field_1724.method_31548().method_67532();
                                this.mc.field_1724.method_31548().method_61496(obsidianSlot);
                                this.isWaitingForPlace = true;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isInteractableBlock(class_2248 block) {
        return block instanceof class_2304
            || block instanceof class_2199
            || block instanceof class_3708
            || block instanceof class_2281
            || block instanceof class_3865
            || block instanceof class_2480
            || block instanceof class_2331;
    }

    private void performPlaceAction(class_3965 hitResult) {
        if (this.simClick.isEnabled()) {
            class_304 useKey = this.mc.field_1690.field_1904;
            KeyBindingAccessor accessor = (KeyBindingAccessor)useKey;
            accessor.setTimesPressed(accessor.getTimesPressed() + 1);
        } else {
            class_304 useKey = this.mc.field_1690.field_1904;
            KeyBindingAccessor accessor = (KeyBindingAccessor)useKey;
            accessor.setTimesPressed(accessor.getTimesPressed() + 1);
        }
    }

    private int findObsidianInHotbar() {
        for (int i = 0; i < 9; i++) {
            class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
            if (stack.method_7909() == class_1802.field_8281) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public void onDisable() {
        this.isWaitingForPlace = false;
        this.originalSlot = -1;
    }
}
