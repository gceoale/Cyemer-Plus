package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Nick extends Module {
    public final StringSetting nickname = new StringSetting("Nickname", "Player");

    public Nick() {
        super("Nick", "Replaces your name client-side.", Category.PLAYER);
        this.addSetting(this.nickname);
    }

    public String getSafeNickname(String originalName) {
        if (!this.isEnabled()) {
            return originalName;
        } else {
            String nick = this.nickname.getValue();
            return nick != null && !nick.trim().isEmpty() ? nick : originalName;
        }
    }
}
