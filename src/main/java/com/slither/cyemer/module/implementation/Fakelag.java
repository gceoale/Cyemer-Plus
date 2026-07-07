package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ColorSetting;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import java.awt.Color;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.class_12249;
import net.minecraft.class_1657;
import net.minecraft.class_1753;
import net.minecraft.class_1799;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2596;
import net.minecraft.class_2797;
import net.minecraft.class_2799;
import net.minecraft.class_2827;
import net.minecraft.class_332;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_9334;
import net.minecraft.class_9779;
import net.minecraft.class_9799;
import net.minecraft.class_4587.class_4665;
import net.minecraft.class_4597.class_4598;

@Environment(EnvType.CLIENT)
public class Fakelag extends Module {
    private static Fakelag instance;
    private final SliderSetting minDelay = new SliderSetting("Min Delay", 200.0, 50.0, 1000.0, 0);
    private final SliderSetting maxDelay = new SliderSetting("Max Delay", 400.0, 50.0, 1000.0, 0);
    private final BooleanSetting showBar = new BooleanSetting("Show Bar", true);
    private final BooleanSetting showMs = new BooleanSetting("Show MS", true);
    private final BooleanSetting avoidMode = new BooleanSetting("Avoid Mode", true);
    private final BooleanSetting disableOnElytra = new BooleanSetting("Disable on Elytra", true);
    private final BooleanSetting flushOnAction = new BooleanSetting("Flush on Action", true);
    final BooleanSetting highlight = new BooleanSetting("Highlight", true);
    private final ModeSetting highlightMode = new ModeSetting("Mode", "Both", "Box", "Lines");
    private final ColorSetting highlightColor = new ColorSetting("Color", new Color(200, 50, 50, 120));
    private final Queue<class_2596<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private final Random random = new Random();
    private long lastSendTime;
    private long currentDelay;
    private boolean sendingQueue = false;
    private class_243 serverSidePosition;
    private class_243 prevServerSidePosition;

    public Fakelag() {
        super("Fakelag", "Simulates network latency by delaying packets.", Category.PLAYER);
        this.addSetting(this.minDelay);
        this.addSetting(this.maxDelay);
        this.addSetting(this.showBar);
        this.addSetting(this.showMs);
        this.addSetting(this.avoidMode);
        this.addSetting(this.disableOnElytra);
        this.addSetting(this.flushOnAction);
        this.addSetting(this.highlight);
        this.addSetting(this.highlightMode);
        this.addSetting(this.highlightColor);
        instance = this;
        WorldRenderEvents.END_MAIN.register(this::renderHighlight);
    }

    public static Fakelag getInstance() {
        return instance;
    }

    public void onHudRender(class_332 context, class_9779 tickDelta) {
        if ((this.showBar.isEnabled() || this.showMs.isEnabled()) && this.mc.field_1724 != null && this.currentDelay != 0L) {
            long timeElapsed = System.currentTimeMillis() - this.lastSendTime;
            long remainingMillis = Math.max(0L, this.currentDelay - timeElapsed);
            float progress = (float)remainingMillis / (float)this.currentDelay;
            int screenWidth = context.method_51421();
            int screenHeight = context.method_51443();
            if (this.showBar.isEnabled()) {
                int barWidth = 80;
                int barHeight = 5;
                int barX = (screenWidth - barWidth) / 2;
                int barY = screenHeight / 2 + 10;
                int red = (int)(255.0F * (1.0F - progress));
                int green = (int)(255.0F * progress);
                int color = 0xFF000000 | red << 16 | green << 8;
                context.method_25294(barX, barY, barX + barWidth, barY + barHeight, Integer.MIN_VALUE);
                context.method_25294(barX, barY, barX + (int)(barWidth * progress), barY + barHeight, color);
            }

            if (this.showMs.isEnabled()) {
                String timerText = String.format("%dms", remainingMillis);
                int textWidth = this.mc.field_1772.method_1727(timerText);
                int textX = (screenWidth - textWidth) / 2;
                int textY = screenHeight / 2 + 10;
                if (this.showBar.isEnabled()) {
                    textY += 7;
                }

                context.method_25303(this.mc.field_1772, timerText, textX, textY, Color.WHITE.getRGB());
            }
        }
    }

