package com.slither.cyemer.module;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    PLAYER("Player"),
    MISC("Misc"),
    CLIENT("Client"),
    // Appended rather than slotted in, so no existing category's ordinal moves.
    DONUT_SMP("Donut SMP");

    public final String name;

    private Category(String name) {
        this.name = name;
    }
}
