package com.slither.cyemer.rendering;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public class ParticleBackground {
    private static final Random RANDOM = new Random();
    private int screenWidth;
    private int screenHeight;
    private final List<ParticleBackground.Particle> particles = new ArrayList<>();
    private final List<ParticleBackground.Spark> sparks = new ArrayList<>();
    private static final int PARTICLE_COUNT = 60;
    private static final int SPARK_COUNT = 25;
    private static final int[] COLORS = new int[]{-13430341, -11197731, -8960769, -12311860, -6728193, -14548822, -10079250, -7855395};

    public void init(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.particles.clear();
        this.sparks.clear();

        for (int i = 0; i < 60; i++) {
            this.particles.add(this.spawnParticle(true));
        }

        for (int i = 0; i < 25; i++) {
            this.sparks.add(this.spawnSpark(true));
        }
    }

    private ParticleBackground.Particle spawnParticle(boolean randomY) {
        ParticleBackground.Particle p = new ParticleBackground.Particle();
        p.x = RANDOM.nextFloat() * this.screenWidth;
        p.y = randomY ? RANDOM.nextFloat() * this.screenHeight : this.screenHeight + 4;
        p.vx = (RANDOM.nextFloat() - 0.5F) * 0.3F;
        p.vy = -(0.15F + RANDOM.nextFloat() * 0.4F);
        p.size = 1 + RANDOM.nextInt(3);
        p.alpha = 0.3F + RANDOM.nextFloat() * 0.7F;
        p.fadeSpeed = 0.001F + RANDOM.nextFloat() * 0.003F;
        p.color = COLORS[RANDOM.nextInt(COLORS.length)];
        p.life = 1.0F;
        return p;
    }

    private ParticleBackground.Spark spawnSpark(boolean randomPos) {
        ParticleBackground.Spark s = new ParticleBackground.Spark();
        s.x = RANDOM.nextFloat() * this.screenWidth;
        s.y = randomPos ? RANDOM.nextFloat() * this.screenHeight : this.screenHeight + 2;
        float angle = (float)((Math.PI * 3.0 / 2.0) + (RANDOM.nextFloat() - 0.5F) * 1.2F);
        float speed = 0.8F + RANDOM.nextFloat() * 1.8F;
        s.vx = (float)Math.cos(angle) * speed;
        s.vy = (float)Math.sin(angle) * speed;
        s.life = 1.0F;
        s.decay = 0.008F + RANDOM.nextFloat() * 0.015F;
        s.length = 3 + RANDOM.nextInt(5);
        s.color = RANDOM.nextBoolean() ? -5609729 : -10074881;
        return s;
    }

    public void render(class_332 context, float delta) {
        float dt = Math.min(delta, 3.0F);

        for (int i = this.particles.size() - 1; i >= 0; i--) {
            ParticleBackground.Particle p = this.particles.get(i);
            p.x = p.x + p.vx * dt;
            p.y = p.y + p.vy * dt;
            p.life = p.life - p.fadeSpeed * dt;
            if (!(p.life <= 0.0F) && !(p.y < -10.0F) && !(p.x < -10.0F) && !(p.x > this.screenWidth + 10)) {
                int alpha = (int)(p.alpha * p.life * 255.0F);
                alpha = Math.max(0, Math.min(255, alpha));
                int color = p.color & 16777215 | alpha << 24;
                int px = (int)p.x;
                int py = (int)p.y;
                int sz = p.size;
                context.method_25294(px, py, px + sz, py + sz, color);
            } else {
                this.particles.set(i, this.spawnParticle(false));
            }
        }

        for (int ix = this.sparks.size() - 1; ix >= 0; ix--) {
            ParticleBackground.Spark s = this.sparks.get(ix);
            s.x = s.x + s.vx * dt;
            s.y = s.y + s.vy * dt;
            s.vy += 0.02F * dt;
            s.life = s.life - s.decay * dt;
            if (!(s.life <= 0.0F) && !(s.y < -20.0F) && !(s.y > this.screenHeight + 20)) {
                int alpha = (int)(s.life * 220.0F);
                alpha = Math.max(0, Math.min(255, alpha));
                int color = s.color & 16777215 | alpha << 24;
                float tailX = s.x - s.vx * s.length;
                float tailY = s.y - s.vy * s.length;
                this.drawLine(context, s.x, s.y, tailX, tailY, color);
            } else {
                this.sparks.set(ix, this.spawnSpark(false));
            }
        }
    }

    private void drawLine(class_332 context, float x1, float y1, float x2, float y2, int color) {
        int steps = Math.max(1, (int)Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)));

        for (int i = 0; i <= steps; i++) {
            float t = (float)i / steps;
            int px = (int)(x1 + (x2 - x1) * t);
            int py = (int)(y1 + (y2 - y1) * t);
            context.method_25294(px, py, px + 1, py + 1, color);
        }
    }

    @Environment(EnvType.CLIENT)
    private static class Particle {
        float x;
        float y;
        float vx;
        float vy;
        float alpha;
        float fadeSpeed;
        float life;
        int size;
        int color;
    }

    @Environment(EnvType.CLIENT)
    private static class Spark {
        float x;
        float y;
        float vx;
        float vy;
        float life;
        float decay;
        float length;
        int color;
    }
}
