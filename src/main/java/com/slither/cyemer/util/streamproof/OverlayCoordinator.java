package com.slither.cyemer.util.streamproof;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

/**
 * Static coordinator for the stream-proof overlay pipeline. Owns the
 * OverlayWindow + OverlayFramebuffer and mediates the per-frame flow:
 *
 *  1. blitAndClearForFrame() runs at the very start of each HUD render
 *     pass. It blits the previous frame's accumulated cyemer content
 *     into the overlay window, then clears the FBO to transparent so
 *     this frame starts empty.
 *  2. beginRedirect() / endRedirect() bracket every cyemer render entry
 *     point (both HudRenderCallback registrations and cyemer's Screen
 *     subclasses). Everything drawn between them lands on the FBO
 *     instead of MC's main framebuffer.
 */
@Environment(EnvType.CLIENT)
public final class OverlayCoordinator {
    private static final OverlayWindow WINDOW = new OverlayWindow();
    private static final OverlayFramebuffer FRAMEBUFFER = new OverlayFramebuffer();

    private static volatile boolean active = false;
    private static int redirectDepth = 0;

    private OverlayCoordinator() {}

    public static synchronized boolean activate(long parentGlfwWindow) {
        if (active) return true;
        if (!WINDOW.create(parentGlfwWindow)) return false;
        active = true;
        return true;
    }

    public static synchronized void deactivate() {
        if (!active) return;
        active = false;
        redirectDepth = 0;
        WINDOW.destroy();
        FRAMEBUFFER.destroy();
    }

    public static boolean isActive() {
        return active && WINDOW.isAlive();
    }

    /** Called from the earliest per-frame hook. Blits + clears for the new frame. */
    public static void blitAndClearForFrame() {
        if (!isActive()) return;
        ensureFbSized();
        if (FRAMEBUFFER.isReady()) {
            WINDOW.blitFrom(FRAMEBUFFER.getTextureId(), FRAMEBUFFER.getWidth(), FRAMEBUFFER.getHeight());
            FRAMEBUFFER.clear();
        }
        WINDOW.syncGeometry();
    }

    /** Bracket every cyemer render entry point with these. Nest-safe. */
    public static void beginRedirect() {
        if (!isActive()) return;
        ensureFbSized();
        if (!FRAMEBUFFER.isReady()) return;
        if (redirectDepth == 0) {
            FRAMEBUFFER.beginWrite();
        }
        redirectDepth++;
    }

    public static void endRedirect() {
        if (!isActive()) return;
        if (!FRAMEBUFFER.isReady()) return;
        if (redirectDepth <= 0) return;
        redirectDepth--;
        if (redirectDepth == 0) {
            FRAMEBUFFER.endWrite();
        }
    }

    private static void ensureFbSized() {
        long mainWindow = GLFW.glfwGetCurrentContext();
        if (mainWindow == 0L) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(mainWindow, w, h);
            FRAMEBUFFER.ensureCreated(w.get(0), h.get(0));
        }
    }
}
