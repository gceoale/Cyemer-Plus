package com.slither.cyemer.module.implementation;

import com.slither.cyemer.mixin.PlayerInventoryAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.AttackValidator;
import com.slither.cyemer.util.ModuleRandomDelay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1743;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1887;
import net.minecraft.class_1890;
import net.minecraft.class_1893;
import net.minecraft.class_239;
import net.minecraft.class_3966;
import net.minecraft.class_5321;
import net.minecraft.class_6880;
import net.minecraft.class_7924;
import net.minecraft.class_239.class_240;

@Environment(EnvType.CLIENT)
public class MaceSwap extends Module {
    private final BooleanSetting notOnAxe = new BooleanSetting("Not on Axe (stunslam)", true);
    private final BooleanSetting weaponOnly = new BooleanSetting("Weapon Only", true);
    private final BooleanSetting randomization = new BooleanSetting("Randomization", false);
    private final SliderSetting densityHeight = new SliderSetting("Density Height", 3.0, 0.0, 10.0, 1);
    private final SliderSetting randomMinDelay = new SliderSetting("Random Min (ms)", 0.0, 0.0, 500.0, 0);
    private final SliderSetting randomMaxDelay = new SliderSetting("Random Max (ms)", 25.0, 0.0, 500.0, 0);
    private boolean isSwapping = false;
    private int originalSlot = -1;
    private int swapTicks = 0;
    private double highestY = 0.0;
    private boolean wasOnGround = true;

    public MaceSwap() {
        super("MaceSwap", "Automatically swaps to mace and attacks", Category.COMBAT);
        this.addSetting(this.notOnAxe);
        this.addSetting(this.weaponOnly);
        this.addSetting(this.densityHeight);
        this.addSetting(this.randomization);
        this.addSetting(this.randomMinDelay);
        this.addSetting(this.randomMaxDelay);
    }

    @Override
    public void onEnable() {
        if (this.mc.field_1724 != null) {
            this.highestY = this.mc.field_1724.method_23318();
            this.wasOnGround = this.mc.field_1724.method_24828();
        }
    }

    @Override
    public void onDisable() {
        this.resetState();
        this.wasOnGround = true;
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null) {
            boolean isOnGroundNow = this.mc.field_1724.method_24828();
            if (isOnGroundNow) {
                this.highestY = this.mc.field_1724.method_23318();
            } else {
                this.highestY = Math.max(this.highestY, this.mc.field_1724.method_23318());
            }

            this.wasOnGround = isOnGroundNow;
            if (this.isSwapping) {
                this.swapTicks++;
                if (this.swapTicks == 1) {
                    if (this.randomization.isEnabled()
                        && !ModuleRandomDelay.gateAction("combat.attack.maceswap", this.getRandomMinDelay(), this.getRandomMaxDelay())) {
                        return;
                    }

                    boolean success = AttackValidator.tryAttack(this.mc, "combat.attack.maceswap");
                    if (!success) {
                        ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).setSelectedSlot(this.originalSlot);
                        this.resetState();
                        return;
                    }
                }

                if (this.swapTicks >= 3) {
                    ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).setSelectedSlot(this.originalSlot);
                    this.resetState();
                }
            }
        }
    }

    public boolean handleAttack() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null && !this.isSwapping) {
            class_1799 mainHandStack = this.mc.field_1724.method_6047();
            if (this.weaponOnly.isEnabled() && !this.isWeapon(mainHandStack)) {
                return false;
            } else if (this.notOnAxe.isEnabled() && mainHandStack.method_7909() instanceof class_1743) {
                return false;
            } else {
                class_239 hitResult = this.mc.field_1765;
                if (hitResult != null && hitResult.method_17783() == class_240.field_1331) {
                    class_3966 entityHit = (class_3966)hitResult;
                    class_1297 target = entityHit.method_17782();
                    if (!(target instanceof class_1309) || !target.method_5805() || target == this.mc.field_1724) {
                        return false;
                    } else if (mainHandStack.method_7909() == class_1802.field_49814) {
                        return false;
                    } else {
                        double manualFallDist = Math.max(0.0, this.highestY - this.mc.field_1724.method_23318());
                        double densityThreshold = this.densityHeight.getValue();
                        int maceSlot;
                        if (manualFallDist > densityThreshold) {
                            maceSlot = this.findMaceByEnchantment(class_1893.field_50157);
                            if (maceSlot == -1) {
                                maceSlot = this.findMaceInHotbar();
                            }
                        } else {
                            maceSlot = this.findMaceByEnchantment(class_1893.field_50158);
                            if (maceSlot == -1) {
                                return false;
                            }
                        }

                        if (maceSlot == -1) {
                            return false;
                        } else {
                            this.isSwapping = true;
                            this.swapTicks = 0;
                            this.originalSlot = ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).getSelectedSlot();
                            ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).setSelectedSlot(maceSlot);
                            return true;
                        }
                    }
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    private boolean isWeapon(class_1799 stack) {
        return stack.method_7960()
            ? false
            : stack.method_7909() instanceof class_1743
                || stack.method_7909() == class_1802.field_49814
                || stack.method_7909() == class_1802.field_8547
                || stack.method_7909().toString().contains("sword");
    }

    private int findMaceInHotbar() {
        for (int i = 0; i < 9; i++) {
            class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
            if (stack.method_7909() == class_1802.field_49814) {
                return i;
            }
        }

        return -1;
    }

    private int findMaceByEnchantment(class_5321<class_1887> enchantmentKey) {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            class_6880<class_1887> enchantmentEntry = this.mc.field_1687.method_30349().method_30530(class_7924.field_41265).method_46747(enchantmentKey);

            for (int i = 0; i < 9; i++) {
                class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
                if (stack.method_7909() == class_1802.field_49814 && class_1890.method_8225(enchantmentEntry, stack) > 0) {
                    return i;
                }
            }

            return -1;
        } else {
            return -1;
        }
    }

    private void resetState() {
        this.isSwapping = false;
        this.originalSlot = -1;
        this.swapTicks = 0;
    }

    private long getRandomMinDelay() {
        return (long)this.randomMinDelay.getValue();
    }

    private long getRandomMaxDelay() {
        return (long)this.randomMaxDelay.getValue();
    }
}
