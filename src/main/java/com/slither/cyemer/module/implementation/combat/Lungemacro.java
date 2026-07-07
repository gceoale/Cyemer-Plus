package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.mixin.MinecraftClientAccessor;
import com.slither.cyemer.mixin.PlayerInventoryAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1887;
import net.minecraft.class_6880;
import net.minecraft.class_9304;
import net.minecraft.class_9334;

@Environment(EnvType.CLIENT)
public class Lungemacro extends Module {
    private final BooleanSetting swapBack = new BooleanSetting("Swap Back", true);
    private final BooleanSetting randomization = new BooleanSetting("Randomization", false);
    private final SliderSetting minRandom = new SliderSetting("Min Random", 0.0, 0.0, 10.0, 1);
    private final SliderSetting maxRandom = new SliderSetting("Max Random", 1.0, 0.0, 10.0, 1);
    private boolean isSwapping = false;
    private int originalSlot = -1;
    private int swapTicks = 0;
    private int targetTicks = 3;

    public Lungemacro() {
        super("Lungemacro", "Automatically swaps to lunge spear and attacks", Category.COMBAT);
        this.addSetting(this.swapBack);
        this.addSetting(this.randomization);
        this.addSetting(this.minRandom);
        this.addSetting(this.maxRandom);
    }

    @Override
    public void onEnable() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            this.originalSlot = ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).getSelectedSlot();
            this.targetTicks = 3;
            if (this.randomization.isEnabled()) {
                this.targetTicks = this.targetTicks + (int)(Math.random() * 2.0);
            }

            int lungeSlot = this.findLungeSlot();
            if (lungeSlot == -1) {
                this.toggle();
            } else {
                this.isSwapping = true;
                this.swapTicks = 0;
                this.equip(lungeSlot);
                if (this.mc.field_1724.method_7261(0.0F) >= 1.0F) {
                    ((MinecraftClientAccessor)this.mc).attack();
                    this.swapTicks = 1;
                }
            }
        } else {
            this.toggle();
        }
    }

    @Override
    public void onDisable() {
        this.isSwapping = false;
        this.originalSlot = -1;
        this.swapTicks = 0;
    }

    @Override
    public void onTick() {
        if (this.isSwapping && this.mc.field_1724 != null) {
            this.swapTicks++;
            if (this.swapTicks == 0) {
                if (this.mc.field_1724.method_7261(0.0F) >= 1.0F) {
                    ((MinecraftClientAccessor)this.mc).attack();
                }

                int lungeSlot = this.findLungeSlot();
                if (lungeSlot != -1) {
                    this.equip(lungeSlot);
                    this.swapTicks = 1;
                } else {
                    this.toggle();
                }
            } else {
                if (this.swapTicks >= this.targetTicks) {
                    if (this.swapBack.isEnabled() && this.originalSlot != -1) {
                        this.equip(this.originalSlot);
                    }

                    this.toggle();
                }
            }
        }
    }

    private int findLungeSlot() {
        for (int i = 0; i < 9; i++) {
            class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
            if (this.isLungeSpear(stack)) {
                return i;
            }
        }

        return -1;
    }

    private boolean isLungeSpear(class_1799 stack) {
        if (stack.method_7960()) {
            return false;
        } else {
            boolean isSpear = stack.method_31574(class_1802.field_8547) || stack.method_7964().getString().toLowerCase().contains("spear");
            if (!isSpear) {
                return false;
            } else {
                class_9304 enchantments = (class_9304)stack.method_58694(class_9334.field_49633);
                if (enchantments == null) {
                    return false;
                } else {
                    for (Entry<class_6880<class_1887>> entry : enchantments.method_57539()) {
                        class_6880<class_1887> enchantment = (class_6880<class_1887>)entry.getKey();
                        String id = enchantment.method_40230().map(k -> k.method_29177().method_12832()).orElse("").toLowerCase();
                        String name = ((class_1887)enchantment.comp_349()).comp_2686().getString().toLowerCase();
                        if (id.contains("lunge") || name.contains("lunge")) {
                            return true;
                        }
                    }

                    return false;
                }
            }
        }
    }

    private void equip(int slot) {
        ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).setSelectedSlot(slot);
    }
}
