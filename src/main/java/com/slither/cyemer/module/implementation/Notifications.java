package com.slither.cyemer.module.implementation;

import com.slither.cyemer.gui.new_ui.notifications.Notification;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;
import net.minecraft.class_3532;

@Environment(EnvType.CLIENT)
public class Notifications extends Module {
    private final SliderSetting duration = new SliderSetting("Duration", 3.0, 1.0, 10.0, 1);
    private final SliderSetting size = new SliderSetting("Size", 1.0, 0.6, 1.8, 2);
    private final BooleanSetting ambientGlow = new BooleanSetting("Ambient Glow", true);
    private final List<Notification> notifications = new CopyOnWriteArrayList<>();

    public Notifications() {
        super("Notifications", "Visual status feedback.", Category.CLIENT);
        this.setEnabled(true);
        this.addSetting(this.duration);
        this.addSetting(this.size);
        this.addSetting(this.ambientGlow);
    }

    public void show(String title, String message, Notification.NotificationType type) {
        if (this.isEnabled()) {
            long durationMs = (long)(this.duration.getValue() * 1000.0);
            this.notifications.add(new Notification(title, message, type, durationMs));
        }
    }

    @Override
    public void onRender(class_332 context, float tickDelta) {
        if (!this.notifications.isEmpty()) {
            int screenWidth = this.mc.method_22683().method_4486();
            int screenHeight = this.mc.method_22683().method_4502();
            float pixelRatio = this.mc.method_22683().method_4495();
            if (Renderer.get().beginFrame(screenWidth, screenHeight, pixelRatio)) {
                double scale = this.size.getValue();
                double currentY = screenHeight - 40.0 * scale;
                this.notifications.removeIf(Notification::isExpired);

                for (Notification notification : this.notifications) {
                    float animation = (float)notification.getAniProg();
                    float width = (float)(150.0 * scale);
                    float height = (float)(32.0 * scale);
                    double targetX = screenWidth - width - 10.0 * scale;
                    double offscreenX = screenWidth + 10;
                    double currentX = offscreenX + (targetX - offscreenX) * animation;
                    this.drawNotif(context, (float)currentX, (float)(currentY - height), width, height, notification, animation, (float)scale);
                    currentY -= height + 6.0 * scale;
                }

                Renderer.get().endFrame();
            }
        }
    }

    private void drawNotif(class_332 context, float x, float y, float width, float height, Notification notification, float alpha, float scale) {
        Notification.NotificationType type = notification.getType();
        Color accent = this.getTypeColor(type);
        if (this.ambientGlow.isEnabled()) {
            Renderer.get().drawRoundedRect(context, x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F, 4.0F * scale, this.applyOpacity(accent, alpha * 0.2F));
        }

        Color bg = this.applyOpacity(new Color(15, 15, 18), alpha * 0.9F);
        Renderer.get().drawRoundedRect(context, x, y, width, height, 3.0F * scale, bg);
        float life = 1.0F - this.getLifeProgress(notification);
        float barPadding = 3.0F * scale;
        float maxBarHeight = height - barPadding * 2.0F;
        float progressHeight = maxBarHeight * life;
        Renderer.get()
            .drawRoundedRect(
                context,
                x + 2.0F * scale,
                y + barPadding + (maxBarHeight - progressHeight),
                2.0F * scale,
                progressHeight,
                1.0F * scale,
                this.applyOpacity(accent, alpha)
            );
        float iconSize = 12.0F * scale;
        float centerX = x + 16.0F * scale;
        float centerY = y + height / 2.0F;
        Renderer.get()
            .drawRoundedRect(
                context,
                centerX - 10.0F * scale,
                centerY - 10.0F * scale,
                20.0F * scale,
                20.0F * scale,
                10.0F * scale,
                this.applyOpacity(accent, alpha * 0.15F)
            );
        this.drawTypeIcon(context, centerX, centerY, iconSize * 0.5F, type, this.applyOpacity(accent, alpha), scale);
        float textX = x + 32.0F * scale;
        Renderer.get().drawText(context, notification.getTitle(), textX, y + 6.0F * scale, 10.0F * scale, this.applyOpacity(Color.WHITE, alpha), false);
        if (notification.getMessage() != null) {
            Renderer.get()
                .drawText(
                    context,
                    notification.getMessage(),
                    textX,
                    y + 18.0F * scale,
                    7.0F * scale,
                    this.applyOpacity(new Color(180, 180, 180), alpha * 0.8F),
                    false
                );
        }
    }

    private void drawTypeIcon(class_332 context, float cx, float cy, float s, Notification.NotificationType type, Color c, float scale) {
        float t = 1.5F * scale;
        switch (type) {
            case SUCCESS:
                this.drawLine(context, cx - s, cy, cx - s / 3.0F, cy + s, t, c);
                this.drawLine(context, cx - s / 3.0F, cy + s, cx + s, cy - s, t, c);
                break;
            case ERROR:
                this.drawLine(context, cx - s, cy - s, cx + s, cy + s, t, c);
                this.drawLine(context, cx + s, cy - s, cx - s, cy + s, t, c);
                break;
            case WARNING:
                this.drawLine(context, cx, cy - s, cx - s, cy + s, t, c);
                this.drawLine(context, cx, cy - s, cx + s, cy + s, t, c);
                this.drawLine(context, cx - s, cy + s, cx + s, cy + s, t, c);
                break;
            case INFO:
                Renderer.get().drawRoundedRect(context, cx - t / 2.0F, cy - s, t, t, 0.0F, c);
                Renderer.get().drawRoundedRect(context, cx - t / 2.0F, cy - s / 3.0F, t, s * 1.2F, 0.0F, c);
        }
    }

    private Color getTypeColor(Notification.NotificationType type) {
        return switch (type) {
            case SUCCESS -> new Color(46, 255, 113);
            case ERROR -> new Color(255, 66, 66);
            case WARNING -> new Color(255, 184, 0);
            case INFO -> new Color(66, 165, 255);
        };
    }

    private void drawLine(class_332 context, float x1, float y1, float x2, float y2, float thickness, Color color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float)Math.sqrt(dx * dx + dy * dy);
        float angle = (float)Math.atan2(dy, dx);
        Renderer.get().save();
        Renderer.get().translate(x1, y1);
        Renderer.get().rotate(angle);
        Renderer.get().drawRoundedRect(context, 0.0F, -thickness / 2.0F, length, thickness, thickness / 2.0F, color);
        Renderer.get().restore();
    }

    private float getLifeProgress(Notification n) {
        return class_3532.method_15363((float)(System.currentTimeMillis() - n.getCreationTime()) / (float)n.getDuration(), 0.0F, 1.0F);
    }

    private Color applyOpacity(Color c, float a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(c.getAlpha() * class_3532.method_15363(a, 0.0F, 1.0F)));
    }

    public void success(String t, String m) {
        this.show(t, m, Notification.NotificationType.SUCCESS);
    }

    public void warn(String t, String m) {
        this.show(t, m, Notification.NotificationType.WARNING);
    }

    public void error(String t, String m) {
        this.show(t, m, Notification.NotificationType.ERROR);
    }

    public void info(String t, String m) {
        this.show(t, m, Notification.NotificationType.INFO);
    }
}
