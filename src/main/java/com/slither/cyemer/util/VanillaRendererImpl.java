package com.slither.cyemer.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.slither.cyemer.module.implementation.CustomFont;
import com.slither.cyemer.rendering.font.MSDFFontRenderer;
import com.slither.cyemer.shader.CoreShaderManager;
import java.awt.Color;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1044;
import net.minecraft.class_10799;
import net.minecraft.class_11231;
import net.minecraft.class_11241;
import net.minecraft.class_12137;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public class VanillaRendererImpl implements ICyemerRenderer {
    private static final class_2960 DEFAULT_MSDF_FONT = class_2960.method_60655("dynamic_fps", "sans");
    private static final float UV_RADIUS_PACK_STRIDE = 2.0F;
    private static final float UV_RADIUS_PACK_SCALE = 4.0F;
    private float cachedClientFontScale = 1.0F;
    private boolean cachedUseClientShadow = true;
    private boolean cachedUseGlobalShadow = true;
    private class_2960 cachedFontId = null;
    private boolean cachedMsdfEnabled = false;
    private long lastFontCacheFrame = -1L;
    private final Matrix4f currentMatrix = new Matrix4f();
    private final Stack<Matrix4f> matrixStack = new Stack<>();
    private float globalPixelRatio;
    private class_332 activeContext;
    private final Map<Integer, class_2960> textureMap = new HashMap<>();
    private int nextTextureId = 1;

    private boolean shouldUseCustomPipelines() {
        return true;
    }

    private GuiShaderStyle getDefaultRoundedStyle() {
        return RenderBackendDetector.isVulkanBackend() ? GuiShaderStyle.HUD : GuiShaderStyle.DEFAULT;
    }

    private RenderPipeline getRoundedRectPipeline(GuiShaderStyle style) {
        GuiShaderStyle effectiveStyle = style != null ? style : this.getDefaultRoundedStyle();
        if (effectiveStyle == GuiShaderStyle.ICON) {
            effectiveStyle = this.getDefaultRoundedStyle();
        }

        RenderPipeline pipeline = CoreShaderManager.getInstance().getPipeline(effectiveStyle.pipelineId());
        if (pipeline != null) {
            return pipeline;
        } else {
            return effectiveStyle != GuiShaderStyle.DEFAULT ? CoreShaderManager.getInstance().getPipeline(GuiShaderStyle.DEFAULT.pipelineId()) : null;
        }
    }

    private RenderPipeline getTexturePipeline(GuiShaderStyle style) {
        if (style != null) {
            RenderPipeline styledPipeline = CoreShaderManager.getInstance().getPipeline(style.pipelineId());
            if (styledPipeline != null) {
                return styledPipeline;
            }
        }

        return CoreShaderManager.getInstance().getPipeline(class_2960.method_60655("dynamic_fps", "image"));
    }

    private float encodeRadiusForPackedUv(float radius) {
        float clampedRadius = Math.max(0.0F, Math.min(255.0F, radius));
        float packedRadius = Math.round(clampedRadius * 4.0F);
        return packedRadius * 2.0F;
    }

    private float packUvXWithRadius(float uvX, float radius) {
        return this.encodeRadiusForPackedUv(radius) + uvX;
    }

    @Override
    public void init() {
    }

    @Override
    public boolean beginFrame(float width, float height, float pixelRatio) {
        this.globalPixelRatio = pixelRatio;
        this.currentMatrix.identity();
        this.matrixStack.clear();
        this.activeContext = null;
        return true;
    }

    @Override
    public void endFrame() {
        this.activeContext = null;
    }

    @Override
    public void save() {
        this.matrixStack.push(new Matrix4f(this.currentMatrix));
    }

    @Override
    public void restore() {
        if (!this.matrixStack.isEmpty()) {
            this.currentMatrix.set((Matrix4fc)this.matrixStack.pop());
        }
    }

    @Override
    public void translate(float x, float y) {
        this.currentMatrix.translate(x, y, 0.0F);
    }

    @Override
    public void scale(float x, float y) {
        this.currentMatrix.scale(x, y, 1.0F);
    }

    @Override
    public void cleanup() {
        this.matrixStack.clear();
        this.textureMap.clear();
    }

    @Override
    public void drawRect(class_332 context, float x, float y, float width, float height, Color color) {
        if (context != null) {
            Vector4f p1 = new Vector4f(x, y, 0.0F, 1.0F).mul(this.currentMatrix);
            Vector4f p2 = new Vector4f(x + width, y + height, 0.0F, 1.0F).mul(this.currentMatrix);
            context.method_25294(Math.round(p1.x), Math.round(p1.y), Math.round(p2.x), Math.round(p2.y), color.getRGB());
        }
    }

    @Override
    public void drawRoundedRect(class_332 context, float x, float y, float width, float height, float radius, Color color) {
        this.drawRoundedRectInternal(context, x, y, width, height, radius, color, this.getDefaultRoundedStyle());
    }

    @Override
    public void drawRoundedRectStyled(class_332 context, float x, float y, float width, float height, float radius, Color color, GuiShaderStyle style) {
        this.drawRoundedRectInternal(context, x, y, width, height, radius, color, style);
    }

    private void drawRoundedRectInternal(class_332 context, float x, float y, float width, float height, float radius, Color color, GuiShaderStyle style) {
        if (context != null) {
            if (radius <= 0.5F) {
                this.drawRect(context, x, y, width, height, color);
            } else {
                RenderPipeline curvePipeline = this.shouldUseCustomPipelines() ? this.getRoundedRectPipeline(style) : null;
                if (curvePipeline == null) {
                    this.drawRoundedRectSoftware(context, x, y, width, height, radius, color);
                } else {
                    Vector4f p1 = new Vector4f(x, y, 0.0F, 1.0F).mul(this.currentMatrix);
                    Vector4f p2 = new Vector4f(x + width, y + height, 0.0F, 1.0F).mul(this.currentMatrix);
                    float packedU0 = this.packUvXWithRadius(0.0F, radius);
                    float packedU1 = this.packUvXWithRadius(1.0F, radius);
                    context.field_59826
                        .method_70919(
                            new class_11241(
                                curvePipeline,
                                class_11231.method_70899(),
                                new Matrix3x2f(context.method_51448()),
                                Math.round(p1.x),
                                Math.round(p1.y),
                                Math.round(p2.x),
                                Math.round(p2.y),
                                packedU0,
                                packedU1,
                                0.0F,
                                1.0F,
                                color.getRGB(),
                                context.field_44659.method_70863()
                            )
                        );
                }
            }
        }
    }

    @Override
    public void drawGlowingRect(
        class_332 context, float x, float y, float width, float height, float radius, Color innerColor, Color outerColor, float glowSize
    ) {
        if (context != null) {
            int layers = Math.min(5, (int)glowSize);

            for (int i = layers; i >= 1; i--) {
                float expand = i * 1.2F;
                int a = (int)(innerColor.getAlpha() * (1.0F - i / glowSize) * 0.4F);
                if (a >= 5) {
                    Color layerColor = new Color(innerColor.getRed(), innerColor.getGreen(), innerColor.getBlue(), Math.max(0, Math.min(255, a)));
                    this.drawRoundedRectStyled(
                        context, x - expand, y - expand, width + expand * 2.0F, height + expand * 2.0F, radius + expand, layerColor, GuiShaderStyle.GLOW
                    );
                }
            }
        }
    }

    @Override
    public void drawRectOutline(class_332 context, float x, float y, float width, float height, float strokeWidth, Color color) {
        if (context != null) {
            Vector4f p1 = new Vector4f(x, y, 0.0F, 1.0F).mul(this.currentMatrix);
            float sX = this.currentMatrix.m00();
            float sY = this.currentMatrix.m11();
            int x0 = Math.round(p1.x);
            int y0 = Math.round(p1.y);
            int w0 = Math.round(width * sX);
            int h0 = Math.round(height * sY);
            if (w0 > 0 && h0 > 0) {
                int x1 = x0 + w0 - 1;
                int y1 = y0 + h0 - 1;
                int rgb = color.getRGB();
                context.method_51738(x0, x1, y0, rgb);
                context.method_51738(x0, x1, y1, rgb);
                context.method_51742(x0, y0, y1, rgb);
                context.method_51742(x1, y0, y1, rgb);
            }
        }
    }

    @Override
    public void drawRoundedRectOutline(class_332 context, float x, float y, float width, float height, float radius, float strokeWidth, Color color) {
        if (context != null) {
            if (radius <= 0.5F) {
                this.drawRectOutline(context, x, y, width, height, strokeWidth, color);
            } else {
                this.drawRoundedRect(
                    context, x - strokeWidth, y - strokeWidth, width + strokeWidth * 2.0F, height + strokeWidth * 2.0F, radius + strokeWidth, color
                );
            }
        }
    }

    @Override
    public void drawRoundedRectGradient(class_332 context, float x, float y, float width, float height, float radius, Color c1, Color c2, boolean vertical) {
        this.drawRoundedRectGradientInternal(context, x, y, width, height, radius, c1, c2, vertical, this.getDefaultRoundedStyle());
    }

    @Override
    public void drawRoundedRectGradientStyled(
        class_332 context, float x, float y, float width, float height, float radius, Color c1, Color c2, boolean vertical, GuiShaderStyle style
    ) {
        this.drawRoundedRectGradientInternal(context, x, y, width, height, radius, c1, c2, vertical, style);
    }

    private void drawRoundedRectGradientInternal(
        class_332 context, float x, float y, float width, float height, float radius, Color c1, Color c2, boolean vertical, GuiShaderStyle style
    ) {
        if (context != null && !(width <= 0.0F) && !(height <= 0.0F)) {
            if (c1.getRGB() == c2.getRGB()) {
                this.drawRoundedRect(context, x, y, width, height, radius, c1);
            } else if (!(radius <= 0.5F)) {
                RenderPipeline curvePipeline = this.shouldUseCustomPipelines() ? this.getRoundedRectPipeline(style) : null;
                if (curvePipeline == null) {
                    this.drawRoundedRectGradientSoftware(context, x, y, width, height, radius, c1, c2, vertical);
                } else {
                    Vector4f fullP1 = new Vector4f(x, y, 0.0F, 1.0F).mul(this.currentMatrix);
                    Vector4f fullP2 = new Vector4f(x + width, y + height, 0.0F, 1.0F).mul(this.currentMatrix);
                    int fullX0 = Math.round(Math.min(fullP1.x, fullP2.x));
                    int fullY0 = Math.round(Math.min(fullP1.y, fullP2.y));
                    int fullX1 = Math.round(Math.max(fullP1.x, fullP2.x));
                    int fullY1 = Math.round(Math.max(fullP1.y, fullP2.y));
                    int totalPixelSpan = vertical ? fullY1 - fullY0 : fullX1 - fullX0;
                    if (totalPixelSpan > 0) {
                        float packedU0 = this.packUvXWithRadius(0.0F, radius);
                        float packedU1 = this.packUvXWithRadius(1.0F, radius);
                        Matrix3x2f mat = new Matrix3x2f(context.method_51448());
                        int prevRgb = Integer.MIN_VALUE;
                        int runStart = 0;

                        for (int px = 0; px <= totalPixelSpan; px++) {
                            int curRgb = prevRgb;
                            if (px < totalPixelSpan) {
                                float t = totalPixelSpan <= 1 ? 0.0F : (px + 0.5F) / totalPixelSpan;
                                curRgb = this.interpolate(c1, c2, t).getRGB();
                            }

                            if (curRgb != prevRgb && prevRgb != Integer.MIN_VALUE) {
                                int stripY0;
                                int stripX1;
                                int stripY1;
                                int stripX0;
                                if (vertical) {
                                    stripX0 = fullX0;
                                    stripX1 = fullX1;
                                    stripY0 = fullY0 + runStart;
                                    stripY1 = fullY0 + px;
                                } else {
                                    stripX0 = fullX0 + runStart;
                                    stripX1 = fullX0 + px;
                                    stripY0 = fullY0;
                                    stripY1 = fullY1;
                                }

                                context.method_44379(stripX0, stripY0, stripX1, stripY1);
                                context.field_59826
                                    .method_70919(
                                        new class_11241(
                                            curvePipeline,
                                            class_11231.method_70899(),
                                            mat,
                                            fullX0,
                                            fullY0,
                                            fullX1,
                                            fullY1,
                                            packedU0,
                                            packedU1,
                                            0.0F,
                                            1.0F,
                                            prevRgb,
                                            context.field_44659.method_70863()
                                        )
                                    );
                                context.method_44380();
                                runStart = px;
                            }

                            prevRgb = curRgb;
                        }
                    }
                }
            } else {
                Vector4f p1 = new Vector4f(x, y, 0.0F, 1.0F).mul(this.currentMatrix);
                Vector4f p2 = new Vector4f(x + width, y + height, 0.0F, 1.0F).mul(this.currentMatrix);
                if (vertical) {
                    context.method_25296(Math.round(p1.x), Math.round(p1.y), Math.round(p2.x), Math.round(p2.y), c1.getRGB(), c2.getRGB());
                } else {
                    int xStart = Math.round(Math.min(p1.x, p2.x));
                    int xEnd = Math.round(Math.max(p1.x, p2.x));
                    int yStart = Math.round(Math.min(p1.y, p2.y));
                    int yEnd = Math.round(Math.max(p1.y, p2.y));
                    int span = Math.max(1, xEnd - xStart);

                    for (int ix = 0; ix < span; ix++) {
                        float t = span <= 1 ? 0.0F : (ix + 0.5F) / span;
                        context.method_25294(xStart + ix, yStart, xStart + ix + 1, yEnd, this.interpolate(c1, c2, t).getRGB());
                    }
                }
            }
        }
    }

    private void drawRoundedRectSoftware(class_332 context, float x, float y, float width, float height, float radius, Color color) {
        Vector4f p1 = new Vector4f(x, y, 0.0F, 1.0F).mul(this.currentMatrix);
        Vector4f p2 = new Vector4f(x + width, y + height, 0.0F, 1.0F).mul(this.currentMatrix);
        int x0 = Math.round(Math.min(p1.x, p2.x));
        int y0 = Math.round(Math.min(p1.y, p2.y));
        int x1 = Math.round(Math.max(p1.x, p2.x));
        int y1 = Math.round(Math.max(p1.y, p2.y));
        int scaledRadius = this.scaleRadius(radius, x1 - x0, y1 - y0);
        this.fillRoundedRectPixels(context, x0, y0, x1, y1, scaledRadius, color.getRGB());
    }

    private void drawRoundedRectGradientSoftware(
        class_332 context, float x, float y, float width, float height, float radius, Color c1, Color c2, boolean vertical
    ) {
        Vector4f p1 = new Vector4f(x, y, 0.0F, 1.0F).mul(this.currentMatrix);
        Vector4f p2 = new Vector4f(x + width, y + height, 0.0F, 1.0F).mul(this.currentMatrix);
        int x0 = Math.round(Math.min(p1.x, p2.x));
        int y0 = Math.round(Math.min(p1.y, p2.y));
        int x1 = Math.round(Math.max(p1.x, p2.x));
        int y1 = Math.round(Math.max(p1.y, p2.y));
        int w = x1 - x0;
        int h = y1 - y0;
        if (w > 0 && h > 0) {
            int r = this.scaleRadius(radius, w, h);
            if (r > 0) {
                if (vertical) {
                    for (int iy = 0; iy < h; iy++) {
                        float t = h <= 1 ? 0.0F : (float)iy / (h - 1);
                        int rgb = this.interpolate(c1, c2, t).getRGB();
                        float insetF = this.roundedInsetFloatForRow(iy, h, r);
                        int inset = (int)Math.floor(insetF);
                        float frac = insetF - inset;
                        int lx = x0 + inset;
                        int rx = x1 - inset;
                        if (rx > lx) {
                            context.method_25294(lx, y0 + iy, rx, y0 + iy + 1, rgb);
                        }

                        int aaColor = this.scaleAlpha(rgb, 1.0F - frac);
                        if (aaColor >>> 24 != 0) {
                            if (lx - 1 >= x0) {
                                context.method_25294(lx - 1, y0 + iy, lx, y0 + iy + 1, aaColor);
                            }

                            if (rx < x1) {
                                context.method_25294(rx, y0 + iy, rx + 1, y0 + iy + 1, aaColor);
                            }
                        }
                    }
                } else {
                    for (int ix = 0; ix < w; ix++) {
                        float tx = w <= 1 ? 0.0F : (float)ix / (w - 1);
                        int rgbx = this.interpolate(c1, c2, tx).getRGB();
                        float insetFx = this.roundedInsetFloatForColumn(ix, w, r);
                        int insetx = (int)Math.floor(insetFx);
                        float fracx = insetFx - insetx;
                        int ty = y0 + insetx;
                        int by = y1 - insetx;
                        if (by > ty) {
                            context.method_25294(x0 + ix, ty, x0 + ix + 1, by, rgbx);
                        }

                        int aaColor = this.scaleAlpha(rgbx, 1.0F - fracx);
                        if (aaColor >>> 24 != 0) {
                            if (ty - 1 >= y0) {
                                context.method_25294(x0 + ix, ty - 1, x0 + ix + 1, ty, aaColor);
                            }

                            if (by < y1) {
                                context.method_25294(x0 + ix, by, x0 + ix + 1, by + 1, aaColor);
                            }
                        }
                    }
                }
            } else {
                if (vertical) {
                    context.method_25296(x0, y0, x1, y1, c1.getRGB(), c2.getRGB());
                } else {
                    for (int ix = 0; ix < w; ix++) {
                        float txx = w <= 1 ? 0.0F : (float)ix / (w - 1);
                        int rgbxx = this.interpolate(c1, c2, txx).getRGB();
                        context.method_25294(x0 + ix, y0, x0 + ix + 1, y1, rgbxx);
                    }
                }
            }
        }
    }

    private void fillRoundedRectPixels(class_332 context, int x0, int y0, int x1, int y1, int radius, int color) {
        int w = x1 - x0;
        int h = y1 - y0;
        if (w > 0 && h > 0) {
            int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
            if (r <= 0) {
                context.method_25294(x0, y0, x1, y1, color);
            } else {
                context.method_25294(x0 + r, y0, x1 - r, y1, color);
                context.method_25294(x0, y0 + r, x0 + r, y1 - r, color);
                context.method_25294(x1 - r, y0 + r, x1, y1 - r, color);

                for (int iy = 0; iy < r; iy++) {
                    float insetF = this.roundedInsetFloatFromEdge(iy, r);
                    int inset = (int)Math.floor(insetF);
                    float frac = insetF - inset;
                    int lx = x0 + inset;
                    int rx = x1 - inset;
                    int topY = y0 + iy;
                    int bottomY = y1 - iy - 1;
                    if (rx > lx) {
                        context.method_25294(lx, topY, rx, topY + 1, color);
                        context.method_25294(lx, bottomY, rx, bottomY + 1, color);
                    }

                    int aaColor = this.scaleAlpha(color, 1.0F - frac);
                    if (aaColor >>> 24 != 0) {
                        if (lx - 1 >= x0) {
                            context.method_25294(lx - 1, topY, lx, topY + 1, aaColor);
                            context.method_25294(lx - 1, bottomY, lx, bottomY + 1, aaColor);
                        }

                        if (rx < x1) {
                            context.method_25294(rx, topY, rx + 1, topY + 1, aaColor);
                            context.method_25294(rx, bottomY, rx + 1, bottomY + 1, aaColor);
                        }
                    }
                }
            }
        }
    }

    private int roundedInsetForRow(int iy, int height, int radius) {
        return (int)Math.floor(this.roundedInsetFloatForRow(iy, height, radius));
    }

    private int roundedInsetForColumn(int ix, int width, int radius) {
        return (int)Math.floor(this.roundedInsetFloatForColumn(ix, width, radius));
    }

    private int roundedInsetFromEdge(int edgeCoord, int radius) {
        return (int)Math.floor(this.roundedInsetFloatFromEdge(edgeCoord, radius));
    }

    private float roundedInsetFloatForRow(int iy, int height, int radius) {
        if (iy < radius) {
            return this.roundedInsetFloatFromEdge(iy, radius);
        } else {
            return iy >= height - radius ? this.roundedInsetFloatFromEdge(height - iy - 1, radius) : 0.0F;
        }
    }

    private float roundedInsetFloatForColumn(int ix, int width, int radius) {
        if (ix < radius) {
            return this.roundedInsetFloatFromEdge(ix, radius);
        } else {
            return ix >= width - radius ? this.roundedInsetFloatFromEdge(width - ix - 1, radius) : 0.0F;
        }
    }

    private float roundedInsetFloatFromEdge(int edgeCoord, int radius) {
        double dy = radius - edgeCoord - 0.5;
        double dx = Math.sqrt(Math.max(0.0, radius * radius - dy * dy));
        return (float)Math.max(0.0, radius - dx);
    }

    private int scaleAlpha(int argb, float factor) {
        int alpha = argb >>> 24 & 0xFF;
        if (alpha == 0) {
            return argb;
        } else {
            float clamped = Math.max(0.0F, Math.min(1.0F, factor));
            int scaledAlpha = Math.max(0, Math.min(255, Math.round(alpha * clamped)));
            return scaledAlpha << 24 | argb & 16777215;
        }
    }

    private int scaleRadius(float radius, int width, int height) {
        float sx = Math.abs(this.currentMatrix.m00());
        float sy = Math.abs(this.currentMatrix.m11());
        float scaled = radius * Math.max(1.0E-4F, Math.min(sx, sy));
        int maxRadius = Math.max(0, Math.min(width, height) / 2);
        return Math.max(0, Math.min(maxRadius, Math.round(scaled)));
    }

    @Override
    public void drawText(class_332 context, String text, float x, float y, float fontSize, Color color, boolean shadow) {
        if (context != null) {
            Vector4f pos = new Vector4f(x, y, 0.0F, 1.0F).mul(this.currentMatrix);
            float currentScaleX = this.currentMatrix.m00();
            float fontScale = this.normalizeFontScale(this.getTextScale(fontSize) * this.getClientFontScale() * currentScaleX);
            float snappedX = this.snapToPixel(pos.x, fontScale);
            float snappedY = this.snapToPixel(pos.y, fontScale);
            boolean renderShadow = this.useClientShadow(shadow) && this.useGlobalShadow(shadow);
            class_2960 selectedFont = this.getEffectiveFontId();
            if (this.shouldPreferMsdf(selectedFont)) {
                boolean rendered = MSDFFontRenderer.drawText(context, selectedFont, text, snappedX, snappedY, fontScale, color.getRGB(), renderShadow);
                if (rendered) {
                    return;
                }
            }

            context.method_51448().pushMatrix();
            context.method_51448().translate(snappedX, snappedY);
            context.method_51448().scale(fontScale, fontScale);
            CustomFont.drawText(context, class_310.method_1551().field_1772, text, 0.0F, 0.0F, color.getRGB(), renderShadow);
            context.method_51448().popMatrix();
        }
    }

    @Override
    public void drawBlur(class_332 context, float x, float y, float width, float height, float blurRadius) {
        if (context != null && !(width <= 0.0F) && !(height <= 0.0F) && !(blurRadius <= 0.0F)) {
            int layers = Math.max(2, Math.min(8, Math.round(blurRadius / 3.0F)));
            float maxExpand = Math.max(1.0F, Math.min(blurRadius, 14.0F));
            float baseRadius = Math.min(Math.min(width, height) * 0.12F, 8.0F);

            for (int i = layers; i >= 1; i--) {
                float t = (float)i / layers;
                float expand = maxExpand * t;
                int alpha = Math.max(2, Math.min(34, Math.round((0.1F + 0.2F * t) * 255.0F)));
                Color layer = new Color(18, 22, 30, alpha);
                this.drawRoundedRectStyled(
                    context, x - expand, y - expand, width + expand * 2.0F, height + expand * 2.0F, baseRadius + expand, layer, GuiShaderStyle.PANEL_GLASS
                );
            }
        }
    }

    public void drawBlurSimple(class_332 context, float x, float y, float width, float height, float blurRadius) {
        this.drawBlur(context, x, y, width, height, blurRadius);
    }

    @Override
    public void rotate(float angle) {
        this.currentMatrix.rotate(angle, 0.0F, 0.0F, 1.0F);
    }

    @Override
    public void drawCircle(class_332 context, float x, float y, float radius, Color color) {
        if (context != null) {
            Vector4f center = new Vector4f(x, y, 0.0F, 1.0F).mul(this.currentMatrix);
            float scaleX = this.currentMatrix.m00();
            float scaleY = this.currentMatrix.m11();
            float scaledRadius = radius * Math.max(scaleX, scaleY);
            int segments = Math.max(16, (int)(scaledRadius * 2.0F));
            double angleStep = (Math.PI * 2) / segments;

            for (int i = 0; i < segments; i++) {
                double angle1 = i * angleStep;
                double angle2 = (i + 1) * angleStep;
                float x1 = center.x + (float)(Math.cos(angle1) * scaledRadius);
                float y1 = center.y + (float)(Math.sin(angle1) * scaledRadius);
                float x2 = center.x + (float)(Math.cos(angle2) * scaledRadius);
                float y2 = center.y + (float)(Math.sin(angle2) * scaledRadius);
                context.method_25294((int)center.x, (int)center.y, (int)x1, (int)y1, color.getRGB());
                context.method_25294((int)x1, (int)y1, (int)x2, (int)y2, color.getRGB());
            }
        }
    }

    @Override
    public void drawArc(class_332 context, float cx, float cy, float radius, float startAngle, float sweepAngle, float strokeWidth, Color color) {
        if (context != null) {
            Vector4f center = new Vector4f(cx, cy, 0.0F, 1.0F).mul(this.currentMatrix);
            float scaleX = this.currentMatrix.m00();
            float scaleY = this.currentMatrix.m11();
            float scaledRadius = radius * Math.max(scaleX, scaleY);
            float scaledStroke = strokeWidth * Math.max(scaleX, scaleY);
            double startRad = Math.toRadians(startAngle - 90.0F);
            double sweepRad = Math.toRadians(sweepAngle);
            int segments = Math.max(8, (int)(Math.abs(sweepAngle) / 5.0F));
            double angleStep = sweepRad / segments;

            for (int i = 0; i < segments; i++) {
                double angle1 = startRad + i * angleStep;
                double angle2 = startRad + (i + 1) * angleStep;
                float x1 = center.x + (float)(Math.cos(angle1) * scaledRadius);
                float y1 = center.y + (float)(Math.sin(angle1) * scaledRadius);
                float x2 = center.x + (float)(Math.cos(angle2) * scaledRadius);
                float y2 = center.y + (float)(Math.sin(angle2) * scaledRadius);
                this.drawThickLine(context, x1, y1, x2, y2, scaledStroke, color);
            }
        }
    }

    private void drawThickLine(class_332 context, float x1, float y1, float x2, float y2, float thickness, Color color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float)Math.sqrt(dx * dx + dy * dy);
        if (!(len < 0.001F)) {
            float perpX = -dy / len * (thickness / 2.0F);
            float perpY = dx / len * (thickness / 2.0F);
            int p1x = (int)(x1 + perpX);
            int p1y = (int)(y1 + perpY);
            int p2x = (int)(x1 - perpX);
            int p2y = (int)(y1 - perpY);
            int p3x = (int)(x2 - perpX);
            int p3y = (int)(y2 - perpY);
            int p4x = (int)(x2 + perpX);
            int p4y = (int)(y2 + perpY);
            context.method_25294(
                Math.min(p1x, Math.min(p2x, p3x)),
                Math.min(p1y, Math.min(p2y, p3y)),
                Math.max(p1x, Math.max(p2x, Math.max(p3x, p4x))),
                Math.max(p1y, Math.max(p2y, Math.max(p3y, p4y))),
                color.getRGB()
            );
        }
    }

    @Override
    public void drawCenteredText(class_332 context, String text, float x, float y, float fontSize, Color color, boolean shadow) {
        if (context != null) {
            float width = this.getTextWidth(text, fontSize);
            float heightOffset = this.getTextHeight(fontSize) / 2.0F;
            this.drawText(context, text, x - width / 2.0F, y - heightOffset, fontSize, color, shadow);
        }
    }

    @Override
    public float getTextWidth(String text, float fontSize) {
        float scaledFont = this.normalizeFontScale(this.getTextScale(fontSize) * this.getClientFontScale());
        class_2960 selectedFont = this.getEffectiveFontId();
        if (this.shouldPreferMsdf(selectedFont)) {
            float msdfWidth = MSDFFontRenderer.getTextWidth(selectedFont, text);
            if (msdfWidth > 0.0F) {
                return msdfWidth * scaledFont;
            }
        }

        return this.getCustomFontTextWidth(text) * scaledFont;
    }

    @Override
    public float getTextHeight(float fontSize) {
        float scaledFont = this.normalizeFontScale(this.getTextScale(fontSize) * this.getClientFontScale());
        class_2960 selectedFont = this.getEffectiveFontId();
        if (this.shouldPreferMsdf(selectedFont)) {
            float msdfHeight = MSDFFontRenderer.getTextHeight(selectedFont);
            if (msdfHeight > 0.0F) {
                return msdfHeight * scaledFont;
            }
        }

        return this.getCustomFontTextHeight() * scaledFont;
    }

    @Override
    public void setFontBlur(float blur) {
    }

    @Override
    public void drawTexture(
        class_332 context, int nvgImageId, float x, float y, float width, float height, float u, float v, float rW, float rH, float tW, float tH
    ) {
        this.drawTextureInternal(context, nvgImageId, x, y, width, height, u, v, rW, rH, tW, tH, null, 0.0F);
    }

    @Override
    public void drawTextureStyled(
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
        this.drawTextureInternal(context, nvgImageId, x, y, width, height, u, v, rW, rH, tW, tH, style, 0.0F);
    }

    private class_11231 buildTextureSetup(class_2960 textureId) {
        try {
            class_1044 abstractTexture = class_310.method_1551().method_1531().method_4619(textureId);
            class_12137 linearSampler = RenderSystem.getSamplerCache()
                .method_75293(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, false);
            return class_11231.method_70900(abstractTexture.method_71659(), linearSampler);
        } catch (Exception var4) {
            return class_11231.method_70899();
        }
    }

    private void drawTextureInternal(
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
        GuiShaderStyle style,
        float cornerRadius
    ) {
        if (context != null) {
            class_2960 texture = this.textureMap.get(nvgImageId);
            if (texture != null) {
                RenderPipeline imagePipeline = this.shouldUseCustomPipelines() ? this.getTexturePipeline(style) : null;
                Vector4f p1 = new Vector4f(x, y, 0.0F, 1.0F).mul(this.currentMatrix);
                Vector4f p2 = new Vector4f(x + width, y + height, 0.0F, 1.0F).mul(this.currentMatrix);
                int drawX = (int)p1.x;
                int drawY = (int)p1.y;
                int drawW = (int)(p2.x - p1.x);
                int drawH = (int)(p2.y - p1.y);
                if (drawW > 0 && drawH > 0) {
                    float u0 = u / tW;
                    float v0 = v / tH;
                    float u1 = (u + rW) / tW;
                    float v1 = (v + rH) / tH;
                    float packedU0 = u0;
                    float packedU1 = u1;
                    if (cornerRadius > 0.5F) {
                        packedU0 = this.packUvXWithRadius(u0, cornerRadius);
                        packedU1 = this.packUvXWithRadius(u1, cornerRadius);
                    }

                    if (imagePipeline != null) {
                        class_11231 setup = this.buildTextureSetup(texture);
                        context.field_59826
                            .method_70919(
                                new class_11241(
                                    imagePipeline,
                                    setup,
                                    new Matrix3x2f(context.method_51448()),
                                    drawX,
                                    drawY,
                                    drawX + drawW,
                                    drawY + drawH,
                                    packedU0,
                                    packedU1,
                                    v0,
                                    v1,
                                    -1,
                                    context.field_44659.method_70863()
                                )
                            );
                    } else {
                        int regionW = Math.max(1, Math.round(rW));
                        int regionH = Math.max(1, Math.round(rH));
                        int textureW = Math.max(regionW, Math.round(tW));
                        int textureH = Math.max(regionH, Math.round(tH));
                        context.method_25302(class_10799.field_56883, texture, drawX, drawY, u, v, drawW, drawH, regionW, regionH, textureW, textureH);
                    }
                }
            }
        }
    }

    @Override
    public void drawTextureRounded(
        class_332 context, int nvgImageId, float x, float y, float width, float height, float u, float v, float rW, float rH, float tW, float tH, float radius
    ) {
        this.drawTextureInternal(context, nvgImageId, x, y, width, height, u, v, rW, rH, tW, tH, null, radius);
    }

    @Override
    public int createImageFromTexture(int textureId, int width, int height) {
        return -1;
    }

    @Override
    public void deleteImage(int imageId) {
        this.textureMap.remove(imageId);
    }

    @Override
    public int createImageFromFile(String resourcePath) {
        if (resourcePath == null) {
            return -1;
        } else {
            String cleanPath = resourcePath;
            if (resourcePath.startsWith("/")) {
                cleanPath = resourcePath.substring(1);
            }

            if (cleanPath.startsWith("assets/")) {
                String namespacedPath = cleanPath.substring("assets/".length());
                int slashIndex = namespacedPath.indexOf("/");
                if (slashIndex > 0 && slashIndex < namespacedPath.length() - 1) {
                    String namespace = namespacedPath.substring(0, slashIndex);
                    String path = namespacedPath.substring(slashIndex + 1);
                    class_2960 id = this.tryParseIdentifier(namespace, path);
                    if (id != null) {
                        int newId = this.nextTextureId++;
                        this.textureMap.put(newId, id);
                        return newId;
                    }
                }
            }

            if (cleanPath.contains(":")) {
                class_2960 id = this.tryParseIdentifier(cleanPath);
                if (id != null) {
                    int newId = this.nextTextureId++;
                    this.textureMap.put(newId, id);
                    return newId;
                }
            }

            return -1;
        }
    }

    private class_2960 tryParseIdentifier(String value) {
        try {
            return class_2960.method_60654(value);
        } catch (Exception var5) {
            String lower = value.toLowerCase(Locale.ROOT);
            if (!lower.equals(value)) {
                try {
                    return class_2960.method_60654(lower);
                } catch (Exception var4) {
                }
            }

            return null;
        }
    }

    private class_2960 tryParseIdentifier(String namespace, String path) {
        try {
            return class_2960.method_60655(namespace, path);
        } catch (Exception var7) {
            String lowerNamespace = namespace.toLowerCase(Locale.ROOT);
            String lowerPath = path.toLowerCase(Locale.ROOT);
            if (!lowerNamespace.equals(namespace) || !lowerPath.equals(path)) {
                try {
                    return class_2960.method_60655(lowerNamespace, lowerPath);
                } catch (Exception var6) {
                }
            }

            return null;
        }
    }

    @Override
    public void scissor(class_332 context, float x, float y, float width, float height) {
        Vector4f p1 = new Vector4f(x, y, 0.0F, 1.0F).mul(this.currentMatrix);
        Vector4f p2 = new Vector4f(x + width, y + height, 0.0F, 1.0F).mul(this.currentMatrix);
        if (context != null) {
            this.activeContext = context;
            if (context.field_44659.method_70863() != null) {
                context.method_44380();
            }

            context.method_44379((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
        }
    }

    @Override
    public void scissor(float x, float y, float width, float height) {
        this.scissor(null, x, y, width, height);
    }

    @Override
    public void resetScissor() {
        if (this.activeContext != null && this.activeContext.field_44659.method_70863() != null) {
            this.activeContext.method_44380();
        }
    }

    private float snapToPixel(float value, float scale) {
        return scale <= 0.0F ? value : Math.round(value * scale) / scale;
    }

    private Color interpolate(Color start, Color end, float t) {
        float clamped = Math.max(0.0F, Math.min(1.0F, t));
        int r = (int)(start.getRed() + (end.getRed() - start.getRed()) * clamped);
        int g = (int)(start.getGreen() + (end.getGreen() - start.getGreen()) * clamped);
        int b = (int)(start.getBlue() + (end.getBlue() - start.getBlue()) * clamped);
        int a = (int)(start.getAlpha() + (end.getAlpha() - start.getAlpha()) * clamped);
        return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)), Math.max(0, Math.min(255, a)));
    }

    private float normalizeFontScale(float scale) {
        if (scale <= 0.0F) {
            return 1.0F;
        } else {
            float snapped = Math.round(scale * 16.0F) / 16.0F;
            return Math.abs(snapped - 1.0F) < 0.01F ? 1.0F : snapped;
        }
    }

    private float getClientFontScale() {
        return CustomFont.getClientFontScale();
    }

    private boolean useClientShadow(boolean shadow) {
        return CustomFont.useClientShadow(shadow);
    }

    private boolean useGlobalShadow(boolean shadow) {
        return CustomFont.useGlobalShadow(shadow);
    }

    private class_2960 getSelectedFontId() {
        return CustomFont.getSelectedFontId();
    }

    private class_2960 getEffectiveFontId() {
        class_2960 selected = this.getSelectedFontId();
        if (selected != null) {
            return selected;
        } else {
            return RenderBackendDetector.isVulkanBackend() ? DEFAULT_MSDF_FONT : null;
        }
    }

    private boolean shouldPreferMsdf(class_2960 fontId) {
        if (fontId == null) {
            return false;
        } else {
            return RenderBackendDetector.isVulkanBackend() ? true : this.isMsdfEnabled();
        }
    }

    private boolean isMsdfEnabled() {
        return CustomFont.isMsdfEnabled();
    }

    private float getCustomFontTextWidth(String text) {
        return CustomFont.getTextWidth(class_310.method_1551().field_1772, text);
    }

    private float getCustomFontTextHeight() {
        return CustomFont.getTextHeight(class_310.method_1551().field_1772);
    }

    private float getTextScale(float fontSize) {
        return fontSize / (this.getEffectiveFontId() != null ? 10.0F : 8.0F);
    }
}
