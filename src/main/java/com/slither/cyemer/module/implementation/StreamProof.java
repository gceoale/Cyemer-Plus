package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.streamproof.MacOSCapture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFWNativeCocoa;

/**
 * Quick-and-dirty capture blackout: when a cyemer screen is open, flip
 * the Minecraft main window's NSWindow sharingType to None so OBS can't
 * capture it. The whole game goes dark in OBS for the duration - not
 * surgical, but it's fast to do and it works on 1.21.11 without wrestling
 * the deferred GUI renderer.
 *
 * Detection is by class package (com.slither.cyemer.*) so vanilla screens
 * (inventory, pause menu, chat) and other mods' screens don't trigger it.
 */
@Environment(EnvType.CLIENT)
public class StreamProof extends Module {
    private boolean hiding = false;
    private long cachedNsWindow = 0L;

    public StreamProof() {
        super("StreamProof",
                "Blacks out the whole MC window in OBS while a cyemer screen is open.",
                Category.CLIENT);
    }

    @Override
    public void onDisable() {
        if (this.hiding && this.cachedNsWindow != 0L) {
            MacOSCapture.restoreCapture(this.cachedNsWindow);
            this.hiding = false;
        }
    }

    @Override
    public void onTick() {
        if (this.mc == null || this.mc.method_22683() == null) return;
        if (this.cachedNsWindow == 0L) {
            long glfwHandle = this.mc.method_22683().method_4490();
            if (glfwHandle == 0L) return;
            this.cachedNsWindow = GLFWNativeCocoa.glfwGetCocoaWindow(glfwHandle);
            if (this.cachedNsWindow == 0L) return;
        }

        class_437 screen = this.mc.field_1755;
        boolean shouldHide = screen != null && this.isCyemerScreen(screen);

        if (shouldHide && !this.hiding) {
            MacOSCapture.excludeFromCapture(this.cachedNsWindow);
            this.hiding = true;
        } else if (!shouldHide && this.hiding) {
            MacOSCapture.restoreCapture(this.cachedNsWindow);
            this.hiding = false;
        }
    }

    private boolean isCyemerScreen(class_437 screen) {
        return screen.getClass().getName().startsWith("com.slither.cyemer.");
    }
}
