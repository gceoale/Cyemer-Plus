package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.theme.Theme;
import com.slither.cyemer.theme.ThemeManager;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public class Watermark extends Module {
    private final SliderSetting textSize = new SliderSetting("Text Size", 2.0, 0.5, 4.0, 2);
    private final BooleanSetting adoptTheme = new BooleanSetting("Adopt Theme", true);
    private final BooleanSetting glow = new BooleanSetting("Glow", true);
    private final SliderSetting glowSize = new SliderSetting("Glow Size", 15.0, 5.0, 100.0, 1);
    private final SliderSetting glowIntensity = new SliderSetting("Glow Intensity", 0.6, 0.1, 1.0, 2);
    private final SliderSetting glowLayers = new SliderSetting("Glow Layers", 8.0, 3.0, 20.0, 1);
    private final BooleanSetting textShadow = new BooleanSetting("Text Shadow", true);
    private final BooleanSetting rainbowMode = new BooleanSetting("Rainbow Mode", false);
    private final SliderSetting rainbowSpeed = new SliderSetting("Rainbow Speed", 5.0, 1.0, 20.0, 1);
    private final BooleanSetting background = new BooleanSetting("Background", false);
    private final BooleanSetting roundedCorners = new BooleanSetting("Rounded Corners", true);
    private final SliderSetting cornerRadius = new SliderSetting("Corner Radius", 6.0, 0.0, 15.0, 1);
    private final SliderSetting backgroundOpacity = new SliderSetting("BG Opacity", 0.7, 0.0, 1.0, 2);
    private final SliderSetting padding = new SliderSetting("Padding", 8.0, 2.0, 20.0, 1);
    private final BooleanSetting particles = new BooleanSetting("Particles", true);
    private final SliderSetting particleCount = new SliderSetting("Particle Count", 8.29, 5.0, 50.0, 1);
    private final SliderSetting particleSpeed = new SliderSetting("Particle Speed", 0.1, 0.1, 3.0, 2);
    private final SliderSetting particleSize = new SliderSetting("Particle Size", 2.0, 0.5, 5.0, 2);
    private final SliderSetting particleOpacity = new SliderSetting("Particle Opacity", 0.6, 0.1, 1.0, 2);
    private float rainbowHue = 0.0F;
    private long lastFrameTime = System.currentTimeMillis();
    private final List<Watermark.Particle> particleList = new ArrayList<>();
    private boolean particlesInitialized = false;

    public Watermark() {
        super("Watermark", "Displays 'cyemer' watermark with animated gradient and glow", Category.CLIENT);
        this.setEnabled(true);
        this.addSetting(this.textSize);
        this.addSetting(this.adoptTheme);
        this.addSetting(this.glow);
        this.addSetting(this.glowSize);
        this.addSetting(this.glowIntensity);
        this.addSetting(this.glowLayers);
        this.addSetting(this.textShadow);
        this.addSetting(this.rainbowMode);
        this.addSetting(this.rainbowSpeed);
        this.addSetting(this.background);
        this.addSetting(this.roundedCorners);
        this.addSetting(this.cornerRadius);
        this.addSetting(this.backgroundOpacity);
        this.addSetting(this.padding);
        this.addSetting(this.particles);
        this.addSetting(this.particleCount);
        this.addSetting(this.particleSpeed);
        this.addSetting(this.particleSize);
        this.addSetting(this.particleOpacity);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onRender(class_332 context, float tickDelta) {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            Theme theme = ThemeManager.getInstance().getCurrentTheme();
            float scale = (float)this.textSize.getValue();
            float pad = (float)this.padding.getValue();
            float fontSize = 9.0F * scale;
            String text = "cyemer";
            long currentTime = System.currentTimeMillis();
            float deltaTime = (float)(currentTime - this.lastFrameTime) / 1000.0F;
            this.lastFrameTime = currentTime;
            this.rainbowHue = this.rainbowHue + (float)(this.rainbowSpeed.getValue() * deltaTime * 0.1);
            if (this.rainbowHue > 1.0F) {
                this.rainbowHue--;
            }

            int screenWidth = this.mc.method_22683().method_4486();
            int screenHeight = this.mc.method_22683().method_4502();
            float pixelRatio = this.mc.method_22683().method_4495();
            if (Renderer.get().beginFrame(screenWidth, screenHeight, pixelRatio)) {
                float textWidth = Renderer.get().getTextWidth(text, fontSize);
                float actualTextHeight = Renderer.get().getTextHeight(fontSize);
                float width = textWidth + pad * 2.0F;
                float height = actualTextHeight + pad * 2.0F;
                float x = 5.0F;
                float y = 5.0F;
                float radius = this.roundedCorners.isEnabled() ? (float)this.cornerRadius.getValue() : 0.0F;
                Color bgColor;
                if (this.adoptTheme.isEnabled()) {
                    bgColor = theme.getPanelBackgroundColor();
                } else {
                    bgColor = new Color(10, 10, 10, 200);
                }

                int bgAlpha = (int)(this.backgroundOpacity.getValue() * 255.0);
                bgColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), bgAlpha);
                Color gradientStart;
                Color gradientEnd;
                if (this.rainbowMode.isEnabled()) {
                    gradientStart = Color.getHSBColor(this.rainbowHue, 0.8F, 1.0F);
                    gradientEnd = Color.getHSBColor((this.rainbowHue + 0.15F) % 1.0F, 0.8F, 1.0F);
                } else if (this.adoptTheme.isEnabled()) {
                    float t = (float)(Math.sin(currentTime / 1000.0 * this.rainbowSpeed.getValue() * 0.5) + 1.0) / 2.0F;
                    gradientStart = this.lerpColor(theme.getAccentColor(), theme.getSecondaryAccentColor(), t);
                    gradientEnd = this.lerpColor(theme.getSecondaryAccentColor(), theme.getAccentColor(), t);
                } else {
                    float t = (float)(Math.sin(currentTime / 1000.0 * this.rainbowSpeed.getValue() * 0.5) + 1.0) / 2.0F;
                    Color pink = new Color(255, 100, 180);
                    Color blue = new Color(100, 180, 255);
                    gradientStart = this.lerpColor(pink, blue, t);
                    gradientEnd = this.lerpColor(blue, pink, t);
                }

                int targetParticleCount = (int)this.particleCount.getValue();
                if (this.particles.isEnabled()) {
                    if (!this.particlesInitialized || this.particleList.size() != targetParticleCount) {
                        this.initializeParticles(targetParticleCount, x, y, width, height, gradientStart, gradientEnd);
                        this.particlesInitialized = true;
                    }

                    float pSpeed = (float)this.particleSpeed.getValue();
                    this.particleList.removeIf(Watermark.Particle::isDead);

                    for (Watermark.Particle particle : this.particleList) {
                        particle.update(deltaTime * pSpeed * 30.0F);
                        if (particle.x < x - 10.0F || particle.x > x + width + 10.0F || particle.y < y - 10.0F || particle.y > y + height + 10.0F) {
                            this.respawnParticle(particle, x, y, width, height, gradientStart, gradientEnd);
                        }
                    }

                    while (this.particleList.size() < targetParticleCount) {
                        this.particleList.add(this.createRandomParticle(x, y, width, height, gradientStart, gradientEnd));
                    }
                } else {
                    this.particlesInitialized = false;
                    this.particleList.clear();
                }

                if (this.background.isEnabled()) {
                    Renderer.get().drawRoundedRect(context, x, y, width, height, radius, bgColor);
                }

                if (this.particles.isEnabled() && !this.particleList.isEmpty()) {
                    float pSize = (float)this.particleSize.getValue();
                    float pOpacity = (float)this.particleOpacity.getValue();

                    for (Watermark.Particle particlex : this.particleList) {
                        float particleAlpha = particlex.getAlpha() * pOpacity;
                        particleAlpha = Math.max(0.0F, Math.min(1.0F, particleAlpha));
                        Color particleColor = new Color(
                            particlex.color.getRed(), particlex.color.getGreen(), particlex.color.getBlue(), (int)(particleAlpha * 255.0F)
                        );
                        Renderer.get().setFontBlur(pSize * 0.5F);
                        Renderer.get()
                            .drawRoundedRect(
                                context,
                                particlex.x - particlex.size / 2.0F,
                                particlex.y - particlex.size / 2.0F,
                                particlex.size * pSize,
                                particlex.size * pSize,
                                particlex.size * pSize / 2.0F,
                                particleColor
                            );
                        Renderer.get().setFontBlur(0.0F);
                    }
                }

                if (this.glow.isEnabled()) {
                    float glowS = (float)this.glowSize.getValue();
                    float intensity = (float)this.glowIntensity.getValue();
                    int numLayers = (int)this.glowLayers.getValue();
                    float charX = x + pad;

                    for (char c : text.toCharArray()) {
                        String character = String.valueOf(c);
                        float charWidth = Renderer.get().getTextWidth(character, fontSize);
                        float charProgress = (charX - x - pad) / textWidth;
                        Color charColor = this.lerpColor(gradientStart, gradientEnd, charProgress);

                        for (int i = numLayers; i >= 1; i--) {
                            float layerProgress = (float)i / numLayers;
                            float layerBlur = glowS * layerProgress;
                            float glowAlpha = intensity * (1.0F - layerProgress + 0.2F) / numLayers * 2.0F;
                            Renderer.get().setFontBlur(layerBlur);
                            Color glowColor = new Color(charColor.getRed(), charColor.getGreen(), charColor.getBlue(), Math.min(255, (int)(glowAlpha * 255.0F)));

                            for (float ox = -layerProgress * 0.5F; ox <= layerProgress * 0.5F; ox += 0.25F) {
                                for (float oy = -layerProgress * 0.5F; oy <= layerProgress * 0.5F; oy += 0.25F) {
                                    Renderer.get().drawText(context, character, charX + ox, y + pad + oy, fontSize, glowColor, false);
                                }
                            }
                        }

                        charX += charWidth;
                    }

                    Renderer.get().setFontBlur(0.0F);
                }

                float textX = x + pad;

                for (char c : text.toCharArray()) {
                    String character = String.valueOf(c);
                    float charWidth = Renderer.get().getTextWidth(character, fontSize);
                    float progress = (textX - x - pad) / textWidth;
                    Color charColor = this.lerpColor(gradientStart, gradientEnd, progress);
                    if (this.textShadow.isEnabled()) {
                        Color shadowColor = new Color(0, 0, 0, 150);
                        Renderer.get().drawText(context, character, textX + 1.0F, y + pad + 1.0F, fontSize, shadowColor, false);
                    }

                    Renderer.get().drawText(context, character, textX, y + pad, fontSize, charColor, false);
                    textX += charWidth;
                }

                Renderer.get().endFrame();
            }
        }
    }

    private Color lerpColor(Color c1, Color c2, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int r = (int)(c1.getRed() * (1.0F - t) + c2.getRed() * t);
        int g = (int)(c1.getGreen() * (1.0F - t) + c2.getGreen() * t);
        int b = (int)(c1.getBlue() * (1.0F - t) + c2.getBlue() * t);
        int a = (int)(c1.getAlpha() * (1.0F - t) + c2.getAlpha() * t);
        return new Color(r, g, b, a);
    }

    private void initializeParticles(int count, float x, float y, float width, float height, Color c1, Color c2) {
        this.particleList.clear();

        for (int i = 0; i < count; i++) {
            this.particleList.add(this.createRandomParticle(x, y, width, height, c1, c2));
        }
    }

    private Watermark.Particle createRandomParticle(float x, float y, float width, float height, Color c1, Color c2) {
        float px = x + (float)(Math.random() * width);
        float py = y + (float)(Math.random() * height);
        float angle = (float)(Math.random() * Math.PI * 2.0);
        float speed = 5.0F + (float)(Math.random() * 10.0);
        float vx = (float)Math.cos(angle) * speed;
        float vy = (float)Math.sin(angle) * speed;
        float size = 1.0F + (float)(Math.random() * 2.0);
        float maxLife = 2.0F + (float)(Math.random() * 3.0);
        Color color = this.lerpColor(c1, c2, (float)Math.random());
        return new Watermark.Particle(px, py, vx, vy, size, maxLife, color);
    }

    private void respawnParticle(Watermark.Particle particle, float x, float y, float width, float height, Color c1, Color c2) {
        int edge = (int)(Math.random() * 4.0);
        switch (edge) {
            case 0:
                particle.x = x + (float)(Math.random() * width);
                particle.y = y - 5.0F;
                break;
            case 1:
                particle.x = x + width + 5.0F;
                particle.y = y + (float)(Math.random() * height);
                break;
            case 2:
                particle.x = x + (float)(Math.random() * width);
                particle.y = y + height + 5.0F;
                break;
            case 3:
                particle.x = x - 5.0F;
                particle.y = y + (float)(Math.random() * height);
        }

        float centerX = x + width / 2.0F;
        float centerY = y + height / 2.0F;
        float angle = (float)Math.atan2(centerY - particle.y, centerX - particle.x);
        angle += (float)((Math.random() - 0.5) * Math.PI / 2.0);
        float speed = 5.0F + (float)(Math.random() * 10.0);
        particle.vx = (float)Math.cos(angle) * speed;
        particle.vy = (float)Math.sin(angle) * speed;
        particle.life = 2.0F + (float)(Math.random() * 3.0);
        particle.maxLife = particle.life;
        particle.size = 1.0F + (float)(Math.random() * 2.0);
        particle.color = this.lerpColor(c1, c2, (float)Math.random());
    }

    @Environment(EnvType.CLIENT)
    private static class Particle {
        float x;
        float y;
        float vx;
        float vy;
        float size;
        float life;
        float maxLife;
        Color color;

        public Particle(float x, float y, float vx, float vy, float size, float maxLife, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.life = maxLife;
            this.maxLife = maxLife;
            this.color = color;
        }

        public void update(float deltaTime) {
            this.x = this.x + this.vx * deltaTime;
            this.y = this.y + this.vy * deltaTime;
            this.life -= deltaTime;
        }

        public boolean isDead() {
            return this.life <= 0.0F;
        }

        public float getAlpha() {
            return this.life / this.maxLife;
        }
    }
}
