package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.mixin.MinecraftClientAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.module.implementation.KeybindSetting;
import com.slither.cyemer.util.RotationManager;
import com.slither.cyemer.util.SystemInputSimulator;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1268;
import net.minecraft.class_1792;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_3675;
import net.minecraft.class_3965;
import net.minecraft.class_4969;

@Environment(EnvType.CLIENT)
public class AutoAnchor extends Module {
    private final BooleanSetting speedMode = new BooleanSetting("Speed Mode", false);
    private final SliderSetting speedSetting = new SliderSetting("Speed", 7.5, 1.0, 10.0, 1);
    private final BooleanSetting useTotem = new BooleanSetting("Use Totem", true);
    private final BooleanSetting simClick = new BooleanSetting("Sim Click", false);
    private final BooleanSetting safeMode = new BooleanSetting("Safe Mode", false);
    private final BooleanSetting onlyOwnAnchors = new BooleanSetting("Only Own Anchors", true);
    private final BooleanSetting silentRotation = new BooleanSetting("Silent Rotation", false);
    private final SliderSetting rotationStrength = new SliderSetting("Rotation Strength", 10.0, 1.0, 20.0, 1);
    private final ModeSetting rotPattern = new ModeSetting("Pattern", "Sine", "Smooth", "Linear", "Instant");
    private final SliderSetting rotJitter = new SliderSetting("Jitter", 0.1, 0.0, 1.0, 2);
    // Standalone keybind - click to rebind, right-click to clear. While held,
    // runs the packet-based safe anchor cycle at 5/s (one action per tick).
    private final KeybindSetting safeAnchorKey = new KeybindSetting("Safe Anchor Key");
    private static final double REACH_DISTANCE_SQ = 25.0;
    private static final long COOLDOWN_MS = 400L;
    private static final long TIMEOUT_MS = 1250L;
    private static final long SLOT_RESTORE_DELAY_MS = 50L;
    private static final long ROTATION_WAIT_MS = 50L;
    private static final long GLOWSTONE_TIMEOUT_MS = 200L;
    private AutoAnchor.State currentState = AutoAnchor.State.IDLE;
    private class_2338 anchorPos = null;
    private class_2338 glowstoneBlockPos = null;
    private class_2338 glowstonePlaceAgainst = null;
    private class_2350 glowstonePlaceDirection = null;
    private int originalSlot = -1;
    private long lastTime = 0L;
    private long glowstoneAttemptStart = 0L;
    private long lastRenderExecTime = 0L;
    private final Set<class_2338> placedAnchors = new HashSet<>();
    private class_2680 cachedAnchorState = null;

    // Independent packet-based state machine driven by the safe key. One
    // action per tick -> a full place/charge/shield/detonate cycle takes
    // 4 ticks = 200 ms, so 5 cycles/second when the key is held.
    private SafeKeyState safeKeyState = SafeKeyState.IDLE;
    private class_2338 safeKeyAnchorPos = null;
    private int safeKeyOriginalSlot = -1;

    public AutoAnchor() {
        super("AutoAnchor", "Fills and explodes anchors with randomized patterns.", Category.COMBAT);
        this.addSetting(this.speedMode);
        this.addSetting(this.speedSetting);
        this.addSetting(this.useTotem);
        this.addSetting(this.simClick);
        this.addSetting(this.safeMode);
        this.addSetting(this.onlyOwnAnchors);
        this.addSetting(this.silentRotation);
        this.addSetting(this.rotationStrength);
        this.addSetting(this.rotPattern);
        this.addSetting(this.rotJitter);
        this.addSetting(this.safeAnchorKey);
    }

    @Override
    public void onEnable() {
        this.reset();
    }

    @Override
    public void onDisable() {
        RotationManager.clearTarget(this);
        this.restoreOriginalSlot();
        this.reset();
        this.placedAnchors.clear();
    }