    private void renderHighlight(WorldRenderContext context) {
        if (this.highlight.isEnabled() && this.serverSidePosition != null && this.prevServerSidePosition != null && this.mc.field_1724 != null) {
            class_4587 matrices = context.matrices();
            class_4184 camera = context.gameRenderer().method_19418();
            float tickDelta = this.mc.method_61966().method_60637(true);
            class_243 interpolatedPosition = this.prevServerSidePosition.method_35590(this.serverSidePosition, tickDelta);
            if (!this.mc.field_1690.method_31044().method_31034() || !(camera.method_71156().method_1025(interpolatedPosition) < 4.0)) {
                Color color = this.highlightColor.getValue();
                float r = color.getRed() / 255.0F;
                float g = color.getGreen() / 255.0F;
                float b = color.getBlue() / 255.0F;
                float a = color.getAlpha() / 255.0F;
                class_238 renderBox = this.mc.field_1724.method_5829().method_997(interpolatedPosition.method_1020(this.mc.field_1724.method_73189()));
                matrices.method_22903();
                class_243 camPos = camera.method_71156();
                matrices.method_22904(-camPos.field_1352, -camPos.field_1351, -camPos.field_1350);
                class_9799 allocator = new class_9799(1536);

                try {
                    class_4598 vertexConsumers = class_4597.method_22991(allocator);
                    class_4588 lineConsumer = vertexConsumers.method_73477(class_12249.method_76015());
                    String mode = this.highlightMode.getCurrentMode();
                    if (mode.equals("Box") || mode.equals("Both")) {
                        drawBox(
                            matrices,
                            lineConsumer,
                            renderBox.field_1323,
                            renderBox.field_1322,
                            renderBox.field_1321,
                            renderBox.field_1320,
                            renderBox.field_1325,
                            renderBox.field_1324,
                            r,
                            g,
                            b,
                            a
                        );
                    }

                    if (mode.equals("Lines") || mode.equals("Both")) {
                        class_243 playerFeet = this.mc.field_1724.method_73189();
                        class_243 corner1 = new class_243(renderBox.field_1323, renderBox.field_1322, renderBox.field_1321);
                        class_243 corner2 = new class_243(renderBox.field_1320, renderBox.field_1322, renderBox.field_1321);
                        class_243 corner3 = new class_243(renderBox.field_1320, renderBox.field_1322, renderBox.field_1324);
                        class_243 corner4 = new class_243(renderBox.field_1323, renderBox.field_1322, renderBox.field_1324);
                        class_4665 entry = matrices.method_23760();
                        this.drawLine(lineConsumer, entry, playerFeet, corner1, r, g, b, a);
                        this.drawLine(lineConsumer, entry, playerFeet, corner2, r, g, b, a);
                        this.drawLine(lineConsumer, entry, playerFeet, corner3, r, g, b, a);
                        this.drawLine(lineConsumer, entry, playerFeet, corner4, r, g, b, a);
                    }

                    vertexConsumers.method_22993();
                } catch (Throwable var24) {
                    try {
                        allocator.close();
                    } catch (Throwable var23) {
                        var24.addSuppressed(var23);
                    }

                    throw var24;
                }

                allocator.close();
                matrices.method_22909();
            }
        }
    }

    private void drawLine(class_4588 consumer, class_4665 matrixEntry, class_243 p1, class_243 p2, float r, float g, float b, float a) {
        consumer.method_56824(matrixEntry, (float)p1.field_1352, (float)p1.field_1351, (float)p1.field_1350)
            .method_22915(r, g, b, a)
            .method_75298(2.0F)
            .method_22914(0.0F, 1.0F, 0.0F);
        consumer.method_56824(matrixEntry, (float)p2.field_1352, (float)p2.field_1351, (float)p2.field_1350)
            .method_22915(r, g, b, a)
            .method_75298(2.0F)
            .method_22914(0.0F, 1.0F, 0.0F);
    }

