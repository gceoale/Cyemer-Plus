package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.streamproof.MacOSCapture;
import com.slither.cyemer.util.streamproof.StreamProofPresenter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
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
        if (!MacOSCapture.isAvailable()) {
            // Hiding a window from capture is done through Cocoa; there is no
            // Windows or Linux path yet, so say so instead of failing.
            if (mc.field_1724 != null) {
                mc.field_1724.method_7353(class_2561.method_43470("§c[StreamProof]§r §7only works on macOS for now"), false);
            }
            this.setEnabled(false);
            return;
        }
        try {
            if (!StreamProofPresenter.enable(mc.method_22683().method_4490())) {
                this.setEnabled(false);
            }
        } catch (Throwable t) {
            this.setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        StreamProofPresenter.disable();
    }
}
