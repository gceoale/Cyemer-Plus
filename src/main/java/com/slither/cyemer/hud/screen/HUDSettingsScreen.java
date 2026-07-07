package com.slither.cyemer.hud.screen;

import com.slither.cyemer.hud.HUDElement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;

@Environment(EnvType.CLIENT)
public class HUDSettingsScreen extends class_437 {
    private final class_437 parentScreen;
    private final HUDElement element;

    public HUDSettingsScreen(class_437 parentScreen, HUDElement element) {
        super(class_2561.method_43470("HUD Settings"));
        this.parentScreen = parentScreen;
        this.element = element;
    }

    protected void method_25426() {
        this.method_37063(
            class_4185.method_46430(class_2561.method_43470("Done"), button -> this.field_22787.method_1507(this.parentScreen))
                .method_46434(this.field_22789 / 2 - 100, this.field_22790 - 40, 200, 20)
                .method_46431()
        );
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        this.method_25420(context, mouseX, mouseY, delta);
        context.method_25300(this.field_22793, "Settings for " + this.element.getName(), this.field_22789 / 2, 20, 16777215);
        super.method_25394(context, mouseX, mouseY, delta);
    }

    public void method_25419() {
        this.field_22787.method_1507(this.parentScreen);
    }
}
