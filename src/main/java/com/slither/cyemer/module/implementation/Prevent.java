package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Prevent extends Module {
    private final BooleanSetting preventEscape = new BooleanSetting("Prevent Escape", false);
    private final SliderSetting doubleClickTime = new SliderSetting("Double Click Time", 300.0, 100.0, 1000.0, 0);
    private final BooleanSetting tabOut = new BooleanSetting("TabOut", false);
    private long lastEscapePress = 0L;
    private boolean allowNextEscape = false;

    public Prevent() {
        super("Prevent", "Prevents certain player actions", Category.MISC);
        this.addSetting(this.preventEscape);
        this.addSetting(this.doubleClickTime);
        this.addSetting(this.tabOut);
    }

    public boolean shouldPreventEscape() {
        if (this.isEnabled() && this.preventEscape.isEnabled()) {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastPress = currentTime - this.lastEscapePress;
            if (timeSinceLastPress <= (long)this.doubleClickTime.getPreciseValue()) {
                this.allowNextEscape = true;
                this.lastEscapePress = 0L;
                return false;
            } else {
                this.lastEscapePress = currentTime;
                this.allowNextEscape = false;
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean shouldPreventTabOut() {
        return this.isEnabled() && this.tabOut.isEnabled();
    }

    @Override
    public void onDisable() {
        this.lastEscapePress = 0L;
        this.allowNextEscape = false;
    }
}
