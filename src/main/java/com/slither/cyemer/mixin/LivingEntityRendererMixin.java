package com.slither.cyemer.mixin;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.ColorSetting;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.ModuleAccess;
import com.slither.cyemer.util.render.types.ShaderRenderLayers;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10042;
import net.minecraft.class_10055;
import net.minecraft.class_1058;
import net.minecraft.class_11659;
import net.minecraft.class_12075;
import net.minecraft.class_12249;
import net.minecraft.class_1921;
import net.minecraft.class_2960;
import net.minecraft.class_3879;
import net.minecraft.class_4587;
import net.minecraft.class_583;
import net.minecraft.class_591;
import net.minecraft.class_922;
import net.minecraft.class_11683.class_11792;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({class_922.class})
public abstract class LivingEntityRendererMixin {
    @Shadow
    protected class_583 field_4737;

    @Redirect(
        method = {"method_4054(Lnet/minecraft/class_10042;Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;Lnet/minecraft/class_12075;)V"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_11659;method_73490(Lnet/minecraft/class_3879;Ljava/lang/Object;Lnet/minecraft/class_4587;Lnet/minecraft/class_1921;IIILnet/minecraft/class_1058;ILnet/minecraft/class_11683$class_11792;)V"
        )
    )
    private void cyemer$maybeSkipBaseModel(
        class_11659 queue,
        class_3879 model,
        Object state,
        class_4587 matrices,
        class_1921 layer,
        int light,
        int overlay,
        int color,
        class_1058 sprite,
        int outlineColor,
        class_11792 crumbling
    ) {
        if (state instanceof class_10055 playerState) {
            Module targetEffect = Cyemer.getInstance().getModuleManager().getModule("TargetEffect");
            boolean hideModel = ModuleAccess.invokeBoolean(targetEffect, "shouldHideModel", false, new Class[]{class_10055.class}, playerState);
            if (targetEffect != null && targetEffect.isEnabled() && hideModel) {
                return;
            }
        }

        queue.method_73490(model, state, matrices, layer, light, overlay, color, sprite, outlineColor, crumbling);
    }

    @Inject(
        method = {"method_4054(Lnet/minecraft/class_10042;Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;Lnet/minecraft/class_12075;)V"},
        at = {@At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_4587;method_22909()V",
            shift = Shift.BEFORE
        )}
    )
    private void cyemer$renderTargetEffectOverlay(class_10042 state, class_4587 matrices, class_11659 queue, class_12075 cameraRenderState, CallbackInfo ci) {
        if (state instanceof class_10055 playerState) {
            if (this.field_4737 instanceof class_591) {
                Module targetEffect = Cyemer.getInstance().getModuleManager().getModule("TargetEffect");
                boolean hideModel = ModuleAccess.invokeBoolean(targetEffect, "shouldHideModel", false, new Class[]{class_10055.class}, playerState);
                if (targetEffect != null && targetEffect.isEnabled() && hideModel) {
                    ModeSetting themeSetting = (ModeSetting)targetEffect.getSetting("Theme");
                    ColorSetting colorSetting = (ColorSetting)targetEffect.getSetting("Color");
                    BooleanSetting rainbowSetting = (BooleanSetting)targetEffect.getSetting("Rainbow");
                    BooleanSetting opaqueSetting = (BooleanSetting)targetEffect.getSetting("Opaque");
                    SliderSetting scaleSetting = (SliderSetting)targetEffect.getSetting("Scale");
                    BooleanSetting glowSetting = (BooleanSetting)targetEffect.getSetting("Glow");
                    SliderSetting glowSizeSetting = (SliderSetting)targetEffect.getSetting("Glow Size");
                    SliderSetting glowOpacitySetting = (SliderSetting)targetEffect.getSetting("Glow Opacity");
                    if (themeSetting != null
                        && colorSetting != null
                        && rainbowSetting != null
                        && opaqueSetting != null
                        && scaleSetting != null
                        && glowSetting != null
                        && glowSizeSetting != null
                        && glowOpacitySetting != null) {
                        Object theme = ModuleAccess.invokeStatic(
                            targetEffect,
                            "com.slither.cyemer.module.implementation.targeteffect.TextureTheme",
                            "fromString",
                            new Class[]{String.class},
                            themeSetting.getCurrentMode()
                        );
                        Color baseColor = colorSetting.getValue();
                        if (rainbowSetting.isEnabled()
                            && ModuleAccess.invokeStatic(
                                targetEffect,
                                "com.slither.cyemer.module.implementation.targeteffect.EffectRenderer",
                                "getRainbowColor",
                                new Class[]{long.class},
                                System.currentTimeMillis()
                            ) instanceof Color rainbowColor) {
                            baseColor = rainbowColor;
                        }

                        boolean opaqueMode = opaqueSetting.isEnabled();
                        float mainAlpha = opaqueMode ? 1.0F : 0.6F;
                        float baseScale = 0.9F + 0.05F * (float)scaleSetting.getValue();
                        class_2960 skinTexture = playerState.field_53520.comp_1626().comp_3627();
                        this.submitOverlayPass(queue, playerState, matrices, skinTexture, theme, baseColor, mainAlpha, baseScale, opaqueMode, false);
                        if (glowSetting.isEnabled()) {
                            float glowAlpha = (float)glowOpacitySetting.getValue();
                            float glowScale = baseScale + (float)glowSizeSetting.getValue();
                            Color glowColor = baseColor;
                            if (ModuleAccess.invokeOn(theme, "applyThemeTint", new Class[]{Color.class}, baseColor) instanceof Color tintColor) {
                                glowColor = tintColor;
                            }

                            this.submitOverlayPass(queue, playerState, matrices, skinTexture, theme, glowColor, glowAlpha, glowScale, false, true);
                        }
                    }
                }
            }
        }
    }

    private void submitOverlayPass(
        class_11659 queue,
        class_10055 state,
        class_4587 matrices,
        class_2960 skinTexture,
        Object theme,
        Color color,
        float alpha,
        float scale,
        boolean opaqueMode,
        boolean glowPass
    ) {
        boolean usesShader = ModuleAccess.invokeBooleanOn(theme, "usesShader", false, null);
        String shaderName = null;
        if (ModuleAccess.invokeOn(theme, "getShaderName", null) instanceof String shaderNameValue) {
            shaderName = shaderNameValue;
        }

        class_1921 layer;
        if (usesShader && shaderName != null) {
            layer = ShaderRenderLayers.getShaderEntityLayer(shaderName, skinTexture, glowPass);
        } else if (!glowPass && opaqueMode) {
            layer = class_12249.method_75994(skinTexture);
        } else {
            layer = class_12249.method_76000(skinTexture);
        }

        int argb = color.getRGB() & 16777215 | (int)(alpha * 255.0F) << 24;
        int overlay = class_922.method_23622(state, 0.0F);
        matrices.method_22903();
        matrices.method_22905(scale, scale, scale);
        if (this.field_4737 != null) {
            this.field_4737.method_2819(state);
        }

        queue.method_73490(this.field_4737, state, matrices, layer, state.field_61820, overlay, argb, null, state.field_61821, null);
        matrices.method_22909();
    }
}
