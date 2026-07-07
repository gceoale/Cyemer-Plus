package com.slither.cyemer.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_304;
import net.minecraft.class_3675.class_306;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin({class_304.class})
public interface KeyBindingAccessor {
    @Accessor("field_1661")
    int getTimesPressed();

    @Accessor("field_1661")
    void setTimesPressed(int var1);

    @Accessor("field_1655")
    class_306 getBoundKey();
}
