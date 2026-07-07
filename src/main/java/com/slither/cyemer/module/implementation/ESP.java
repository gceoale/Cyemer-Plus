package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ColorSetting;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.render.RenderUtils;
import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1303;
import net.minecraft.class_1309;
import net.minecraft.class_1542;
import net.minecraft.class_1657;
import net.minecraft.class_2199;
import net.minecraft.class_2244;
import net.minecraft.class_2248;
import net.minecraft.class_2304;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2580;
import net.minecraft.class_2586;
import net.minecraft.class_2589;
import net.minecraft.class_2595;
import net.minecraft.class_2601;
import net.minecraft.class_2605;
import net.minecraft.class_2608;
import net.minecraft.class_2611;
import net.minecraft.class_2614;
import net.minecraft.class_2627;
import net.minecraft.class_2818;
import net.minecraft.class_3532;
import net.minecraft.class_3719;
import net.minecraft.class_3720;
import net.minecraft.class_3723;
import net.minecraft.class_3866;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_9799;
import net.minecraft.class_4597.class_4598;

@Environment(EnvType.CLIENT)
public class ESP extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "Box", "Box See-Through", "Effect", "Sphere", "Cylinder", "Cone");
    private final ColorSetting color = new ColorSetting("Color 1", new Color(255, 50, 50));
    private final BooleanSetting gradient = new BooleanSetting("Gradient", false);
    private final ColorSetting gradientColor = new ColorSetting("Color 2", new Color(255, 90, 220));
    private final SliderSetting gradientSpeed = new SliderSetting("Gradient Speed", 1.0, 0.1, 5.0, 1);
    private final BooleanSetting opaque = new BooleanSetting("Opaque", false);
    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls", true);
    private final SliderSetting sizeMultiplier = new SliderSetting("Size", 1.0, 0.1, 3.0, 1);
    public final BooleanSetting hideArmor = new BooleanSetting("Hide Armor", false);
    private final BooleanSetting utilityBlocks = new BooleanSetting("Utility Blocks", false);
    private final BooleanSetting tracers = new BooleanSetting("Tracers", false);
    private final BooleanSetting entities = new BooleanSetting("Entities", false);
    private final ColorSetting utilityColor = new ColorSetting("Utility Color", new Color(100, 255, 100));
    private final SliderSetting utilityRange = new SliderSetting("Utility Range", 32.0, 8.0, 64.0, 1);
    private final ColorSetting tracerColor = new ColorSetting("Tracer Color", new Color(255, 255, 50));
    private final ColorSetting entityColor = new ColorSetting("Entity Color", new Color(50, 150, 255));
    private Set<class_2338> utilityBlockCache = new HashSet<>();
    private long lastUtilityScan = 0L;
    private static final long UTILITY_SCAN_INTERVAL = 1000L;

    public ESP() {
        super("ESP", "Extra Sensory Perception (See players through walls)", Category.RENDER);
        this.addSetting(this.mode);
        this.addSetting(this.color);
        this.addSetting(this.gradient);
        this.addSetting(this.gradientColor);
        this.addSetting(this.gradientSpeed);
        this.addSetting(this.opaque);
        this.addSetting(this.throughWalls);
        this.addSetting(this.sizeMultiplier);
        this.addSetting(this.hideArmor);
        this.addSetting(this.utilityBlocks);
        this.addSetting(this.utilityColor);
        this.addSetting(this.utilityRange);
        this.addSetting(this.tracers);
        this.addSetting(this.tracerColor);
        this.addSetting(this.entities);
        this.addSetting(this.entityColor);
    }

    public boolean shouldApplyGlow() {
        return this.isEnabled() && this.mode.getCurrentMode().equals("Effect");
    }

    @Override
    public void onWorldRender(class_4587 matrices, float tickDelta) {
        if (!this.mode.getCurrentMode().equals("Effect")) {
            if (this.mc.field_1687 != null && this.mc.field_1724 != null) {
                class_243 cameraPos = this.mc.field_1773.method_19418().method_71156();
                long now = System.currentTimeMillis();
                class_9799 allocator = new class_9799(1536);

                try {
                    class_4598 immediate = class_4597.method_22991(allocator);

                    for (class_1657 entity : this.mc.field_1687.method_18456()) {
                        if (this.isValid(entity)) {
                            double x = class_3532.method_16436(tickDelta, entity.field_6014, entity.method_23317()) - cameraPos.field_1352;
                            double y = class_3532.method_16436(tickDelta, entity.field_6036, entity.method_23318()) - cameraPos.field_1351;
                            double z = class_3532.method_16436(tickDelta, entity.field_5969, entity.method_23321()) - cameraPos.field_1350;
                            Color playerColor = this.resolveRenderColor(this.color.getValue(), now, entity.method_5628(), entity.method_23318());
                            this.renderEspForEntity(matrices, entity, immediate, x, y, z, playerColor);
                        }
                    }

                    if (this.utilityBlocks.isEnabled()) {
                        this.renderUtilityBlocks(matrices, immediate, cameraPos, now);
                    }

                    if (this.entities.isEnabled()) {
                        this.renderEntities(matrices, immediate, cameraPos, tickDelta, now);
                    }

                    immediate.method_22993();
                } catch (Throwable var19) {
                    try {
                        allocator.close();
                    } catch (Throwable var17) {
                        var19.addSuppressed(var17);
                    }

                    throw var19;
                }

                allocator.close();
                if (this.tracers.isEnabled()) {
                    try {
                        this.renderTracers(matrices, cameraPos, tickDelta, now);
                    } catch (Throwable var18) {
                    }
                }
            }
        }
    }

    private void renderUtilityBlocks(class_4587 matrices, class_4597 vertexConsumers, class_243 cameraPos, long now) {
        int alpha = this.opaque.isEnabled() ? 255 : 80;
        float alphaF = alpha / 255.0F;
        int range = (int)this.utilityRange.getValue();
        class_2338 playerPos = this.mc.field_1724.method_24515();
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastUtilityScan > 1000L) {
            this.utilityBlockCache.clear();
            this.scanUtilityBlocks(playerPos, range);
            this.lastUtilityScan = currentTime;
        }

        for (class_2338 pos : this.utilityBlockCache) {
            if (!(pos.method_10262(playerPos) > range * range)) {
                Color espColor = this.resolveRenderColor(this.utilityColor.getValue(), now, pos.hashCode(), pos.method_10264());
                this.renderUtilityBox(matrices, vertexConsumers, pos, cameraPos, espColor, alphaF);
            }
        }
    }

    private void scanUtilityBlocks(class_2338 playerPos, int range) {
        int searchRange = Math.min(range, 32);
        int minChunkX = playerPos.method_10263() - searchRange >> 4;
        int maxChunkX = playerPos.method_10263() + searchRange >> 4;
        int minChunkZ = playerPos.method_10260() - searchRange >> 4;
        int maxChunkZ = playerPos.method_10260() + searchRange >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (this.mc.field_1687.method_8393(chunkX, chunkZ)) {
                    class_2818 chunk = this.mc.field_1687.method_8497(chunkX, chunkZ);

                    for (class_2338 pos : chunk.method_12021()) {
                        if (!(pos.method_10262(playerPos) > range * range)) {
                            class_2586 blockEntity = chunk.method_8321(pos);
                            if (blockEntity != null && this.isUtilityBlock(blockEntity)) {
                                this.utilityBlockCache.add(pos.method_10062());
                            }
                        }
                    }
                }
            }
        }

        int minY = Math.max(this.mc.field_1687.method_31607(), playerPos.method_10264() - searchRange);
        int maxY = Math.min(320, playerPos.method_10264() + searchRange);

        for (int x = playerPos.method_10263() - searchRange; x <= playerPos.method_10263() + searchRange; x++) {
            for (int z = playerPos.method_10260() - searchRange; z <= playerPos.method_10260() + searchRange; z++) {
                for (int y = minY; y <= maxY; y++) {
                    class_2338 posx = new class_2338(x, y, z);
                    if (!(posx.method_10262(playerPos) > range * range)) {
                        class_2248 block = this.mc.field_1687.method_8320(posx).method_26204();
                        if (this.isUtilityBlockType(block)) {
                            this.utilityBlockCache.add(posx.method_10062());
                        }
                    }
                }
            }
        }
    }

    private void renderUtilityBox(class_4587 matrices, class_4597 vertexConsumers, class_2338 pos, class_243 cameraPos, Color color, float alpha) {
        double bx = pos.method_10263() - cameraPos.field_1352;
        double by = pos.method_10264() - cameraPos.field_1351;
        double bz = pos.method_10260() - cameraPos.field_1350;
        matrices.method_22903();
        matrices.method_22904(bx, by, bz);
        class_238 box = new class_238(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        RenderUtils.drawBoxEsp(matrices, vertexConsumers, box, color, alpha, false);
        matrices.method_22909();
    }

    private void renderTracers(class_4587 matrices, class_243 cameraPos, float tickDelta, long now) {
        float alpha = this.opaque.isEnabled() ? 1.0F : 0.8F;
        class_9799 allocator = new class_9799(1536);

        try {
            class_4598 immediate = class_4597.method_22991(allocator);
            class_243 start = new class_243(0.0, this.mc.field_1724.method_5751(), 0.0);

            for (class_1657 entity : this.mc.field_1687.method_18456()) {
                if (this.isValid(entity)) {
                    double x = class_3532.method_16436(tickDelta, entity.field_6014, entity.method_23317()) - cameraPos.field_1352;
                    double y = class_3532.method_16436(tickDelta, entity.field_6036, entity.method_23318())
                        - cameraPos.field_1351
                        + entity.method_17682() / 2.0;
                    double z = class_3532.method_16436(tickDelta, entity.field_5969, entity.method_23321()) - cameraPos.field_1350;
                    Color espColor = this.resolveRenderColor(this.tracerColor.getValue(), now, entity.method_5628(), entity.method_23318());
                    RenderUtils.drawLine(matrices, immediate, start, new class_243(x, y, z), espColor, alpha);
                }
            }

            immediate.method_22993();
        } catch (Throwable var20) {
            try {
                allocator.close();
            } catch (Throwable var19) {
                var20.addSuppressed(var19);
            }

            throw var20;
        }

        allocator.close();
    }

    private void renderEntities(class_4587 matrices, class_4597 vertexConsumers, class_243 cameraPos, float tickDelta, long now) {
        int alpha = this.opaque.isEnabled() ? 255 : 80;
        float alphaF = alpha / 255.0F;
        double size = this.sizeMultiplier.getValue();

        for (class_1297 entity : this.mc.field_1687.method_18112()) {
            if (entity != this.mc.field_1724
                && !(entity instanceof class_1657)
                && !(entity instanceof class_1542)
                && !(entity instanceof class_1303)
                && entity.method_5805()) {
                double x = class_3532.method_16436(tickDelta, entity.field_6014, entity.method_23317()) - cameraPos.field_1352;
                double y = class_3532.method_16436(tickDelta, entity.field_6036, entity.method_23318()) - cameraPos.field_1351;
                double z = class_3532.method_16436(tickDelta, entity.field_5969, entity.method_23321()) - cameraPos.field_1350;
                matrices.method_22903();
                matrices.method_22904(x, y, z);
                Color espColor = this.resolveRenderColor(this.entityColor.getValue(), now, entity.method_5628(), entity.method_23318());
                this.renderEntityWithMode(matrices, entity, vertexConsumers, espColor, alphaF, size);
                matrices.method_22909();
            }
        }
    }

    private void renderEntityWithMode(class_4587 matrices, class_1297 entity, class_4597 vertexConsumers, Color espColor, float alphaF, double size) {
        String currentMode = this.mode.getCurrentMode();
        boolean seeThrough = this.shouldRenderSeeThrough(currentMode);
        if (currentMode.equals("Box") || currentMode.equals("Box See-Through")) {
            class_238 box = entity.method_5829().method_989(-entity.method_23317(), -entity.method_23318(), -entity.method_23321());
            box = this.scaleBox(box, size);
            RenderUtils.drawBoxEsp(matrices, vertexConsumers, box, espColor, alphaF, seeThrough);
        } else if (currentMode.equals("Sphere")) {
            class_243 center = new class_243(0.0, entity.method_17682() / 2.0, 0.0);
            double radius = Math.max((double)entity.method_17681(), entity.method_17682() / 2.0) * 0.8 * size;
            RenderUtils.drawSphereEsp(matrices, vertexConsumers, center, radius, espColor, alphaF, 20, seeThrough);
        } else if (currentMode.equals("Cylinder")) {
            class_243 base = class_243.field_1353;
            double radius = entity.method_17681() * 0.5 * size;
            double height = entity.method_17682() * size;
            RenderUtils.drawCylinderEsp(matrices, vertexConsumers, base, radius, height, espColor, alphaF, 20, seeThrough);
        } else if (currentMode.equals("Cone")) {
            class_243 base = class_243.field_1353;
            double radius = entity.method_17681() * 0.5 * size;
            double height = entity.method_17682() * size;
            RenderUtils.drawConeEsp(matrices, vertexConsumers, base, radius, height, espColor, alphaF, 16, seeThrough);
        }
    }

    private void renderEspForEntity(class_4587 matrices, class_1657 entity, class_4597 vertexConsumers, double x, double y, double z, Color espColor) {
        String currentMode = this.mode.getCurrentMode();
        int alpha = this.opaque.isEnabled() ? 255 : 80;
        float alphaF = alpha / 255.0F;
        double size = this.sizeMultiplier.getValue();
        boolean seeThrough = this.shouldRenderSeeThrough(currentMode);
        matrices.method_22903();
        matrices.method_22904(x, y, z);
        if (currentMode.equals("Box") || currentMode.equals("Box See-Through")) {
            class_238 box = entity.method_5829().method_989(-entity.method_23317(), -entity.method_23318(), -entity.method_23321());
            box = this.scaleBox(box, size);
            RenderUtils.drawBoxEsp(matrices, vertexConsumers, box, espColor, alphaF, seeThrough);
        } else if (currentMode.equals("Sphere")) {
            class_243 center = new class_243(0.0, entity.method_17682() / 2.0, 0.0);
            double radius = entity.method_17681() * 0.8 * size;
            RenderUtils.drawSphereEsp(matrices, vertexConsumers, center, radius, espColor, alphaF, 20, seeThrough);
        } else if (currentMode.equals("Cylinder")) {
            class_243 base = class_243.field_1353;
            double radius = entity.method_17681() * 0.5 * size;
            double height = entity.method_17682() * size;
            RenderUtils.drawCylinderEsp(matrices, vertexConsumers, base, radius, height, espColor, alphaF, 20, seeThrough);
        } else if (currentMode.equals("Cone")) {
            class_243 base = class_243.field_1353;
            double radius = entity.method_17681() * 0.5 * size;
            double height = entity.method_17682() * size;
            RenderUtils.drawConeEsp(matrices, vertexConsumers, base, radius, height, espColor, alphaF, 16, seeThrough);
        }

        matrices.method_22909();
    }

    private boolean shouldRenderSeeThrough(String currentMode) {
        return this.throughWalls.isEnabled() ? true : "Box See-Through".equals(currentMode);
    }

    private class_238 scaleBox(class_238 box, double scale) {
        class_243 center = box.method_1005();
        double width = (box.field_1320 - box.field_1323) * scale / 2.0;
        double height = (box.field_1325 - box.field_1322) * scale / 2.0;
        double depth = (box.field_1324 - box.field_1321) * scale / 2.0;
        return new class_238(
            center.field_1352 - width,
            center.field_1351 - height,
            center.field_1350 - depth,
            center.field_1352 + width,
            center.field_1351 + height,
            center.field_1350 + depth
        );
    }

    private boolean isUtilityBlock(class_2586 blockEntity) {
        return blockEntity instanceof class_2595
            || blockEntity instanceof class_3719
            || blockEntity instanceof class_3866
            || blockEntity instanceof class_3720
            || blockEntity instanceof class_3723
            || blockEntity instanceof class_2614
            || blockEntity instanceof class_2601
            || blockEntity instanceof class_2608
            || blockEntity instanceof class_2605
            || blockEntity instanceof class_2589
            || blockEntity instanceof class_2580
            || blockEntity instanceof class_2611
            || blockEntity instanceof class_2627;
    }

    private boolean isUtilityBlockType(class_2248 block) {
        return block instanceof class_2244 || block instanceof class_2304 || block instanceof class_2199;
    }

    public boolean shouldHideArmor(class_1309 entity) {
        return this.isEnabled() && this.hideArmor.isEnabled() && this.isValid(entity);
    }

    private boolean isValid(class_1309 entity) {
        return entity instanceof class_1657 && entity != this.mc.field_1724 && entity.method_5805();
    }

    private Color resolveRenderColor(Color startColor, long now, int seed, double y) {
        if (!this.gradient.isEnabled()) {
            return startColor;
        } else {
            double phase = now * 0.001 * this.gradientSpeed.getValue() + seed * 0.11 + y * 0.07;
            float t = (float)(Math.sin(phase) * 0.5 + 0.5);
            return this.blend(startColor, this.gradientColor.getValue(), t);
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

    @Override
    public void onDisable() {
        super.onDisable();
        this.utilityBlockCache.clear();
    }
}
