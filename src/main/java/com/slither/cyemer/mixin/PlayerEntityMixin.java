package com.slither.cyemer.mixin;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.ModuleAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1657;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({class_1657.class})
public class PlayerEntityMixin {
    @Inject(
        method = {"method_5476()Lnet/minecraft/class_2561;"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void onGetDisplayName(CallbackInfoReturnable<class_2561> cir) {
        class_1657 self = (class_1657)(Object)this;
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 != null && mc.field_1724.equals(self)) {
            Module nickModule = Cyemer.getInstance().getModuleManager().getModule("Nick");
            if (nickModule != null && nickModule.isEnabled()) {
                String originalName = self.method_5477().getString();
                String finalName = ModuleAccess.invokeString(nickModule, "getSafeNickname", originalName, new Class[]{String.class}, originalName);
                cir.setReturnValue(class_2561.method_30163(finalName));
            }
        }
    }
}
