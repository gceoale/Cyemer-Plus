package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.streamproof.OverlayCoordinator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;

/**
 * Toggle for the "hide from screen capture" pipeline. When enabled the
 * OverlayCoordinator brings up the second GLFW window and every cyemer
 * render entry point (HudRenderCallback wrappers + ScreenMixin) diverts
 * its drawing into the offscreen framebuffer that the overlay window
 * displays.
 */
@Environment(EnvType.CLIENT)
public class StreamProof extends Module {
    private static StreamProof instance;

    public StreamProof() {
        super("StreamProof", "Hides the cyemer GUI from OBS / screen capture on macOS.", Category.CLIENT);
        instance = this;
    }

    @Override
    public void onEnable() {
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.method_22683() == null) return;
        OverlayCoordinator.activate(mc.method_22683().method_4490());
    }

    @Override
    public void onDisable() {
        OverlayCoordinator.deactivate();
    }

    public static boolean isActiveStatic() {
        return instance != null && instance.isEnabled();
    }
}
