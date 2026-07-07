package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.friend.FriendManager;
import com.slither.cyemer.mixin.PlayerInventoryAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.AttackValidator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1657;
import net.minecraft.class_1743;
import net.minecraft.class_1802;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_3532;
import net.minecraft.class_3966;
import net.minecraft.class_239.class_240;

@Environment(EnvType.CLIENT)
public class AutoShieldBreak extends Module {
    public static boolean breakingShield = false;
    private final SliderSetting cps = new SliderSetting("CPS", 20.0, 1.0, 20.0, 0);
    private final SliderSetting reactionDelay = new SliderSetting("Reaction Delay", 0.0, 0.0, 250.0, 0);
    private final SliderSetting swapDelay = new SliderSetting("Swap Delay", 50.0, 0.0, 500.0, 0);
    private final SliderSetting attackDelay = new SliderSetting("Attack Delay", 50.0, 0.0, 500.0, 0);
    private final SliderSetting swapBackDelay = new SliderSetting("Swap Back Delay", 100.0, 0.0, 500.0, 0);
    private final BooleanSetting revertSlot = new BooleanSetting("Revert Slot", true);
    private final BooleanSetting rayTraceCheck = new BooleanSetting("Check Facing", true);
    private final BooleanSetting autoStun = new BooleanSetting("Auto Stun", true);
    private final BooleanSetting disableIfUsingItem = new BooleanSetting("Disable if using item", true);
    private final BooleanSetting ignoreFriends = new BooleanSetting("Ignore Friends", false);
    private final SliderSetting hitboxAccuracy = new SliderSetting("Hitbox Accuracy", 0.0, 0.0, 1.0, 2);
    private long lastCpsMs;
    private long lastReactionMs;
    private long lastSwapMs;
    private long lastAttackMs;
    private long lastSwapBackMs;
    private int savedSlot = -1;

    public AutoShieldBreak() {
        super("AutoShieldBreak", "Automatically breaks the opponent's shield", Category.COMBAT);
        this.addSetting(this.cps);
        this.addSetting(this.reactionDelay);
        this.addSetting(this.swapDelay);
        this.addSetting(this.attackDelay);
        this.addSetting(this.swapBackDelay);
        this.addSetting(this.revertSlot);
        this.addSetting(this.rayTraceCheck);
        this.addSetting(this.autoStun);
        this.addSetting(this.disableIfUsingItem);
        this.addSetting(this.ignoreFriends);
        this.addSetting(this.hitboxAccuracy);
    }