    private static void drawBox(
        class_4587 matrices,
        class_4588 vertexConsumer,
        double x1,
        double y1,
        double z1,
        double x2,
        double y2,
        double z2,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        class_4665 entry = matrices.method_23760();
        float f = (float)x1;
        float g = (float)y1;
        float h = (float)z1;
        float i = (float)x2;
        float j = (float)y2;
        float k = (float)z2;
        vertexConsumer.method_56824(entry, f, g, h).method_22915(red, green, blue, alpha).method_75298(2.0F).method_60831(entry, 1.0F, 0.0F, 0.0F);
        vertexConsumer.method_56824(entry, i, g, h).method_22915(red, green, blue, alpha).method_75298(2.0F).method_60831(entry, 1.0F, 0.0F, 0.0F);
        vertexConsumer.method_56824(entry, f, g, h).method_22915(red, green, blue, alpha).method_75298(2.0F).method_60831(entry, 0.0F, 1.0F, 0.0F);
        vertexConsumer.method_56824(entry, f, j, h).method_22915(red, green, blue, alpha).method_75298(2.0F).method_60831(entry, 0.0F, 1.0F, 0.0F);
        vertexConsumer.method_56824(entry, f, g, h).method_22915(red, green, blue, alpha).method_75298(2.0F).method_60831(entry, 0.0F, 0.0F, 1.0F);
        vertexConsumer.method_56824(entry, f, g, k).method_22915(red, green, blue, alpha).method_75298(2.0F).method_60831(entry, 0.0F, 0.0F, 1.0F);
        vertexConsumer.method_56824(entry, i, g, h).method_22915(red, green, blue, alpha).method_75298(2.0F).method_60831(entry, 0.0F, 1.0F, 0.0F);
        vertexConsumer.method_56824(entry, i, j, h).method_22915(red, green, blue, alpha).method_75298(2.0F).method_60831(entry, 0.0F, 1.0F, 0.0F);
        vertexConsumer.method_56824(entry, i, j, h).method_22915(red, green, blue, alpha).method_75298(2.0F).method_60831(entry, 0.0F, 0.0F, 1.0F);
        vertexConsumer.method_56824(entry, i, j, k).method_22915(red, green, blue, alpha).method_75298(2.0F).method_60831(entry, 0.0F, 0.0F, 1.0F);
        vertexConsumer.method_56824(entry, f, j, h).method_22915(red, green, blue, alpha).method_75298(2.0F).method_60831(entry, 0.0F, 0.0F, 1.0F);
        vertexConsumer.method_56824(entry, f, j, k).method_22915(red, green, blue, alpha).method_75298(2.0F).method_60831(entry, 0.0F, 0.0F, 1.0F);
        vertexConsumer.method_56824(entry, f, j, k).method_22915(red, green, blue, alpha).method_75298(2.0F).method_60831(entry, 1.0F, 0.0F, 0.0F);
        vertexConsumer.method_56824(entry, i, j, k).method_22915(red, green, blue, alpha).method_75298(2.0F).method_60831(entry, 1.0F, 0.0F, 0.0F);
        vertexConsumer.method_56824(entry, i, g, k).method_22915(red, green, blue, alpha).method_75298(2.0F).method_60831(entry, 0.0F, 1.0F, 0.0F);
        vertexConsumer.method_56824(entry, i, j, k).method_22915(red, green, blue, alpha).method_75298(2.0F).method_60831(entry, 0.0F, 1.0F, 0.0F);
        vertexConsumer.method_56824(entry, f, g, k).method_22915(red, green, blue, alpha).method_75298(2.0F).method_60831(entry, 1.0F, 0.0F, 0.0F);
        vertexConsumer.method_56824(entry, i, g, k).method_22915(red, green, blue, alpha).method_75298(2.0F).method_60831(entry, 1.0F, 0.0F, 0.0F);
    }

