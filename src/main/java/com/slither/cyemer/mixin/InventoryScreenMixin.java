package com.slither.cyemer.mixin;

import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.ModuleAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;
import net.minecraft.class_490;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({class_490.class})
public class InventoryScreenMixin {
    @Inject(
        method = {"method_2389(Lnet/minecraft/class_332;FII)V"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void onDrawBackground(class_332 context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        Module interfaceModule = ModuleAccess.getModule("Interface");
        boolean cancelVanilla = ModuleAccess.readBooleanField(interfaceModule, "shouldCancelVanillaInventory", false);
        if (interfaceModule != null && cancelVanilla) {
            ci.cancel();
        }
    }

    @Inject(
        method = {"method_25394(Lnet/minecraft/class_332;IIF)V"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void onRender(class_332 context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        class_490 self = (class_490)(Object)this;
        Module interfaceModule = ModuleAccess.getModule("Interface");
        boolean cancelVanilla = ModuleAccess.readBooleanField(interfaceModule, "shouldCancelVanillaInventory", false);
        if (interfaceModule != null && cancelVanilla) {
            ModuleAccess.invoke(
                interfaceModule,
                "renderInventory",
                new Class[]{class_332.class, int.class, int.class, float.class, class_490.class},
                context,
                mouseX,
                mouseY,
                delta,
                self
            );
            ci.cancel();
        }
    }
}
