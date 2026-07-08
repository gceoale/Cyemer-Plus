package com.slither.cyemer.module.implementation;

import com.slither.cyemer.mixin.HandledScreenInterface;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1713;
import net.minecraft.class_1735;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1842;
import net.minecraft.class_1844;
import net.minecraft.class_1847;
import net.minecraft.class_490;
import net.minecraft.class_6880;
import net.minecraft.class_9334;

@Environment(EnvType.CLIENT)
public class PotRefill extends Module {
    private final BooleanSetting onlyStrong = new BooleanSetting("Only Strong", false);
    private final SliderSetting swapDelay = new SliderSetting("Swap Delay (ms)", 80.0, 0.0, 500.0, 0);

    private long lastSwapTime = 0L;

    public PotRefill() {
        super("PotRefill", "Refills health splash pots into hotbar slots 2-9 while hovering a pot in the inventory.", Category.COMBAT);
        this.addSetting(this.onlyStrong);
        this.addSetting(this.swapDelay);
    }

    @Override
    public void onDisable() {
        this.lastSwapTime = 0L;
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 == null || this.mc.field_1761 == null) {
            return;
        }
        if (!(this.mc.field_1755 instanceof class_490 screen)) {
            return;
        }
        class_1735 focused = ((HandledScreenInterface) screen).getFocusedSlot();
        if (focused == null || !focused.method_7681() || !this.isHealthPot(focused.method_7677())) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - this.lastSwapTime < (long) this.swapDelay.getValue()) {
            return;
        }

        int hotbarSlot = this.findHotbarSlotNeedingPot();
        if (hotbarSlot == -1) {
            return;
        }

        this.mc.field_1761.method_2906(
                screen.method_17577().field_7763,
                focused.field_7874,
                hotbarSlot,
                class_1713.field_7791,
                this.mc.field_1724);
        this.lastSwapTime = now;
    }

    private int findHotbarSlotNeedingPot() {
        for (int i = 1; i <= 8; i++) {
            class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
            if (!this.isHealthPot(stack) && !this.isProtectedPot(stack)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isProtectedPot(class_1799 stack) {
        if (stack == null || stack.method_7960() || stack.method_7909() != class_1802.field_8436) {
            return false;
        }
        class_1844 contents = stack.method_58694(class_9334.field_49651);
        if (contents == null) {
            return false;
        }
        Optional<class_6880<class_1842>> potionEntry = contents.comp_2378();
        if (!potionEntry.isPresent()) {
            return false;
        }
        class_6880<class_1842> entry = potionEntry.get();
        return entry.method_55838(class_1847.field_9005)     // swiftness
                || entry.method_55838(class_1847.field_8983) // long_swiftness
                || entry.method_55838(class_1847.field_8966) // strong_swiftness
                || entry.method_55838(class_1847.field_8978) // strength
                || entry.method_55838(class_1847.field_8965) // long_strength
                || entry.method_55838(class_1847.field_8993) // strong_strength
                || entry.method_55838(class_1847.field_8986) // regeneration
                || entry.method_55838(class_1847.field_9003) // long_regeneration
                || entry.method_55838(class_1847.field_8992); // strong_regeneration
    }

    private boolean isHealthPot(class_1799 stack) {
        if (stack == null || stack.method_7960() || stack.method_7909() != class_1802.field_8436) {
            return false;
        }
        class_1844 contents = stack.method_58694(class_9334.field_49651);
        if (contents == null) {
            return false;
        }
        Optional<class_6880<class_1842>> potionEntry = contents.comp_2378();
        if (!potionEntry.isPresent()) {
            return false;
        }
        class_6880<class_1842> entry = potionEntry.get();
        if (this.onlyStrong.isEnabled()) {
            return entry.method_55838(class_1847.field_8980);
        }
        return entry.method_55838(class_1847.field_8963) || entry.method_55838(class_1847.field_8980);
    }
}
