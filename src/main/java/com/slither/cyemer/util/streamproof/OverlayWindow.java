package com.slither.cyemer.util.streamproof;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeCocoa;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

/**
 * Second GLFW window that shadows the main MC window with a shared GL
 * context. Since it shares the context, GL texture IDs created against
 * the main MC context (including class_10868 - MC's OpenGL GpuTexture
 * impl, whose raw id lives in field_57882) are reachable here.
 *
 * Per-frame blit: attach the passed GL texture id as color attachment on
 * a read-side FBO, glBlitFramebuffer that into the overlay's default
 * framebuffer, swap. glfwSwapInterval(0) so it doesn't drag the whole
 * client to the compositor's cadence.
 */
@Environment(EnvType.CLIENT)
public final class OverlayWindow {
    private long handle = 0L;
    private long parentHandle = 0L;
    private long nsWindow = 0L;
    private int readFbo = 0;
    private int lastAttachedTexture = 0;
    private boolean visible = false;
    private boolean overlayGlInitialized = false;

    public boolean create(long parentGlfwWindow) {
        if (this.handle != 0L) return true;
        this.parentHandle = parentGlfwWindow;
        long previousContext = GLFW.glfwGetCurrentContext();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            GLFW.glfwGetWindowSize(parentGlfwWindow, w, h);

            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_TRANSPARENT_FRAMEBUFFER, GLFW.GLFW_TRUE);
            GLFW.glfwWindowHint(GLFW.GLFW_FLOATING, GLFW.GLFW_TRUE);
            GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

            this.handle = GLFW.glfwCreateWindow(w.get(0), h.get(0), "", 0L, parentGlfwWindow);
        }

        if (this.handle == 0L) {
            System.err.println("[cyemer/streamproof] glfwCreateWindow returned NULL");
            GLFW.glfwMakeContextCurrent(previousContext);
            return false;
        }

        this.nsWindow = GLFWNativeCocoa.glfwGetCocoaWindow(this.handle);
        MacOSCapture.excludeFromCapture(this.nsWindow);
        MacOSCapture.setIgnoresMouseEvents(this.nsWindow, true);
        MacOSCapture.setWindowLevel(this.nsWindow, 25L);

        this.syncGeometry();
        GLFW.glfwMakeContextCurrent(previousContext);
        return true;
    }

    public void syncGeometry() {
        if (this.handle == 0L) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer px = stack.mallocInt(1);
            IntBuffer py = stack.mallocInt(1);
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            GLFW.glfwGetWindowPos(this.parentHandle, px, py);
            GLFW.glfwGetWindowSize(this.parentHandle, w, h);
            GLFW.glfwSetWindowPos(this.handle, px.get(0), py.get(0));
            GLFW.glfwSetWindowSize(this.handle, w.get(0), h.get(0));
        }
    }

    public void setVisible(boolean visible) {
        if (this.handle == 0L || this.visible == visible) return;
        if (visible) GLFW.glfwShowWindow(this.handle);
        else GLFW.glfwHideWindow(this.handle);
        this.visible = visible;
    }

    public void blitFromGlTexture(int glTextureId, int srcWidth, int srcHeight) {
        if (this.handle == 0L || glTextureId == 0) return;
        long previousContext = GLFW.glfwGetCurrentContext();
        // Flush the main context's writes so the shared texture actually has
        // the frame content before we start reading it from the overlay
        // context. Without this we can see stale / uninitialized pixels.
        GL30C.glFlush();
        GLFW.glfwMakeContextCurrent(this.handle);
        if (!this.overlayGlInitialized) {
            GL.createCapabilities();
            GLFW.glfwSwapInterval(0);
            this.readFbo = GL30C.glGenFramebuffers();
            this.overlayGlInitialized = true;
        }

        GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, this.readFbo);
        if (this.lastAttachedTexture != glTextureId) {
            GL30C.glFramebufferTexture2D(GL30C.GL_READ_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0,
                    GL30C.GL_TEXTURE_2D, glTextureId, 0);
            this.lastAttachedTexture = glTextureId;
        }
        GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, 0);

        int fbw;
        int fbh;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer bw = stack.mallocInt(1);
            IntBuffer bh = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(this.handle, bw, bh);
            fbw = bw.get(0);
            fbh = bh.get(0);
        }
        GL30C.glViewport(0, 0, fbw, fbh);
        GL30C.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        GL30C.glClear(GL30C.GL_COLOR_BUFFER_BIT);
        // MC's render target already stores content in GL's bottom-left origin,
        // so no Y-flip needed on the blit.
        GL30C.glBlitFramebuffer(
                0, 0, srcWidth, srcHeight,
                0, 0, fbw, fbh,
                GL30C.GL_COLOR_BUFFER_BIT, GL30C.GL_LINEAR);
        GLFW.glfwSwapBuffers(this.handle);

        GLFW.glfwMakeContextCurrent(previousContext);
    }

    public void destroy() {
        if (this.handle == 0L) return;
        long current = GLFW.glfwGetCurrentContext();
        if (current == this.handle) GLFW.glfwMakeContextCurrent(0L);
        MacOSCapture.restoreCapture(this.nsWindow);
        GLFW.glfwDestroyWindow(this.handle);
        this.handle = 0L;
        this.nsWindow = 0L;
        this.parentHandle = 0L;
        this.readFbo = 0;
        this.lastAttachedTexture = 0;
        this.visible = false;
        this.overlayGlInitialized = false;
    }

    public boolean isAlive() {
        return this.handle != 0L;
    }
}
