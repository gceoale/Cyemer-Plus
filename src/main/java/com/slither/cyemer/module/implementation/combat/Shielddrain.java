package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.event.EventBus;
import com.slither.cyemer.event.EventTarget;
import com.slither.cyemer.event.impl.ShieldDrainEvent;
import com.slither.cyemer.mixin.MinecraftClientAccessor;
import com.slither.cyemer.mixin.PlayerInventoryAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.AttackValidator;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1268;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_3489;
import net.minecraft.class_3675;
import net.minecraft.class_3966;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class Shielddrain extends Module {
    public static Shielddrain INSTANCE;
    private final BooleanSetting randomization = new BooleanSetting("Randomization", true);
    private final SliderSetting minCps = new SliderSetting("Min CPS", 9.8, 0.0, 18.0, 2);
    private final SliderSetting maxCps = new SliderSetting("Max CPS", 17.92, 0.0, 18.0, 2);
    private final SliderSetting minHitboxAccuracy = new SliderSetting("Hitbox Accuracy %", 3.94, 0.0, 99.0, 2);
    private final SliderSetting drainDuration = new SliderSetting("Drain Duration (ms)", 725.0, 50.0, 5000.0, 0);
    private final BooleanSetting holdMode = new BooleanSetting("Hold To Drain", false);
    private final BooleanSetting swapBack = new BooleanSetting("Swap Back", true);
    private long enableTime = 0L;
    private long lastAttackTime = 0L;
    private long currentDelayMs = 0L;
    private int originalSlot = -1;

    public Shielddrain() {
        super("Shielddrain", "Rapidly attacks with a sword to drain opponent shields.", Category.COMBAT);
        INSTANCE = this;
        this.addSetting(this.randomization);
        this.addSetting(this.minCps);
        this.addSetting(this.maxCps);
        this.addSetting(this.minHitboxAccuracy);
        this.addSetting(this.drainDuration);
        this.addSetting(this.holdMode);
        this.addSetting(this.swapBack);
    }

    @Override
    public void onEnable() {
        if (this.mc.field_1724 == null) {
            this.toggle();
        } else {
            this.enableTime = System.currentTimeMillis();
            this.currentDelayMs = 0L;
            this.originalSlot = ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).getSelectedSlot();
            EventBus.register(this);
        }
    }

    @Override
    public void onDisable() {
        EventBus.unregister(this);
    }

    @EventTarget
    public void onShieldDrain(ShieldDrainEvent event) {
        if (this.isEnabled()) {
            event.setActive(true);
        }
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            if (this.holdMode.isEnabled()) {
                if (!this.isKeyHeld()) {
                    this.finish();
                    return;
                }
            } else if (System.currentTimeMillis() - this.enableTime >= this.drainDuration.getValue()) {
                this.finish();
                return;
            }

            long now = System.currentTimeMillis();
            if (now - this.lastAttackTime >= this.currentDelayMs) {
                if (this.mc.field_1765 instanceof class_3966 hitResult) {
                    if (hitResult.method_17782() instanceof class_1657 target) {
                        if (target != this.mc.field_1724 && target.method_5805() && target.method_6039()) {
                            double distSq = this.mc.field_1724.method_5858(target);
                            if (!(distSq > 9.0)) {
                                if (this.isHitboxAccurate(target, hitResult)) {
                                    int swordSlot = this.findSword();
                                    if (swordSlot != -1) {
                                        boolean swapped = false;
                                        if (this.mc.field_1724.method_31548().method_67532() != swordSlot) {
                                            ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).setSelectedSlot(swordSlot);
                                            swapped = true;
                                        }

                                        if (!swapped) {
                                            int burstsAllowed = 4;
                                            int currentBurst = 0;

                                            while (now - this.lastAttackTime >= this.currentDelayMs && currentBurst < burstsAllowed) {
                                                currentBurst++;
                                                this.tryAttack();
                                                if (this.lastAttackTime != 0L && now - this.lastAttackTime <= 500L) {
                                                    this.lastAttackTime = this.lastAttackTime + this.currentDelayMs;
                                                } else {
                                                    this.lastAttackTime = now;
                                                }

                                                double currentCps = this.maxCps.getValue();
                                                if (this.randomization.isEnabled()) {
                                                    double min = this.minCps.getValue();
                                                    double max = this.maxCps.getValue();
                                                    if (min >= max) {
                                                        this.currentDelayMs = (long)(1000.0 / max);
                                                    } else {
                                                        Random r = new Random();
                                                        double targetCps = min + r.nextDouble() * (max - min);
                                                        double baseDelay = 1000.0 / targetCps;
                                                        double randVal = r.nextDouble();
                                                        double jitter;
                                                        if (randVal < 0.75) {
                                                            jitter = r.nextGaussian() * 5.0;
                                                        } else if (randVal < 0.9) {
                                                            jitter = r.nextGaussian() * 15.0;
                                                        } else {
                                                            jitter = 100.0 + r.nextDouble() * 250.0;
                                                        }

                                                        this.currentDelayMs = (long)(baseDelay + jitter);
                                                        if (this.currentDelayMs < 10L) {
                                                            this.currentDelayMs = 10L;
                                                        }

                                                        if (this.currentDelayMs > 1000L) {
                                                            this.currentDelayMs = 1000L;
                                                        }
                                                    }
                                                } else if (currentCps > 0.0) {
                                                    this.currentDelayMs = (long)(1000.0 / currentCps);
                                                } else {
                                                    this.currentDelayMs = 50L;
                                                }
                                            }

                                            if (this.lastAttackTime < now - 50L) {
                                                this.lastAttackTime = now - 50L;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            this.finish();
        }
    }

    private void finish() {
        if (this.swapBack.isEnabled()
            && this.originalSlot != -1
            && this.mc.field_1724 != null
            && this.mc.field_1724.method_31548().method_67532() != this.originalSlot) {
            ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).setSelectedSlot(this.originalSlot);
        }

        this.originalSlot = -1;
        if (this.isEnabled()) {
            this.toggle();
        }
    }

    private void tryAttack() {
        boolean attacked = AttackValidator.tryAttack(this.mc, "combat.attack.shielddrain");
        if (!attacked && this.mc.field_1724 != null && this.mc.field_1761 != null && this.mc.field_1765 instanceof class_3966) {
            ((MinecraftClientAccessor)this.mc).attack();
            this.mc.field_1724.method_6104(class_1268.field_5808);
        }
    }

    private int findSword() {
        for (int i = 0; i < 9; i++) {
            class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
            if (!stack.method_7960() && stack.method_31573(class_3489.field_42611)) {
                return i;
            }
        }

        return -1;
    }

    private boolean isKeyHeld() {
        int key = this.getKeyCode();
        if (key == -1) {
            return false;
        } else {
            return key < 0
                ? GLFW.glfwGetMouseButton(this.mc.method_22683().method_4490(), key + 100) == 1
                : class_3675.method_15987(this.mc.method_22683(), key);
        }
    }

    private boolean isHitboxAccurate(class_1309 target, class_3966 entityHit) {
        class_243 hitPos = entityHit.method_17784();
        class_238 targetBox = target.method_5829();
        class_243 center = targetBox.method_1005();
        double dx = Math.abs(hitPos.field_1352 - center.field_1352);
        double dy = Math.abs(hitPos.field_1351 - center.field_1351);
        double dz = Math.abs(hitPos.field_1350 - center.field_1350);
        double halfWidth = targetBox.method_17939() / 2.0;
        double halfHeight = targetBox.method_17940() / 2.0;
        double halfDepth = targetBox.method_17941() / 2.0;
        double xEdgeDistance = halfWidth - dx;
        double yEdgeDistance = halfHeight - dy;
        double zEdgeDistance = halfDepth - dz;
        double minEdgeDistance = Math.min(Math.min(xEdgeDistance, yEdgeDistance), zEdgeDistance);
        double accuracy;
        if (minEdgeDistance == yEdgeDistance) {
            double xAccuracy = (1.0 - dx / halfWidth) * 100.0;
            double zAccuracy = (1.0 - dz / halfDepth) * 100.0;
            accuracy = Math.min(xAccuracy, zAccuracy);
        } else if (minEdgeDistance == xEdgeDistance) {
            double yAccuracy = (1.0 - dy / halfHeight) * 100.0;
            double zAccuracy = (1.0 - dz / halfDepth) * 100.0;
            accuracy = Math.min(yAccuracy, zAccuracy);
        } else {
            double xAccuracy = (1.0 - dx / halfWidth) * 100.0;
            double yAccuracy = (1.0 - dy / halfHeight) * 100.0;
            accuracy = Math.min(xAccuracy, yAccuracy);
        }

        return accuracy >= this.minHitboxAccuracy.getValue();
    }
}
