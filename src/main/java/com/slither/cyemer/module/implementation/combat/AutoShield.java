package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.event.EventBus;
import com.slither.cyemer.event.EventTarget;
import com.slither.cyemer.event.impl.AttackEvent;
import com.slither.cyemer.mixin.MinecraftClientAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.PlaceValidator;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_243;

@Environment(EnvType.CLIENT)
public class AutoShield extends Module {
    private final SliderSetting generalDelay = new SliderSetting("Delay", 50.0, 0.0, 500.0, 0);
    private final BooleanSetting delayRandomization = new BooleanSetting("Random Delay", false);
    private final SliderSetting randomDelayMs = new SliderSetting("Max Random", 20.0, 0.0, 100.0, 0);
    private final SliderSetting unshieldDelay = new SliderSetting("Hold Time", 100.0, 0.0, 500.0, 0);
    private final BooleanSetting unshieldRandomization = new BooleanSetting("Random Hold", false);
    private final SliderSetting unshieldRandomMs = new SliderSetting("Max Random", 20.0, 0.0, 100.0, 0);
    private final SliderSetting fovAngle = new SliderSetting("Target FOV", 90.0, 30.0, 180.0, 0);
    private boolean isPendingShield = false;
    private long shieldExecutionTime = 0L;
    private boolean isShielding = false;
    private long unshieldExecutionTime = 0L;
    private final Random random = new Random();

    public AutoShield() {
        super("AutoShield", "Automatically uses shield on triggerbot attack", Category.COMBAT);
        this.addSetting(this.generalDelay);
        this.addSetting(this.delayRandomization);
        this.addSetting(this.randomDelayMs);
        this.addSetting(this.unshieldDelay);
        this.addSetting(this.unshieldRandomization);
        this.addSetting(this.unshieldRandomMs);
        this.addSetting(this.fovAngle);
    }

    @Override
    public void onEnable() {
        EventBus.register(this);
    }

    @Override
    public void onDisable() {
        EventBus.unregister(this);
        this.isPendingShield = false;
        this.isShielding = false;
        if (this.mc.field_1690.field_1904.method_1434()) {
            this.mc.field_1690.field_1904.method_23481(false);
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            class_1799 offhandStack = this.mc.field_1724.method_6079();
            if (offhandStack.method_7909() == class_1802.field_8255) {
                if (offhandStack.method_7919() < offhandStack.method_7936()) {
                    if (this.isPlayerLookingAtMe()) {
                        long delay = (long)this.generalDelay.getValue();
                        if (this.delayRandomization.isEnabled()) {
                            delay += this.random.nextInt((int)this.randomDelayMs.getValue() + 1);
                        }

                        this.shieldExecutionTime = System.currentTimeMillis() + delay;
                        this.isPendingShield = true;
                    }
                }
            }
        }
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            if (this.isPendingShield && System.currentTimeMillis() >= this.shieldExecutionTime) {
                class_1799 offhandStack = this.mc.field_1724.method_6079();
                if (offhandStack.method_7909() == class_1802.field_8255 && offhandStack.method_7919() < offhandStack.method_7936()) {
                    this.performShield();
                }

                this.isPendingShield = false;
            }

            if (this.isShielding) {
                if (System.currentTimeMillis() >= this.unshieldExecutionTime) {
                    this.stopShield();
                } else if (!this.mc.field_1690.field_1904.method_1434()) {
                    this.mc.field_1690.field_1904.method_23481(true);
                }
            }
        }
    }

    private boolean isPlayerLookingAtMe() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            class_243 myPos = this.mc.field_1724.method_73189();
            double range = 10.0;
            double maxFov = this.fovAngle.getValue();

            for (class_1657 player : this.mc.field_1687.method_18456()) {
                if (player != this.mc.field_1724 && !(player.method_5739(this.mc.field_1724) > range)) {
                    class_243 playerPos = player.method_73189().method_1031(0.0, player.method_18381(player.method_18376()), 0.0);
                    class_243 toMe = myPos.method_1031(0.0, this.mc.field_1724.method_18381(this.mc.field_1724.method_18376()), 0.0)
                        .method_1020(playerPos)
                        .method_1029();
                    class_243 playerLook = player.method_5828(1.0F);
                    double dot = playerLook.method_1026(toMe);
                    double angle = Math.toDegrees(Math.acos(dot));
                    if (angle <= maxFov / 2.0) {
                        return true;
                    }
                }
            }

            return false;
        } else {
            return false;
        }
    }

    private void performShield() {
        if (PlaceValidator.canPlace(this.mc)) {
            ((MinecraftClientAccessor)this.mc).useItem();
        } else {
            ((MinecraftClientAccessor)this.mc).useItem();
        }

        this.mc.field_1690.field_1904.method_23481(true);
        this.isShielding = true;
        long duration = (long)this.unshieldDelay.getValue();
        if (this.unshieldRandomization.isEnabled()) {
            duration += this.random.nextInt((int)this.unshieldRandomMs.getValue() + 1);
        }

        this.unshieldExecutionTime = System.currentTimeMillis() + duration;
    }

    private void stopShield() {
        this.mc.field_1690.field_1904.method_23481(false);
        this.isShielding = false;
    }
}
