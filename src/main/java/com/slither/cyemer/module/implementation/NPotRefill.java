package com.slither.cyemer.module.implementation;

import com.slither.cyemer.mixin.HandledScreenInterface;
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
public class NPotRefill extends Module {
    private static final int TOTEM_HOTBAR_INDEX = 1;      // hotbar slot 2
    private static final int HEALTH_HOTBAR_START = 2;     // hotbar slot 3
    private static final int HEALTH_HOTBAR_END = 6;       // hotbar slot 7
    private static final int STRENGTH_HOTBAR_INDEX = 7;   // hotbar slot 8
    private static final int SPEED_HOTBAR_INDEX = 8;      // hotbar slot 9

    private final SliderSetting swapDelay = new SliderSetting("Swap Delay (ms)", 80.0, 0.0, 500.0, 0);

    private long lastSwapTime = 0L;

    public NPotRefill() {
        super("NPotRefill", "Hover a totem / health / strength / speed pot to place it in slot 2 / 3-7 / 8 / 9.", Category.COMBAT);
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
        if (focused == null || !focused.method_7681()) {
            return;
        }
        class_1799 stack = focused.method_7677();

        int targetHotbarIndex = this.resolveTargetSlot(stack);
        if (targetHotbarIndex == -1) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - this.lastSwapTime < (long) this.swapDelay.getValue()) {
            return;
        }

        this.mc.field_1761.method_2906(
                screen.method_17577().field_7763,
                focused.field_7874,
                targetHotbarIndex,
                class_1713.field_7791,
                this.mc.field_1724);
        this.lastSwapTime = now;
    }

    private int resolveTargetSlot(class_1799 hovered) {
        if (this.isTotem(hovered)) {
            class_1799 existing = this.getHotbarStack(TOTEM_HOTBAR_INDEX);
            return (this.isTotem(existing) || this.isXpBottle(existing)) ? -1 : TOTEM_HOTBAR_INDEX;
        }
        if (this.isStrengthPot(hovered)) {
            class_1799 existing = this.getHotbarStack(STRENGTH_HOTBAR_INDEX);
            return (this.isStrengthPot(existing) || this.isXpBottle(existing)) ? -1 : STRENGTH_HOTBAR_INDEX;
        }
        if (this.isSpeedPot(hovered)) {
            class_1799 existing = this.getHotbarStack(SPEED_HOTBAR_INDEX);
            return (this.isSpeedPot(existing) || this.isXpBottle(existing)) ? -1 : SPEED_HOTBAR_INDEX;
        }
        if (this.isHealthPot(hovered)) {
            for (int i = HEALTH_HOTBAR_START; i <= HEALTH_HOTBAR_END; i++) {
                class_1799 existing = this.getHotbarStack(i);
                if (!this.isHealthPot(existing) && !this.isProtectedPot(existing) && !this.isXpBottle(existing)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean isXpBottle(class_1799 stack) {
        return stack != null && !stack.method_7960() && stack.method_7909() == class_1802.field_8287;
    }

    private class_1799 getHotbarStack(int index) {
        return this.mc.field_1724.method_31548().method_5438(index);
    }

    private boolean isTotem(class_1799 stack) {
        return stack != null && !stack.method_7960() && stack.method_7909() == class_1802.field_8288;
    }

    private boolean isHealthPot(class_1799 stack) {
        return this.matchesSplashPotion(stack,
                class_1847.field_8963,   // healing
                class_1847.field_8980);  // strong_healing
    }

    private boolean isStrengthPot(class_1799 stack) {
        return this.matchesSplashPotion(stack,
                class_1847.field_8978,
                class_1847.field_8965,
                class_1847.field_8993);
    }

    private boolean isSpeedPot(class_1799 stack) {
        return this.matchesSplashPotion(stack,
                class_1847.field_9005,
                class_1847.field_8983,
                class_1847.field_8966);
    }

    private boolean isProtectedPot(class_1799 stack) {
        return this.matchesSplashPotion(stack,
                class_1847.field_9005, class_1847.field_8983, class_1847.field_8966,  // swiftness
                class_1847.field_8978, class_1847.field_8965, class_1847.field_8993,  // strength
                class_1847.field_8986, class_1847.field_9003, class_1847.field_8992); // regeneration
    }

    @SafeVarargs
    private final boolean matchesSplashPotion(class_1799 stack, class_6880<class_1842>... potions) {
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
        for (class_6880<class_1842> p : potions) {
            if (entry.method_55838(p)) {
                return true;
            }
        }
        return false;
    }
}
