package com.slither.cyemer.mixin;

import com.mojang.authlib.GameProfile;
import com.slither.cyemer.Cyemer;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.implementation.CustomCapeModule;
import com.slither.cyemer.util.CapeTextureManager;
import com.slither.cyemer.util.GameProfileCompat;
import com.slither.cyemer.util.ModuleAccess;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_640;
import net.minecraft.class_8685;
import net.minecraft.class_12079.class_10726;
import net.minecraft.class_8685.class_11892;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({class_640.class})
public class PlayerListEntryMixin {
    @Shadow
    @Final
    private GameProfile field_3741;

    @Inject(
        method = {"method_2971()Lnet/minecraft/class_2561;"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void onGetDisplayName(CallbackInfoReturnable<class_2561> cir) {
        Module nickModule = Cyemer.getInstance().getModuleManager().getModule("Nick");
        if (nickModule != null && nickModule.isEnabled() && class_310.method_1551().field_1724 != null) {
            if (class_310.method_1551().field_1724 != null && class_310.method_1551().field_1724.method_5667().equals(GameProfileCompat.getId(this.field_3741))
                )
             {
                String originalName = GameProfileCompat.getName(this.field_3741);
                if (originalName == null) {
                    originalName = "";
                }

                String nickname = ModuleAccess.invokeString(nickModule, "getSafeNickname", originalName, new Class[]{String.class}, originalName);
                cir.setReturnValue(class_2561.method_30163(nickname));
            }
        }
    }

    @Inject(
        method = {"method_52810()Lnet/minecraft/class_8685;"},
        at = {@At("RETURN")},
        cancellable = true,
        require = 0
    )
    private void onGetSkinTextures(CallbackInfoReturnable<class_8685> cir) {
        class_310 client = class_310.method_1551();
        if (client.field_1724 != null) {
            Module customCape = Cyemer.getInstance().getModuleManager().getModule("CustomCape");
            if (customCape != null && customCape.isEnabled()) {
                if (client.field_1724.method_5667().equals(GameProfileCompat.getId(this.field_3741))) {
                    class_8685 current = (class_8685)cir.getReturnValue();
                    if (current != null) {
                        CustomCapeModule module = CustomCapeModule.INSTANCE;
                        if (module != null) {
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
