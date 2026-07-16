package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.mixin.KeyBindingAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1511;
import net.minecraft.class_1774;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_239;
import net.minecraft.class_3965;
import net.minecraft.class_3966;

@Environment(EnvType.CLIENT)
public class AutoCrystal extends Module {
    private final SliderSetting delay = new SliderSetting("Delay (ms)", 100.0, 0.0, 500.0, 0);
    private final BooleanSetting onlyOnRightClick = new BooleanSetting("Only On Right Click", true);
    private final BooleanSetting simClick = new BooleanSetting("SimClick", true);
    private final BooleanSetting swapToCrystal = new BooleanSetting("Swap to crystal on Right Click", true);
    private final BooleanSetting blockOnShield = new BooleanSetting("Block On Shield", true);
    private final BooleanSetting swapBackToSword = new BooleanSetting("Swap Back to Sword", true);
    private long lastActionTime = 0L;
    private int lastSwordSlot = -1;
    private boolean restoreSwordNextTick = false;
    private static final double REACH_DISTANCE_SQUARED = 20.25;
    private static final double ATTACK_REACH = 4.5;

    public AutoCrystal() {
        super("AutoCrystal", "Places and breaks crystals legit", Category.COMBAT);
        this.addSetting(this.delay);
        this.addSetting(this.onlyOnRightClick);
        this.addSetting(this.simClick);
        this.addSetting(this.swapToCrystal);
        this.addSetting(this.blockOnShield);
        this.addSetting(this.swapBackToSword);
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null && this.mc.field_1761 != null) {
            // Remember the hotbar slot the user was holding when it was a sword
            // - this is what we restore to after the swap-to-crystal sequence.
            // No SwordItem class in 1.21.11, match on the item's registry name.
            if (isSword(this.mc.field_1724.method_6047())) {
                this.lastSwordSlot = this.mc.field_1724.method_31548().method_67532();
            }

            // Deferred restore fires the tick after a crystal placement, once
            // MC's use-key handler has actually consumed the placement.
            if (this.restoreSwordNextTick) {
                this.restoreSwordNextTick = false;
                if (this.swapBackToSword.isEnabled() && this.lastSwordSlot != -1) {
                    this.mc.field_1724.method_31548().method_61496(this.lastSwordSlot);
                }
            }

            if ((!this.onlyOnRightClick.isEnabled() || this.mc.field_1690.field_1904.method_1434())
                && System.currentTimeMillis() - this.lastActionTime >= this.delay.getValue()) {
                class_1511 crystalToBreak = this.findCrystalToBreak();
                if (crystalToBreak != null) {
                    this.performBreakAction(crystalToBreak);
                } else {
                    class_239 target = this.mc.field_1765;
                    if (this.swapToCrystal.isEnabled()
                        && !(this.blockOnShield.isEnabled() && this.holdingShield())
                        && this.mc.field_1690.field_1904.method_1434()
                        && target instanceof class_3965 hit
                        && this.mc.field_1687.method_8320(hit.method_17777()).method_26204() == class_2246.field_10540
                        && !(this.mc.field_1724.method_6047().method_7909() instanceof class_1774)) {
                        int crystalSlot = this.findCrystalInHotbar();
                        if (crystalSlot != -1) {
                            this.mc.field_1724.method_31548().method_61496(crystalSlot);
                        }
                    }

                    if (this.mc.field_1724.method_6047().method_7909() instanceof class_1774
                        && target instanceof class_3965 hitx
                        && this.mc.field_1724.method_5707(hitx.method_17777().method_46558()) <= 20.25
                        && this.isValidCrystalBase(this.mc.field_1687.method_8320(hitx.method_17777()).method_26204())
                        && this.mc.field_1687.method_8320(hitx.method_17777().method_10084()).method_26215()) {
                        this.performPlaceAction(hitx);
                    }
                }
            }
        }
    }

    private boolean holdingShield() {
        return this.mc.field_1724.method_6047().method_7909() == class_1802.field_8255
                || this.mc.field_1724.method_6079().method_7909() == class_1802.field_8255;
    }

    private static boolean isSword(class_1799 stack) {
        if (stack == null || stack.method_7960()) return false;
        return stack.method_7909().toString().toLowerCase().contains("sword");
    }

    private void performBreakAction(class_1511 crystal) {
        if (this.simClick.isEnabled()) {
            KeyBindingAccessor attackKey = (KeyBindingAccessor)this.mc.field_1690.field_1886;
            attackKey.setTimesPressed(attackKey.getTimesPressed() + 1);
        } else {
            KeyBindingAccessor attackKey = (KeyBindingAccessor)this.mc.field_1690.field_1886;
            attackKey.setTimesPressed(attackKey.getTimesPressed() + 1);
        }

        this.lastActionTime = System.currentTimeMillis();
    }

    private void performPlaceAction(class_3965 hitResult) {
        if (this.simClick.isEnabled()) {
            KeyBindingAccessor useKey = (KeyBindingAccessor)this.mc.field_1690.field_1904;
            useKey.setTimesPressed(useKey.getTimesPressed() + 1);
        } else {
            KeyBindingAccessor useKey = (KeyBindingAccessor)this.mc.field_1690.field_1904;
            useKey.setTimesPressed(useKey.getTimesPressed() + 1);
        }

        this.lastActionTime = System.currentTimeMillis();
        this.restoreSwordNextTick = true;
    }

    private class_1511 findCrystalToBreak() {
        if (this.simClick.isEnabled()) {
            return this.mc.field_1765 instanceof class_3966 ehr
                    && ehr.method_17782() instanceof class_1511 crystal
                    && this.mc.field_1724.method_5739(crystal) <= 4.5
                ? crystal
                : null;
        } else {
            for (class_1297 entity : this.mc.field_1687.method_18112()) {
                if (entity instanceof class_1511 crystal && this.mc.field_1724.method_6057(entity) && this.mc.field_1724.method_5739(entity) < 4.5) {
                    return crystal;
                }
            }

            return null;
        }
    }

    private int findCrystalInHotbar() {
        for (int i = 0; i < 9; i++) {
            class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
            if (stack.method_7909() instanceof class_1774) {
                return i;
            }
        }

        return -1;
    }

    private boolean isValidCrystalBase(class_2248 block) {
        return block == class_2246.field_10540 || block == class_2246.field_9987;
    }
}
