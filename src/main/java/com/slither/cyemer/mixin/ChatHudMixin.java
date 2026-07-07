package com.slither.cyemer.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.slither.cyemer.Cyemer;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.ModuleAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_338;
import net.minecraft.class_6903;
import net.minecraft.class_8824;
import net.minecraft.class_7225.class_7874;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({class_338.class})
public class ChatHudMixin {
    @ModifyVariable(
        method = {"method_44811(Lnet/minecraft/class_2561;Lnet/minecraft/class_7469;Lnet/minecraft/class_7591;)V"},
        at = @At("HEAD"),
        argsOnly = true
    )
    private class_2561 onAddMessage(class_2561 message) {
        Module nickModule = Cyemer.getInstance().getModuleManager().getModule("Nick");
        class_310 mc = class_310.method_1551();
        if (nickModule != null && nickModule.isEnabled() && mc.field_1724 != null && mc.field_1687 != null) {
            String originalName = mc.field_1724.method_5477().getString();
            String nickname = ModuleAccess.invokeString(nickModule, "getSafeNickname", originalName, new Class[]{String.class}, originalName);
            if (originalName.equals(nickname)) {
                return message;
            } else {
                try {
                    class_7874 wrapperLookup = mc.field_1687.method_30349();
                    class_6903<JsonElement> ops = class_6903.method_46632(JsonOps.INSTANCE, wrapperLookup);
                    JsonElement encoded = (JsonElement)class_8824.field_46597.encodeStart(ops, message).result().orElse(null);
                    if (encoded == null) {
                        return message;
                    } else {
                        String messageJson = encoded.toString();
                        String modifiedJson = messageJson.replace(originalName, nickname);
                        JsonElement modifiedElement = JsonParser.parseString(modifiedJson);
                        class_2561 modifiedText = (class_2561)class_8824.field_46597.parse(ops, modifiedElement).result().orElse(null);
                        return modifiedText != null ? modifiedText : message;
                    }
                } catch (Exception var13) {
                    return message;
                }
            }
        } else {
            return message;
        }
    }

    @Inject(
        method = {"method_75804(Lnet/minecraft/class_332;Lnet/minecraft/class_327;IIIZZ)V"},
        at = {@At("HEAD")},
        cancellable = true,
        require = 0
    )
    private void onRenderChat(CallbackInfo ci) {
        if (ModuleAccess.isEnabled("StreamerMode")) {
            ci.cancel();
        }
    }
}
