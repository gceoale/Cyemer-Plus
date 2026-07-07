package com.slither.cyemer.mixin;

import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.implementation.CustomCapeModule;
import com.slither.cyemer.util.CapeTextureManager;
import com.slither.cyemer.util.GameProfileCompat;
import com.slither.cyemer.util.ModuleAccess;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_640;
import net.minecraft.class_742;
import net.minecraft.class_8685;
import net.minecraft.class_12079.class_10726;
import net.minecraft.class_8685.class_11892;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({class_742.class})
public abstract class AbstractClientPlayerEntityMixin {
    @Shadow
    protected abstract class_640 method_3123();

    @Inject(
        method = {"method_52814()Lnet/minecraft/class_8685;"},
        at = {@At("RETURN")},
        cancellable = true,
        require = 0
    )
    private void onGetSkin(CallbackInfoReturnable<class_8685> cir) {
        class_310 client = class_310.method_1551();
        if (client.field_1724 != null) {
            Module customCape = ModuleAccess.getModule("CustomCape");
            if (customCape != null && customCape.isEnabled()) {
                class_640 entry = this.method_3123();
                if (entry != null && client.field_1724.method_5667().equals(GameProfileCompat.getId(entry.method_2966()))) {
                    CustomCapeModule module = CustomCapeModule.INSTANCE;
                    if (module != null) {
                        class_8685 current = (class_8685)cir.getReturnValue();
                        if (current != null) {
                            class_10726 capeInfo = CapeTextureManager.getCapeAssetInfo(module.getSelectedCapeName());
                            if (capeInfo != null) {
                                class_11892 skinOverride = class_11892.method_74885(Optional.empty(), Optional.of(capeInfo), Optional.empty(), Optional.empty());
                                cir.setReturnValue(current.method_74883(skinOverride));
                            }
                        }
                    }
                }
            }
        }
    }
}
