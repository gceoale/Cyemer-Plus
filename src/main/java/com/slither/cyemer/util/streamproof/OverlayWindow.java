package com.slither.cyemer.util.streamproof;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeCocoa;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

/**
 * A second GLFW window that shadows the main Minecraft window. Shares
 * its GL context with the main window. On macOS the underlying NSWindow
 * gets sharingType=None so screen-capture tools (OBS, ScreenCaptureKit)
 * skip it while the user still sees it composited on top.
 *
 * The overlay is capped to no vsync (glfwSwapInterval(0)) so its per-
 * frame swap doesn't drag the main game down to a compositor cadence.
 */
@Environment(EnvType.CLIENT)
public final class OverlayWindow {
    private long handle = 0L;
    private long parentHandle = 0L;
    private long nsWindow = 0L;
    private boolean overlayGlInitialized = false;

    private float clearR = 1.0F;
    private float clearG = 0.15F;
    private float clearB = 0.4F;
    private float clearA = 0.35F;

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
        GLFW.glfwShowWindow(this.handle);
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

    public void renderTestFrame() {
        if (this.handle == 0L) return;
        long previous = GLFW.glfwGetCurrentContext();
        GLFW.glfwMakeContextCurrent(this.handle);
        if (!this.overlayGlInitialized) {
            GL.createCapabilities();
            // Kill vsync on the overlay context so glfwSwapBuffers doesn't drag
            // the main game down to the compositor cadence (that's what capped
            // the whole client to 120 fps last try).
            GLFW.glfwSwapInterval(0);
            this.overlayGlInitialized = true;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer fbw = stack.mallocInt(1);
            IntBuffer fbh = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(this.handle, fbw, fbh);
            org.lwjgl.opengl.GL30C.glViewport(0, 0, fbw.get(0), fbh.get(0));
        }

        org.lwjgl.opengl.GL30C.glClearColor(this.clearR, this.clearG, this.clearB, this.clearA);
        org.lwjgl.opengl.GL30C.glClear(org.lwjgl.opengl.GL30C.GL_COLOR_BUFFER_BIT);
        GLFW.glfwSwapBuffers(this.handle);

        GLFW.glfwMakeContextCurrent(previous);
    }

    public void destroy() {
        if (this.handle == 0L) return;
        long current = GLFW.glfwGetCurrentContext();
        if (current == this.handle) {
            GLFW.glfwMakeContextCurrent(0L);
        }
        MacOSCapture.restoreCapture(this.nsWindow);
        GLFW.glfwDestroyWindow(this.handle);
        this.handle = 0L;
        this.nsWindow = 0L;
        this.parentHandle = 0L;
        this.overlayGlInitialized = false;
    }

    public boolean isAlive() {
        return this.handle != 0L;
    }

    public long getHandle() {
        return this.handle;
    }
}
