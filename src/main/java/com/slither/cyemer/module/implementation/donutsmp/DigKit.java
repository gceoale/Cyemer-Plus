package com.slither.cyemer.module.implementation.donutsmp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import com.slither.cyemer.util.RotationManager;
import net.fabricmc.api.Environment;
import net.minecraft.class_1661;
import net.minecraft.class_1747;
import net.minecraft.class_1799;
import net.minecraft.class_1887;
import net.minecraft.class_1890;
import net.minecraft.class_1893;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2346;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_315;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_638;
import net.minecraft.class_6880;
import net.minecraft.class_746;
import net.minecraft.class_7923;
import net.minecraft.class_7924;

/**
 * What SpotFinder's Auto Mine shares between digging its way to a room,
 * mining it out and sealing it: held keys, aiming, tools and timing.
 * Everything here acts like a player would, through the camera and the
 * vanilla key bindings, so vanilla sends every packet.
 */
@Environment(EnvType.CLIENT)
final class DigKit {
    static final int[][] SIDES = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
    /** After a block breaks vanilla waits 5 ticks before the next one starts; turning onto it costs about 3 more. */
    private static final int OVERHEAD_TICKS = 8;
    private static final int INSTANT_OVERHEAD_TICKS = 3;
    /** A tool with this few uses left is left alone rather than broken. */
    private static final int SPARE_DURABILITY = 5;
    private static final double[][] AIM_POINTS = {
        {0.5, 0.5, 0.5}, {0.5, 0.95, 0.5}, {0.5, 0.05, 0.5}, {0.05, 0.5, 0.5}, {0.95, 0.5, 0.5}, {0.5, 0.5, 0.05}, {0.5, 0.5, 0.95}
    };
    /** Plain blocks that are fine to wall a room up with. Never anything valuable. */
    private static final Set<String> FILLERS = Set.of(
        "cobbled_deepslate", "cobblestone", "deepslate", "stone", "tuff", "andesite", "diorite", "granite", "dirt",
        "coarse_dirt", "netherrack", "blackstone", "basalt", "smooth_basalt", "calcite", "end_stone", "mossy_cobblestone",
        "stone_bricks", "deepslate_bricks", "deepslate_tiles", "polished_deepslate", "cobbled_deepslate_bricks");

    private static class_1937 efficiencyWorld;
    private static class_6880<class_1887> efficiency;

    private DigKit() {
    }

    /** The keys Auto Mine presses. It remembers which it holds so it only ever lets go of its own. */
    static final class Keys {
        enum Key { ATTACK, FORWARD, JUMP, SNEAK, USE }

        private final boolean[] held = new boolean[Key.values().length];

        void set(Key key, boolean down) {
            int i = key.ordinal();
            if (this.held[i] == down) {
                return;
            }
            binding(key).method_23481(down);
            this.held[i] = down;
        }

        boolean any() {
            for (boolean h : this.held) {
                if (h) {
                    return true;
                }
            }
            return false;
        }

        void releaseMovement() {
            this.set(Key.FORWARD, false);
            this.set(Key.JUMP, false);
            this.set(Key.SNEAK, false);
        }

        void releaseAll() {
            for (Key key : Key.values()) {
                this.set(key, false);
            }
        }

        private static class_304 binding(Key key) {
            class_315 options = class_310.method_1551().field_1690;
            return switch (key) {
                case ATTACK -> options.field_1886;
                case FORWARD -> options.field_1894;
                case JUMP -> options.field_1903;
                case SNEAK -> options.field_1832;
                case USE -> options.field_1904;
            };
        }
    }

    /**
     * Turns the camera toward a point the way a hand on a mouse does: an
     * eased, frame-rate independent glide (RotationManager's FPS mode), not
     * a constant-speed sweep that stops dead. Turn Speed 22 is a quick,
     * smooth flick.
     */
    static void look(Object owner, class_243 point, double turnSpeed, boolean yawOnly) {
        RotationManager.setRotationSupplier(owner, RotationManager.Priority.NORMAL, () -> point, Math.max(1.5, turnSpeed * 0.35),
                RotationManager.RotationMode.FPS, 0.0, false, yawOnly);
    }

