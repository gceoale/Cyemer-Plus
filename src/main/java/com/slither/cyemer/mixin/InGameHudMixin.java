package com.slither.cyemer.mixin;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.ModuleAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_266;
import net.minecraft.class_329;
import net.minecraft.class_332;
import net.minecraft.class_9779;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({class_329.class})
public class InGameHudMixin {
    @Inject(
        method = {"method_1753(Lnet/minecraft/class_332;Lnet/minecraft/class_9779;)V"},
        at = {@At("TAIL")}
    )
    private void onRender(class_332 context, class_9779 tickCounter, CallbackInfo ci) {
        if (!Cyemer.selfDestructed) {
            float tickDelta = tickCounter.method_60637(true);
            if (Cyemer.INSTANCE != null && Cyemer.INSTANCE.getModuleManager() != null) {
                Cyemer.INSTANCE.getModuleManager().onRender(context, tickDelta);
            }
        }
    }

    @Inject(
        method = {"method_1757(Lnet/minecraft/class_332;Lnet/minecraft/class_266;)V"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void onRenderScoreboardSidebar(class_332 context, class_266 objective, CallbackInfo ci) {
        if (ModuleAccess.isEnabled("StreamerMode")) {
            ci.cancel();
        }
    }

    @Inject(
        method = {"method_1759(Lnet/minecraft/class_332;Lnet/minecraft/class_9779;)V"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void onRenderHotbar(class_332 context, class_9779 tickCounter, CallbackInfo ci) {
        Module interfaceModule = ModuleAccess.getModule("Interface");
        boolean cancelVanilla = ModuleAccess.readBooleanField(interfaceModule, "shouldCancelVanillaHotbar", false);
        if (interfaceModule != null && cancelVanilla) {
            ci.cancel();
        }
    }
}
