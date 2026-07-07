package com.slither.cyemer.module.implementation;

import com.slither.cyemer.mixin.MinecraftClientAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1747;
import net.minecraft.class_1771;
import net.minecraft.class_1777;
import net.minecraft.class_1779;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1823;
import net.minecraft.class_2246;
import net.minecraft.class_4537;

@Environment(EnvType.CLIENT)
public class FastPlace extends Module {
    private final ModeSetting filter = new ModeSetting("Filter", "All", "Blocks", "EXP Bottles", "Projectiles");
    private final SliderSetting cooldown = new SliderSetting("Cooldown", 0.0, 0.0, 4.0, 0);
    private final BooleanSetting noAnchor = new BooleanSetting("No Anchor", false);
    private final BooleanSetting noGlowstone = new BooleanSetting("No Glowstone", false);

    public FastPlace() {
        super("FastPlace", "Removes the delay between placing blocks.", Category.PLAYER);
        this.addSetting(this.filter);
        this.addSetting(this.cooldown);
        this.addSetting(this.noAnchor);
        this.addSetting(this.noGlowstone);
    }

    @Override
    public void onTick() {
        if (this.mc != null && this.mc.field_1724 != null) {
            if (!this.isBlockedItem(this.mc.field_1724.method_6047()) && !this.isBlockedItem(this.mc.field_1724.method_6079())) {
                boolean mainHandValid = this.isItemValid(this.mc.field_1724.method_6047());
                boolean offHandValid = this.isItemValid(this.mc.field_1724.method_6079());
                if (mainHandValid || offHandValid) {
                    MinecraftClientAccessor accessor = (MinecraftClientAccessor)this.mc;
                    int desiredCooldown = (int)this.cooldown.getValue();
                    if (accessor.getItemUseCooldown() > desiredCooldown) {
                        accessor.setItemUseCooldown(desiredCooldown);
                    }
                }
            }
        }
    }

    private boolean isItemValid(class_1799 stack) {
        if (stack.method_7960()) {
            return false;
        } else {
            class_1792 item = stack.method_7909();
            String currentMode = this.filter.getCurrentMode();
            if (item instanceof class_1747 blockItem) {
                if (this.noAnchor.isEnabled() && blockItem.method_7711() == class_2246.field_23152) {
                    return false;
                }

                if (this.noGlowstone.isEnabled() && blockItem.method_7711() == class_2246.field_10171) {
                    return false;
                }
            }

            switch (currentMode) {
                case "All":
                    return true;
                case "Blocks":
                    return item instanceof class_1747;
                case "EXP Bottles":
                    return item instanceof class_1779;
                case "Projectiles":
                    return item instanceof class_1779
                        || item instanceof class_1823
                        || item instanceof class_1771
                        || item instanceof class_4537
                        || item instanceof class_1777;
                default:
                    return false;
            }
        }
    }

    private boolean isBlockedItem(class_1799 stack) {
        if (stack.method_7960()) {
            return false;
        } else {
            return !(stack.method_7909() instanceof class_1747 blockItem)
                ? false
                : this.noAnchor.isEnabled() && blockItem.method_7711() == class_2246.field_23152
                    || this.noGlowstone.isEnabled() && blockItem.method_7711() == class_2246.field_10171;
        }
    }

    @Override
    public void onDisable() {
        if (this.mc != null) {
            ((MinecraftClientAccessor)this.mc).setItemUseCooldown(4);
        }
    }
}
