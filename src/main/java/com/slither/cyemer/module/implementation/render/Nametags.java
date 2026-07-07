package com.slither.cyemer.module.implementation.render;

import com.slither.cyemer.mixin.GameRendererAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ColorSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1657;
import net.minecraft.class_243;
import net.minecraft.class_332;
import net.minecraft.class_4184;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public class Nametags extends Module {
    public static Nametags INSTANCE;
    private final ColorSetting backgroundColor = new ColorSetting("Background", new Color(0, 0, 0, 120));
    private final ColorSetting textColor = new ColorSetting("Text Color", Color.WHITE);
    private final SliderSetting fontSize = new SliderSetting("Font Size", 14.0, 8.0, 32.0, 1);
    private final SliderSetting scale = new SliderSetting("Scale", 1.0, 0.5, 3.0, 1);
    private final SliderSetting maxDistance = new SliderSetting("Max Distance", 64.0, 16.0, 256.0, 0);
    private final BooleanSetting showHealth = new BooleanSetting("Show Health", true);
    private final BooleanSetting showDistance = new BooleanSetting("Show Distance", false);
    private final BooleanSetting showBackground = new BooleanSetting("Show Background", true);
    private final BooleanSetting scalingEnabled = new BooleanSetting("Distance Scaling", true);

    public Nametags() {
        super("Nametags", "Renders player names above their heads", Category.RENDER);
        INSTANCE = this;
        this.addSetting(this.backgroundColor);
        this.addSetting(this.textColor);
        this.addSetting(this.fontSize);
        this.addSetting(this.scale);
        this.addSetting(this.maxDistance);
        this.addSetting(this.showHealth);
        this.addSetting(this.showDistance);
        this.addSetting(this.showBackground);
        this.addSetting(this.scalingEnabled);
    }

    @Override
    public void onRender(class_332 context, float tickDeltaArgument) {
        if (this.mc.field_1687 != null && this.mc.field_1724 != null) {
            float tickDelta = this.mc.method_61966().method_60637(true);
            float width = this.mc.method_22683().method_4486();
            float height = this.mc.method_22683().method_4502();
            float pixelRatio = this.mc.method_22683().method_4489() / width;
            if (Renderer.get().beginFrame(width, height, pixelRatio)) {
                class_4184 camera = this.mc.field_1773.method_19418();
                float activeFov = ((GameRendererAccessor)this.mc.field_1773).callGetFov(camera, tickDelta, true);
                double maxDistSq = this.maxDistance.getValue() * this.maxDistance.getValue();

                for (class_1657 player : this.mc.field_1687.method_18456()) {
                    if ((player != this.mc.field_1724 || !this.mc.field_1690.method_31044().method_31034()) && player.method_5805() && !player.method_7325()) {
                        double distSq = this.mc.field_1724.method_5858(player);
                        if (!(distSq > maxDistSq)) {
                            class_243 basePos = player.method_30950(tickDelta);
                            class_243 nametagPos = basePos.method_1031(0.0, player.method_17682() + 0.5, 0.0);
                            class_243 screenPos = this.worldToScreen(nametagPos, camera, activeFov);
                            if (screenPos != null) {
                                String nameText = player.method_5477().getString();
                                if (this.showHealth.isEnabled()) {
                                    float health = player.method_6032();
                                    float maxHealth = player.method_6063();
                                    nameText = nameText + String.format(" %.1f", health);
                                }

                                if (this.showDistance.isEnabled()) {
                                    double distance = Math.sqrt(distSq);
                                    nameText = nameText + String.format(" [%.1fm]", distance);
                                }

                                float distanceScale = 1.0F;
                                if (this.scalingEnabled.isEnabled()) {
                                    double distance = Math.sqrt(distSq);
                                    distanceScale = (float)Math.max(0.5, Math.min(2.0, 10.0 / distance));
                                }

                                float finalScale = (float)this.scale.getValue() * distanceScale;
                                float finalFontSize = (float)this.fontSize.getValue() * finalScale;
                                this.renderNametag(
                                    context,
                                    nameText,
                                    (float)screenPos.field_1352,
                                    (float)screenPos.field_1351,
                                    finalFontSize,
                                    player.method_6032(),
                                    player.method_6063()
                                );
                            }
                        }
                    }
                }

                Renderer.get().endFrame();
            }
        }
    }

    private void renderNametag(class_332 context, String text, float x, float y, float fontSize, float health, float maxHealth) {
        float textWidth = Renderer.get().getTextWidth(text, fontSize);
        float padding = 4.0F;
        float bgWidth = textWidth + padding * 2.0F;
        float bgHeight = fontSize + padding * 2.0F;
        float bgX = x - bgWidth / 2.0F;
        float bgY = y - bgHeight / 2.0F;
        if (this.showBackground.isEnabled()) {
            Renderer.get().drawRoundedRect(context, bgX, bgY, bgWidth, bgHeight, 2.0F, this.backgroundColor.getValue());
        }

        Color healthColor = this.getHealthColor(health, maxHealth);
        if (this.showHealth.isEnabled()) {
            float healthBarHeight = 2.0F;
            float healthBarY = bgY + bgHeight - healthBarHeight - 1.0F;
            float healthBarWidth = (bgWidth - padding * 2.0F) * (health / maxHealth);
            Renderer.get().drawRoundedRect(context, bgX + padding, healthBarY, healthBarWidth, healthBarHeight, 1.0F, healthColor);
        }

        float textX = x - textWidth / 2.0F;
        float textY = bgY + padding;
        Renderer.get().drawText(context, text, textX, textY, fontSize, this.textColor.getValue(), true);
    }

    private Color getHealthColor(float health, float maxHealth) {
        float ratio = health / maxHealth;
        if (ratio > 0.6F) {
            return new Color(85, 255, 85);
        } else {
            return ratio > 0.3F ? new Color(255, 255, 85) : new Color(255, 85, 85);
        }
    }

    private class_243 worldToScreen(class_243 worldPos, class_4184 camera, float activeFov) {
        class_243 camPos = camera.method_71156();
        Vector3f rel = worldPos.method_46409().sub((float)camPos.field_1352, (float)camPos.field_1351, (float)camPos.field_1350);
        Matrix4f proj = new Matrix4f()
            .perspective(
                (float)Math.toRadians(activeFov),
                (float)this.mc.method_22683().method_4489() / this.mc.method_22683().method_4506(),
                0.05F,
                this.mc.field_1773.method_3193()
            );
        Matrix4f view = new Matrix4f()
            .rotate((float)Math.toRadians(camera.method_19329()), 1.0F, 0.0F, 0.0F)
            .rotate((float)Math.toRadians(camera.method_19330() + 180.0F), 0.0F, 1.0F, 0.0F);
        Vector4f clip = new Vector4f(rel.x, rel.y, rel.z, 1.0F).mul(view).mul(proj);
        if (clip.w <= 0.0F) {
            return null;
        } else {
            Vector3f ndc = new Vector3f(clip.x / clip.w, clip.y / clip.w, clip.z / clip.w);
            if (!(Math.abs(ndc.x) > 1.0F) && !(Math.abs(ndc.y) > 1.0F)) {
                double screenX = (ndc.x + 1.0) / 2.0 * this.mc.method_22683().method_4486();
                double screenY = (1.0 - ndc.y) / 2.0 * this.mc.method_22683().method_4502();
                return new class_243(screenX, screenY, ndc.z);
            } else {
                return null;
            }
        }
    }
}
