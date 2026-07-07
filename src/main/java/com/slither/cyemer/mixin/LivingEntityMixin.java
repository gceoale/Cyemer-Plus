package com.slither.cyemer.mixin;

import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.ModuleAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1268;
import net.minecraft.class_1309;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({class_1309.class})
public abstract class LivingEntityMixin {
    @Shadow
    protected boolean field_6252;
    @Shadow
    protected int field_6279;

    @Inject(
        method = {"method_6028()I"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void modifySwingDuration(CallbackInfoReturnable<Integer> info) {
        Module module = ModuleAccess.getModule("HitAnimations");
        if (module != null && module.isEnabled()) {
            int defaultDuration = 6;
            double speed = ModuleAccess.invokeDouble(module, "getSwingSpeed", 1.0, null);
            if (speed <= 0.1) {
                speed = 0.1;
            }

            int newDuration = (int)(defaultDuration / speed);
            info.setReturnValue(newDuration);
        }
    }

    @Inject(
        method = {"method_23667(Lnet/minecraft/class_1268;Z)V"},
        at = {@At("HEAD")}
    )
    private void allowReSwing(class_1268 hand, boolean fromServer, CallbackInfo ci) {
        Module module = ModuleAccess.getModule("HitAnimations");
        if (module != null && module.isEnabled()) {
            this.field_6252 = false;
            this.field_6279 = -1;
        }
    }
}
