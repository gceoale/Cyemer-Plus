package com.slither.cyemer.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_312;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Environment(EnvType.CLIENT)
@Mixin({class_312.class})
public abstract class MouseMixin {
    @Shadow
    @Final
    private class_310 field_1779;
    @Shadow
    private double field_1789;
    @Shadow
    private double field_1787;
}
