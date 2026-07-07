package com.slither.cyemer.mixin;

import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.ModuleAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11908;
import net.minecraft.class_1735;
import net.minecraft.class_332;
import net.minecraft.class_465;
import net.minecraft.class_490;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({class_465.class})
public class HandledScreenMixin {
    @Inject(
        method = {"method_2385(Lnet/minecraft/class_332;Lnet/minecraft/class_1735;II)V"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void onDrawSlot(class_332 context, class_1735 slot, int x, int y, CallbackInfo ci) {
        class_465<?> screen = (class_465<?>)(Object)this;
        Module interfaceModule = ModuleAccess.getModule("Interface");
        boolean cancelVanilla = ModuleAccess.readBooleanField(interfaceModule, "shouldCancelVanillaInventory", false);
        if (screen instanceof class_490 && interfaceModule != null && cancelVanilla) {
            ci.cancel();
        }
    }

    @Inject(
        method = {"method_64241(Lnet/minecraft/class_332;)V"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void onDrawSlotHighlightBack(class_332 context, CallbackInfo ci) {
        class_465<?> screen = (class_465<?>)(Object)this;
        Module interfaceModule = ModuleAccess.getModule("Interface");
        boolean cancelVanilla = ModuleAccess.readBooleanField(interfaceModule, "shouldCancelVanillaInventory", false);
        if (screen instanceof class_490 && interfaceModule != null && cancelVanilla) {
            ci.cancel();
        }
    }

    @Inject(
        method = {"method_64242(Lnet/minecraft/class_332;)V"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void onDrawSlotHighlightFront(class_332 context, CallbackInfo ci) {
        class_465<?> screen = (class_465<?>)(Object)this;
        Module interfaceModule = ModuleAccess.getModule("Interface");
        boolean cancelVanilla = ModuleAccess.readBooleanField(interfaceModule, "shouldCancelVanillaInventory", false);
        if (screen instanceof class_490 && interfaceModule != null && cancelVanilla) {
            ci.cancel();
        }
    }

    @Inject(
        method = {"method_25404(Lnet/minecraft/class_11908;)Z"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void onKeyPressed(class_11908 keyInput, CallbackInfoReturnable<Boolean> cir) {
        class_465<?> screen = (class_465<?>)(Object)this;
        Module interfaceModule = ModuleAccess.getModule("Interface");
        boolean cancelVanilla = ModuleAccess.readBooleanField(interfaceModule, "shouldCancelVanillaInventory", false);
        if (screen instanceof class_490 && interfaceModule != null && cancelVanilla) {
            boolean handled = ModuleAccess.invokeBoolean(
                interfaceModule, "handleKeyPress", false, new Class[]{class_490.class, class_11908.class}, screen, keyInput
            );
            if (handled) {
                cir.setReturnValue(true);
            }
        }
    }
}