    /** Degrees between where the camera looks and the direction to a point. */
    static double angleTo(class_746 self, class_243 eye, class_243 point) {
        class_243 look = self.method_5828(1.0F);
        double dx = point.field_1352 - eye.field_1352;
        double dy = point.field_1351 - eye.field_1351;
        double dz = point.field_1350 - eye.field_1350;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-6) {
            return 0.0;
        }
        double dot = (look.field_1352 * dx + look.field_1351 * dy + look.field_1350 * dz) / len;
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
    }

    /** A block with at least one side open to air or a non-solid block, the only kind that can be seen. */
    static boolean exposed(class_638 world, class_2338.class_2339 probe, int x, int y, int z) {
        for (int[] o : SIDES) {
            if (!blocking(world, probe, x + o[0], y + o[1], z + o[2])) {
                return true;
            }
        }
        return false;
    }

    /** Something to break: not air or fluid, and not unbreakable. */
    static boolean minable(class_638 world, class_2338.class_2339 probe, int x, int y, int z) {
        class_2680 state = world.method_8320(probe.method_10103(x, y, z));
        return !state.method_26215() && state.method_26227().method_15769() && state.method_26214(world, probe) >= 0.0F;
    }

    /** Has a collision box, so it is in the way of walking. */
    static boolean blocking(class_638 world, class_2338.class_2339 probe, int x, int y, int z) {
        class_2680 state = world.method_8320(probe.method_10103(x, y, z));
        return !state.method_26220(world, probe).method_1110();
    }

    static boolean fluidAt(class_638 world, class_2338.class_2339 probe, int x, int y, int z) {
        return !world.method_8320(probe.method_10103(x, y, z)).method_26227().method_15769();
    }

    static boolean fluidBeside(class_638 world, class_2338.class_2339 probe, int x, int y, int z) {
        for (int[] o : SIDES) {
            if (fluidAt(world, probe, x + o[0], y + o[1], z + o[2])) {
                return true;
            }
        }
        return false;
    }

    static boolean fallingAt(class_638 world, class_2338.class_2339 probe, int x, int y, int z) {
        return world.method_8320(probe.method_10103(x, y, z)).method_26204() instanceof class_2346;
    }

    /** Solid full block, what a sealed room's walls are made of. */
    static boolean solid(class_638 world, class_2338.class_2339 probe, int x, int y, int z) {
        class_2680 state = world.method_8320(probe.method_10103(x, y, z));
        return !state.method_26215() && state.method_26227().method_15769() && state.method_26212(world, probe);
    }

    /**
     * A point on the block the eye can see within reach: its centre or the
     * middle of a face, checked with the same outline raycast the crosshair
     * uses, so aiming there really puts the crosshair on this block.
     */
    static class_243 visiblePoint(class_638 world, class_746 self, class_243 eye, int x, int y, int z, double reach) {
        double reach2 = reach * reach;
        for (double[] o : AIM_POINTS) {
            class_243 point = new class_243(x + o[0], y + o[1], z + o[2]);
            if (point.method_1025(eye) > reach2) {
                continue;
            }
            class_3965 hit = world.method_17742(new class_3959(eye, point, class_3959.class_3960.field_17559, class_3959.class_242.field_1348, self));
            if (hit != null && hit.method_17783() == class_239.class_240.field_1332) {
                class_2338 pos = hit.method_17777();
                if (pos.method_10263() == x && pos.method_10264() == y && pos.method_10260() == z) {
                    return point;
                }
            }
        }
        return null;
    }

    /** The crosshair is on this block right now. */
    static boolean crosshairOn(class_310 mc, int x, int y, int z) {
        if (!(mc.field_1765 instanceof class_3965 hit) || hit.method_17783() != class_239.class_240.field_1332) {
            return false;
        }
        class_2338 pos = hit.method_17777();
        return pos.method_10263() == x && pos.method_10264() == y && pos.method_10260() == z;
    }

    private static class_6880<class_1887> efficiency(class_310 mc) {
        if (efficiencyWorld != mc.field_1687 || efficiency == null) {
            efficiencyWorld = mc.field_1687;
            efficiency = mc.field_1687.method_30349().method_30530(class_7924.field_41265).method_46747(class_1893.field_9131);
        }
        return efficiency;
    }

    private static boolean nearlyBroken(class_1799 stack) {
        return stack.method_7963() && stack.method_7936() - stack.method_7919() <= SPARE_DURABILITY;
    }

    /** How fast a stack mines this block, Efficiency included; 0 for a tool about to break. */
    private static float speed(class_310 mc, class_1799 stack, class_2680 state) {
        if (stack.method_7960() || nearlyBroken(stack)) {
            return 0.0F;
        }
        float speed = stack.method_7924(state);
        if (speed > 1.0F) {
            int level = class_1890.method_8225(efficiency(mc), stack);
            if (level > 0) {
                speed += level * level + 1;
            }
        }
        return speed;
    }

    /** The hotbar slot that mines this block fastest, or -1 if nothing beats a bare hand. */
    private static int bestSlot(class_310 mc, class_2680 state) {
        class_1661 inventory = mc.field_1724.method_31548();
        int best = -1;
        float bestSpeed = 1.0F;
        boolean needsTool = state.method_29291();
        for (int i = 0; i < 9; i++) {
            class_1799 stack = inventory.method_5438(i);
            float speed = speed(mc, stack, state);
            if (needsTool && speed > 0.0F && !stack.method_7951(state)) {
                speed = Math.min(speed, 1.0F); // a wooden pick on obsidian: fast-ish, drops nothing
            }
            if (speed > bestSpeed) {
                bestSpeed = speed;
                best = i;
            }
        }
        return best;
    }

    static void selectBestTool(class_310 mc, class_2680 state) {
        class_1661 inventory = mc.field_1724.method_31548();
        int slot = bestSlot(mc, state);
        if (slot == -1 && nearlyBroken(inventory.method_5438(inventory.method_67532()))) {
            // Nothing beats a hand, and the held tool would break: hold something that cannot.
            for (int i = 0; i < 9; i++) {
                if (!inventory.method_5438(i).method_7963()) {
                    slot = i;
                    break;
                }
            }
        }
        if (slot != -1 && inventory.method_67532() != slot) {
            inventory.method_61496(slot);
        }
    }

    /** It needs no tool, or the hotbar has a working one that harvests it. */
    static boolean canHarvest(class_310 mc, class_2680 state) {
        if (!state.method_29291()) {
            return true;
        }
        class_1661 inventory = mc.field_1724.method_31548();
        for (int i = 0; i < 9; i++) {
            class_1799 stack = inventory.method_5438(i);
            if (!stack.method_7960() && !nearlyBroken(stack) && stack.method_7951(state)) {
                return true;
            }
        }
        return false;
    }

    /** Ticks to break this block with the best tool in the hotbar, the way vanilla counts them. */
    static int breakTicks(class_310 mc, class_638 world, class_2680 state, class_2338 pos) {
        float hardness = state.method_26214(world, pos);
        if (hardness < 0.0F) {
            return 1_000_000;
        }
        if (hardness == 0.0F) {
            return 1;
        }
        int slot = bestSlot(mc, state);
        class_1799 stack = slot >= 0 ? mc.field_1724.method_31548().method_5438(slot) : class_1799.field_8037;
        float speed = slot >= 0 ? speed(mc, stack, state) : 1.0F;
        boolean harvest = !state.method_29291() || (slot >= 0 && stack.method_7951(state));
        double delta = speed / hardness / (harvest ? 30.0 : 100.0);
        return delta >= 1.0 ? 1 : (int) Math.ceil(1.0 / delta);
    }

    /** Break times by block state, for adding up a lot of blocks at once. */
    static final class Clock {
        private final class_310 mc;
        private final class_638 world;
        private final Map<class_2680, Double> seconds = new HashMap<>();

        Clock(class_310 mc, class_638 world) {
            this.mc = mc;
            this.world = world;
        }

        /** Seconds to break the block, turning onto it and vanilla's pause after it included. */
        double seconds(class_2680 state, class_2338 pos) {
            Double cached = this.seconds.get(state);
            if (cached == null) {
                int ticks = breakTicks(this.mc, this.world, state, pos);
                cached = (ticks + (ticks <= 1 ? INSTANT_OVERHEAD_TICKS : OVERHEAD_TICKS)) / 20.0;
                this.seconds.put(state, cached);
            }
            return cached;
        }

        /** What a typical block at this height takes: deepslate below 0, stone above. */
        double typical(int y, class_2338 pos) {
            return this.seconds(y < 0 ? class_2246.field_28888.method_9564() : class_2246.field_10340.method_9564(), pos);
        }
    }

    /** A hotbar slot holding a plain block to seal a room with, or -1. */
    static int fillerSlot(class_310 mc) {
        class_1661 inventory = mc.field_1724.method_31548();
        int selected = inventory.method_67532();
        if (isFiller(inventory.method_5438(selected))) {
            return selected;
        }
        for (int i = 0; i < 9; i++) {
            if (isFiller(inventory.method_5438(i))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isFiller(class_1799 stack) {
        if (stack.method_7960() || !(stack.method_7909() instanceof class_1747 item)) {
            return false;
        }
        return FILLERS.contains(class_7923.field_41175.method_10221(item.method_7711()).method_12832());
    }

    static String time(double seconds) {
        long s = Math.max(0L, Math.round(seconds));
        if (s >= 3600L) {
            return (s / 3600L) + "h " + (s % 3600L) / 60L + "m";
        }
        if (s >= 60L) {
            return (s / 60L) + "m " + (s % 60L) + "s";
        }
        return s + "s";
    }
}