    public void onItemUse(class_3965 hitResult) {
        if (this.onlyOwnAnchors.isEnabled()) {
            if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
                if (this.mc.field_1724.method_6047().method_7909() != class_1802.field_23141) {
                    if (this.mc.field_1724.method_6047().method_7909() == class_1802.field_8801) {
                        class_2338 targetPos = hitResult.method_17777();
                        class_2680 state = this.mc.field_1687.method_8320(targetPos);
                        if (state.method_27852(class_2246.field_23152) && (Integer)state.method_11654(class_4969.field_23153) == 4) {
                            this.placedAnchors.remove(targetPos);
                        }
                    }
                } else {
                    class_2338 clickedPos = hitResult.method_17777();
                    class_2680 clickedState = this.mc.field_1687.method_8320(clickedPos);
                    class_2338 anchorPlacementPos;
                    if (clickedState.method_45474()) {
                        anchorPlacementPos = clickedPos;
                    } else {
                        anchorPlacementPos = clickedPos.method_10093(hitResult.method_17780());
                    }

                    this.placedAnchors.add(anchorPlacementPos);
                }
            }
        }
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            this.handleSafeAnchorKey();

            if (!this.speedMode.isEnabled()) {
                this.updateLogic();
            }

            if (this.speedMode.isEnabled()) {
                long delay = this.getSpeedModeDelay();
                if (System.currentTimeMillis() - this.lastRenderExecTime >= delay) {
                    this.updateLogic();
                    this.lastRenderExecTime = System.currentTimeMillis();
                }
            }
        }
    }

    private void handleSafeAnchorKey() {
        int keyCode = this.safeAnchorKey.getKeyCode();
        if (keyCode <= 0 || this.mc.field_1761 == null) {
            this.abortSafeKeyCycle();
            return;
        }
        boolean held = class_3675.method_15987(class_310.method_1551().method_22683(), keyCode);
        if (!held) {
            this.abortSafeKeyCycle();
            return;
        }

        switch (this.safeKeyState) {
            case IDLE:
                this.safeKeyPlaceAnchor();
                break;
            case CHARGE:
                this.safeKeyChargeAnchor();
                break;
            case SHIELD:
                this.safeKeyPlaceShield();
                break;
            case DETONATE:
                this.safeKeyDetonate();
                break;
        }
    }

    private void safeKeyPlaceAnchor() {
        if (!(this.mc.field_1765 instanceof class_3965 blockHit)) return;
        class_2338 anchorTarget = blockHit.method_17777().method_10093(blockHit.method_17780());
        if (!this.mc.field_1687.method_8320(anchorTarget).method_45474()) return;
        int anchorSlot = this.findItemInHotbar(class_1802.field_23141);
        if (anchorSlot == -1) return;

        if (this.safeKeyOriginalSlot == -1) {
            this.safeKeyOriginalSlot = this.mc.field_1724.method_31548().method_67532();
        }
        this.mc.field_1724.method_31548().method_61496(anchorSlot);
        this.mc.field_1761.method_2896(this.mc.field_1724, class_1268.field_5808, blockHit);
        this.safeKeyAnchorPos = anchorTarget;
        this.safeKeyState = SafeKeyState.CHARGE;
    }

    private void safeKeyChargeAnchor() {
        if (this.safeKeyAnchorPos == null) {
            this.safeKeyState = SafeKeyState.IDLE;
            return;
        }
        int glowSlot = this.findItemInHotbar(class_1802.field_8801);
        if (glowSlot == -1) {
            this.safeKeyState = SafeKeyState.SHIELD;
            return;
        }
        this.mc.field_1724.method_31548().method_61496(glowSlot);
        this.mc.field_1761.method_2896(this.mc.field_1724, class_1268.field_5808,
                this.hitAt(this.safeKeyAnchorPos, class_2350.field_11036));
        this.safeKeyState = SafeKeyState.SHIELD;
    }

    private void safeKeyPlaceShield() {
        if (this.safeKeyAnchorPos == null) {
            this.safeKeyState = SafeKeyState.DETONATE;
            return;
        }
        int shieldSlot = this.findItemInHotbar(class_1802.field_8801);
        if (shieldSlot == -1) {
            this.safeKeyState = SafeKeyState.DETONATE;
            return;
        }
        class_2338 shieldPos = this.safeKeyShieldPos(this.safeKeyAnchorPos);
        if (shieldPos == null || !this.mc.field_1687.method_8320(shieldPos).method_45474()) {
            this.safeKeyState = SafeKeyState.DETONATE;
            return;
        }
        class_2338 placeAgainst = this.safeKeyAdjacentSolid(shieldPos);
        if (placeAgainst == null) {
            this.safeKeyState = SafeKeyState.DETONATE;
            return;
        }
        class_2350 face = class_2350.method_10147(
                shieldPos.method_10263() - placeAgainst.method_10263(),
                shieldPos.method_10264() - placeAgainst.method_10264(),
                shieldPos.method_10260() - placeAgainst.method_10260()
        );
        if (face == null) {
            this.safeKeyState = SafeKeyState.DETONATE;
            return;
        }
        this.mc.field_1724.method_31548().method_61496(shieldSlot);
        this.mc.field_1761.method_2896(this.mc.field_1724, class_1268.field_5808, this.hitAt(placeAgainst, face));
        this.safeKeyState = SafeKeyState.DETONATE;
    }

    private void safeKeyDetonate() {
        if (this.safeKeyAnchorPos == null) {
            this.safeKeyState = SafeKeyState.IDLE;
            return;
        }
        // Switch to something non-glowstone so the interact detonates instead of charging.
        int restoreSlot = this.safeKeyOriginalSlot != -1 ? this.safeKeyOriginalSlot : this.findNonGlowstoneSlot();
        if (restoreSlot != -1) {
            this.mc.field_1724.method_31548().method_61496(restoreSlot);
        }
        this.mc.field_1761.method_2896(this.mc.field_1724, class_1268.field_5808,
                this.hitAt(this.safeKeyAnchorPos, class_2350.field_11036));
        this.safeKeyAnchorPos = null;
        this.safeKeyState = SafeKeyState.IDLE;
    }

    private void abortSafeKeyCycle() {
        if (this.safeKeyState == SafeKeyState.IDLE && this.safeKeyOriginalSlot == -1) return;
        if (this.safeKeyOriginalSlot != -1 && this.mc.field_1724 != null) {
            this.mc.field_1724.method_31548().method_61496(this.safeKeyOriginalSlot);
        }
        this.safeKeyOriginalSlot = -1;
        this.safeKeyAnchorPos = null;
        this.safeKeyState = SafeKeyState.IDLE;
    }

    private class_3965 hitAt(class_2338 blockPos, class_2350 face) {
        class_243 hit = new class_243(
                blockPos.method_10263() + 0.5 + face.method_10148() * 0.5,
                blockPos.method_10264() + 0.5 + face.method_10164() * 0.5,
                blockPos.method_10260() + 0.5 + face.method_10165() * 0.5
        );
        return new class_3965(hit, face, blockPos, false);
    }

    private class_2338 safeKeyShieldPos(class_2338 anchorPos) {
        class_243 playerPos = this.mc.field_1724.method_73189();
        class_243 anchorCenter = anchorPos.method_46558();
        class_243 midpoint = playerPos.method_1019(anchorCenter).method_1021(0.5);
        return new class_2338(
                (int) Math.floor(midpoint.field_1352),
                anchorPos.method_10264(),
                (int) Math.floor(midpoint.field_1350)
        );
    }

    private class_2338 safeKeyAdjacentSolid(class_2338 pos) {
        class_2350[] dirs = {
                class_2350.field_11033, class_2350.field_11036,
                class_2350.field_11043, class_2350.field_11035,
                class_2350.field_11039, class_2350.field_11034
        };
        for (class_2350 dir : dirs) {
            class_2338 adj = pos.method_10093(dir);
            if (!this.mc.field_1687.method_8320(adj).method_45474()) return adj;
        }
        return null;
    }

    private int findNonGlowstoneSlot() {
        for (int i = 0; i < 9; i++) {
            if (this.mc.field_1724.method_31548().method_5438(i).method_7909() != class_1802.field_8801) {
                return i;
            }
        }
        return -1;
    }

    private long getSpeedModeDelay() {
        double speed = this.speedSetting.getValue();
        return speed >= 10.0 ? 0L : (long)((10.0 - speed) * 8.88888888888889);
    }

    private void updateLogic() {
        if (this.currentState != AutoAnchor.State.IDLE && this.currentState != AutoAnchor.State.COOLDOWN && this.timePassed(1250L)) {
            this.reset();
        } else {
            switch (this.currentState) {
                case IDLE:
                    if (!(this.mc.field_1765 instanceof class_3965 blockHitResult)) {
                        return;
                    }

                    class_2338 var9 = blockHitResult.method_17777();
                    if (this.onlyOwnAnchors.isEnabled() && !this.placedAnchors.contains(var9)) {
                        return;
                    }

                    if (this.isNewAnchor(var9)) {
                        this.anchorPos = var9;
                        this.originalSlot = this.mc.field_1724.method_31548().method_67532();
                        this.resetTimer();
                        this.startRotatingToAnchor();
                        this.currentState = AutoAnchor.State.ROTATING_TO_FILL;
                    }
                    break;
                case ROTATING_TO_FILL:
                    if (!this.isAnchorStillValid()) {
                        this.reset();
                        return;
                    }

                    if (this.timePassed(50L) || this.isRotationComplete()) {
                        this.currentState = AutoAnchor.State.FILL_ANCHOR;
                        this.resetTimer();
                    }
                    break;
                case FILL_ANCHOR:
                    if (!this.isAnchorStillValid()) {
                        this.reset();
                        return;
                    }

                    int glowstoneSlot = this.findItemInHotbar(class_1802.field_8801);
                    if (glowstoneSlot != -1) {
                        this.mc.field_1724.method_31548().method_61496(glowstoneSlot);
                        this.simClickUseKey();
                        this.currentState = AutoAnchor.State.WAITING_FOR_FILL;
                        this.resetTimer();
                    } else {
                        this.reset();
                    }
                    break;
                case WAITING_FOR_FILL:
                    if (!this.isAnchorStillValid()) {
                        this.reset();
                        return;
                    }

                    if ((Integer)this.cachedAnchorState.method_11654(class_4969.field_23153) > 0) {
                        if (this.shouldUseSafeMode()) {
                            this.currentState = AutoAnchor.State.ROTATING_TO_GLOWSTONE;
                            this.glowstoneAttemptStart = System.currentTimeMillis();
                        } else {
                            this.currentState = AutoAnchor.State.PREPARE_TO_EXPLODE;
                        }

                        this.resetTimer();
                    }
                    break;
                case ROTATING_TO_GLOWSTONE:
                    if (!this.isAnchorStillValid()) {
                        this.reset();
                        return;
                    }

                    if (System.currentTimeMillis() - this.glowstoneAttemptStart > 200L) {
                        this.resetTimer();
                        this.currentState = AutoAnchor.State.PREPARE_TO_EXPLODE;
                    } else {
                        class_243 playerPos = this.mc.field_1724.method_73189();
                        class_243 anchorCenter = this.anchorPos.method_46558();
                        class_243 midpoint = playerPos.method_1019(anchorCenter).method_1021(0.5);
                        this.glowstoneBlockPos = new class_2338(
                            (int)Math.floor(midpoint.field_1352), this.anchorPos.method_10264(), (int)Math.floor(midpoint.field_1350)
                        );
                        class_2680 glowstoneBlockState = this.mc.field_1687.method_8320(this.glowstoneBlockPos);
                        if (glowstoneBlockState.method_45474()) {
                            int glowstoneBlockSlot = this.findItemInHotbar(class_1802.field_8801);
                            if (glowstoneBlockSlot != -1) {
                                this.mc.field_1724.method_31548().method_61496(glowstoneBlockSlot);
                                this.glowstonePlaceAgainst = this.findAdjacentSolidBlock(this.glowstoneBlockPos);
                                if (this.glowstonePlaceAgainst != null) {
                                    this.glowstonePlaceDirection = class_2350.method_10147(
                                        this.glowstoneBlockPos.method_10263() - this.glowstonePlaceAgainst.method_10263(),
                                        this.glowstoneBlockPos.method_10264() - this.glowstonePlaceAgainst.method_10264(),
                                        this.glowstoneBlockPos.method_10260() - this.glowstonePlaceAgainst.method_10260()
                                    );
                                    if (this.glowstonePlaceDirection != null) {
                                        this.startRotatingToGlowstoneBlock(this.glowstonePlaceAgainst);
                                        this.currentState = AutoAnchor.State.PLACE_GLOWSTONE_BLOCK;
                                        this.resetTimer();
                                        return;
                                    }
                                }
                            } else {
                                this.resetTimer();
                                this.currentState = AutoAnchor.State.PREPARE_TO_EXPLODE;
                            }
                        } else {
                            this.resetTimer();
                            this.currentState = AutoAnchor.State.ROTATING_TO_ANCHOR;
                            this.startRotatingToAnchor();
                        }
                    }
                    break;
                case PLACE_GLOWSTONE_BLOCK:
                    if (!this.isAnchorStillValid()) {
                        this.reset();
                        return;
                    }

                    if (this.timePassed(50L) || this.isRotationComplete()) {
                        if (this.glowstonePlaceAgainst != null && this.glowstonePlaceDirection != null) {
                            this.simClickUseKey();
                            this.resetTimer();
                            this.currentState = AutoAnchor.State.ROTATING_TO_ANCHOR;
                            this.startRotatingToAnchor();
                        } else {
                            this.resetTimer();
                            this.currentState = AutoAnchor.State.PREPARE_TO_EXPLODE;
                        }
                    }
                    break;
                case ROTATING_TO_ANCHOR:
                    if (!this.isAnchorStillValid()) {
                        this.reset();
                        return;
                    }

                    if (this.timePassed(50L) || this.isRotationComplete()) {
                        this.currentState = AutoAnchor.State.PREPARE_TO_EXPLODE;
                        this.resetTimer();
                    }
                    break;
                case PREPARE_TO_EXPLODE:
                    if (!this.isAnchorStillValid()) {
                        this.reset();
                        return;
                    }

                    int slotToSwitchTo = this.useTotem.isEnabled() ? this.findItemInHotbar(class_1802.field_8288) : -1;
                    if (slotToSwitchTo == -1) {
                        slotToSwitchTo = this.findEmptyHotbarSlot();
                    }

                    if (slotToSwitchTo != -1) {
                        this.mc.field_1724.method_31548().method_61496(slotToSwitchTo);
                        this.startRotatingToAnchor();
                        this.currentState = AutoAnchor.State.ROTATING_TO_EXPLODE;
                        this.resetTimer();
                    } else {
                        this.reset();
                    }
                    break;
                case ROTATING_TO_EXPLODE:
                    if (!this.isAnchorStillValid()) {
                        this.reset();
                        return;
                    }

                    if (this.timePassed(50L) || this.isRotationComplete()) {
                        this.currentState = AutoAnchor.State.ARMED;
                        this.resetTimer();
                    }
                    break;
                case ARMED:
                    if (!this.isAnchorStillValid()) {
                        this.reset();
                        return;
                    }

                    this.simClickUseKey();
                    if (this.anchorPos != null) {
                        this.placedAnchors.remove(this.anchorPos);
                    }

                    RotationManager.clearTarget(this);
                    this.resetTimer();
                    this.currentState = AutoAnchor.State.COOLDOWN;
                    break;
                case COOLDOWN:
                    if (this.timePassed(50L) && this.originalSlot != -1) {
                        this.restoreOriginalSlot();
                    }

                    if (this.timePassed(400L)) {
                        this.reset();
                    }
            }
        }
    }

    private boolean shouldUseSimClick() {
        return this.speedMode.isEnabled() ? false : this.simClick.isEnabled();
    }

    private boolean shouldUseSafeMode() {
        return this.speedMode.isEnabled() ? false : this.safeMode.isEnabled();
    }

    private void resetTimer() {
        this.lastTime = System.currentTimeMillis();
    }

    private boolean timePassed(long milliseconds) {
        return System.currentTimeMillis() - this.lastTime >= milliseconds;
    }

    private RotationManager.RotationMode getCurrentMode() {
        try {
            return RotationManager.RotationMode.valueOf(this.rotPattern.getCurrentMode().toUpperCase());
        } catch (Exception var2) {
            return RotationManager.RotationMode.SINE;
        }
    }

    private void startRotatingToAnchor() {
        if (this.anchorPos != null) {
            RotationManager.setRotationSupplier(
                this,
                RotationManager.Priority.HIGH,
                this::findVisibleAnchorPoint,
                this.rotationStrength.getValue(),
                this.getCurrentMode(),
                this.rotJitter.getValue(),
                this.silentRotation.isEnabled(),
                false
            );
        }
    }

    private void startRotatingToGlowstoneBlock(class_2338 targetBlock) {
        if (targetBlock != null) {
            RotationManager.setRotationSupplier(
                this,
                RotationManager.Priority.HIGH,
                () -> targetBlock.method_46558(),
                this.rotationStrength.getValue(),
                this.getCurrentMode(),
                this.rotJitter.getValue(),
                this.silentRotation.isEnabled(),
                false
            );
        }
    }

    private class_243 findVisibleAnchorPoint() {
        if (this.anchorPos == null) {
            return class_243.field_1353;
        } else {
            return this.glowstoneBlockPos != null
                ? new class_243(this.anchorPos.method_10263() + 0.5, this.anchorPos.method_10264() + 0.9, this.anchorPos.method_10260() + 0.5)
                : this.anchorPos.method_46558();
        }
    }

    private void simClickUseKey() {
        if (this.shouldUseSimClick()) {
            SystemInputSimulator.pressUse();
            SystemInputSimulator.releaseUse();
        } else {
            ((MinecraftClientAccessor)this.mc).useItem();
        }
    }

    private boolean isNewAnchor(class_2338 pos) {
        if (this.mc.field_1724.method_33571().method_1025(pos.method_46558()) >= 25.0) {
            return false;
        } else {
            class_2680 state = this.mc.field_1687.method_8320(pos);
            return state.method_27852(class_2246.field_23152) && (Integer)state.method_11654(class_4969.field_23153) == 0;
        }
    }

    private boolean isAnchorStillValid() {
        if (this.anchorPos != null && !(this.mc.field_1724.method_33571().method_1025(this.anchorPos.method_46558()) >= 25.0)) {
            this.cachedAnchorState = this.mc.field_1687.method_8320(this.anchorPos);
            return this.cachedAnchorState.method_27852(class_2246.field_23152);
        } else {
            return false;
        }
    }

    private boolean isRotationComplete() {
        if (this.anchorPos == null || this.mc.field_1724 == null) {
            return false;
        } else if (!RotationManager.isActive()) {
            return true;
        } else {
            class_243 targetPoint = this.findVisibleAnchorPoint();
            float[] needed = RotationManager.calculateRotationsToPos(targetPoint, RotationManager.getFinalYaw());
            float currentYaw = RotationManager.getFinalYaw();
            float currentPitch = RotationManager.getFinalPitch();
            float yawDiff = Math.abs(class_3532.method_15393(needed[0] - currentYaw));
            float pitchDiff = Math.abs(needed[1] - currentPitch);
            return yawDiff < 3.0F && pitchDiff < 3.0F;
        }
    }

    private int findItemInHotbar(class_1792 item) {
        for (int i = 0; i < 9; i++) {
            if (this.mc.field_1724.method_31548().method_5438(i).method_7909() == item) {
                return i;
            }
        }

        return -1;
    }

    private int findEmptyHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (this.mc.field_1724.method_31548().method_5438(i).method_7960()) {
                return i;
            }
        }

        return -1;
    }

    private class_2338 findAdjacentSolidBlock(class_2338 pos) {
        class_2350[] directions = new class_2350[]{
            class_2350.field_11033, class_2350.field_11036, class_2350.field_11043, class_2350.field_11035, class_2350.field_11039, class_2350.field_11034
        };

        for (class_2350 dir : directions) {
            class_2338 adjacentPos = pos.method_10093(dir);
            if (!this.mc.field_1687.method_8320(adjacentPos).method_45474()) {
                return adjacentPos;
            }
        }

        return null;
    }

    private void restoreOriginalSlot() {
        if (this.mc.field_1724 != null && this.originalSlot != -1) {
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
            this.originalSlot = -1;
        }
    }

    private void reset() {
        RotationManager.clearTarget(this);
        this.restoreOriginalSlot();
        this.currentState = AutoAnchor.State.IDLE;
        this.anchorPos = null;
        this.glowstoneBlockPos = null;
        this.glowstonePlaceAgainst = null;
        this.glowstonePlaceDirection = null;
        this.cachedAnchorState = null;
        this.resetTimer();
        this.glowstoneAttemptStart = 0L;
        this.lastRenderExecTime = 0L;
    }

    @Environment(EnvType.CLIENT)
    private static enum SafeKeyState {
        IDLE,
        CHARGE,
        SHIELD,
        DETONATE;
    }

    @Environment(EnvType.CLIENT)
    private static enum State {
        IDLE,
        ROTATING_TO_FILL,
        FILL_ANCHOR,
        WAITING_FOR_FILL,
        ROTATING_TO_GLOWSTONE,
        PLACE_GLOWSTONE_BLOCK,
        ROTATING_TO_ANCHOR,
        PREPARE_TO_EXPLODE,
        ROTATING_TO_EXPLODE,
        ARMED,
        COOLDOWN;
    }
}
