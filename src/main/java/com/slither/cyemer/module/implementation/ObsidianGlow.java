package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ColorSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.render.RenderUtils;
import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_3965;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_9799;
import net.minecraft.class_239.class_240;
import net.minecraft.class_4597.class_4598;

@Environment(EnvType.CLIENT)
public class ObsidianGlow extends Module {
    private class_2338 targetPos = null;
    private class_2338 pendingTargetPos = null;
    private class_2338 lastGlowedPos = null;
    private float transitionProgress = 0.0F;
    private long lastUpdateTime = 0L;
    private long timeLookingAtPending = 0L;
    private List<class_2338> slidePath = null;
    private float slideSpeed = 5.0F;
    private static final long LOOK_DELAY_MS = 25L;
    private static final int MAX_PATHFINDING_DISTANCE = 32;
    private final ColorSetting outerGlowColor = new ColorSetting("Outer Glow", new Color(153, 51, 230, 120));
    private final ColorSetting innerGlowColor = new ColorSetting("Inner Glow", new Color(220, 150, 255, 70));

    public ObsidianGlow() {
        super("ObsidianGlow", "Renders a glowing overlay on looked-at obsidian blocks.", Category.RENDER);
        this.addSetting(this.outerGlowColor);
        this.addSetting(this.innerGlowColor);
    }

