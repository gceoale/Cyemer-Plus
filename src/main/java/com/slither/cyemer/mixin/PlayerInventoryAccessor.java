package com.slither.cyemer.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1661;
import net.minecraft.class_1799;
import net.minecraft.class_2371;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin({class_1661.class})
public interface PlayerInventoryAccessor {
    @Accessor("field_7545")
    int getSelectedSlot();

    @Accessor("field_7545")
    void setSelectedSlot(int var1);

    @Accessor("field_7547")
    class_2371<class_1799> getMain();
}
