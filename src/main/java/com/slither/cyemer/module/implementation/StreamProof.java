package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.streamproof.OverlayWindow;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.class_310;

/**
 * Phase 1 of the "hide from OBS" work. On enable, spawns a second GLFW
 * window that shadows the main Minecraft window and gets marked with
 * NSWindow sharingType=None so it's invisible to screen capture. The
 * overlay currently paints a translucent tint every frame so you can
 * eyeball whether OBS actually skips it — no real GUI content is routed
 * through it yet. That's phase 2.
 */
@Environment(EnvType.CLIENT)
public class StreamProof extends Module {
    private static final OverlayWindow OVERLAY = new OverlayWindow();
    private static boolean callbackRegistered = false;

    public StreamProof() {
        super("StreamProof",
                "Phase 1 - creates an OBS-invisible overlay window with a test tint. Once verified, phase 2 routes the real GUI through it.",
                Category.CLIENT);
    }

    @Override
    public void onEnable() {
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.method_22683() == null) {
            return;
        }
        if (!OVERLAY.isAlive()) {
            OVERLAY.create(mc.method_22683().method_4490());
        }
        registerCallback();
    }

    @Override
    public void onDisable() {
        if (OVERLAY.isAlive()) {
            OVERLAY.destroy();
        }
    }

    private static void registerCallback() {
        if (callbackRegistered) return;
        callbackRegistered = true;
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!OVERLAY.isAlive()) return;
            OVERLAY.syncGeometry();
            OVERLAY.renderTestFrame();
        });
    }
}
