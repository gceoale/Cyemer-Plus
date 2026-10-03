package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.RotationManager;
import com.slither.cyemer.util.render.RenderUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1684;
import net.minecraft.class_1776;
import net.minecraft.class_1792;
import net.minecraft.class_1802;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_3486;
import net.minecraft.class_3532;
import net.minecraft.class_3610;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_640;
import net.minecraft.class_746;
import net.minecraft.class_9239;
import net.minecraft.class_9799;
import net.minecraft.class_4597.class_4598;

@Environment(EnvType.CLIENT)
public class PearlCatch extends Module {
    private final SliderSetting rotationStrength = new SliderSetting("Rotation Speed", 15.0, 1.0, 20.0, 1);
    private final ModeSetting rotPattern = new ModeSetting("Pattern", "Sine", "Smooth", "Linear", "Instant");
    private final SliderSetting rotRandom = new SliderSetting("Randomness", 0.0, 0.0, 1.0, 2);
    private final BooleanSetting silentRotation = new BooleanSetting("Silent Aim", true);
    private final BooleanSetting showTrajectory = new BooleanSetting("Show Trajectory", true);
    // class_1682.method_7490
    private static final double PEARL_GRAVITY = 0.03;
    // class_1682.method_63673 multiplies by these floats, keep the float widening so it matches bit for bit
    private static final double PEARL_DRAG = 0.99F;
    private static final double PEARL_WATER_DRAG = 0.8F;
    // class_3857 owner ctor spawns the pearl at eyeY - 0.1F
    private static final double PEARL_SPAWN_DROP = 0.1F;
    private static final double PEARL_HALF_WIDTH = 0.125;
    private static final double PEARL_HEIGHT = 0.25;
    private static final float FALLBACK_POWER = 1.5F;
    private static final int MAX_PATH_TICKS = 400;
    private static final int WIND_SIM_TICKS = 50;
    private PearlCatch.State currentState = PearlCatch.State.IDLE;
    private int originalSlot = -1;
    private int pearlThrowTick = 0;
    private int executionTimer = 0;
    private final Set<UUID> ignoredPearls = new HashSet<>();
    private UUID trackedPearl = null;
    private boolean pathLanded = false;
    private List<class_243> predictedPearlPath = new ArrayList<>();
    private class_243 renderIntercept = null;
    private class_243 renderAim = null;

    public PearlCatch() {
        super("PearlCatch", "BETA ASF.", Category.MOVEMENT);
        this.addSetting(this.rotationStrength);
        this.addSetting(this.rotPattern);
        this.addSetting(this.rotRandom);
        this.addSetting(this.silentRotation);
        this.addSetting(this.showTrajectory);
    }

    @Override
    public void onEnable() {
        if (this.mc.field_1724 == null || this.mc.field_1687 == null) {
            this.toggle();
        } else if (this.hasItem(class_1802.field_8634) && this.hasItem(class_1802.field_49098)) {
            this.currentState = PearlCatch.State.THROWING_PEARL;
            this.originalSlot = this.mc.field_1724.method_31548().method_67532();
            this.predictedPearlPath.clear();
            this.renderIntercept = null;
            this.renderAim = null;
            this.executionTimer = 0;
            this.trackedPearl = null;
            this.pathLanded = false;
            // our pearls already in the air aren't the one we're about to throw
            this.ignoredPearls.clear();
            for (class_1297 entity : this.mc.field_1687.method_18112()) {
                if (entity instanceof class_1684 pearl && pearl.method_24921() == this.mc.field_1724) {
                    this.ignoredPearls.add(pearl.method_5667());
                }
            }
        } else {
            this.toggle();
        }
    }

