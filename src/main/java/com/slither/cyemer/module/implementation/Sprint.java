package com.slither.cyemer.module.implementation;

import com.slither.cyemer.mixin.MinecraftClientAccessor;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Sprint extends Module {
    public Sprint() {
        super("AutoSprint", "Automatically sprints", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1690 != null) {
            boolean canSprint = this.mc.field_1724.method_7344().method_7586() > 6;
            boolean usingOrPlacing = this.mc.field_1724.method_6115()
                || this.mc.field_1690.field_1904.method_1434()
                || ((MinecraftClientAccessor)this.mc).getItemUseCooldown() > 0;
            boolean shouldSprint = this.mc.field_1724.field_3913 != null
                && this.mc.field_1724.field_3913.method_20622()
                && !this.mc.field_1724.method_5715()
                && !usingOrPlacing
                && canSprint;
            this.mc.field_1690.field_1867.method_23481(shouldSprint);
        }
    }

    @Override
    public void onDisable() {
        if (this.mc.field_1690 != null) {
            this.mc.field_1690.field_1867.method_23481(false);
        }
    }
}
