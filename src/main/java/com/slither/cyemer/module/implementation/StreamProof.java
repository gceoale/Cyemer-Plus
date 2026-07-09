package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.streamproof.OverlayWindow;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.class_310;

/**
 * Phase 1 - creates the OBS-invisible overlay window with a translucent
 * test tint so the capture-exclusion is visually verifiable. Phase 2
 * (routing cyemer's actual GUI through the overlay) is blocked on MC
 * 1.21.11's deferred GUI renderer; the naive FBO-around-callback trick
 * doesn't work because class_332 draws are queued into class_11246 and
 * flushed elsewhere.
 */
@Environment(EnvType.CLIENT)
public class StreamProof extends Module {
    private static final OverlayWindow OVERLAY = new OverlayWindow();
    private static boolean callbackRegistered = false;

    public StreamProof() {
        super("StreamProof",
                "Hides an overlay window from OBS. Currently phase 1 - shows a test tint; real GUI redirect is still in the oven.",
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
