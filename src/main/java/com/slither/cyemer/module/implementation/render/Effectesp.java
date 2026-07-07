package com.slither.cyemer.module.implementation.render;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ColorSetting;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.module.implementation.targeteffect.EffectRenderContext;
import com.slither.cyemer.module.implementation.targeteffect.EffectRenderer;
import com.slither.cyemer.module.implementation.targeteffect.TextureTheme;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1657;
import net.minecraft.class_243;
import net.minecraft.class_4587;

@Environment(EnvType.CLIENT)
public class Effectesp extends Module {
    private final ModeSetting effect = new ModeSetting(
        "Effect",
        "Spinning Spheres",
        "Orbit Ring",
        "Pulse",
        "Helix",
        "Galaxy",
        "Tornado",
        "Rings",
        "Spiral",
        "Lightning",
        "Shockwave",
        "Scanlines",
        "Goofball",
        "Overlay"
    );
    private final ModeSetting theme = new ModeSetting("Theme", TextureTheme.getDisplayNames());
    private final ColorSetting color = new ColorSetting("Color 1", new Color(0, 255, 230));
    private final BooleanSetting gradient = new BooleanSetting("Gradient", false);
    private final ColorSetting gradientColor = new ColorSetting("Color 2", new Color(255, 90, 220));
    private final SliderSetting gradientSpeed = new SliderSetting("Gradient Speed", 1.0, 0.1, 5.0, 1);
    private final BooleanSetting opaque = new BooleanSetting("Opaque", false);
    private final BooleanSetting rainbow = new BooleanSetting("Rainbow", false);
    private final SliderSetting speed = new SliderSetting("Speed", 1.0, 0.1, 5.0, 1);
    private final SliderSetting scale = new SliderSetting("Scale", 1.0, 0.5, 3.0, 1);
    private final SliderSetting quality = new SliderSetting("Quality", 32.0, 16.0, 128.0, 1);
    private final BooleanSetting glow = new BooleanSetting("Glow", true);
    private final SliderSetting glowSize = new SliderSetting("Glow Size", 0.05, 0.001, 0.1, 3);
    private final SliderSetting glowOpacity = new SliderSetting("Glow Opacity", 0.6, 0.1, 1.0, 2);
    private final SliderSetting maxDistance = new SliderSetting("Max Distance", 64.0, 16.0, 128.0, 1);
    private final SliderSetting holdTime = new SliderSetting("Hold Time", 15.0, 5.0, 30.0, 0);
    private final EffectRenderer renderer = new EffectRenderer();
    private final Map<Integer, Long> lastSeen = new HashMap<>();

    public Effectesp() {
        super("EffectESP", "Effect visuals for nearby players", Category.RENDER);
        this.addSetting(this.effect);
        this.addSetting(this.theme);
        this.addSetting(this.color);
        this.addSetting(this.gradient);
        this.addSetting(this.gradientColor);
        this.addSetting(this.gradientSpeed);
        this.addSetting(this.rainbow);
        this.addSetting(this.opaque);
        this.addSetting(this.speed);
        this.addSetting(this.scale);
        this.addSetting(this.quality);
        this.addSetting(this.glow);
        this.addSetting(this.glowSize);
        this.addSetting(this.glowOpacity);
        this.addSetting(this.maxDistance);
        this.addSetting(this.holdTime);
    }

    @Override
    public void onWorldRender(class_4587 matrices, float tickDelta) {
        if (this.mc.field_1687 != null && this.mc.field_1724 != null) {
            long now = System.currentTimeMillis();
            class_243 cameraPos = this.mc.field_1773.method_19418().method_71156();
            TextureTheme currentTheme = TextureTheme.fromString(this.theme.getCurrentMode());
            String selectedEffect = this.effect.getCurrentMode();
            long holdMs = (long)(this.holdTime.getValue() * 1000.0);
            if ("Overlay".equals(selectedEffect)) {
                selectedEffect = "Pulse";
            }

            for (class_1657 player : this.mc.field_1687.method_18456()) {
                if (this.shouldRenderPlayer(player)) {
                    class_243 playerPos = player.method_30950(tickDelta);
                    double distance = cameraPos.method_1022(playerPos);
                    boolean inRange = distance <= this.maxDistance.getValue();
                    if (inRange) {
                        this.lastSeen.put(player.method_5628(), now);
                    } else {
                        Long seenAt = this.lastSeen.get(player.method_5628());
                        if (seenAt == null || now - seenAt > holdMs) {
                            continue;
                        }
                    }

                    if (!(distance > this.maxDistance.getValue() * 1.5)) {
                        Color currentColor = this.resolveRenderColor(now, player.method_5628(), distance);
                        EffectRenderContext context = new EffectRenderContext(
                            player,
                            selectedEffect,
                            currentColor,
                            this.opaque.isEnabled() ? 1.0F : 0.6F,
                            now,
                            this.speed.getValue(),
                            this.scale.getValue(),
                            EffectRenderer.calculateLOD(distance, (int)this.quality.getValue()),
                            this.glow.isEnabled(),
                            this.glowSize.getValue(),
                            this.glowOpacity.getValue(),
                            this.opaque.isEnabled(),
                            currentTheme
                        );
                        this.renderer.render(matrices, cameraPos, playerPos, context);
                    }
                }
            }

            long cutoff = now - holdMs;
            this.lastSeen.entrySet().removeIf(entry -> entry.getValue() < cutoff);
        }
    }

    private Color resolveRenderColor(long now, int seed, double distance) {
        if (this.rainbow.isEnabled()) {
            return EffectRenderer.getRainbowColor(now + seed * 53L);
        } else if (!this.gradient.isEnabled()) {
            return this.color.getValue();
        } else {
            double phase = now * 0.001 * this.gradientSpeed.getValue() + seed * 0.37 + distance * 0.08;
            float t = (float)(Math.sin(phase) * 0.5 + 0.5);
            return this.blend(this.color.getValue(), this.gradientColor.getValue(), t);
        }
    }

    private Color blend(Color start, Color end, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int r = (int)(start.getRed() + (end.getRed() - start.getRed()) * t);
        int g = (int)(start.getGreen() + (end.getGreen() - start.getGreen()) * t);
        int b = (int)(start.getBlue() + (end.getBlue() - start.getBlue()) * t);
        int a = (int)(start.getAlpha() + (end.getAlpha() - start.getAlpha()) * t);
        return new Color(r, g, b, a);
    }

    private boolean shouldRenderPlayer(class_1657 player) {
        return player != null && player != this.mc.field_1724 && player.method_5805() && !player.method_7325() && !player.method_6113();
    }
}
