package com.slither.cyemer.gui.new_ui.settings;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.config.ConfigManager;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.implementation.ClickGUIModule;
import com.slither.cyemer.module.implementation.KeybindSetting;
import com.slither.cyemer.util.GuiShaderStyle;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public class KeybindComponent extends SettingComponent {
    private final KeybindSetting keybindSetting;
    private boolean waitingForInput = false;

    public KeybindComponent(KeybindSetting keybindSetting) {
        super(keybindSetting);
        this.keybindSetting = keybindSetting;
    }

    private Module getModule() {
        return this.keybindSetting.getModule();
    }

    @Override
    public void render(class_332 context, int mouseX, int mouseY, float delta, double alpha) {
        float boxRadius = 6.0F;
        Color bgColor = ClickGUIModule.getColor(ClickGUIModule.getSettingsBackground(), alpha);
        Renderer.get()
            .drawRoundedRectStyled(context, (float)this.x, (float)this.y, (float)this.width, (float)this.height, boxRadius, bgColor, GuiShaderStyle.CONTROL);
        String text = this.getModule().isBinding() ? "Key: ..." : "Key: " + this.getModule().getKeyDisplayName();
        Color textColor = this.getModule().isBinding()
            ? ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledText(), alpha)
            : ClickGUIModule.getColor(ClickGUIModule.getSettingsText(), alpha);
        float fontSize = 9.0F;
        float textY = (float)(this.y + (this.height - Renderer.get().getTextHeight(fontSize)) / 2.0);
        Renderer.get().drawText(context, text, (float)(this.x + 4.0), textY, fontSize, textColor, ClickGUIModule.useShadows());
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        Module currentModule = this.getModule();
        if (this.isHovered(mouseX, mouseY)) {
            if (currentModule.isBinding() && this.waitingForInput) {
                currentModule.setKeyCode(button - 100);
                currentModule.setBinding(false);
                this.waitingForInput = false;
                ConfigManager.getInstance().save("default");
                return;
            }

            if (button == 0) {
                Cyemer.getInstance().getModuleManager().getModules().stream().filter(m -> m != currentModule).forEach(m -> m.setBinding(false));
                currentModule.setBinding(true);
                currentModule.setBindStartTime(System.currentTimeMillis());
                this.waitingForInput = true;
            } else if (button == 1) {
                currentModule.setKeyCode(-1);
                currentModule.setBinding(false);
                this.waitingForInput = false;
                ConfigManager.getInstance().save("default");
            }
        } else if (currentModule.isBinding()) {
            currentModule.setBinding(false);
            this.waitingForInput = false;
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        Module currentModule = this.getModule();
        if (currentModule.isBinding()) {
            if (keyCode == 256) {
                currentModule.setKeyCode(-1);
            } else {
                currentModule.setKeyCode(keyCode);
            }

            currentModule.setBinding(false);
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
