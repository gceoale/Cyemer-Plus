package com.slither.cyemer.util.streamproof;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

/**
 * Runs right before Minecraft's window.updateDisplay() call. If we're
 * currently sitting on a cyemer screen, copies the live main-window
 * backbuffer to the OverlayWindow (which the user sees) and blits a
 * saved "clean" snapshot on top of the main backbuffer (which is what
 * OBS captures on the next swap). If we're on a normal frame, refreshes
 * the "clean" snapshot from the current backbuffer so it stays in sync.
 *
 * Net effect: while a cyemer screen is open, OBS sees the frame from
 * the moment right before the screen appeared - frozen but no cyemer -
 * and the user sees the live game + ClickGUI via the overlay window on
 * top. When the screen closes, everything unfreezes.
 */
@Environment(EnvType.CLIENT)
public final class FramePresenter {
    private static final OverlayWindow OVERLAY = new OverlayWindow();

    private static volatile boolean enabled = false;
    private static boolean initialized = false;

    private static int snapshotFbo = 0;
    private static int snapshotTexture = 0;
    private static int liveFbo = 0;
    private static int liveTexture = 0;
    private static int width = 0;
    private static int height = 0;
    private static boolean snapshotValid = false;

    private FramePresenter() {}

    public static synchronized boolean enable(long parentGlfwWindow) {
        if (enabled) return true;
        if (!OVERLAY.create(parentGlfwWindow)) return false;
        enabled = true;
        return true;
    }

    public static synchronized void disable() {
        if (!enabled) return;
        enabled = false;
        snapshotValid = false;
        OVERLAY.setVisible(false);
        OVERLAY.destroy();
        destroyFbos();
        initialized = false;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** Invoked right before MinecraftClient calls window.updateDisplay. */
    public static void beforeDisplayUpdate() {
        if (!enabled) return;
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.method_22683() == null) return;

        long mainWindow = mc.method_22683().method_4490();
        if (mainWindow == 0L) return;

        int fbw;
        int fbh;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer bw = stack.mallocInt(1);
            IntBuffer bh = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(mainWindow, bw, bh);
            fbw = bw.get(0);
            fbh = bh.get(0);
        }
        if (fbw <= 0 || fbh <= 0) return;
        ensureFbos(fbw, fbh);

        class_437 screen = mc.field_1755;
        boolean cyemerScreen = screen != null && screen.getClass().getName().startsWith("com.slither.cyemer.");

        int savedRead = GL30C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
        int savedDraw = GL30C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);

        if (cyemerScreen && snapshotValid) {
            // 1. Copy live main backbuffer to the "live" FBO so we can hand it to the overlay.
            GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, 0);
            GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, liveFbo);
            GL30C.glBlitFramebuffer(0, 0, fbw, fbh, 0, 0, fbw, fbh,
                    GL30C.GL_COLOR_BUFFER_BIT, GL30C.GL_NEAREST);

            // 2. Blit the saved snapshot over the main backbuffer - OBS will grab that.
            GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, snapshotFbo);
            GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, 0);
            GL30C.glBlitFramebuffer(0, 0, fbw, fbh, 0, 0, fbw, fbh,
                    GL30C.GL_COLOR_BUFFER_BIT, GL30C.GL_NEAREST);

            // 3. Show the overlay with the live snapshot on top for the user.
            OVERLAY.syncGeometry();
            OVERLAY.setVisible(true);
            OVERLAY.blitFromTexture(liveTexture, fbw, fbh);
        } else {
            OVERLAY.setVisible(false);
            // Refresh snapshot every frame so we always have a fresh "no cyemer" copy handy.
            GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, 0);
            GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, snapshotFbo);
            GL30C.glBlitFramebuffer(0, 0, fbw, fbh, 0, 0, fbw, fbh,
                    GL30C.GL_COLOR_BUFFER_BIT, GL30C.GL_NEAREST);
            snapshotValid = true;
        }

        GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, savedRead);
        GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, savedDraw);
    }

    private static void ensureFbos(int w, int h) {
        if (!initialized) {
            snapshotFbo = GL30C.glGenFramebuffers();
            snapshotTexture = GL30C.glGenTextures();
            liveFbo = GL30C.glGenFramebuffers();
            liveTexture = GL30C.glGenTextures();
            initialized = true;
        }
        if (w != width || h != height) {
            width = w;
            height = h;
            snapshotValid = false;
            attachTexture(snapshotFbo, snapshotTexture, w, h);
            attachTexture(liveFbo, liveTexture, w, h);
        }
    }

    private static void attachTexture(int fbo, int tex, int w, int h) {
        GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, tex);
        GL30C.glTexImage2D(GL30C.GL_TEXTURE_2D, 0, GL30C.GL_RGBA8, w, h, 0,
                GL30C.GL_RGBA, GL30C.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        GL30C.glTexParameteri(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MIN_FILTER, GL30C.GL_LINEAR);
        GL30C.glTexParameteri(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MAG_FILTER, GL30C.GL_LINEAR);
        GL30C.glTexParameteri(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_WRAP_S, GL30C.GL_CLAMP_TO_EDGE);
        GL30C.glTexParameteri(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_WRAP_T, GL30C.GL_CLAMP_TO_EDGE);
        GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, 0);

        int prior = GL30C.glGetInteger(GL30C.GL_FRAMEBUFFER_BINDING);
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, fbo);
        GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0,
                GL30C.GL_TEXTURE_2D, tex, 0);
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, prior);
    }

    private static void destroyFbos() {
        if (snapshotFbo != 0) GL30C.glDeleteFramebuffers(snapshotFbo);
        if (snapshotTexture != 0) GL30C.glDeleteTextures(snapshotTexture);
        if (liveFbo != 0) GL30C.glDeleteFramebuffers(liveFbo);
        if (liveTexture != 0) GL30C.glDeleteTextures(liveTexture);
        snapshotFbo = 0;
        snapshotTexture = 0;
        liveFbo = 0;
        liveTexture = 0;
        width = 0;
        height = 0;
    }
}
