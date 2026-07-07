package com.slither.cyemer.module.implementation;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ColorSetting;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public class ArrayListModule extends Module {
    private final ModeSetting position = new ModeSetting("Position", "Top-Right", "Top-Left", "Bottom-Right", "Bottom-Left");
    private final BooleanSetting sort = new BooleanSetting("Sort", true);
    private final SliderSetting textSize = new SliderSetting("Text Size", 1.0, 0.5, 2.0, 2);
    private final SliderSetting padding = new SliderSetting("Padding", 3.0, 0.0, 10.0, 1);
    private final BooleanSetting defaultColours = new BooleanSetting("Default Colors", true);
    private final ColorSetting textStartColor = new ColorSetting("Text Color 1", new Color(255, 255, 255));
    private final ColorSetting textEndColor = new ColorSetting("Text Color 2", new Color(0, 150, 255));
    private final ColorSetting sidebarStartColor = new ColorSetting("Bar Color 1", new Color(40, 40, 255));
    private final ColorSetting sidebarEndColor = new ColorSetting("Bar Color 2", new Color(180, 40, 255));
    private final SliderSetting colorSpeed = new SliderSetting("Color Speed", 3.0, 0.1, 10.0, 1);
    private final SliderSetting colorSpread = new SliderSetting("Color Spread", 0.5, 0.1, 2.0, 1);
    private final SliderSetting backgroundOpacity = new SliderSetting("BG Opacity", 0.5, 0.0, 1.0, 2);
    private final BooleanSetting sidebar = new BooleanSetting("Sidebar", true);
    private final SliderSetting sidebarWidth = new SliderSetting("Sidebar Width", 2.0, 1.0, 5.0, 1);
    private final BooleanSetting particles = new BooleanSetting("Particles", true);
    private final BooleanSetting textShadow = new BooleanSetting("Text Shadow", true);
    private final ModeSetting popAnimation = new ModeSetting("Pop Animation", "Slide", "Pop", "Fade", "None");
    private final SliderSetting animationSpeed = new SliderSetting("Animation Speed", 4.0, 0.5, 10.0, 1);
    private long lastFrameTime = System.currentTimeMillis();
    private final Map<String, Float> moduleAnimations = new HashMap<>();
    private final Map<String, Boolean> moduleRemoving = new HashMap<>();
    private final Map<String, Boolean> moduleIsNew = new HashMap<>();
    private final Set<String> flurryPlayed = new HashSet<>();
    private final List<ArrayListModule.Particle> particleList = new ArrayList<>();
    private final Random random = new Random();
    private final Color CYEMER_PINK = new Color(255, 100, 180);
    private final Color CYEMER_BLUE = new Color(100, 180, 255);

    public ArrayListModule() {
        super("ArrayList", "Displays enabled modules on screen", Category.CLIENT);
        this.setEnabled(true);
        this.addSetting(this.position);
        this.addSetting(this.sort);
        this.addSetting(this.textSize);
        this.addSetting(this.padding);
        this.addSetting(this.defaultColours);
        this.addSetting(this.textStartColor);
        this.addSetting(this.textEndColor);
        this.addSetting(this.sidebarStartColor);
        this.addSetting(this.sidebarEndColor);
        this.addSetting(this.colorSpeed);
        this.addSetting(this.colorSpread);
        this.addSetting(this.backgroundOpacity);
        this.addSetting(this.sidebar);
        this.addSetting(this.sidebarWidth);
        this.addSetting(this.particles);
        this.addSetting(this.textShadow);
        this.addSetting(this.popAnimation);
        this.addSetting(this.animationSpeed);
    }

    @Override
    public void onRender(class_332 context, float tickDelta) {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            List<Module> enabledModules = Cyemer.getInstance()
                .getModuleManager()
                .getModules()
                .stream()
                .filter(modulex -> modulex.isEnabled() && modulex != this)
                .collect(Collectors.toList());
            float scale = (float)this.textSize.getValue();
            float pad = (float)this.padding.getValue();
            float fontSize = 9.0F * scale;
            float renderedTextHeight = Renderer.get().getTextHeight(fontSize);
            float itemHeight = (float)Math.ceil(renderedTextHeight + pad * 2.0F);
            int screenWidth = this.mc.method_22683().method_4486();
            int screenHeight = this.mc.method_22683().method_4502();
            float pixelRatio = this.mc.method_22683().method_4495();
            if (Renderer.get().beginFrame(screenWidth, screenHeight, pixelRatio)) {
                List<Module> allModules = new ArrayList<>(enabledModules);

                for (String moduleName : new HashMap<>(this.moduleAnimations).keySet()) {
                    boolean stillEnabled = enabledModules.stream().anyMatch(m -> m.getName().equals(moduleName));
                    boolean isAlreadyRemoving = this.moduleRemoving.getOrDefault(moduleName, false);
                    if (!stillEnabled && !isAlreadyRemoving) {
                        this.moduleRemoving.put(moduleName, true);
                        this.flurryPlayed.remove(moduleName);
                        Cyemer.getInstance()
                            .getModuleManager()
                            .getModules()
                            .stream()
                            .filter(m -> m.getName().equals(moduleName))
                            .findFirst()
                            .ifPresent(allModules::add);
                    }
                }

                if (this.sort.isEnabled()) {
                    try {
                        allModules.sort(Comparator.<Module>comparingInt(m -> (int)Renderer.get().getTextWidth(m.getName(), fontSize)).reversed());
                    } catch (Exception var52) {
                        allModules.sort(Comparator.<Module>comparingInt(m -> this.mc.field_1772.method_1727(m.getName())).reversed());
                    }
                }

                long currentTime = System.currentTimeMillis();
                float deltaTime = (float)(currentTime - this.lastFrameTime) / 1000.0F;
                this.lastFrameTime = currentTime;
                this.updateAniStates(enabledModules, deltaTime);
                boolean alignRight = this.position.getCurrentMode().contains("Right");
                boolean alignBottom = this.position.getCurrentMode().contains("Bottom");
                float currentY = alignBottom ? screenHeight - 2 : 2.0F;
                Color tStart;
                Color tEnd;
                Color sStart;
                Color sEnd;
                if (this.defaultColours.isEnabled()) {
                    tStart = this.CYEMER_PINK;
                    tEnd = this.CYEMER_BLUE;
                    sStart = this.CYEMER_PINK;
                    sEnd = this.CYEMER_BLUE;
                } else {
                    tStart = this.textStartColor.getValue();
                    tEnd = this.textEndColor.getValue();
                    sStart = this.sidebarStartColor.getValue();
                    sEnd = this.sidebarEndColor.getValue();
                }

                if (this.particles.isEnabled()) {
                    this.particles(context, deltaTime, tStart, tEnd);
                }

                int index = 0;

                for (Module module : allModules) {
                    String name = module.getName();
                    boolean isRemoving = this.moduleRemoving.getOrDefault(name, false);
                    float animProgress = this.moduleAnimations.getOrDefault(name, 1.0F);
                    boolean isNew = this.moduleIsNew.getOrDefault(name, false);
                    if (isRemoving && animProgress <= 0.0F) {
                        this.flurryPlayed.remove(name);
                    } else {
                        float itemWidth;
                        try {
                            itemWidth = Renderer.get().getTextWidth(name, fontSize);
                        } catch (Exception var51) {
                            itemWidth = this.mc.field_1772.method_1727(name) * scale;
                        }

                        String animMode = this.popAnimation.getCurrentMode();
                        float xOffset = 0.0F;
                        float heightScale = 1.0F;
                        float alpha = 1.0F;
                        if (!animMode.equals("None") && animProgress < 1.0F) {
                            heightScale = animProgress;
                            if (animMode.equals("Slide")) {
                                xOffset = (1.0F - animProgress) * (itemWidth + 20.0F);
                                if (alignRight) {
                                    xOffset = -xOffset;
                                }
                            } else if (animMode.equals("Fade")) {
                                alpha = animProgress;
                            }
                        }

                        double timeFactor = currentTime / 1000.0 * this.colorSpeed.getValue();
                        double indexFactor = index * this.colorSpread.getValue();
                        float waveT = (float)(Math.sin(timeFactor - indexFactor) + 1.0) / 2.0F;
                        Color renderTextColor = this.interColor(tStart, tEnd, waveT);
                        Color renderSidebarColor = this.interColor(sStart, sEnd, waveT);
                        renderSidebarColor = this.applyAlpha(renderSidebarColor, alpha);
                        renderTextColor = this.applyAlpha(renderTextColor, alpha);
                        float sideWidth = this.sidebar.isEnabled() ? (float)this.sidebarWidth.getValue() : 0.0F;
                        float fullWidth = itemWidth + pad * 2.0F + sideWidth;
                        float x;
                        if (alignRight) {
                            x = screenWidth - fullWidth - 2.0F;
                            x -= xOffset;
                        } else {
                            x = 2.0F;
                            x += xOffset;
                        }

                        float drawHeight = itemHeight * heightScale;
                        if (heightScale >= 1.0F) {
                            drawHeight = (float)Math.ceil(drawHeight);
                        }

                        if (alignBottom) {
                            currentY -= drawHeight;
                        }

                        if (this.particles.isEnabled()
                            && !this.flurryPlayed.contains(name)
                            && (isNew && animProgress > 0.01F || isRemoving && animProgress < 0.99F)) {
                            float centerX = x + fullWidth / 2.0F;
                            float centerY = currentY + drawHeight / 2.0F;
                            this.spawnFlurryThingo(centerX, centerY, renderTextColor);
                            this.flurryPlayed.add(name);
                        }

                        int bgAlpha = (int)(this.backgroundOpacity.getValue() * 255.0 * alpha);
                        if (bgAlpha > 0) {
                            Renderer.get().drawRect(context, x, currentY, fullWidth, drawHeight, new Color(0, 0, 0, bgAlpha));
                        }

                        if (this.sidebar.isEnabled()) {
                            float barX = alignRight ? x + fullWidth - sideWidth : x;
                            Renderer.get().drawRect(context, barX, currentY, sideWidth, drawHeight, renderSidebarColor);
                        }

                        float textX = alignRight ? x + pad : x + sideWidth + pad;
                        float textY = currentY + (drawHeight - renderedTextHeight) / 2.0F;
                        if (animMode.equals("Pop") && animProgress < 1.0F) {
                            Renderer.get().save();
                            float centerX = x + fullWidth / 2.0F;
                            float centerY = currentY + drawHeight / 2.0F;
                            Renderer.get().translate(centerX, centerY);
                            Renderer.get().scale(animProgress, animProgress);
                            Renderer.get().translate(-centerX, -centerY);
                        }

                        Renderer.get().drawText(context, name, textX, textY, fontSize, renderTextColor, this.textShadow.isEnabled());
                        if (animMode.equals("Pop") && animProgress < 1.0F) {
                            Renderer.get().restore();
                        }

                        if (!alignBottom) {
                            currentY += drawHeight;
                        }

                        index++;
                    }
                }

                Renderer.get().endFrame();
                this.cleanupAni();
            }
        }
    }

    private void spawnFlurryThingo(float x, float y, Color color) {
        for (int i = 0; i < 15; i++) {
            ArrayListModule.Particle p = new ArrayListModule.Particle(x, y, color);
            p.isFlurry = true;
            p.velocityX = (this.random.nextFloat() - 0.5F) * 60.0F;
            p.velocityY = (this.random.nextFloat() - 0.5F) * 60.0F;
            p.maxLife = 1.0F;
            p.life = p.maxLife;
            this.particleList.add(p);
        }
    }

    private void particles(class_332 context, float deltaTime, Color startC, Color endC) {
        int screenWidth = this.mc.method_22683().method_4486();
        int screenHeight = this.mc.method_22683().method_4502();
        if (this.particleList.size() < 60 && this.random.nextFloat() < 0.1F) {
            boolean alignRight = this.position.getCurrentMode().contains("Right");
            float pX = alignRight ? screenWidth - this.random.nextFloat() * 120.0F : this.random.nextFloat() * 120.0F;
            float pY = this.random.nextFloat() * screenHeight;
            float waveT = (float)(Math.sin(System.currentTimeMillis() / 1000.0 * this.colorSpeed.getValue()) + 1.0) / 2.0F;
            Color pColor = this.interColor(startC, endC, waveT);
            this.particleList.add(new ArrayListModule.Particle(pX, pY, pColor));
        }

        Iterator<ArrayListModule.Particle> it = this.particleList.iterator();

        while (it.hasNext()) {
            ArrayListModule.Particle p = it.next();
            if (p.isFlurry) {
                p.x = p.x + p.velocityX * deltaTime;
                p.y = p.y + p.velocityY * deltaTime;
                p.velocityY = (float)(p.velocityY + 9.8 * deltaTime);
            } else {
                p.y -= 10.0F * deltaTime;
                p.x = (float)(p.x + Math.sin(System.currentTimeMillis() / 300.0 + p.offset) * 10.0 * deltaTime);
            }

            p.life -= deltaTime;
            if (p.life <= 0.0F) {
                it.remove();
            } else {
                float alpha = p.life / p.maxLife;
                Color renderColor = this.applyAlpha(p.color, alpha);
                Renderer.get().drawRect(context, p.x, p.y, 2.0F, 2.0F, renderColor);
            }
        }
    }

    private Color interColor(Color c1, Color c2, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int r = (int)(c1.getRed() + t * (c2.getRed() - c1.getRed()));
        int g = (int)(c1.getGreen() + t * (c2.getGreen() - c1.getGreen()));
        int b = (int)(c1.getBlue() + t * (c2.getBlue() - c1.getBlue()));
        return new Color(r, g, b);
    }

    private Color applyAlpha(Color c, float alpha) {
        if (alpha >= 1.0F) {
            return c;
        } else {
            return alpha <= 0.0F
                ? new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)
                : new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(c.getAlpha() * alpha));
        }
    }

    private void updateAniStates(List<Module> enabledModules, float deltaTime) {
        String currentAnimMode = this.popAnimation.getCurrentMode();
        boolean hasAnimations = !currentAnimMode.equals("None");

        for (Module module : enabledModules) {
            String name = module.getName();
            if (!this.moduleAnimations.containsKey(name)) {
                this.moduleRemoving.put(name, false);
                this.flurryPlayed.remove(name);
                if (hasAnimations) {
                    this.moduleAnimations.put(name, 0.0F);
                    this.moduleIsNew.put(name, true);
                } else {
                    this.moduleAnimations.put(name, 1.0F);
                    this.moduleIsNew.put(name, false);
                }
            }
        }

        float speed = (float)this.animationSpeed.getValue() * 4.0F;

        for (String moduleName : new HashMap<>(this.moduleAnimations).keySet()) {
            boolean isRemoving = this.moduleRemoving.getOrDefault(moduleName, false);
            boolean isNew = this.moduleIsNew.getOrDefault(moduleName, false);
            float animProgress = this.moduleAnimations.getOrDefault(moduleName, 1.0F);
            if (hasAnimations) {
                float dt = Math.min(deltaTime, 0.05F);
                float factor = (float)(1.0 - Math.exp(-speed * dt));
                if (isRemoving) {
                    animProgress += (0.0F - animProgress) * factor;
                    if (animProgress < 0.005F) {
                        animProgress = 0.0F;
                    }
                } else if (isNew) {
                    animProgress += (1.0F - animProgress) * factor;
                    if (animProgress > 0.995F) {
                        animProgress = 1.0F;
                        this.moduleIsNew.put(moduleName, false);
                    }
                }

                this.moduleAnimations.put(moduleName, animProgress);
            } else {
                this.moduleAnimations.put(moduleName, 1.0F);
            }
        }
    }

    private void cleanupAni() {
        List<String> toCleanup = new ArrayList<>();

        for (String name : this.moduleAnimations.keySet()) {
            boolean isRemoving = this.moduleRemoving.getOrDefault(name, false);
            float progress = this.moduleAnimations.getOrDefault(name, 1.0F);
            if (isRemoving && progress <= 0.0F) {
                toCleanup.add(name);
            }
        }

        for (String namex : toCleanup) {
            this.moduleAnimations.remove(namex);
            this.moduleRemoving.remove(namex);
            this.moduleIsNew.remove(namex);
            this.flurryPlayed.remove(namex);
        }
    }

    @Environment(EnvType.CLIENT)
    private class Particle {
        float x;
        float y;
        float life;
        float maxLife;
        float offset;
        Color color;
        boolean isFlurry = false;
        float velocityX;
        float velocityY;

        public Particle(float x, float y, Color c) {
            this.x = x;
            this.y = y;
            this.color = c;
            this.maxLife = 1.5F + ArrayListModule.this.random.nextFloat() * 1.5F;
            this.life = this.maxLife;
            this.offset = ArrayListModule.this.random.nextFloat() * 10.0F;
        }
    }
}
