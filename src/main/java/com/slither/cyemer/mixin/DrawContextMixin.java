package com.slither.cyemer.mixin;

import com.slither.cyemer.module.implementation.CustomFont;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_2960;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_5250;
import net.minecraft.class_11719.class_11721;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({class_332.class})
public class DrawContextMixin {
    private static final ThreadLocal<Boolean> CYEMER_GLOBAL_FONT_REENTRY = ThreadLocal.withInitial(() -> false);

    @Inject(
        method = {"method_51433(Lnet/minecraft/class_327;Ljava/lang/String;IIIZ)V"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void cyemer$applyGlobalFontToString(class_327 textRenderer, String text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
        if (!CYEMER_GLOBAL_FONT_REENTRY.get() && CustomFont.isGlobalFontEnabled() && text != null) {
            class_2960 fontId = CustomFont.getSelectedFontId();
            if (fontId != null) {
                class_2561 styled = class_2561.method_43470(text).method_10862(class_2583.field_24360.method_27704(new class_11721(fontId)));
                CYEMER_GLOBAL_FONT_REENTRY.set(true);

                try {
                    ((class_332)(Object)this).method_51439(textRenderer, styled, x, y, color, shadow);
                    ci.cancel();
                } finally {
                    CYEMER_GLOBAL_FONT_REENTRY.set(false);
                }
            }
        }
    }

    @Inject(
        method = {"method_51439(Lnet/minecraft/class_327;Lnet/minecraft/class_2561;IIIZ)V"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void cyemer$applyGlobalFontToText(class_327 textRenderer, class_2561 text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
        if (!CYEMER_GLOBAL_FONT_REENTRY.get() && CustomFont.isGlobalFontEnabled() && text != null) {
            class_2960 fontId = CustomFont.getSelectedFontId();
            if (fontId != null) {
                class_5250 styled = text.method_27661().method_27694(style -> style.method_27704(new class_11721(fontId)));
                CYEMER_GLOBAL_FONT_REENTRY.set(true);

                try {
                    ((class_332)(Object)this).method_51439(textRenderer, styled, x, y, color, shadow);
                    ci.cancel();
                } finally {
                    CYEMER_GLOBAL_FONT_REENTRY.set(false);
                }
            }
        }
    }
}
