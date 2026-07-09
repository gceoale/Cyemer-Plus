package com.slither.cyemer.util.streamproof;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

/**
 * Offscreen framebuffer + color texture that cyemer's GUI draws into
 * instead of the main window's framebuffer. Kept sized to match the
 * main GLFW window's framebuffer so DrawContext math continues to work
 * unchanged.
 */
@Environment(EnvType.CLIENT)
public final class OverlayFramebuffer {
    private int fboId = 0;
    private int textureId = 0;
    private int depthRbo = 0;
    private int width = 0;
    private int height = 0;

    private int savedFbo = 0;
    private final int[] savedViewport = new int[4];

    public void ensureCreated(int w, int h) {
        if (w <= 0 || h <= 0) return;
        if (this.fboId == 0) {
            this.fboId = GL30C.glGenFramebuffers();
            this.textureId = GL30C.glGenTextures();
            this.depthRbo = GL30C.glGenRenderbuffers();
        }
        if (w != this.width || h != this.height) {
            this.width = w;
            this.height = h;

            GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, this.textureId);
            GL30C.glTexImage2D(GL30C.GL_TEXTURE_2D, 0, GL30C.GL_RGBA8, w, h, 0,
                    GL30C.GL_RGBA, GL30C.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
            GL30C.glTexParameteri(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MIN_FILTER, GL30C.GL_LINEAR);
            GL30C.glTexParameteri(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MAG_FILTER, GL30C.GL_LINEAR);
            GL30C.glTexParameteri(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_WRAP_S, GL30C.GL_CLAMP_TO_EDGE);
            GL30C.glTexParameteri(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_WRAP_T, GL30C.GL_CLAMP_TO_EDGE);
            GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, 0);

            GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, this.depthRbo);
            GL30C.glRenderbufferStorage(GL30C.GL_RENDERBUFFER, GL30C.GL_DEPTH_COMPONENT24, w, h);
            GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, 0);

            int prior = GL30C.glGetInteger(GL30C.GL_FRAMEBUFFER_BINDING);
            GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, this.fboId);
            GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0,
                    GL30C.GL_TEXTURE_2D, this.textureId, 0);
            GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT,
                    GL30C.GL_RENDERBUFFER, this.depthRbo);
            int status = GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER);
            GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, prior);
            if (status != GL30C.GL_FRAMEBUFFER_COMPLETE) {
                System.err.println("[cyemer/streamproof] framebuffer incomplete: 0x" + Integer.toHexString(status));
            }
        }
    }

    /** Bind the FBO, save prior state so endWrite can restore it. */
    public void beginWrite() {
        if (this.fboId == 0) return;
        this.savedFbo = GL30C.glGetInteger(GL30C.GL_FRAMEBUFFER_BINDING);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer vp = stack.mallocInt(4);
            GL30C.glGetIntegerv(GL30C.GL_VIEWPORT, vp);
            for (int i = 0; i < 4; i++) this.savedViewport[i] = vp.get(i);
        }
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, this.fboId);
        GL30C.glViewport(0, 0, this.width, this.height);
    }

    public void endWrite() {
        if (this.fboId == 0) return;
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, this.savedFbo);
        GL30C.glViewport(this.savedViewport[0], this.savedViewport[1],
                this.savedViewport[2], this.savedViewport[3]);
    }

    /** Clear the FBO to fully transparent so cyemer can accumulate content from scratch. */
    public void clear() {
        if (this.fboId == 0) return;
        int prior = GL30C.glGetInteger(GL30C.GL_FRAMEBUFFER_BINDING);
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, this.fboId);
        GL30C.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        GL30C.glClear(GL30C.GL_COLOR_BUFFER_BIT | GL30C.GL_DEPTH_BUFFER_BIT);
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, prior);
    }

    public void destroy() {
        if (this.fboId != 0) GL30C.glDeleteFramebuffers(this.fboId);
        if (this.textureId != 0) GL30C.glDeleteTextures(this.textureId);
        if (this.depthRbo != 0) GL30C.glDeleteRenderbuffers(this.depthRbo);
        this.fboId = 0;
        this.textureId = 0;
        this.depthRbo = 0;
        this.width = 0;
        this.height = 0;
    }

    public int getFboId() { return this.fboId; }
    public int getTextureId() { return this.textureId; }
    public int getWidth() { return this.width; }
    public int getHeight() { return this.height; }
    public boolean isReady() { return this.fboId != 0; }
}
