package com.slither.cyemer.gui.new_ui.notifications;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_327;

@Environment(EnvType.CLIENT)
public class Notification {
    private final String title;
    private final String message;
    private final Notification.NotificationType type;
    private final long creationTime;
    private final long duration;
    private final double width;
    private final double height;
    private static final double ANIMATION_TIME = 400.0;

    public Notification(String title, String message, Notification.NotificationType type, long duration) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.duration = duration;
        this.creationTime = System.currentTimeMillis();
        class_327 textRenderer = class_310.method_1551().field_1772;
        int titleWidth = textRenderer.method_1727(title);
        int messageWidth = textRenderer.method_1727(message);
        int maxTextWidth = Math.max(titleWidth, messageWidth);
        this.width = 40 + maxTextWidth + 24;
        this.height = message.isEmpty() ? 40.0 : 56.0;
    }

    public Notification(String title, Notification.NotificationType type, long duration) {
        this(title, "", type, duration);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > this.creationTime + this.duration + 800.0;
    }

    public double getAniProg() {
        long currentTime = System.currentTimeMillis();
        long timeSinceCreation = currentTime - this.creationTime;
        if (timeSinceCreation < 400.0) {
            double linear = timeSinceCreation / 400.0;
            return this.easeOutCubic(linear);
        } else if (currentTime > this.creationTime + this.duration + 400.0) {
            long timeIntoFadeOut = (long)(currentTime - (this.creationTime + this.duration + 400.0));
            double linear = 1.0 - Math.min(timeIntoFadeOut / 400.0, 1.0);
            return this.easeInCubic(linear);
        } else {
            return 1.0;
        }
    }

    private double easeOutCubic(double x) {
        return 1.0 - Math.pow(1.0 - x, 3.0);
    }

    private double easeInCubic(double x) {
        return Math.pow(x, 3.0);
    }

    public String getTitle() {
        return this.title;
    }

    public String getMessage() {
        return this.message;
    }

    public Notification.NotificationType getType() {
        return this.type;
    }

    public double getWidth() {
        return this.width;
    }

    public double getHeight() {
        return this.height;
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    public long getDuration() {
        return this.duration;
    }

    @Environment(EnvType.CLIENT)
    public static enum NotificationType {
        SUCCESS,
        WARNING,
        ERROR,
        INFO;
    }
}
