package com.slither.cyemer.mixin;

import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.ModuleAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2535;
import net.minecraft.class_2596;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({class_2535.class})
public class ClientConnectionMixin {
    @Inject(
        method = {"method_10743(Lnet/minecraft/class_2596;)V"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void onSendPacket(class_2596<?> packet, CallbackInfo ci) {
        class_310 client = class_310.method_1551();
        if (client != null && client.field_1724 != null && client.field_1687 != null) {
            Module blink = ModuleAccess.getModule("Blink");
            if (blink != null && blink.isEnabled() && ModuleAccess.invokeBoolean(blink, "handleOutgoingPacket", false, new Class[]{class_2596.class}, packet)) {
                ci.cancel();
            } else {
                Module fakelag = ModuleAccess.getModule("Fakelag");
                if (fakelag != null
                    && fakelag.isEnabled()
                    && ModuleAccess.invokeBoolean(fakelag, "handleOutgoingPacket", false, new Class[]{class_2596.class}, packet)) {
                    ci.cancel();
                }
            }
        }
    }
}
