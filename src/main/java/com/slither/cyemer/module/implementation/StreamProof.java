package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.streamproof.StreamProofPresenter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;

/**
 * Toggles the surgical present-time substitution. When enabled and a
 * cyemer screen is up, the OBS-visible main window shows the last
 * "no cyemer" snapshot while the user sees the live game + ClickGUI
 * via a capture-excluded overlay window.
 */
@Environment(EnvType.CLIENT)
public class StreamProof extends Module {
    public StreamProof() {
        super("StreamProof",
                "Freezes OBS on the pre-open frame while a cyemer screen is up; you keep seeing the live game + GUI via a capture-excluded overlay.",
                Category.CLIENT);
    }

    @Override
    public void onEnable() {
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.method_22683() == null) return;
        StreamProofPresenter.enable(mc.method_22683().method_4490());
    }

    @Override
    public void onDisable() {
        StreamProofPresenter.disable();
    }
}
