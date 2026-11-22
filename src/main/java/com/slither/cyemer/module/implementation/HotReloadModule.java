package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.HotReload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;

/**
 * Loads a new Cyemer build without restarting the game. Turning it on loads
 * the installed build if it is newer than the one running; while it is on,
 * every new build `gradlew build` installs is loaded within a couple of
 * seconds by itself. Reload Now loads the installed build regardless.
 *
 * Module code reloads; core code (mixins, the GUI, managers, setting types)
 * needs a restart, and it says so when a build changes any.
 */
@Environment(EnvType.CLIENT)
public class HotReloadModule extends Module {
    private final BooleanSetting autoReload = new BooleanSetting("Auto Reload", true);
    /** A button: turning it on reloads and turns itself back off. */
    private final BooleanSetting reloadNow = new BooleanSetting("Reload Now", false);
    private int ticks = 0;

    public HotReloadModule() {
        super("HotReload", "Loads a new Cyemer build into the running game, no restart.", Category.CLIENT);
        this.addSetting(this.autoReload);
        this.addSetting(this.reloadNow);
    }

    @Override
    public void onEnable() {
        if (!HotReload.available()) {
            this.say("§cHotReload needs Cyemer running from its jar in the mods folder");
        } else if (HotReload.newBuildReady()) {
            HotReload.request();
        }
    }

    @Override
    public void onTick() {
        if (this.reloadNow.isEnabled()) {
            this.reloadNow.setEnabled(false);
            HotReload.request();
            return;
        }
        if (this.autoReload.isEnabled() && ++this.ticks % 20 == 0 && HotReload.newBuildReady()) {
            this.say("§7New build installed, loading it...");
            HotReload.request();
        }
    }

    private void say(String message) {
        if (this.mc.field_1724 != null) {
            this.mc.field_1724.method_7353(class_2561.method_43470("§b[Cyemer]§r " + message), false);
        }
    }
}
