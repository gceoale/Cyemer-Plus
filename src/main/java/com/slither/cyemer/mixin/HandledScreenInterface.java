package com.slither.cyemer.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1735;
import net.minecraft.class_465;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin({class_465.class})
public interface HandledScreenInterface {
    @Accessor("field_2787")
    class_1735 getFocusedSlot();
}