    @Override
    public void onWorldRender(class_4587 matrices, float tickDelta) {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            class_2338 currentLookedAtPos = null;
            if (this.mc.field_1765 != null && this.mc.field_1765.method_17783() == class_240.field_1332) {
                class_2338 pos = ((class_3965)this.mc.field_1765).method_17777();
                if (this.isGlowBlock(pos)) {
                    currentLookedAtPos = pos;
                }
            } else if (this.mc.field_1765 != null && this.mc.field_1765.method_17783() == class_240.field_1331) {
                class_243 hitPos = this.mc.field_1765.method_17784();
                class_2338 crystalPos = class_2338.method_49638(hitPos);
                class_2338 belowPos = crystalPos.method_10074();
                if (this.isGlowBlock(belowPos)) {
                    currentLookedAtPos = belowPos;
                }
            }

            long currentTime = System.currentTimeMillis();
            if (currentLookedAtPos != null && !currentLookedAtPos.equals(this.pendingTargetPos)) {
                this.pendingTargetPos = currentLookedAtPos;
                this.timeLookingAtPending = currentTime;
            } else if (currentLookedAtPos == null) {
                this.pendingTargetPos = null;
            }

            if (this.pendingTargetPos != null && !this.pendingTargetPos.equals(this.targetPos)) {
                if (currentTime - this.timeLookingAtPending > 25L) {
                    this.targetPos = this.pendingTargetPos;
                    this.transitionProgress = 0.0F;
                    if (this.lastGlowedPos != null && !this.lastGlowedPos.equals(this.targetPos)) {
                        this.slidePath = this.findObsidianPath(this.lastGlowedPos, this.targetPos);
                        if (this.slidePath == null) {
                            this.slidePath = Arrays.asList(this.lastGlowedPos, this.targetPos);
                        }

                        double distance = Math.sqrt(this.lastGlowedPos.method_10262(this.targetPos));
                        this.slideSpeed = Math.max(4.0F, (float)distance * 0.75F);
                    } else {
                        this.slidePath = null;
                    }
                }
            } else if (this.pendingTargetPos == null && this.targetPos != null) {
                this.targetPos = null;
                this.transitionProgress = 0.0F;
                this.slidePath = null;
            }

            float deltaTime = this.lastUpdateTime == 0L ? 0.0F : (float)(currentTime - this.lastUpdateTime) / 1000.0F;
            this.lastUpdateTime = currentTime;
            if (this.transitionProgress < 1.0F) {
                this.transitionProgress = Math.min(1.0F, this.transitionProgress + deltaTime * (this.slideSpeed / 2.0F));
            }

            class_243 camera = this.mc.field_1773.method_19418().method_71156();
            long worldTime = this.mc.field_1687.method_75260();
            boolean isSliding = this.slidePath != null && !this.slidePath.isEmpty();
            class_9799 allocator = new class_9799(1536);

            try {
                class_4598 immediate = class_4597.method_22991(allocator);
                if (this.targetPos != null) {
                    if (isSliding) {
                        this.renderSlide(matrices, immediate, camera, worldTime, tickDelta);
                    } else {
                        this.renderGlowAt(matrices, immediate, camera, this.targetPos, worldTime, tickDelta, this.transitionProgress);
                    }

                    this.lastGlowedPos = this.targetPos;
                } else if (this.lastGlowedPos != null) {
                    float alpha = 1.0F - this.transitionProgress;
                    this.renderGlowAt(matrices, immediate, camera, this.lastGlowedPos, worldTime, tickDelta, alpha);
                }

                immediate.method_22993();
            } catch (Throwable var15) {
                try {
                    allocator.close();
                } catch (Throwable var14) {
                    var15.addSuppressed(var14);
                }

                throw var15;
            }

            allocator.close();
        }
    }

    private void renderSlide(class_4587 matrices, class_4597 vertexConsumers, class_243 camera, long worldTime, float tickDelta) {
        class_2338 endPos = this.slidePath.get(this.slidePath.size() - 1);
        if (this.transitionProgress >= 1.0F) {
            this.renderGlowAt(matrices, vertexConsumers, camera, endPos, worldTime, tickDelta, 1.0F);
        } else {
            float pathIndexFloat = this.transitionProgress * (this.slidePath.size() - 1);
            int fromIndex = (int)pathIndexFloat;
            int toIndex = Math.min(fromIndex + 1, this.slidePath.size() - 1);
            class_2338 fromPos = this.slidePath.get(fromIndex);
            class_2338 toPos = this.slidePath.get(toIndex);
            float segmentProgress = pathIndexFloat - fromIndex;
            double renderX = fromPos.method_10263() + (toPos.method_10263() - fromPos.method_10263()) * segmentProgress;
            double renderY = fromPos.method_10264() + (toPos.method_10264() - fromPos.method_10264()) * segmentProgress;
            double renderZ = fromPos.method_10260() + (toPos.method_10260() - fromPos.method_10260()) * segmentProgress;
            class_2338 currentBlockPos = class_2338.method_49637(renderX + 0.5, renderY + 0.5, renderZ + 0.5);
            if (this.isGlowBlock(currentBlockPos)) {
                this.renderGlowAt(matrices, vertexConsumers, camera, renderX, renderY, renderZ, worldTime, tickDelta, 1.0F);
            }
        }
    }

    private List<class_2338> findObsidianPath(class_2338 start, class_2338 end) {
        if (start.equals(end)) {
            return null;
        } else {
            Queue<class_2338> queue = new LinkedList<>();
            Map<class_2338, class_2338> parentMap = new HashMap<>();
            Set<class_2338> visited = new HashSet<>();
            queue.add(start);
            visited.add(start);

            while (!queue.isEmpty()) {
                class_2338 current = queue.poll();
                if (current.equals(end)) {
                    LinkedList<class_2338> path = new LinkedList<>();

                    for (class_2338 at = end; at != null; at = parentMap.get(at)) {
                        path.addFirst(at);
                    }

                    return path;
                }

                if (!(Math.sqrt(current.method_10262(start)) > 32.0)) {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) == 1) {
                                    class_2338 neighbor = current.method_10069(dx, dy, dz);
                                    if (!visited.contains(neighbor) && this.isGlowBlock(neighbor)) {
                                        visited.add(neighbor);
                                        parentMap.put(neighbor, current);
                                        queue.add(neighbor);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return null;
        }
    }

    private void renderGlowAt(
        class_4587 matrices, class_4597 vertexConsumers, class_243 camera, class_2338 pos, long worldTime, float tickDelta, float alphaMultiplier
    ) {
        this.renderGlowAt(matrices, vertexConsumers, camera, pos.method_10263(), pos.method_10264(), pos.method_10260(), worldTime, tickDelta, alphaMultiplier);
    }

    private void renderGlowAt(
        class_4587 matrices, class_4597 vertexConsumers, class_243 camera, double x, double y, double z, long worldTime, float tickDelta, float alphaMultiplier
    ) {
        if (!(alphaMultiplier <= 0.01F)) {
            float time = ((float)worldTime + tickDelta) / 20.0F;
            float pulse = (float)(Math.sin(time * 2.0F) * 0.5 + 0.5);
            float expand = 0.002F + pulse * 0.003F;
            matrices.method_22903();
            matrices.method_22904(x - camera.field_1352 - expand, y - camera.field_1351 - expand, z - camera.field_1350 - expand);
            float scale = 1.0F + expand * 2.0F;
            matrices.method_22905(scale, scale, scale);
            class_238 unitBox = new class_238(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
            Color outerColor = this.outerGlowColor.getValue();
            float outerAlpha = clamp01((outerColor.getAlpha() / 255.0F - 0.1F + pulse * 0.2F) * alphaMultiplier);
            if (outerAlpha > 0.0F) {
                RenderUtils.drawFilledBox(matrices, vertexConsumers, unitBox, outerColor, outerAlpha);
            }

            matrices.method_46416(0.05F, 0.05F, 0.05F);
            matrices.method_22905(0.9F, 0.9F, 0.9F);
            Color innerColor = this.innerGlowColor.getValue();
            float innerPulse = (float)(Math.sin(time * 3.0F) * 0.5 + 0.5);
            float innerAlpha = clamp01((innerColor.getAlpha() / 255.0F - 0.15F + innerPulse * 0.3F) * alphaMultiplier);
            if (innerAlpha > 0.0F) {
                RenderUtils.drawFilledBox(matrices, vertexConsumers, unitBox, innerColor, innerAlpha);
            }

            matrices.method_22909();
        }
    }

    private static float clamp01(float v) {
        return v < 0.0F ? 0.0F : Math.min(v, 1.0F);
    }

    private boolean isGlowBlock(class_2338 pos) {
        class_2248 block = this.mc.field_1687.method_8320(pos).method_26204();
        return block == class_2246.field_10540 || block == class_2246.field_22423;
    }
}
