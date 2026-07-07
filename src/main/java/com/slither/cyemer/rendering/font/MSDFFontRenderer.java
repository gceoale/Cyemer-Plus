package com.slither.cyemer.rendering.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.slither.cyemer.shader.CoreShaderManager;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_1044;
import net.minecraft.class_10799;
import net.minecraft.class_11231;
import net.minecraft.class_11241;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3298;
import net.minecraft.class_332;
import org.joml.Matrix3x2f;

@Environment(EnvType.CLIENT)
public final class MSDFFontRenderer {
    private static final int FIRST_CHAR = 32;
    private static final int LAST_CHAR = 126;
    private static final int CHAR_COUNT = 95;
    private static final int COLUMNS = 16;
    private static final int BASE_FONT_SIZE = 192;
    private static final int PADDING = 24;
    private static final int SDF_SPREAD = 16;
    private static final float TARGET_LINE_HEIGHT = 9.0F;
    private static final float BASELINE_SHIFT = -0.75F;
    private static final class_2960 MSDF_PIPELINE_ID = class_2960.method_60655("dynamic_fps", "msdf_text");
    private static final Map<class_2960, MSDFFontRenderer.FontAtlas> ATLASES = new ConcurrentHashMap<>();
    private static final Set<class_2960> FAILED_ATLASES = ConcurrentHashMap.newKeySet();
    private static final Set<class_2960> BUILDING = ConcurrentHashMap.newKeySet();
    private static final Map<class_2960, MSDFFontRenderer.PendingAtlas> PENDING = new ConcurrentHashMap<>();
    private static final ForkJoinPool SDF_POOL = new ForkJoinPool(Math.max(2, Runtime.getRuntime().availableProcessors() - 1));
    private static final ExecutorService ATLAS_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "cyemer-msdf-atlas-builder");
        t.setDaemon(true);
        t.setPriority(4);
        return t;
    });

    private MSDFFontRenderer() {
    }

    public static void preloadFont(class_2960 fontId) {
        getAtlas(fontId);
    }

    public static boolean drawText(class_332 context, class_2960 fontId, String text, float x, float y, float scale, int argb, boolean shadow) {
        if (context != null && fontId != null && text != null && !text.isEmpty()) {
            MSDFFontRenderer.FontAtlas atlas = getAtlas(fontId);
            if (atlas == null) {
                return false;
            } else {
                if (shadow) {
                    float shadowOffset = Math.max(1.0F, scale * 0.75F);
                    drawInternal(context, atlas, text, x + shadowOffset, y + shadowOffset, scale, toShadowColor(argb));
                }

                drawInternal(context, atlas, text, x, y, scale, argb);
                return true;
            }
        } else {
            return false;
        }
    }

    public static float getTextWidth(class_2960 fontId, String text) {
        if (fontId != null && text != null && !text.isEmpty()) {
            MSDFFontRenderer.FontAtlas atlas = getAtlas(fontId);
            if (atlas == null) {
                return 0.0F;
            } else {
                float lineWidth = 0.0F;
                float maxWidth = 0.0F;

                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    if (c == '\n') {
                        maxWidth = Math.max(maxWidth, lineWidth);
                        lineWidth = 0.0F;
                    } else {
                        MSDFFontRenderer.Glyph glyph = atlas.getGlyph(c);
                        lineWidth += (glyph != null ? glyph.advance : atlas.spaceAdvance) * atlas.metricScale;
                    }
                }

                return Math.max(maxWidth, lineWidth);
            }
        } else {
            return 0.0F;
        }
    }

    public static float getTextHeight(class_2960 fontId) {
        MSDFFontRenderer.FontAtlas atlas = getAtlas(fontId);
        return atlas == null ? 9.0F : atlas.lineHeight * atlas.metricScale;
    }

    private static MSDFFontRenderer.FontAtlas getAtlas(class_2960 fontId) {
        uploadPendingAtlas(fontId);
        MSDFFontRenderer.FontAtlas ready = ATLASES.get(fontId);
        if (ready != null) {
            return ready;
        } else if (FAILED_ATLASES.contains(fontId)) {
            return null;
        } else {
            if (BUILDING.add(fontId)) {
                ATLAS_EXECUTOR.submit(() -> {
                    try {
                        MSDFFontRenderer.PendingAtlas pending = buildAtlasOffThread(fontId);
                        if (pending == null) {
                            FAILED_ATLASES.add(fontId);
                        } else {
                            PENDING.put(fontId, pending);
                        }
                    } catch (Exception var5) {
                        FAILED_ATLASES.add(fontId);
                    } finally {
                        BUILDING.remove(fontId);
                    }
                });
            }

            return null;
        }
    }

    private static void uploadPendingAtlas(class_2960 fontId) {
        MSDFFontRenderer.PendingAtlas pending = PENDING.remove(fontId);
        if (pending != null) {
            try {
                class_1043 texture = new class_1043(() -> "cyemer_msdf_" + fontId.method_12832(), pending.nativeImage);
                configureTextureFilter(texture);
                texture.method_4524();
                class_2960 textureId = class_2960.method_60655("dynamic_fps", "generated/msdf_" + fontId.method_12832());
                class_310.method_1551().method_1531().method_4616(textureId, texture);
                ATLASES.put(
                    fontId,
                    new MSDFFontRenderer.FontAtlas(
                        textureId, pending.atlasWidth, pending.atlasHeight, pending.lineHeight, pending.spaceAdvance, pending.metricScale, pending.glyphs
                    )
                );
            } catch (Exception var4) {
                FAILED_ATLASES.add(fontId);
            }
        }
    }

    private static void drawInternal(class_332 context, MSDFFontRenderer.FontAtlas atlas, String text, float x, float y, float scale, int color) {
        RenderPipeline pipeline = CoreShaderManager.getInstance().getPipeline(MSDF_PIPELINE_ID);
        if (pipeline == null) {
            pipeline = class_10799.field_56883;
        }

        class_1044 texture = class_310.method_1551().method_1531().method_4619(atlas.textureId);
        if (texture != null && texture.method_71659() != null && texture.method_75484() != null) {
            class_11231 textureSetup = class_11231.method_70900(texture.method_71659(), texture.method_75484());
            float renderScale = scale * atlas.metricScale;
            float cursorX = x;
            float cursorY = y + -0.75F * scale;
            float lineHeightScaled = atlas.lineHeight * renderScale;

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\n') {
                    cursorX = x;
                    cursorY += lineHeightScaled;
                } else {
                    MSDFFontRenderer.Glyph glyph = atlas.getGlyph(c);
                    if (glyph == null) {
                        cursorX += atlas.spaceAdvance * renderScale;
                    } else {
                        float quadX = cursorX - 24.0F * renderScale;
                        float quadY = cursorY - 24.0F * renderScale;
                        float quadW = (glyph.width + 48) * renderScale;
                        float quadH = (glyph.height + 48) * renderScale;
                        int x1 = (int)Math.floor(quadX);
                        int y1 = (int)Math.floor(quadY);
                        int x2 = (int)Math.ceil(quadX + quadW);
                        int y2 = (int)Math.ceil(quadY + quadH);
                        if (x2 > x1 && y2 > y1) {
                            float u1 = Math.max(0.0F, (float)(glyph.u - 24) / atlas.width);
                            float u2 = Math.min(1.0F, (float)(glyph.u + glyph.width + 24) / atlas.width);
                            float v1 = Math.max(0.0F, (float)(glyph.v - 24) / atlas.height);
                            float v2 = Math.min(1.0F, (float)(glyph.v + glyph.height + 24) / atlas.height);
                            context.field_59826
                                .method_70919(
                                    new class_11241(
                                        pipeline,
                                        textureSetup,
                                        new Matrix3x2f(context.method_51448()),
                                        x1,
                                        y1,
                                        x2,
                                        y2,
                                        u1,
                                        u2,
                                        v1,
                                        v2,
                                        color,
                                        context.field_44659.method_70863()
                                    )
                                );
                        }

                        cursorX += glyph.advance * renderScale;
                    }
                }
            }
        }
    }

    private static MSDFFontRenderer.PendingAtlas buildAtlasOffThread(class_2960 fontId) throws IOException, FontFormatException {
        class_2960 ttfResource = resolveTtfResource(fontId);
        if (ttfResource == null) {
            return null;
        } else {
            MSDFFontRenderer.PendingAtlas var39;
            try (InputStream stream = openFontStream(ttfResource)) {
                Font font = Font.createFont(0, stream).deriveFont(192.0F);
                BufferedImage probe = new BufferedImage(1, 1, 2);
                Graphics2D probeGraphics = probe.createGraphics();
                configureGraphics(probeGraphics);
                probeGraphics.setFont(font);
                FontMetrics metrics = probeGraphics.getFontMetrics();
                int ascent = metrics.getAscent();
                int lineHeight = Math.max(1, metrics.getHeight());
                int[] advances = new int[95];
                int maxAdvance = 1;

                for (int c = 32; c <= 126; c++) {
                    advances[c - 32] = Math.max(1, metrics.charWidth((char)c));
                    if (advances[c - 32] > maxAdvance) {
                        maxAdvance = advances[c - 32];
                    }
                }

                int spaceAdvance = advances[0];
                probeGraphics.dispose();
                int cellWidth = maxAdvance + 48;
                int cellHeight = lineHeight + 48;
                int rows = 6;
                int atlasWidth = nextPowerOfTwo(cellWidth * 16);
                int atlasHeight = nextPowerOfTwo(cellHeight * rows);
                BufferedImage glyphImage = new BufferedImage(atlasWidth, atlasHeight, 2);
                Graphics2D graphics = glyphImage.createGraphics();
                configureGraphics(graphics);
                graphics.setComposite(AlphaComposite.Src);
                graphics.setColor(new Color(0, 0, 0, 0));
                graphics.fillRect(0, 0, atlasWidth, atlasHeight);
                graphics.setFont(font);
                graphics.setColor(Color.WHITE);
                Map<Character, MSDFFontRenderer.Glyph> glyphs = new HashMap<>(190);

                for (int index = 0; index < 95; index++) {
                    int cx = 32 + index;
                    char ch = (char)cx;
                    int row = index / 16;
                    int col = index % 16;
                    int cellX = col * cellWidth;
                    int cellY = row * cellHeight;
                    int drawX = cellX + 24;
                    int drawY = cellY + 24 + ascent;
                    int advance = advances[index];
                    graphics.drawString(String.valueOf(ch), drawX, drawY);
                    glyphs.put(ch, new MSDFFontRenderer.Glyph(drawX, cellY + 24, advance, lineHeight, advance));
                }

                graphics.dispose();
                int[] pixels = glyphImage.getRGB(0, 0, atlasWidth, atlasHeight, null, 0, atlasWidth);
                float[] sdf = computeSDFParallel(pixels, atlasWidth, atlasHeight, 16);
                class_1011 nativeImage = new class_1011(atlasWidth, atlasHeight, true);

                for (int py = 0; py < atlasHeight; py++) {
                    for (int px = 0; px < atlasWidth; px++) {
                        int i = py * atlasWidth + px;
                        int value = Math.max(0, Math.min(255, Math.round(sdf[i] * 255.0F)));
                        nativeImage.method_61941(px, py, 0xFF000000 | value << 16 | value << 8 | value);
                    }
                }

                float metricScale = 9.0F / lineHeight;
                var39 = new MSDFFontRenderer.PendingAtlas(nativeImage, atlasWidth, atlasHeight, lineHeight, spaceAdvance, metricScale, glyphs);
            }

            return var39;
        }
    }

    private static float[] computeSDFParallel(int[] argbPixels, int width, int height, int spread) {
        boolean[] inside = new boolean[width * height];

        for (int i = 0; i < argbPixels.length; i++) {
            inside[i] = (argbPixels[i] >>> 24 & 0xFF) > 127;
        }

        float[] sdf = new float[width * height];
        float spreadF = spread;
        int searchRadius = spread + 2;

        try {
            SDF_POOL.submit(() -> IntStream.range(0, height).parallel().forEach(py -> {
                for (int px = 0; px < width; px++) {
                    int idx = py * width + px;
                    boolean isInside = inside[idx];
                    float minDistSq = spreadF * spreadF + 1.0F;
                    int x0 = Math.max(0, px - searchRadius);
                    int x1 = Math.min(width - 1, px + searchRadius);
                    int y0 = Math.max(0, py - searchRadius);
                    int y1 = Math.min(height - 1, py + searchRadius);

                    for (int sy = y0; sy <= y1; sy++) {
                        int rowBase = sy * width;

                        for (int sx = x0; sx <= x1; sx++) {
                            if (inside[rowBase + sx] != isInside) {
                                float dx = sx - px;
                                float dy = sy - py;
                                float dSq = dx * dx + dy * dy;
                                if (dSq < minDistSq) {
                                    minDistSq = dSq;
                                }
                            }
                        }
                    }

                    float dist = (float)Math.sqrt(minDistSq);
                    sdf[idx] = isInside ? 0.5F + 0.5F * Math.min(dist / spreadF, 1.0F) : 0.5F - 0.5F * Math.min(dist / spreadF, 1.0F);
                }
            })).get();
            return sdf;
        } catch (Exception var9) {
            return computeSDFFallback(inside, width, height, spreadF, searchRadius);
        }
    }

    private static float[] computeSDFFallback(boolean[] inside, int width, int height, float spreadF, int searchRadius) {
        float[] sdf = new float[width * height];

        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                int idx = py * width + px;
                boolean isInside = inside[idx];
                float minDistSq = spreadF * spreadF + 1.0F;
                int x0 = Math.max(0, px - searchRadius);
                int x1 = Math.min(width - 1, px + searchRadius);
                int y0 = Math.max(0, py - searchRadius);
                int y1 = Math.min(height - 1, py + searchRadius);

                for (int sy = y0; sy <= y1; sy++) {
                    int rowBase = sy * width;

                    for (int sx = x0; sx <= x1; sx++) {
                        if (inside[rowBase + sx] != isInside) {
                            float dx = sx - px;
                            float dy = sy - py;
                            float dSq = dx * dx + dy * dy;
                            if (dSq < minDistSq) {
                                minDistSq = dSq;
                            }
                        }
                    }
                }

                float dist = (float)Math.sqrt(minDistSq);
                sdf[idx] = isInside ? 0.5F + 0.5F * Math.min(dist / spreadF, 1.0F) : 0.5F - 0.5F * Math.min(dist / spreadF, 1.0F);
            }
        }

        return sdf;
    }

    private static InputStream openFontStream(class_2960 resourceId) throws IOException {
        class_310 client = class_310.method_1551();
        if (client != null && client.method_1478() != null) {
            Optional<class_3298> optional = client.method_1478().method_14486(resourceId);
            if (optional.isPresent()) {
                return optional.get().method_14482();
            }
        }

        String classpathPath = "/assets/" + resourceId.method_12836() + "/" + resourceId.method_12832();
        InputStream classpathStream = MSDFFontRenderer.class.getResourceAsStream(classpathPath);
        if (classpathStream != null) {
            return classpathStream;
        } else {
            throw new IOException("Missing font resource " + resourceId);
        }
    }

    private static void configureGraphics(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    private static void configureTextureFilter(class_1043 texture) {
        try {
            Method blurAndMipmap = texture.getClass().getMethod("setFilter", boolean.class, boolean.class);
            blurAndMipmap.invoke(texture, true, false);
        } catch (ReflectiveOperationException var3) {
            try {
                Method blurOnly = texture.getClass().getMethod("setFilter", boolean.class);
                blurOnly.invoke(texture, true);
            } catch (ReflectiveOperationException var2) {
            }
        }
    }

    private static class_2960 resolveTtfResource(class_2960 fontId) {
        String path = fontId.method_12832();
        if ("sans".equals(path)) {
            return class_2960.method_60655("dynamic_fps", "font/opensans-regular.ttf");
        } else if ("semibold".equals(path)) {
            return class_2960.method_60655("dynamic_fps", "font/semibold.ttf");
        } else {
            return "cyemer".equals(path) ? class_2960.method_60655("dynamic_fps", "font/font.ttf") : null;
        }
    }

    private static int nextPowerOfTwo(int value) {
        int size = 1;

        while (size < value) {
            size <<= 1;
        }

        return size;
    }

    private static int toShadowColor(int argb) {
        int alpha = argb >>> 24 & 0xFF;
        int shadowAlpha = Math.max(0, Math.min(255, Math.round(alpha * 0.45F)));
        return shadowAlpha << 24;
    }

    @Environment(EnvType.CLIENT)
    private record FontAtlas(
        class_2960 textureId, int width, int height, int lineHeight, int spaceAdvance, float metricScale, Map<Character, MSDFFontRenderer.Glyph> glyphs
    ) {
        private MSDFFontRenderer.Glyph getGlyph(char c) {
            MSDFFontRenderer.Glyph glyph = this.glyphs.get(c);
            return glyph != null ? glyph : this.glyphs.get('?');
        }
    }

    @Environment(EnvType.CLIENT)
    private record Glyph(int u, int v, int width, int height, int advance) {
    }

    @Environment(EnvType.CLIENT)
    private record PendingAtlas(
        class_1011 nativeImage,
        int atlasWidth,
        int atlasHeight,
        int lineHeight,
        int spaceAdvance,
        float metricScale,
        Map<Character, MSDFFontRenderer.Glyph> glyphs
    ) {
    }
}