    private boolean canRunAuto() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null && this.mc.field_1755 == null) {
            int axeSlot = this.findAxe();
            if (axeSlot == -1) {
                return false;
            } else {
                return this.mc.field_1724.method_6115() && this.disableIfUsingItem.isEnabled()
                    ? false
                    : System.currentTimeMillis() - this.lastCpsMs >= (long)(1000.0 / Math.max(0.1, this.cps.getValue()));
            }
        } else {
            return false;
        }
    }

    private class_1657 getTargetPlayer() {
        if (this.mc.field_1765 != null && this.mc.field_1765.method_17783() == class_240.field_1331) {
            class_3966 hit = (class_3966)this.mc.field_1765;
            if (hit.method_17782() instanceof class_1657 target) {
                return this.ignoreFriends.isEnabled() && FriendManager.getInstance().isFriend(target.method_5667()) ? null : target;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private int findAxe() {
        for (int i = 0; i < 9; i++) {
            if (this.mc.field_1724.method_31548().method_5438(i).method_7909() instanceof class_1743) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public void onTickPost() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            class_1657 target = this.getTargetPlayer();
            if (this.savedSlot != -1 && System.currentTimeMillis() - this.lastSwapBackMs >= this.swapBackDelay.getValue()) {
                boolean shouldSwapBack = false;
                if (target == null) {
                    shouldSwapBack = true;
                } else {
                    boolean isBlocking = target.method_6039() && target.method_24518(class_1802.field_8255);
                    boolean canBreak = true;
                    if (this.rayTraceCheck.isEnabled()) {
                        class_243 dirToMe = this.mc.field_1724.method_73189().method_1020(target.method_73189()).method_1029();
                        class_243 targetLook = target.method_5828(1.0F).method_1029();
                        if (dirToMe.method_1026(targetLook) <= -0.6) {
                            canBreak = false;
                        }
                    }

                    if (canBreak && !this.isLookingAtHitbox(target)) {
                        canBreak = false;
                    }

                    if (!isBlocking || !canBreak) {
                        shouldSwapBack = true;
                    }
                }

                if (shouldSwapBack) {
                    if (this.revertSlot.isEnabled() && ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).getSelectedSlot() != this.savedSlot) {
                        ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).setSelectedSlot(this.savedSlot);
                    }

                    this.savedSlot = -1;
                    breakingShield = false;
                    return;
                }
            }

            if (this.canRunAuto()) {
                if (target != null) {
                    boolean isBlockingx = target.method_6039() && target.method_24518(class_1802.field_8255);
                    boolean canBreakx = true;
                    if (this.rayTraceCheck.isEnabled()) {
                        class_243 dirToMe = this.mc.field_1724.method_73189().method_1020(target.method_73189()).method_1029();
                        class_243 targetLook = target.method_5828(1.0F).method_1029();
                        if (dirToMe.method_1026(targetLook) <= -0.6) {
                            canBreakx = false;
                        }
                    }

                    if (canBreakx && !this.isLookingAtHitbox(target)) {
                        canBreakx = false;
                    }

                    if (!isBlockingx || !canBreakx) {
                        if (System.currentTimeMillis() - this.lastReactionMs < (long)(this.reactionDelay.getValue() / 2.0)) {
                            this.lastReactionMs = System.currentTimeMillis();
                        }
                    } else if (!(this.mc.field_1724.method_6047().method_7909() instanceof class_1743)) {
                        if (System.currentTimeMillis() - this.lastReactionMs >= (long)this.reactionDelay.getValue()
                            && System.currentTimeMillis() - this.lastSwapMs >= (long)this.swapDelay.getValue()) {
                            breakingShield = true;
                            if (this.savedSlot == -1) {
                                this.savedSlot = ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).getSelectedSlot();
                            }

                            int axeSlot = this.findAxe();
                            if (axeSlot != -1) {
                                ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).setSelectedSlot(axeSlot);
                            }

                            this.lastAttackMs = System.currentTimeMillis();
                            this.lastSwapMs = System.currentTimeMillis();
                        }
                    } else {
                        if (System.currentTimeMillis() - this.lastAttackMs >= (long)this.attackDelay.getValue() || this.savedSlot == -1) {
                            AttackValidator.tryAttack(this.mc, "combat.attack.autoshieldbreak.recode");
                            if (this.autoStun.isEnabled()) {
                                AttackValidator.tryAttack(this.mc, "combat.attack.autoshieldbreak.recode.stun");
                            }

                            this.lastCpsMs = System.currentTimeMillis();
                            this.lastAttackMs = System.currentTimeMillis();
                            this.lastSwapBackMs = System.currentTimeMillis();
                            breakingShield = false;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        if (this.mc.field_1724 != null && this.savedSlot != -1 && this.revertSlot.isEnabled()) {
            ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).setSelectedSlot(this.savedSlot);
        }

        this.savedSlot = -1;
        breakingShield = false;
        super.onDisable();
    }

    private boolean isLookingAtHitbox(class_1657 target) {
        if (this.mc.field_1724 == null) {
            return false;
        } else if (this.mc.field_1765 instanceof class_3966 entityHit && entityHit.method_17782() == target) {
            double requiredInside = class_3532.method_15350(this.hitboxAccuracy.getValue(), 0.0, 1.0);
            if (requiredInside <= 0.0) {
                return true;
            } else {
                class_243 eyePos = this.mc.field_1724.method_33571();
                class_238 box = target.method_5829();
                class_243 center = box.method_1005();
                double dx = center.field_1352 - eyePos.field_1352;
                double dy = center.field_1351 - eyePos.field_1351;
                double dz = center.field_1350 - eyePos.field_1350;
                double horizontalDist = Math.sqrt(dx * dx + dz * dz);
                float targetYaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
                float targetPitch = (float)(-Math.toDegrees(Math.atan2(dy, horizontalDist)));
                double yawDiff = Math.abs(class_3532.method_15393(this.mc.field_1724.method_36454() - targetYaw));
                double pitchDiff = Math.abs(class_3532.method_15393(this.mc.field_1724.method_36455() - targetPitch));
                double safeDist = Math.max(horizontalDist, 0.001);
                double halfWidth = Math.max(box.method_17939(), box.method_17941()) * 0.5;
                double yawHalfSpan = Math.toDegrees(Math.atan2(Math.max(halfWidth, 0.001), safeDist));
                double yawInside = 1.0 - Math.min(1.0, yawDiff / Math.max(yawHalfSpan, 0.001));
                double safeVertical = Math.max(horizontalDist, 0.001);
                double halfHeight = box.method_17940() * 0.5;
                double pitchHalfSpan = Math.toDegrees(Math.atan2(Math.max(halfHeight, 0.001), safeVertical));
                double pitchInside = 1.0 - Math.min(1.0, pitchDiff / Math.max(pitchHalfSpan, 0.001));
                return Math.min(yawInside, pitchInside) >= requiredInside;
            }
        } else {
            return false;
        }
    }
}