    @Override
    public void onEnable() {
        if (this.mc.field_1724 != null) {
            this.serverSidePosition = this.mc.field_1724.method_73189();
            this.prevServerSidePosition = this.mc.field_1724.method_73189();
        }

        this.packetQueue.clear();
        this.resetTimer();
    }

    @Override
    public void onDisable() {
        this.sendQueuedPackets();
        this.serverSidePosition = null;
        this.prevServerSidePosition = null;
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            if (this.serverSidePosition != null) {
                this.prevServerSidePosition = this.serverSidePosition;
            }

            if (this.avoidMode.isEnabled() && !this.packetQueue.isEmpty() && this.serverSidePosition != null) {
                class_238 serverSideBox = this.mc.field_1724.method_5829().method_997(this.serverSidePosition.method_1020(this.mc.field_1724.method_73189()));

                for (class_1657 player : this.mc.field_1687.method_18456()) {
                    if (player != this.mc.field_1724 && player.method_5805() && !(this.mc.field_1724.method_5739(player) > 5.0F)) {
                        class_243 eyePos = player.method_33571();
                        class_243 lookVec = player.method_5720();
                        Optional<class_243> hit = serverSideBox.method_992(eyePos, eyePos.method_1019(lookVec.method_1021(8.0)));
                        if (hit.isPresent()) {
                            this.sendQueuedPackets();
                            return;
                        }
                    }
                }
            }

            if (System.currentTimeMillis() - this.lastSendTime > this.currentDelay) {
                this.sendQueuedPackets();
            }
        }
    }

    public boolean handleOutgoingPacket(class_2596<?> packet) {
        if (this.mc.field_1724 == null || !this.mc.field_1724.method_5805()) {
            this.sendQueuedPackets();
            return false;
        } else if (this.sendingQueue) {
            return false;
        } else if (this.disableOnElytra.isEnabled() && this.mc.field_1724.method_6128()) {
            this.sendQueuedPackets();
            return false;
        } else if (this.flushOnAction.isEnabled() && this.isPlayerInAction()) {
            this.sendQueuedPackets();
            return false;
        } else if (!(packet instanceof class_2827) && !(packet instanceof class_2799) && !(packet instanceof class_2797)) {
            this.packetQueue.add(packet);
            return true;
        } else {
            return false;
        }
    }

    private boolean isPlayerInAction() {
        if (this.mc.field_1724 == null) {
            return false;
        } else if (this.mc.field_1724.field_6252) {
            return true;
        } else {
            if (this.mc.field_1724.method_6115()) {
                class_1799 activeStack = this.mc.field_1724.method_6030();
                if (activeStack.method_57353().method_57832(class_9334.field_50075) || activeStack.method_7909() instanceof class_1753) {
                    return true;
                }
            }

            return false;
        }
    }

    public void sendQueuedPackets() {
        if (this.mc.method_1562() != null && !this.packetQueue.isEmpty()) {
            this.sendingQueue = true;

            try {
                while (!this.packetQueue.isEmpty()) {
                    this.mc.method_1562().method_52787(this.packetQueue.poll());
                }
            } finally {
                this.sendingQueue = false;
            }
        }

        this.resetTimer();
    }

    private void resetTimer() {
        this.lastSendTime = System.currentTimeMillis();
        this.currentDelay = this.calculateNextDelay();
        if (this.mc.field_1724 != null) {
            this.serverSidePosition = this.mc.field_1724.method_73189();
            if (this.prevServerSidePosition == null) {
                this.prevServerSidePosition = this.serverSidePosition;
            }
        }
    }

    private long calculateNextDelay() {
        double min = this.minDelay.getValue();
        double max = this.maxDelay.getValue();
        return min >= max ? (long)min : this.random.nextLong((long)(max - min + 1.0)) + (long)min;
    }
}
