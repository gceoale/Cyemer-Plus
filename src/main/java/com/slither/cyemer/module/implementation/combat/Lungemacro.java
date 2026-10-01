package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.mixin.ClientPlayerInteractionManagerMixin;
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
import net.minecraft.class_3489;
import net.minecraft.class_3675;
import net.minecraft.class_6880;
import net.minecraft.class_746;
import net.minecraft.class_9304;
import net.minecraft.class_9334;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class Lungemacro extends Module {
    private final BooleanSetting holdMode = new BooleanSetting("Hold To Lunge", true);
    private final BooleanSetting instantSwap = new BooleanSetting("Instant Swap", true);
    private final BooleanSetting swapBack = new BooleanSetting("Swap Back", true);
    private final BooleanSetting randomization = new BooleanSetting("Randomization", false);
    private final SliderSetting minRandom = new SliderSetting("Min Random", 0.0, 0.0, 10.0, 1);
    private final SliderSetting maxRandom = new SliderSetting("Max Random", 1.0, 0.0, 10.0, 1);
    private boolean isSwapping = false;
    private boolean holding = false;
    private int originalSlot = -1;
    private int swapTicks = 0;
    private int targetTicks = 3;
    private int waitTicks = 0;

    public Lungemacro() {
        super("Lungemacro", "Swaps to your lunge spear and jabs. Hold the bind to keep lunging", Category.COMBAT);
        this.addSetting(this.holdMode);
        this.addSetting(this.instantSwap);
        this.addSetting(this.swapBack);
        this.addSetting(this.randomization);
        this.addSetting(this.minRandom);
        this.addSetting(this.maxRandom);
    }

    @Override
    public void onEnable() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            this.originalSlot = ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).getSelectedSlot();
            if (this.holdMode.isEnabled()) {
                if (this.findLungeSlot() == -1) {
                    this.toggle();
                } else {
                    this.holding = true;
                    this.waitTicks = 0;
                    // jab on the press itself so a quick tap still lunges once
                    this.tryJab();
                }

                return;
            }

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
        // without Instant Swap the spear stays out while held, put the old slot back on release
        if (this.holding
            && !this.instantSwap.isEnabled()
            && this.swapBack.isEnabled()
            && this.originalSlot != -1
            && this.mc.field_1724 != null
            && this.mc.field_1724.method_31548().method_67532() != this.originalSlot) {
            this.equip(this.originalSlot);
        }

        this.holding = false;
        this.isSwapping = false;
        this.originalSlot = -1;
        this.swapTicks = 0;
        this.waitTicks = 0;
    }

    @Override
    public void onTick() {
        if (this.holding) {
            if (this.mc.field_1724 == null || !this.isKeyHeld()) {
                this.toggle();
            } else if (this.waitTicks > 0) {
                this.waitTicks--;
            } else {
                this.tryJab();
            }

            return;
        }

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

    private void tryJab() {
        class_746 player = this.mc.field_1724;
        // lunge doesn't fire while riding, gliding or in water, no point burning the charge on a plain jab
        if (player.method_5765() || player.method_6128() || player.method_5799()) {
            return;
        }

        int lungeSlot = this.findLungeSlot();
        if (lungeSlot == -1) {
            return;
        }

        // spears need a full charge to jab, this is the same gate vanilla and the server use
        if (player.method_75202(player.method_31548().method_5438(lungeSlot), 0)) {
            return;
        }

        int slot = player.method_31548().method_67532();
        boolean swapped = slot != lungeSlot;
        if (swapped) {
            this.equip(lungeSlot);
        }

        ((MinecraftClientAccessor)this.mc).attack();
        if (swapped && this.instantSwap.isEnabled() && this.swapBack.isEnabled()) {
            this.equip(slot);
            // flush the swap back now so the server never ticks with the spear in hand
            ((ClientPlayerInteractionManagerMixin.ClientPlayerInteractionManagerAccessor)this.mc.field_1761).invokeSyncSelectedSlot();
        }

        this.waitTicks = this.randomDelay();
    }

    private int randomDelay() {
        if (!this.randomization.isEnabled()) {
            return 0;
        }

        double min = Math.min(this.minRandom.getValue(), this.maxRandom.getValue());
        double max = Math.max(this.minRandom.getValue(), this.maxRandom.getValue());
        return (int)Math.round(min + Math.random() * (max - min));
    }

    private boolean isKeyHeld() {
        int key = this.getKeyCode();
        if (key == -1) {
            return false;
        } else {
            return key < 0
                ? GLFW.glfwGetMouseButton(this.mc.method_22683().method_4490(), key + 100) == 1
                : class_3675.method_15987(this.mc.method_22683(), key);
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
            boolean isSpear = stack.method_31573(class_3489.field_63257)
                || stack.method_31574(class_1802.field_8547)
                || stack.method_7964().getString().toLowerCase().contains("spear");
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
