package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.friend.FriendManager;
import com.slither.cyemer.manager.TargetManager;
import com.slither.cyemer.mixin.ClientPlayerInteractionManagerMixin;
import com.slither.cyemer.mixin.MinecraftClientAccessor;
import com.slither.cyemer.mixin.PlayerInventoryAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.RotationManager;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1887;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_3489;
import net.minecraft.class_3675;
import net.minecraft.class_6880;
import net.minecraft.class_746;
import net.minecraft.class_9304;
import net.minecraft.class_9334;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class Lungemacro extends Module {
    private final BooleanSetting holdMode = new BooleanSetting("Hold To Lunge", true);
    private final BooleanSetting instantSwap = new BooleanSetting("Instant Swap", true);
    private final BooleanSetting aimAtTarget = new BooleanSetting("Aim At Target", true);
    private final SliderSetting aimRange = new SliderSetting("Aim Range", 12.0, 3.0, 30.0, 1);
    private final SliderSetting aimSpeed = new SliderSetting("Aim Speed", 24.0, 1.0, 35.0, 1);
    private final BooleanSetting silentAim = new BooleanSetting("Silent Aim", false);
    private final BooleanSetting ignoreFriends = new BooleanSetting("Ignore Friends", true);
    private final BooleanSetting swapBack = new BooleanSetting("Swap Back", true);
    private final BooleanSetting randomization = new BooleanSetting("Randomization", false);
    private final SliderSetting minRandom = new SliderSetting("Min Random", 0.0, 0.0, 10.0, 1);
    private final SliderSetting maxRandom = new SliderSetting("Max Random", 1.0, 0.0, 10.0, 1);
    private static final float AIM_TOLERANCE = 4.0F;
    private static final int MAX_AIM_WAIT = 10;
    private static final int TAP_GRACE_TICKS = 6;
    private boolean isSwapping = false;
    private boolean holding = false;
    private int originalSlot = -1;
    private int swapTicks = 0;
    private int targetTicks = 3;
    private int waitTicks = 0;
    private class_1657 aimTarget = null;
    private int aimedTicks = 0;
    private int aimWaitTicks = 0;
    private int releasedTicks = 0;
    private boolean jabbed = false;

    public Lungemacro() {
        super("Lungemacro", "Swaps to your lunge spear and jabs. Hold the bind to keep lunging", Category.COMBAT);
        this.addSetting(this.holdMode);
        this.addSetting(this.instantSwap);
        this.addSetting(this.aimAtTarget);
        this.addSetting(this.aimRange);
        this.addSetting(this.aimSpeed);
        this.addSetting(this.silentAim);
        this.addSetting(this.ignoreFriends);
        this.addSetting(this.swapBack);
        this.addSetting(this.randomization);
        this.addSetting(this.minRandom);
        this.addSetting(this.maxRandom);
    }

    @Override
    public void onEnable() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            this.originalSlot = ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).getSelectedSlot();
            if (this.holdMode.isEnabled()) {
                if (this.findLungeSlot() == -1) {
                    this.toggle();
                } else {
                    this.holding = true;
                    this.waitTicks = 0;
                    // the rotation we start with is already on the server, so facing the target counts right away
                    this.aimedTicks = 1;
                    this.aimWaitTicks = 0;
                    this.releasedTicks = 0;
                    this.jabbed = false;
                    // jab on the press itself so a quick tap still lunges once, unless there's someone to turn toward first
                    if (this.updateAim()) {
                        this.jabbed = this.tryJab();
                    }
                }

                return;
            }

            this.targetTicks = 3;
            if (this.randomization.isEnabled()) {
                this.targetTicks = this.targetTicks + (int)(Math.random() * 2.0);
            }

            int lungeSlot = this.findLungeSlot();
            if (lungeSlot == -1) {
                this.toggle();
            } else {
                this.isSwapping = true;
                this.swapTicks = 0;
                this.equip(lungeSlot);
                if (this.mc.field_1724.method_7261(0.0F) >= 1.0F) {
                    ((MinecraftClientAccessor)this.mc).attack();
                    this.swapTicks = 1;
                }
            }
        } else {
            this.toggle();
        }
    }

    @Override
    public void onDisable() {
        // without Instant Swap the spear stays out while held, put the old slot back on release
        if (this.holding
            && !this.instantSwap.isEnabled()
            && this.swapBack.isEnabled()
            && this.originalSlot != -1
            && this.mc.field_1724 != null
            && this.mc.field_1724.method_31548().method_67532() != this.originalSlot) {
            this.equip(this.originalSlot);
        }

        RotationManager.stop(this);
        this.holding = false;
        this.isSwapping = false;
        this.originalSlot = -1;
        this.swapTicks = 0;
        this.waitTicks = 0;
        this.aimTarget = null;
        this.aimedTicks = 0;
        this.aimWaitTicks = 0;
        this.releasedTicks = 0;
        this.jabbed = false;
    }

    @Override
    public void onTick() {
        if (this.holding) {
            if (this.mc.field_1724 == null) {
                this.toggle();
                return;
            }

            boolean aimed = this.updateAim();
            if (!this.isKeyHeld()) {
                // a tap that's still turning toward someone gets its one lunge before we let go, but not forever
                if (this.jabbed || this.aimTarget == null || ++this.releasedTicks > TAP_GRACE_TICKS) {
                    this.toggle();
                    return;
                }
            }

            if (this.waitTicks > 0) {
                this.waitTicks--;
            } else if (aimed && this.tryJab()) {
                this.jabbed = true;
            }

            return;
        }

        if (this.isSwapping && this.mc.field_1724 != null) {
            this.swapTicks++;
            if (this.swapTicks == 0) {
                if (this.mc.field_1724.method_7261(0.0F) >= 1.0F) {
                    ((MinecraftClientAccessor)this.mc).attack();
                }

                int lungeSlot = this.findLungeSlot();
                if (lungeSlot != -1) {
                    this.equip(lungeSlot);
                    this.swapTicks = 1;
                } else {
                    this.toggle();
                }
            } else {
                if (this.swapTicks >= this.targetTicks) {
                    if (this.swapBack.isEnabled() && this.originalSlot != -1) {
                        this.equip(this.originalSlot);
                    }

                    this.toggle();
                }
            }
        }
    }

    /**
     * Turns toward the nearest target while the bind is down. The lunge pushes along wherever the server last
     * saw us looking (class_12129: look vector with y zeroed, times 0.458 per level), and the jab packet carries
     * no rotation of its own, so the jab waits until the aim has sat on the target for a tick and gone out in a
     * movement packet. True when it's fine to jab.
     */
    private boolean updateAim() {
        if (!this.aimAtTarget.isEnabled()) {
            return true;
        }

        this.aimTarget = this.findTarget();
        if (this.aimTarget == null) {
            RotationManager.clearTarget(this);
            this.aimedTicks = 0;
            this.aimWaitTicks = 0;
            return true;
        }

        // NORMAL sits under AutoMace's HIGH, a smash always gets the camera first
        RotationManager.setRotationSupplier(
            this,
            RotationManager.Priority.NORMAL,
            () -> this.aimTarget != null ? this.aimPos(this.aimTarget) : null,
            this.aimSpeed.getValue(),
            RotationManager.RotationMode.SMOOTH,
            0.0,
            this.silentAim.isEnabled(),
            false
        );
        if (!RotationManager.isControlledBy(this)) {
            // something else is steering, don't hold the lunge for an aim we don't have
            this.aimedTicks = 0;
            return true;
        }

        this.aimedTicks = RotationManager.isRotationComplete(AIM_TOLERANCE) ? this.aimedTicks + 1 : 0;
        if (this.aimedTicks >= 2) {
            this.aimWaitTicks = 0;
            return true;
        }

        // never sit on the bind forever if the aim can't settle
        return ++this.aimWaitTicks > MAX_AIM_WAIT;
    }

    private class_1657 findTarget() {
        class_746 self = this.mc.field_1724;
        double rangeSq = this.aimRange.getValue() * this.aimRange.getValue();
        // whoever we last hit comes first, same lock AutoMace's Target Mode uses
        if (TargetManager.getLockedTarget() instanceof class_1657 locked && this.isValidTarget(locked) && self.method_5858(locked) <= rangeSq) {
            return locked;
        }

        class_1657 best = null;
        double bestSq = rangeSq;

        for (class_1657 player : this.mc.field_1687.method_18456()) {
            if (this.isValidTarget(player)) {
                double distSq = self.method_5858(player);
                if (distSq <= bestSq) {
                    best = player;
                    bestSq = distSq;
                }
            }
        }

        return best;
    }

    private boolean isValidTarget(class_1657 player) {
        return player != this.mc.field_1724
            && player.method_5805()
            && !player.method_7325()
            && !(this.ignoreFriends.isEnabled() && FriendManager.getInstance().isFriend(player.method_5477().getString()));
    }

    private class_243 aimPos(class_1657 target) {
        class_238 box = target.method_5829();
        class_243 centre = box.method_1005();
        return new class_243(centre.field_1352, box.field_1322 + target.method_17682() * 0.65, centre.field_1350);
    }

    /** True if a jab actually went out. */
    private boolean tryJab() {
        class_746 player = this.mc.field_1724;
        // lunge doesn't fire while riding, gliding or in water, no point burning the charge on a plain jab
        if (player.method_5765() || player.method_6128() || player.method_5799()) {
            return false;
        }

        int lungeSlot = this.findLungeSlot();
        if (lungeSlot == -1) {
            return false;
        }

        // spears need a full charge to jab, this is the same gate vanilla and the server use
        if (player.method_75202(player.method_31548().method_5438(lungeSlot), 0)) {
            return false;
        }

        int slot = player.method_31548().method_67532();
        boolean swapped = slot != lungeSlot;
        if (swapped) {
            this.equip(lungeSlot);
        }

        ((MinecraftClientAccessor)this.mc).attack();
        if (swapped && this.instantSwap.isEnabled() && this.swapBack.isEnabled()) {
            this.equip(slot);
            // flush the swap back now so the server never ticks with the spear in hand
            ((ClientPlayerInteractionManagerMixin.ClientPlayerInteractionManagerAccessor)this.mc.field_1761).invokeSyncSelectedSlot();
        }

        this.waitTicks = this.randomDelay();
        return true;
    }

    private int randomDelay() {
        if (!this.randomization.isEnabled()) {
            return 0;
        }

        double min = Math.min(this.minRandom.getValue(), this.maxRandom.getValue());
        double max = Math.max(this.minRandom.getValue(), this.maxRandom.getValue());
        return (int)Math.round(min + Math.random() * (max - min));
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

    private int findLungeSlot() {
        for (int i = 0; i < 9; i++) {
            class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
            if (this.isLungeSpear(stack)) {
                return i;
            }
        }

        return -1;
    }

    private boolean isLungeSpear(class_1799 stack) {
        if (stack.method_7960()) {
            return false;
        } else {
            boolean isSpear = stack.method_31573(class_3489.field_63257)
                || stack.method_31574(class_1802.field_8547)
                || stack.method_7964().getString().toLowerCase().contains("spear");
            if (!isSpear) {
                return false;
            } else {
                class_9304 enchantments = (class_9304)stack.method_58694(class_9334.field_49633);
                if (enchantments == null) {
                    return false;
                } else {
                    for (Entry<class_6880<class_1887>> entry : enchantments.method_57539()) {
                        class_6880<class_1887> enchantment = (class_6880<class_1887>)entry.getKey();
                        String id = enchantment.method_40230().map(k -> k.method_29177().method_12832()).orElse("").toLowerCase();
                        String name = ((class_1887)enchantment.comp_349()).comp_2686().getString().toLowerCase();
                        if (id.contains("lunge") || name.contains("lunge")) {
                            return true;
                        }
                    }

                    return false;
                }
            }
        }
    }

    private void equip(int slot) {
        ((PlayerInventoryAccessor)this.mc.field_1724.method_31548()).setSelectedSlot(slot);
    }
}
