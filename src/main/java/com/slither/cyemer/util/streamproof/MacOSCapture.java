package com.slither.cyemer.util.streamproof;

import com.sun.jna.Function;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Thin JNA bridge to the ObjC runtime used to talk to AppKit from Java.
 * Provides the two calls we need to make an NSWindow invisible to screen
 * capture on macOS: setSharingType:NSWindowSharingNone (0) and
 * setIgnoresMouseEvents:YES so the overlay lets clicks fall through.
 */
@Environment(EnvType.CLIENT)
public final class MacOSCapture {
    private static final boolean AVAILABLE = System.getProperty("os.name", "").toLowerCase().contains("mac");
    private static final NativeLibrary OBJC = AVAILABLE ? NativeLibrary.getInstance("objc.A") : null;
    private static final Function OBJC_MSG_SEND = AVAILABLE ? OBJC.getFunction("objc_msgSend") : null;
    private static final Function SEL_REGISTER = AVAILABLE ? OBJC.getFunction("sel_registerName") : null;

    private static final Pointer SET_SHARING_TYPE = AVAILABLE ? selector("setSharingType:") : null;
    private static final Pointer SET_IGNORES_MOUSE_EVENTS = AVAILABLE ? selector("setIgnoresMouseEvents:") : null;
    private static final Pointer SET_LEVEL = AVAILABLE ? selector("setLevel:") : null;

    private MacOSCapture() {}

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    /** NSWindowSharingType.NSWindowSharingNone = 0 — hides the window from screen capture APIs. */
    public static void excludeFromCapture(long nsWindow) {
        if (!AVAILABLE || nsWindow == 0L) return;
        OBJC_MSG_SEND.invoke(new Object[]{new Pointer(nsWindow), SET_SHARING_TYPE, 0L});
    }

    /** Undo excludeFromCapture — NSWindowSharingReadOnly = 1 is the default. */
    public static void restoreCapture(long nsWindow) {
        if (!AVAILABLE || nsWindow == 0L) return;
        OBJC_MSG_SEND.invoke(new Object[]{new Pointer(nsWindow), SET_SHARING_TYPE, 1L});
    }

    /** Click-through: all mouse events pass to the window below. */
    public static void setIgnoresMouseEvents(long nsWindow, boolean ignore) {
        if (!AVAILABLE || nsWindow == 0L) return;
        OBJC_MSG_SEND.invoke(new Object[]{new Pointer(nsWindow), SET_IGNORES_MOUSE_EVENTS, ignore ? 1L : 0L});
    }

    /** NSStatusWindowLevel (~25) keeps the overlay above ordinary windows including the game. */
    public static void setWindowLevel(long nsWindow, long level) {
        if (!AVAILABLE || nsWindow == 0L) return;
        OBJC_MSG_SEND.invoke(new Object[]{new Pointer(nsWindow), SET_LEVEL, level});
    }

    private static Pointer selector(String name) {
        return (Pointer) SEL_REGISTER.invoke(Pointer.class, new Object[]{name});
    }
}
