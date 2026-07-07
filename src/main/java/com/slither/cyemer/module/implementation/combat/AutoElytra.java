package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.event.EventBus;
import com.slither.cyemer.event.EventTarget;
import com.slither.cyemer.event.impl.AutoMaceSyncEvent;
import com.slither.cyemer.event.impl.MaceHitEvent;
import com.slither.cyemer.event.impl.PearlThrowEvent;
import com.slither.cyemer.friend.FriendManager;
import com.slither.cyemer.mixin.KeyBindingAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10192;
import net.minecraft.class_1304;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_3532;
import net.minecraft.class_3959;
import net.minecraft.class_9334;
import net.minecraft.class_239.class_240;
import net.minecraft.class_3959.class_242;
import net.minecraft.class_3959.class_3960;

@Environment(EnvType.CLIENT)
public class AutoElytra extends Module {
    private final BooleanSetting randomization = new BooleanSetting("Randomization", false);
    private final SliderSetting randomMinDelay = new SliderSetting("Min Random (ms)", 0.0, 0.0, 500.0, 0);
    private final SliderSetting randomMaxDelay = new SliderSetting("Max Random (ms)", 25.0, 0.0, 500.0, 0);
    private final BooleanSetting autoMaceSync = new BooleanSetting("Automace Sync", true);
    private final BooleanSetting inAir = new BooleanSetting("In Air", false);
    private final BooleanSetting onPearl = new BooleanSetting("On Pearl", true);
    private final BooleanSetting disableOnGround = new BooleanSetting("Disable on Ground", true);
    private final SliderSetting reequipDelay = new SliderSetting("Reequip Delay (ms)", 100.0, 0.0, 1000.0, 0);
    private final SliderSetting heightDiff = new SliderSetting("Height Diff", 4.0, 1.0, 10.0, 1);
    private final SliderSetting heightRange = new SliderSetting("Height Range", 10.0, 1.0, 50.0, 1);
    private final SliderSetting preHitRange = new SliderSetting("Mace Pre-Hit Range", 3.4, 2.5, 5.0, 1);
    private final SliderSetting minDropDistance = new SliderSetting("Min Drop Dist", 1.5, 0.5, 5.0, 1);
    private static final int MAX_WAIT_TICKS = 10;
    private static final int MIN_AIR_TICKS = 4;
    private static final double PRE_HIT_MIN_HEIGHT = 1.0;
    private static final double IN_AIR_MIN_GROUND_DIST = 2.0;
    private boolean swapInProgress = false;
    private boolean swapTargetElytra = false;
    private int swapOriginalSlot = -1;
    private int swapWaitTicks = 0;
    private long swapUseAt = 0L;
    private boolean swapUseSent = false;
    private boolean queuedSwap = false;
    private boolean queuedSwapTargetElytra = false;
    private long queuedSwapAt = 0L;
    private int airTicks = 0;
    private double highestY = 0.0;
    private boolean wasOnGround = true;
    private long suppressChestplateUntil = 0L;
    private long maceReequipAt = 0L;
    private long pearlReequipAt = 0L;
    private long autoMaceSyncPauseUntil = 0L;
    private long autoMaceSyncActiveUntil = 0L;

    public AutoElytra() {
        super("AutoElytra", "Auto swaps Elytra and chestplate around mace windows", Category.COMBAT);
        this.addSetting(this.randomization);
        this.addSetting(this.randomMinDelay);
        this.addSetting(this.randomMaxDelay);
        this.addSetting(this.autoMaceSync);
        this.addSetting(this.inAir);
        this.addSetting(this.onPearl);
        this.addSetting(this.disableOnGround);
        this.addSetting(this.reequipDelay);
        this.addSetting(this.heightDiff);
        this.addSetting(this.heightRange);
        this.addSetting(this.preHitRange);
        this.addSetting(this.minDropDistance);
    }

    @Override
    public void onEnable() {
        EventBus.register(this);
        this.resetSwapState();
        this.clearQueuedSwap();
        this.suppressChestplateUntil = 0L;
        this.maceReequipAt = 0L;
        this.pearlReequipAt = 0L;
        this.autoMaceSyncPauseUntil = 0L;
        this.autoMaceSyncActiveUntil = 0L;
        if (this.mc.field_1724 != null) {
            this.highestY = this.mc.field_1724.method_23318();
            this.wasOnGround = this.mc.field_1724.method_24828();
        }

        this.airTicks = 0;
    }