    @Override
    public void onDisable() {
        if (this.mc.field_1724 != null && this.originalSlot != -1) {
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
        }

        if (this.mc.field_1690 != null) {
            this.mc.field_1690.field_1904.method_23481(false);
        }

        RotationManager.stop(this);
        this.currentState = PearlCatch.State.IDLE;
        this.renderIntercept = null;
        this.renderAim = null;
        this.trackedPearl = null;
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            switch (this.currentState) {
                case THROWING_PEARL:
                    this.handlePearlThrow();
                    break;
                case NORMAL_THROW:
                    this.handleNormalThrow();
                    break;
                case TRACKING:
                    this.handleTracking();
                    break;
                case EXECUTING_CATCH:
                    this.handleExecution();
                    break;
                case RESETTING:
                    this.handleReset();
            }
        }
    }

    private void handlePearlThrow() {
        int pearlSlot = this.findItemSlot(class_1802.field_8634);
        if (pearlSlot == -1) {
            this.toggle();
        } else {
            this.mc.field_1724.method_31548().method_61496(pearlSlot);
            this.mc.field_1690.field_1904.method_23481(true);
            this.predictThrow();
            if (!this.predictedPearlPath.isEmpty() && this.isInterceptPossible()) {
                this.currentState = PearlCatch.State.TRACKING;
                this.pearlThrowTick = this.mc.field_1724.field_6012;
            } else {
                this.currentState = PearlCatch.State.NORMAL_THROW;
                this.executionTimer = 0;
            }
        }
    }

    private void handleNormalThrow() {
        this.mc.field_1690.field_1904.method_23481(true);
        this.executionTimer++;
        if (this.executionTimer > 1) {
            this.mc.field_1690.field_1904.method_23481(false);
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
            this.toggle();
        }
    }

    private void handleTracking() {
        this.mc.field_1690.field_1904.method_23481(false);
        int ticksSinceThrow = this.mc.field_1724.field_6012 - this.pearlThrowTick;
        class_1684 pearl = this.findOurPearl();
        if (pearl != null) {
            // the real pearl already has the server's random spread and any knockback baked in,
            // so once it exists the path is rebuilt from it every tick instead of the throw-time guess
            this.trackedPearl = pearl.method_5667();
            this.simulatePearl(pearl.method_73189(), pearl.method_18798(), pearl.method_5799(), pearl);
        } else if (this.trackedPearl != null) {
            // it landed or despawned, nothing left to catch
            this.endTracking();
            return;
        }

        if (ticksSinceThrow > 60) {
            this.endTracking();
            return;
        }

        if (this.trackedPearl == null) {
            // spawn packet isn't here yet, don't aim at a guess
            return;
        }

        int lead = this.leadTicks();
        if (this.predictedPearlPath.size() <= lead + 5) {
            // lands before a wind charge thrown now could reach it
            this.endTracking();
            return;
        }

        class_243 predictedPos = this.mc.field_1724.method_73189().method_1019(this.mc.field_1724.method_18798());
        class_243 predictedEyePos = new class_243(
            predictedPos.field_1352, predictedPos.field_1351 + this.mc.field_1724.method_5751(), predictedPos.field_1350
        );
        class_243 predictedVel = this.mc.field_1724.method_18798();
        PearlCatch.SolverResult result = this.solveInterceptWithPrediction(lead, predictedEyePos, predictedVel);
        if (result != null) {
            this.renderIntercept = result.interceptPos;
            this.renderAim = result.aimDirection;
            RotationManager.setRotationSupplier(this, RotationManager.Priority.HIGHEST, () -> {
                class_243 nextPos = this.mc.field_1724.method_73189().method_1019(this.mc.field_1724.method_18798());
                class_243 nextEyePos = new class_243(nextPos.field_1352, nextPos.field_1351 + this.mc.field_1724.method_5751(), nextPos.field_1350);
                class_243 nextVel = this.mc.field_1724.method_18798();
                PearlCatch.SolverResult freshResult = this.solveInterceptWithPrediction(this.leadTicks(), nextEyePos, nextVel);
                return freshResult != null ? freshResult.aimDirection : result.aimDirection;
            }, this.rotationStrength.getValue(), this.getRotationMode(), this.rotRandom.getValue(), this.silentRotation.isEnabled(), false);
            if (RotationManager.isRotationComplete(1.5F)) {
                int windSlot = this.findItemSlot(class_1802.field_49098);
                if (windSlot != -1) {
                    this.mc.field_1724.method_31548().method_61496(windSlot);
                    this.currentState = PearlCatch.State.EXECUTING_CATCH;
                    this.executionTimer = 0;
                }
            }
        } else {
            this.renderIntercept = null;
            RotationManager.clearTarget(this);
        }
    }

    private void endTracking() {
        RotationManager.clearTarget(this);
        this.currentState = PearlCatch.State.RESETTING;
    }

    private void handleExecution() {
        int windSlot = this.findItemSlot(class_1802.field_49098);
        if (windSlot == -1) {
            RotationManager.clearTarget(this);
            this.currentState = PearlCatch.State.RESETTING;
        } else {
            this.mc.field_1724.method_31548().method_61496(windSlot);
            this.mc.field_1690.field_1904.method_23481(true);
            this.executionTimer++;
            if (this.executionTimer > 3) {
                RotationManager.clearTarget(this);
                this.currentState = PearlCatch.State.RESETTING;
            }
        }
    }

    private void handleReset() {
        this.mc.field_1690.field_1904.method_23481(false);
        if (RotationManager.isActive() && this.executionTimer <= 10) {
            this.executionTimer++;
        } else {
            RotationManager.stop(this);
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
            this.toggle();
        }
    }

    private RotationManager.RotationMode getRotationMode() {
        try {
            return RotationManager.RotationMode.valueOf(this.rotPattern.getCurrentMode().toUpperCase());
        } catch (Exception var2) {
            return RotationManager.RotationMode.SINE;
        }
    }

    /**
     * The pearl we're about to throw, built the way the server builds it: class_3857 spawns it at
     * eyeY - 0.1F, class_1676.method_24919 aims it from yaw/pitch through the MathHelper tables and
     * adds our last tick of movement (no vertical part on the ground). The server also adds a small
     * random spread nobody can know ahead of time, so this only stands in until the real pearl shows up.
     */
    private void predictThrow() {
        class_746 player = this.mc.field_1724;
        float yaw = class_3532.method_15393(player.method_36454());
        float pitch = player.method_36455();
        float dx = -class_3532.method_15374(yaw * 0.017453292F) * class_3532.method_15362(pitch * 0.017453292F);
        float dy = -class_3532.method_15374(pitch * 0.017453292F);
        float dz = class_3532.method_15362(yaw * 0.017453292F) * class_3532.method_15362(pitch * 0.017453292F);
        class_243 velocity = new class_243(dx, dy, dz).method_1029().method_1021(this.power(class_1776.field_55033));
        velocity = velocity.method_1031(
            player.method_23317() - player.field_6014,
            player.method_24828() ? 0.0 : player.method_23318() - player.field_6036,
            player.method_23321() - player.field_5969
        );
        class_243 start = new class_243(player.method_23317(), player.method_23320() - PEARL_SPAWN_DROP, player.method_23321());
        this.simulatePearl(start, velocity, false, player);
    }

    /**
     * class_1682.method_5773 one tick at a time: gravity, then drag (0.8F in water, 0.99F in air,
     * from the water state at the end of the previous tick), then a COLLIDER raycast from where the
     * pearl is to where it's going. A block in the way ends the flight on the hit point, which is
     * exactly where the pearl lands you.
     */
    private void simulatePearl(class_243 start, class_243 velocity, boolean inWater, class_1297 shapeContext) {
        this.predictedPearlPath.clear();
        this.pathLanded = false;
        this.predictedPearlPath.add(start);
        double x = start.field_1352;
        double y = start.field_1351;
        double z = start.field_1350;
        double vx = velocity.field_1352;
        double vy = velocity.field_1351;
        double vz = velocity.field_1350;
        boolean water = inWater;
        int floor = this.mc.field_1687.method_31607() - 64;

        for (int t = 0; t < MAX_PATH_TICKS; t++) {
            vy -= PEARL_GRAVITY;
            double drag = water ? PEARL_WATER_DRAG : PEARL_DRAG;
            vx *= drag;
            vy *= drag;
            vz *= drag;
            class_243 from = new class_243(x, y, z);
            class_243 to = new class_243(x + vx, y + vy, z + vz);
            class_3965 hit = this.mc
                .field_1687
                .method_17742(new class_3959(from, to, class_3959.class_3960.field_17558, class_3959.class_242.field_1348, shapeContext));
            if (hit.method_17783() != class_239.class_240.field_1333) {
                this.predictedPearlPath.add(hit.method_17784());
                this.pathLanded = true;
                return;
            }

            x = to.field_1352;
            y = to.field_1351;
            z = to.field_1350;
            this.predictedPearlPath.add(to);
            if (y < floor) {
                return;
            }

            water = this.touchingWater(x, y, z);
        }
    }

    /** Entity.method_5692 for water on the pearl's 0.25 box, contracted by 0.001 like vanilla. */
    private boolean touchingWater(double x, double y, double z) {
        double minX = x - PEARL_HALF_WIDTH + 0.001;
        double maxX = x + PEARL_HALF_WIDTH - 0.001;
        double minY = y + 0.001;
        double maxY = y + PEARL_HEIGHT - 0.001;
        double minZ = z - PEARL_HALF_WIDTH + 0.001;
        double maxZ = z + PEARL_HALF_WIDTH - 0.001;

        for (int bx = class_3532.method_15357(minX); bx < class_3532.method_15384(maxX); bx++) {
            for (int by = class_3532.method_15357(minY); by < class_3532.method_15384(maxY); by++) {
                for (int bz = class_3532.method_15357(minZ); bz < class_3532.method_15384(maxZ); bz++) {
                    class_2338 pos = new class_2338(bx, by, bz);
                    class_3610 fluid = this.mc.field_1687.method_8316(pos);
                    if (fluid.method_15767(class_3486.field_15517) && (float)by + fluid.method_15763(this.mc.field_1687, pos) >= minY) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /** Wind charges hold their velocity: drag 1.0 in air and water, acceleration zeroed, no gravity (class_9236). */
    private List<class_243> windLine(class_243 start, class_243 velocity) {
        List<class_243> line = new ArrayList<>(WIND_SIM_TICKS);
        class_243 pos = start;

        for (int i = 0; i < WIND_SIM_TICKS; i++) {
            line.add(pos);
            pos = pos.method_1019(velocity);
        }

        return line;
    }

    /**
     * Ticks between the pearl state we're looking at and the server tick our wind charge first moves in.
     * We see the pearl a one-way trip late and the wind charge reaches the server a one-way trip late,
     * so that's a full round trip, plus the tick we wait before throwing.
     */
    private int leadTicks() {
        int ping = 0;
        if (this.mc.method_1562() != null && this.mc.field_1724 != null) {
            class_640 entry = this.mc.method_1562().method_2871(this.mc.field_1724.method_5667());
            if (entry != null) {
                ping = entry.method_2959();
            }
        }

        return 1 + Math.round(ping / 50.0F);
    }

    private class_1684 findOurPearl() {
        class_1684 newest = null;

        for (class_1297 entity : this.mc.field_1687.method_18112()) {
            if (entity instanceof class_1684 pearl && pearl.method_5805()) {
                if (this.trackedPearl != null) {
                    if (pearl.method_5667().equals(this.trackedPearl)) {
                        return pearl;
                    }
                } else if (pearl.method_24921() == this.mc.field_1724
                    && !this.ignoredPearls.contains(pearl.method_5667())
                    && (newest == null || pearl.field_6012 < newest.field_6012)) {
                    newest = pearl;
                }
            }
        }

        return newest;
    }

    private float power(float itemPower) {
        return itemPower > 0.0F ? itemPower : FALLBACK_POWER;
    }

    private boolean isInterceptPossible() {
        class_243 windStartPos = this.mc.field_1724.method_33571();
        class_243 currentPlayerVel = this.mc.field_1724.method_18798();
        class_243 inheritedVelocity = new class_243(
            currentPlayerVel.field_1352, this.mc.field_1724.method_24828() ? 0.0 : currentPlayerVel.field_1351, currentPlayerVel.field_1350
        );
        double windPower = this.power(class_9239.field_55047);

        for (int pearlTick = 5; pearlTick < Math.min(this.predictedPearlPath.size(), 50); pearlTick++) {
            class_243 pearlPos = this.predictedPearlPath.get(pearlTick);
            class_243 aimDir = pearlPos.method_1020(windStartPos).method_1029();
            List<class_243> windPath = this.windLine(windStartPos, aimDir.method_1021(windPower).method_1019(inheritedVelocity));

            for (int w = 0; w < Math.min(windPath.size(), pearlTick - 2); w++) {
                int pearlIdx = w + 2;
                if (pearlIdx >= this.predictedPearlPath.size()) {
                    break;
                }

                if (windPath.get(w).method_1022(this.predictedPearlPath.get(pearlIdx)) < 3.0) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * The path is anchored at the pearl's current state, so the wind charge's w-th tick lines up with
     * path index lead + w.
     */
    private PearlCatch.SolverResult solveInterceptWithPrediction(int lead, class_243 predictedEyePos, class_243 predictedVel) {
        class_243 windStartPos = predictedEyePos;
        class_243 inheritedVelocity = new class_243(
            predictedVel.field_1352, this.mc.field_1724.method_24828() ? 0.0 : predictedVel.field_1351, predictedVel.field_1350
        );
        double windPower = this.power(class_9239.field_55047);
        PearlCatch.SolverResult bestResult = null;
        double bestDistance = Double.MAX_VALUE;
        int searchStart = lead + 1;
        int searchEnd = Math.min(this.predictedPearlPath.size(), lead + 50);

        for (int targetPearlTick = searchStart; targetPearlTick < searchEnd; targetPearlTick++) {
            class_243 targetPearlPos = this.predictedPearlPath.get(targetPearlTick);
            class_243 baseAim = targetPearlPos.method_1020(windStartPos).method_1029();

            for (double yawOff = -0.3; yawOff <= 0.3; yawOff += 0.05) {
                for (double pitchOff = -0.3; pitchOff <= 0.3; pitchOff += 0.05) {
                    class_243 testAim = this.rotateVector(baseAim, pitchOff, yawOff);
                    List<class_243> windPath = this.windLine(windStartPos, testAim.method_1021(windPower).method_1019(inheritedVelocity));

                    for (int w = 1; w < windPath.size(); w++) {
                        int pearlIdx = lead + w;
                        if (pearlIdx >= this.predictedPearlPath.size()) {
                            break;
                        }

                        double dist = windPath.get(w).method_1022(this.predictedPearlPath.get(pearlIdx));
                        if (dist < bestDistance && dist < 1.5) {
                            bestDistance = dist;
                            class_243 aimPoint = windStartPos.method_1019(testAim.method_1021(100.0));
                            bestResult = new PearlCatch.SolverResult(this.predictedPearlPath.get(pearlIdx), aimPoint, pearlIdx);
                        }
                    }
                }
            }
        }

        return bestResult != null && bestDistance < 1.2 ? this.refineAim(windStartPos, inheritedVelocity, windPower, bestResult, lead) : bestResult;
    }

    private PearlCatch.SolverResult refineAim(
        class_243 windStartPos, class_243 inheritedVelocity, double windPower, PearlCatch.SolverResult coarse, int lead
    ) {
        class_243 coarseAim = coarse.aimDirection.method_1020(windStartPos).method_1029();
        PearlCatch.SolverResult bestResult = coarse;
        double bestDistance = Double.MAX_VALUE;

        for (double yawOff = -0.04; yawOff <= 0.04; yawOff += 0.01) {
            for (double pitchOff = -0.04; pitchOff <= 0.04; pitchOff += 0.01) {
                class_243 testAim = this.rotateVector(coarseAim, pitchOff, yawOff);
                List<class_243> windPath = this.windLine(windStartPos, testAim.method_1021(windPower).method_1019(inheritedVelocity));

                for (int w = 1; w < windPath.size(); w++) {
                    int pearlIdx = lead + w;
                    if (pearlIdx >= this.predictedPearlPath.size()) {
                        break;
                    }

                    double dist = windPath.get(w).method_1022(this.predictedPearlPath.get(pearlIdx));
                    if (dist < bestDistance) {
                        bestDistance = dist;
                        class_243 aimPoint = windStartPos.method_1019(testAim.method_1021(100.0));
                        bestResult = new PearlCatch.SolverResult(this.predictedPearlPath.get(pearlIdx), aimPoint, pearlIdx);
                    }
                }
            }
        }

        return bestResult;
    }

    private class_243 rotateVector(class_243 vec, double pitchOffset, double yawOffset) {
        double x = vec.field_1352;
        double y = vec.field_1351;
        double z = vec.field_1350;
        double cosYaw = Math.cos(yawOffset);
        double sinYaw = Math.sin(yawOffset);
        double newX = x * cosYaw - z * sinYaw;
        double newZ = x * sinYaw + z * cosYaw;
        double cosPitch = Math.cos(pitchOffset);
        double sinPitch = Math.sin(pitchOffset);
        double newY = y * cosPitch - newZ * sinPitch;
        newZ = y * sinPitch + newZ * cosPitch;
        return new class_243(newX, newY, newZ).method_1029();
    }

    private int findItemSlot(class_1792 item) {
        for (int i = 0; i < 9; i++) {
            if (this.mc.field_1724.method_31548().method_5438(i).method_7909() == item) {
                return i;
            }
        }

        return -1;
    }

    private boolean hasItem(class_1792 item) {
        return this.findItemSlot(item) != -1;
    }

    @Override
    public void onWorldRender(class_4587 matrices, float tickDelta) {
        if (this.showTrajectory.isEnabled() && !this.predictedPearlPath.isEmpty() && this.mc.field_1724 != null) {
            class_4184 camera = this.mc.field_1773.method_19418();
            class_243 cameraPos = camera.method_71156();
            class_9799 allocator = new class_9799(2048);

            try {
                class_4598 vertexConsumers = class_4597.method_22991(allocator);
                matrices.method_22903();
                matrices.method_22904(-cameraPos.field_1352, -cameraPos.field_1351, -cameraPos.field_1350);

                for (int i = 0; i < this.predictedPearlPath.size() - 1; i++) {
                    class_243 pos = this.predictedPearlPath.get(i);
                    class_243 nextPos = this.predictedPearlPath.get(i + 1);
                    float progress = (float)i / this.predictedPearlPath.size();
                    int r = 0;
                    int g = (int)(255.0F - progress * 100.0F);
                    int b = 255;
                    RenderUtils.drawLine(matrices, vertexConsumers, pos, nextPos, new Color(r, g, b), 0.78431374F);
                }

                if (this.pathLanded) {
                    class_243 land = this.predictedPearlPath.get(this.predictedPearlPath.size() - 1);
                    double size = 0.15;
                    class_238 landBox = new class_238(
                        land.field_1352 - size, land.field_1351 - size, land.field_1350 - size, land.field_1352 + size, land.field_1351 + size, land.field_1350 + size
                    );
                    RenderUtils.drawBox(matrices, vertexConsumers, landBox, new Color(255, 0, 255), 0.7058824F, true);
                }

                if (this.renderIntercept != null) {
                    double size = 0.3;
                    class_238 interceptBox = new class_238(
                        this.renderIntercept.field_1352 - size,
                        this.renderIntercept.field_1351 - size,
                        this.renderIntercept.field_1350 - size,
                        this.renderIntercept.field_1352 + size,
                        this.renderIntercept.field_1351 + size,
                        this.renderIntercept.field_1350 + size
                    );
                    RenderUtils.drawBox(matrices, vertexConsumers, interceptBox, new Color(0, 255, 0), 0.7058824F, true);
                    class_243 playerPos = this.mc.field_1724.method_33571();
                    RenderUtils.drawLine(matrices, vertexConsumers, playerPos, this.renderIntercept, new Color(255, 255, 0), 0.5882353F);
                }

                if (this.renderAim != null) {
                    class_243 playerPos = this.mc.field_1724.method_33571();
                    class_243 aimDir = this.renderAim.method_1020(playerPos).method_1029().method_1021(50.0);
                    class_243 aimEnd = playerPos.method_1019(aimDir);
                    RenderUtils.drawLine(matrices, vertexConsumers, playerPos, aimEnd, new Color(255, 0, 0), 0.78431374F);
                }

                matrices.method_22909();
                vertexConsumers.method_22993();
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

    @Environment(EnvType.CLIENT)
    private record SolverResult(class_243 interceptPos, class_243 aimDirection, int ticksFromNow) {
    }

    @Environment(EnvType.CLIENT)
    private static enum State {
        IDLE,
        THROWING_PEARL,
        NORMAL_THROW,
        TRACKING,
        EXECUTING_CATCH,
        RESETTING;
    }
}
