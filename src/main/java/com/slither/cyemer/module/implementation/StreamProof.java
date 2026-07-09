package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.streamproof.FramePresenter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;

/**
 * Enables the freeze-on-cyemer-screen presenter. While a cyemer screen
 * is open the OBS-visible main window is fed a saved snapshot from
 * before it opened (game + vanilla HUD, no cyemer), and the user sees
 * the live game + ClickGUI via the capture-excluded overlay window.
 */
@Environment(EnvType.CLIENT)
public class StreamProof extends Module {
    public StreamProof() {
        super("StreamProof",
                "Hides cyemer screens from OBS. OBS freezes on the pre-open frame while a cyemer screen is up.",
                Category.CLIENT);
    }

    @Override
    public void onEnable() {
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.method_22683() == null) return;
        FramePresenter.enable(mc.method_22683().method_4490());
    }

    @Override
    public void onDisable() {
        FramePresenter.disable();
    }
}
