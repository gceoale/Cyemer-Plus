package com.slither.cyemer.module.implementation;

import com.slither.cyemer.gui.new_ui.HudEditorScreen;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class HudEditor extends Module {
    public HudEditor() {
        super("HudEditor", "Opens the HUD editor screen.", Category.CLIENT);
    }

    @Override
    public void onEnable() {
        if (this.mc != null) {
            this.mc.method_1507(new HudEditorScreen(this.mc.field_1755));
        }

        this.toggle();
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onTick() {
    }
}
