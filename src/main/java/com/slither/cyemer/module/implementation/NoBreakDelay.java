package com.slither.cyemer.module.implementation;

import com.slither.cyemer.mixin.ClientPlayerInteractionManagerMixin;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class NoBreakDelay extends Module {
    public NoBreakDelay() {
        super("NoBreakDelay", "Removes the delay between breaking blocks.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1761 != null) {
            if (this.mc.field_1761 instanceof ClientPlayerInteractionManagerMixin.ClientPlayerInteractionManagerAccessor accessor) {
                accessor.setBlockBreakingCooldown(0);
            }
        }
    }
}