    @Override
    public void onDisable() {
        EventBus.unregister(this);
        this.finishSwap();
        this.clearQueuedSwap();
        this.airTicks = 0;
        this.suppressChestplateUntil = 0L;
        this.maceReequipAt = 0L;
        this.pearlReequipAt = 0L;
        this.autoMaceSyncPauseUntil = 0L;
        this.autoMaceSyncActiveUntil = 0L;
    }

    @EventTarget
    public void onMaceHit(MaceHitEvent event) {
        if (this.isEnabled() && this.mc.field_1724 != null) {
            this.scheduleMaceReequip();
            this.suppressChestplateUntil = System.currentTimeMillis() + (long)this.reequipDelay.getValue() + this.getRandomMaxDelay() + 250L;
        }
    }

    @EventTarget
    public void onPearl(PearlThrowEvent event) {
        if (this.isEnabled() && this.onPearl.isEnabled() && this.mc.field_1724 != null) {
            this.schedulePearlReequip();
        }
    }

    @EventTarget
    public void onAutoMaceSync(AutoMaceSyncEvent event) {
        if (this.isEnabled() && this.mc.field_1724 != null && this.autoMaceSync.isEnabled()) {
            long now = System.currentTimeMillis();
            this.autoMaceSyncActiveUntil = now + 250L;
            if (now >= this.autoMaceSyncPauseUntil) {
                if (this.isWearingElytra()) {
                    if (!this.swapInProgress || this.swapTargetElytra) {
                        if (this.swapInProgress && this.swapTargetElytra) {
                            this.finishSwap();
                        }

                        this.clearQueuedSwap();
                        this.queueSwap(false, 0L);
                    }
                }
            }
        }
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            this.updateAirTracking();
            this.processPendingEventReequips();
            boolean autoMaceSyncPriority = this.autoMaceSync.isEnabled() && System.currentTimeMillis() < this.autoMaceSyncActiveUntil;
            boolean forceElytraForAir = !autoMaceSyncPriority && this.inAir.isEnabled() && this.shouldForceElytraInAir();
            if (autoMaceSyncPriority && this.queuedSwap && this.queuedSwapTargetElytra) {
                this.clearQueuedSwap();
            }

            if (forceElytraForAir) {
                if (!this.isWearingElytra()) {
                    if (this.swapInProgress && !this.swapTargetElytra) {
                        this.finishSwap();
                    }

                    this.queueSwap(true, 0L);
                }

                if (this.queuedSwap && !this.queuedSwapTargetElytra) {
                    this.clearQueuedSwap();
                }
            } else if (this.disableOnGround.isEnabled() && this.mc.field_1724.method_24828()) {
                if (this.isWearingElytra()) {
                    this.queueSwap(false, 0L);
                }

                if (this.queuedSwap && this.queuedSwapTargetElytra) {
                    this.clearQueuedSwap();
                }
            } else if (this.shouldSwapToChestplateForCombat()) {
                this.queueSwap(false, 0L);
            }

            if (!this.swapInProgress) {
                this.tryStartQueuedSwap();
            }

            if (this.swapInProgress) {
                this.processSwap();
            }
        }
    }

    private void scheduleMaceReequip() {
        this.maceReequipAt = System.currentTimeMillis() + (long)this.reequipDelay.getValue();
    }

    private void schedulePearlReequip() {
        this.pearlReequipAt = System.currentTimeMillis() + (long)this.reequipDelay.getValue();
    }

    private void processPendingEventReequips() {
        long now = System.currentTimeMillis();
        boolean consumedMace = false;
        if (this.maceReequipAt > 0L && now >= this.maceReequipAt) {
            this.queueSwap(true, 0L);
            this.maceReequipAt = 0L;
            consumedMace = true;
        }

        if (this.pearlReequipAt > 0L && now >= this.pearlReequipAt) {
            if (!consumedMace) {
                this.queueSwap(true, 0L);
            }

            this.pearlReequipAt = 0L;
        }
    }

    private boolean shouldSwapToChestplateForCombat() {
        if (!this.isWearingElytra()) {
            return false;
        } else if (System.currentTimeMillis() < this.suppressChestplateUntil) {
            return false;
        } else if (this.mc.field_1724.method_24828()
            || this.mc.field_1724.method_5799()
            || this.mc.field_1724.method_5771()
            || this.mc.field_1724.method_6101()) {
            return false;
        } else if (this.airTicks < 4) {
            return false;
        } else {
            double manualDrop = this.getManualDropDistance();
            if (manualDrop < this.minDropDistance.getValue()) {
                return false;
            } else if (this.mc.field_1724.method_18798().field_1351 > -0.03) {
                return false;
            } else {
                class_1657 nearest = this.findNearestEnemy(Math.max(this.heightRange.getValue(), this.preHitRange.getValue()));
                if (nearest == null) {
                    return false;
                } else {
                    double diffY = this.mc.field_1724.method_23318() - nearest.method_23318();
                    if (diffY <= 0.0) {
                        return false;
                    } else {
                        double horizontalDistance = this.horizontalDistanceTo(nearest);
                        double totalDistance = this.mc.field_1724.method_5739(nearest);
                        boolean higherThanEnemy = diffY >= this.heightDiff.getValue() && horizontalDistance <= this.heightRange.getValue();
                        boolean almostMaceHitRange = diffY >= 1.0 && totalDistance <= this.preHitRange.getValue();
                        return higherThanEnemy || almostMaceHitRange;
                    }
                }
            }
        }
    }

    private void queueSwap(boolean toElytra, long baseDelayMs) {
        long requestedAt = System.currentTimeMillis() + Math.max(0L, baseDelayMs);
        this.queuedSwap = true;
        this.queuedSwapTargetElytra = toElytra;
        this.queuedSwapAt = requestedAt;
    }

    private void clearQueuedSwap() {
        this.queuedSwap = false;
        this.queuedSwapTargetElytra = false;
        this.queuedSwapAt = 0L;
    }

    private void tryStartQueuedSwap() {
        if (this.queuedSwap && this.mc.field_1724 != null) {
            if (System.currentTimeMillis() >= this.queuedSwapAt) {
                if (this.queuedSwapTargetElytra && this.isWearingElytra()) {
                    this.clearQueuedSwap();
                } else if (!this.queuedSwapTargetElytra && !this.isWearingElytra() && this.isChestplate(this.mc.field_1724.method_6118(class_1304.field_6174))) {
                    this.clearQueuedSwap();
                } else {
                    if (this.startSwap(this.queuedSwapTargetElytra)) {
                        this.clearQueuedSwap();
                    } else {
                        this.clearQueuedSwap();
                    }
                }
            }
        }
    }

    private boolean startSwap(boolean toElytra) {
        if (this.mc.field_1724 != null && !this.swapInProgress) {
            int targetSlot = toElytra ? this.findElytraSlot() : this.findChestplateSlot();
            if (targetSlot == -1) {
                return false;
            } else {
                this.swapOriginalSlot = this.mc.field_1724.method_31548().method_67532();
                this.mc.field_1724.method_31548().method_61496(targetSlot);
                this.swapTargetElytra = toElytra;
                this.swapWaitTicks = 0;
                this.swapUseSent = false;
                this.swapUseAt = System.currentTimeMillis() + this.getRandomExtraDelay();
                this.swapInProgress = true;
                return true;
            }
        } else {
            return false;
        }
    }

    private void processSwap() {
        if (this.mc.field_1724 == null) {
            this.finishSwap();
        } else if (!this.swapUseSent) {
            if (System.currentTimeMillis() >= this.swapUseAt) {
                KeyBindingAccessor useKey = (KeyBindingAccessor)this.mc.field_1690.field_1904;
                useKey.setTimesPressed(useKey.getTimesPressed() + 1);
                this.swapUseSent = true;
            }
        } else {
            this.swapWaitTicks++;
            class_1799 currentChest = this.mc.field_1724.method_6118(class_1304.field_6174);
            boolean nowWearingElytra = currentChest.method_31574(class_1802.field_8833);
            boolean swapComplete = this.swapTargetElytra && nowWearingElytra || !this.swapTargetElytra && !nowWearingElytra && this.isChestplate(currentChest);
            if (swapComplete || this.swapWaitTicks >= 10) {
                if (!swapComplete) {
                    this.handleSwapFailure();
                }

                this.finishSwap();
            }
        }
    }

    private void handleSwapFailure() {
        this.autoMaceSyncPauseUntil = System.currentTimeMillis() + 250L;
        if (this.mc.field_1724 != null) {
            if (!this.isWearingElytra()) {
                if (this.findElytraSlot() != -1) {
                    this.clearQueuedSwap();
                    this.queueSwap(true, 0L);
                }
            }
        }
    }

    private void finishSwap() {
        if (this.mc.field_1724 != null && this.swapOriginalSlot != -1) {
            this.mc.field_1724.method_31548().method_61496(this.swapOriginalSlot);
        }

        this.resetSwapState();
    }

    private void resetSwapState() {
        this.swapInProgress = false;
        this.swapTargetElytra = false;
        this.swapOriginalSlot = -1;
        this.swapWaitTicks = 0;
        this.swapUseAt = 0L;
        this.swapUseSent = false;
    }

    private void updateAirTracking() {
        if (this.mc.field_1724 != null) {
            boolean onGround = this.mc.field_1724.method_24828();
            if (onGround) {
                this.highestY = this.mc.field_1724.method_23318();
                this.airTicks = 0;
            } else if (this.wasOnGround) {
                this.highestY = this.mc.field_1724.method_23318();
                this.airTicks = 1;
            } else {
                this.highestY = Math.max(this.highestY, this.mc.field_1724.method_23318());
                this.airTicks++;
            }

            this.wasOnGround = onGround;
        }
    }

    private double getManualDropDistance() {
        return this.mc.field_1724 == null ? 0.0 : Math.max(0.0, this.highestY - this.mc.field_1724.method_23318());
    }

    private boolean shouldForceElytraInAir() {
        if (this.mc.field_1724 == null || this.mc.field_1687 == null) {
            return false;
        } else {
            return !this.mc.field_1724.method_24828()
                    && !this.mc.field_1724.method_5799()
                    && !this.mc.field_1724.method_5771()
                    && !this.mc.field_1724.method_6101()
                ? this.getGroundDistanceBelowPlayer() >= 2.0
                : false;
        }
    }

    private double getGroundDistanceBelowPlayer() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            class_243 start = new class_243(
                this.mc.field_1724.method_23317(), this.mc.field_1724.method_5829().field_1322 + 0.05, this.mc.field_1724.method_23321()
            );
            class_243 end = start.method_1031(0.0, -64.0, 0.0);
            class_239 hit = this.mc.field_1687.method_17742(new class_3959(start, end, class_3960.field_17558, class_242.field_1348, this.mc.field_1724));
            return hit.method_17783() != class_240.field_1332 ? Double.MAX_VALUE : Math.max(0.0, start.field_1351 - hit.method_17784().field_1351);
        } else {
            return 0.0;
        }
    }

    private double horizontalDistanceTo(class_1657 target) {
        double dx = this.mc.field_1724.method_23317() - target.method_23317();
        double dz = this.mc.field_1724.method_23321() - target.method_23321();
        return class_3532.method_15355((float)(dx * dx + dz * dz));
    }

    private class_1657 findNearestEnemy(double maxDistance) {
        return this.mc
            .field_1687
            .method_18456()
            .stream()
            .filter(p -> p != this.mc.field_1724 && p.method_5805() && !p.method_68878() && !p.method_7325())
            .filter(p -> this.mc.field_1724.method_5739(p) <= maxDistance)
            .filter(p -> !FriendManager.getInstance().isFriend(p.method_5667()))
            .min(Comparator.comparingDouble(p -> this.mc.field_1724.method_5739(p)))
            .orElse(null);
    }

    private boolean isWearingElytra() {
        return this.mc.field_1724 != null && this.mc.field_1724.method_6118(class_1304.field_6174).method_31574(class_1802.field_8833);
    }

    private int findChestplateSlot() {
        if (this.mc.field_1724 == null) {
            return -1;
        } else {
            for (int i = 0; i < 9; i++) {
                class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
                if (this.isChestplate(stack)) {
                    return i;
                }
            }

            return -1;
        }
    }

    private int findElytraSlot() {
        if (this.mc.field_1724 == null) {
            return -1;
        } else {
            for (int i = 0; i < 9; i++) {
                class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
                if (stack.method_31574(class_1802.field_8833)) {
                    return i;
                }
            }

            return -1;
        }
    }

    private boolean isChestplate(class_1799 stack) {
        if (!stack.method_7960() && !stack.method_31574(class_1802.field_8833)) {
            class_10192 equippable = (class_10192)stack.method_58694(class_9334.field_54196);
            return equippable != null && equippable.comp_3174() == class_1304.field_6174;
        } else {
            return false;
        }
    }

    private long getRandomExtraDelay() {
        if (!this.randomization.isEnabled()) {
            return 0L;
        } else {
            long min = Math.max(0L, this.getRandomMinDelay());
            long max = Math.max(min, this.getRandomMaxDelay());
            return min == max ? min : ThreadLocalRandom.current().nextLong(min, max + 1L);
        }
    }

    private long getRandomMinDelay() {
        return (long)this.randomMinDelay.getValue();
    }

    private long getRandomMaxDelay() {
        return (long)this.randomMaxDelay.getValue();
    }
}
