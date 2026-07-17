package com.slither.cyemer.gui.new_ui.settings;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.config.ConfigManager;
import com.slither.cyemer.module.implementation.ClickGUIModule;
import com.slither.cyemer.module.implementation.KeybindSetting;
import com.slither.cyemer.util.GuiShaderStyle;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;

/**
 * Renders + edits a KeybindSetting. Works uniformly for both the
 * module-linked keybind (delegates through to the module) and any
 * standalone secondary keybinds (state lives on the KeybindSetting).
 */
@Environment(EnvType.CLIENT)
public class KeybindComponent extends SettingComponent {
    private final KeybindSetting keybindSetting;
    private boolean waitingForInput = false;

    public KeybindComponent(KeybindSetting keybindSetting) {
        super(keybindSetting);
        this.keybindSetting = keybindSetting;
    }

    @Override
    public void render(class_332 context, int mouseX, int mouseY, float delta, double alpha) {
        float boxRadius = 6.0F;
        Color bgColor = ClickGUIModule.getColor(ClickGUIModule.getSettingsBackground(), alpha);
        Renderer.get()
            .drawRoundedRectStyled(context, (float)this.x, (float)this.y, (float)this.width, (float)this.height, boxRadius, bgColor, GuiShaderStyle.CONTROL);
        String label = this.keybindSetting.isStandalone() ? this.keybindSetting.getName() : "Key";
        String text = this.keybindSetting.isBinding() ? label + ": ..." : label + ": " + this.keybindSetting.getKeyDisplayName();
        Color textColor = this.keybindSetting.isBinding()
            ? ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledText(), alpha)
            : ClickGUIModule.getColor(ClickGUIModule.getSettingsText(), alpha);
        float fontSize = 9.0F;
        float textY = (float)(this.y + (this.height - Renderer.get().getTextHeight(fontSize)) / 2.0);
        Renderer.get().drawText(context, text, (float)(this.x + 4.0), textY, fontSize, textColor, ClickGUIModule.useShadows());
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isHovered(mouseX, mouseY)) {
            if (this.keybindSetting.isBinding() && this.waitingForInput) {
                this.keybindSetting.setKeyCode(button - 100);
                this.keybindSetting.setBinding(false);
                this.waitingForInput = false;
                ConfigManager.getInstance().save("default");
                return;
            }

            if (button == 0) {
                // Clear any other in-progress binds (module keybinds only - simplest and matches prior behavior).
                Cyemer.getInstance().getModuleManager().getModules().stream()
                    .filter(m -> m != this.keybindSetting.getModule())
                    .forEach(m -> m.setBinding(false));
                this.keybindSetting.setBinding(true);
                this.keybindSetting.setBindStartTime(System.currentTimeMillis());
                this.waitingForInput = true;
            } else if (button == 1) {
                this.keybindSetting.setKeyCode(-1);
                this.keybindSetting.setBinding(false);
                this.waitingForInput = false;
                ConfigManager.getInstance().save("default");
            }
        } else if (this.keybindSetting.isBinding()) {
            this.keybindSetting.setBinding(false);
            this.waitingForInput = false;
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.keybindSetting.isBinding()) {
            if (keyCode == 256) {
                this.keybindSetting.setKeyCode(-1);
            } else {
                this.keybindSetting.setKeyCode(keyCode);
            }
            this.keybindSetting.setBinding(false);
            this.waitingForInput = false;
            ConfigManager.getInstance().save("default");
        }
    }

    @Override
    public double getComponentHeight() {
        return 12.0;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }
}
