package com.slither.cyemer.util.streamproof;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_437;

import java.lang.reflect.Field;

/**
 * Owns the backup GpuTexture / GpuTextureView pair and coordinates the
 * present-time substitution:
 *
 *  - When there is no cyemer screen open: copy MC's live main texture
 *    into the backup so the backup always holds a fresh "no cyemer"
 *    snapshot. Present MC's live view unchanged.
 *
 *  - When a cyemer screen is open: extract the raw GL texture id of
 *    MC's live main texture (class_10868.field_57882) via reflection,
 *    blit that into the OBS-invisible overlay window (user sees this),
 *    then present the BACKUP view - MC's window shows the last no-cyemer
 *    snapshot, which is what OBS captures.
 *
 * Called from the @Redirect mixin on class_276.method_1237's
 * CommandEncoder.presentTexture invocation.
 */
@Environment(EnvType.CLIENT)
public final class StreamProofPresenter {
    private static final OverlayWindow OVERLAY = new OverlayWindow();

    private static volatile boolean enabled = false;
    private static GpuTexture backupTexture = null;
    private static GpuTextureView backupView = null;
    private static int backupWidth = 0;
    private static int backupHeight = 0;

    // Reflection cache for class_10868.field_57882 (raw GL texture id).
    private static Field glIdField = null;

    private StreamProofPresenter() {}

    public static synchronized boolean enable(long parentGlfwWindow) {
        if (enabled) return true;
        if (!OVERLAY.create(parentGlfwWindow)) return false;
        enabled = true;
        return true;
    }

    public static synchronized void disable() {
        if (!enabled) return;
        enabled = false;
        OVERLAY.setVisible(false);
        OVERLAY.destroy();
        disposeBackup();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Called by mixin @Redirect on class_276.method_1237's presentTexture
     * invocation. Decides whether to present the live view (normal) or a
     * frozen backup view (while cyemer screen is open), and if the
     * latter, also blits the live texture into the overlay window.
     */
    public static void present(CommandEncoder encoder, GpuTextureView liveView) {
        if (!enabled) {
            encoder.presentTexture(liveView);
            return;
        }

        class_310 mc = class_310.method_1551();
        class_437 screen = mc == null ? null : mc.field_1755;
        boolean cyemerScreen = screen != null && screen.getClass().getName().startsWith("com.slither.cyemer.");

        GpuTexture liveTex = viewTexture(liveView);
        if (liveTex == null) {
            encoder.presentTexture(liveView);
            return;
        }

        int liveW = liveTex.getWidth(0);
        int liveH = liveTex.getHeight(0);
        ensureBackup(liveTex, liveW, liveH);

        if (cyemerScreen && backupView != null) {
            // Show the live main to the user via the overlay window (macOS
            // sharingType=None -> OBS can't see it).
            int glId = extractGlId(liveTex);
            if (glId > 0) {
                OVERLAY.syncGeometry();
                OVERLAY.setVisible(true);
                OVERLAY.blitFromGlTexture(glId, liveW, liveH);
            }
            // OBS: hand it the last no-cyemer snapshot.
            encoder.presentTexture(backupView);
        } else {
            // No cyemer screen right now - refresh the backup with the
            // current live texture and present live as normal.
            OVERLAY.setVisible(false);
            if (backupTexture != null) {
                try {
                    encoder.copyTextureToTexture(liveTex, backupTexture, 0, 0, 0, 0, 0, liveW, liveH);
                } catch (Throwable t) {
                    // Some formats or usage flags may reject the copy;
                    // fall through to normal presentation.
                }
            }
            encoder.presentTexture(liveView);
        }
    }

    private static void ensureBackup(GpuTexture template, int w, int h) {
        if (backupTexture != null && backupWidth == w && backupHeight == h) return;
        disposeBackup();
        backupWidth = w;
        backupHeight = h;
        int usage = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC
                | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT;
        try {
            backupTexture = RenderSystem.getDevice().createTexture(
                    "cyemer_streamproof_backup", usage, template.getFormat(), w, h, 1, 1);
            backupView = RenderSystem.getDevice().createTextureView(backupTexture);
        } catch (Throwable t) {
            System.err.println("[cyemer/streamproof] createTexture failed: " + t);
            backupTexture = null;
            backupView = null;
        }
    }

    private static void disposeBackup() {
        try {
            if (backupTexture != null) backupTexture.close();
        } catch (Throwable ignored) {}
        backupTexture = null;
        backupView = null;
        backupWidth = 0;
        backupHeight = 0;
    }

    private static GpuTexture viewTexture(GpuTextureView view) {
        if (view == null) return null;
        // GpuTextureView has protected/package field for its underlying texture.
        // Try a couple of common names via reflection.
        try {
            for (Field f : view.getClass().getSuperclass().getDeclaredFields()) {
                if (GpuTexture.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return (GpuTexture) f.get(view);
                }
            }
            for (Field f : view.getClass().getDeclaredFields()) {
                if (GpuTexture.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return (GpuTexture) f.get(view);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static int extractGlId(GpuTexture tex) {
        try {
            if (glIdField == null) {
                // class_10868 has "field_57882" holding the raw GL texture id.
                Field candidate = null;
                for (Field f : tex.getClass().getDeclaredFields()) {
                    if (f.getType() == int.class && "field_57882".equals(f.getName())) {
                        candidate = f;
                        break;
                    }
                }
                // Fallback: first int field.
                if (candidate == null) {
                    for (Field f : tex.getClass().getDeclaredFields()) {
                        if (f.getType() == int.class) {
                            candidate = f;
                            break;
                        }
                    }
                }
                if (candidate == null) return 0;
                candidate.setAccessible(true);
                glIdField = candidate;
            }
            return glIdField.getInt(tex);
        } catch (Throwable t) {
            return 0;
        }
    }
}
