package com.slither.cyemer.util;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public interface ICyemerRenderer {
    void init();

    boolean beginFrame(float var1, float var2, float var3);

    void endFrame();

    void drawRect(class_332 var1, float var2, float var3, float var4, float var5, Color var6);

    void drawRoundedRect(class_332 var1, float var2, float var3, float var4, float var5, float var6, Color var7);

    default void drawRoundedRectStyled(class_332 context, float x, float y, float width, float height, float radius, Color color, GuiShaderStyle style) {
        this.drawRoundedRect(context, x, y, width, height, radius, color);
    }

    void drawGlowingRect(class_332 var1, float var2, float var3, float var4, float var5, float var6, Color var7, Color var8, float var9);

    void drawRectOutline(class_332 var1, float var2, float var3, float var4, float var5, float var6, Color var7);

    void drawRoundedRectOutline(class_332 var1, float var2, float var3, float var4, float var5, float var6, float var7, Color var8);

    void drawRoundedRectGradient(class_332 var1, float var2, float var3, float var4, float var5, float var6, Color var7, Color var8, boolean var9);

    default void drawRoundedRectGradientStyled(
        class_332 context, float x, float y, float width, float height, float radius, Color color1, Color color2, boolean vertical, GuiShaderStyle style
    ) {
        this.drawRoundedRectGradient(context, x, y, width, height, radius, color1, color2, vertical);
    }

    void drawCircle(class_332 var1, float var2, float var3, float var4, Color var5);

    void drawArc(class_332 var1, float var2, float var3, float var4, float var5, float var6, float var7, Color var8);

    void drawBlur(class_332 var1, float var2, float var3, float var4, float var5, float var6);

    void rotate(float var1);

    void drawText(class_332 var1, String var2, float var3, float var4, float var5, Color var6, boolean var7);

    void drawCenteredText(class_332 var1, String var2, float var3, float var4, float var5, Color var6, boolean var7);

    void setFontBlur(float var1);

    float getTextWidth(String var1, float var2);

    float getTextHeight(float var1);

    void drawTexture(
        class_332 var1, int var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12
    );

    default void drawTextureStyled(
        class_332 context,
        int nvgImageId,
        float x,
        float y,
        float width,
        float height,
        float u,
        float v,
        float rW,
        float rH,
        float tW,
        float tH,
        GuiShaderStyle style
    ) {
        this.drawTexture(context, nvgImageId, x, y, width, height, u, v, rW, rH, tW, tH);
    }

    void drawTextureRounded(
        class_332 var1,
        int var2,
        float var3,
        float var4,
        float var5,
        float var6,
        float var7,
        float var8,
        float var9,
        float var10,
        float var11,
        float var12,
        float var13
    );

    int createImageFromTexture(int var1, int var2, int var3);

    void deleteImage(int var1);

    int createImageFromFile(String var1);

    void scissor(class_332 var1, float var2, float var3, float var4, float var5);

    void scissor(float var1, float var2, float var3, float var4);

    void resetScissor();

    void save();

    void restore();

    void translate(float var1, float var2);

    void scale(float var1, float var2);

    void cleanup();
}
