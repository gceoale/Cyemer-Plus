package com.slither.cyemer.module.implementation.render;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1937;
import net.minecraft.class_1959;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_2794;
import net.minecraft.class_2818;
import net.minecraft.class_2826;
import net.minecraft.class_2902;
import net.minecraft.class_2919;
import net.minecraft.class_2975;
import net.minecraft.class_3122;
import net.minecraft.class_3124;
import net.minecraft.class_3532;
import net.minecraft.class_5317;
import net.minecraft.class_5321;
import net.minecraft.class_5363;
import net.minecraft.class_5450;
import net.minecraft.class_5868;
import net.minecraft.class_5875;
import net.minecraft.class_6017;
import net.minecraft.class_6122;
import net.minecraft.class_638;
import net.minecraft.class_6677;
import net.minecraft.class_6792;
import net.minecraft.class_6793;
import net.minecraft.class_6795;
import net.minecraft.class_6796;
import net.minecraft.class_6797;
import net.minecraft.class_6799;
import net.minecraft.class_6880;
import net.minecraft.class_6885;
import net.minecraft.class_7145;
import net.minecraft.class_7225;
import net.minecraft.class_7510;
import net.minecraft.class_7887;
import net.minecraft.class_7924;

/**
 * Works out where ore generated from the world seed, the way the server's
 * world generator did, so ore hidden by anti-xray can still be found.
 *
 * The feature data (which ores exist, their counts, heights, vein sizes and
 * the index each one is seeded by) is not sent to clients, so it is built
 * from the vanilla worldgen registries bundled in the game itself. The
 * placement code is a straight port of OreFeature and ScatteredOreFeature,
 * checked against 1.21.11 bytecode, with every random call in vanilla's order:
 * ChunkRandom population seed per chunk, decorator seed per feature, then the
 * placement modifiers depth-first and the vein interleaved exactly as the
 * placed-feature stream runs them.
 *
 * Where vanilla reads the world, this reads what the client sees, and an
 * overlay of ore it already placed in the same chunk. Anti-xray shows stone
 * where ore is, which is what the generator saw. What breaks it locally is
 * anything changed since generation that sits on a vein (a mined tunnel, a
 * dripstone or lush cave, sculk): for ores with a discard chance between 0
 * and 1 (diamonds) one wrong read shifts the rest of that ore in that chunk.
 * Ancient debris never draws randomness from its surroundings, so it is the
 * most reliable.
 */
@Environment(EnvType.CLIENT)
final class OreSim {
    /** One ore feature as the generator runs it. */
    static final class Spec {
        final int step;
        final int index;
        final List<class_6797> modifiers;
        final class_3124 config;
        final boolean scattered;
        final Set<class_2248> produces = new HashSet<>();

        Spec(int step, int index, List<class_6797> modifiers, class_3124 config, boolean scattered) {
            this.step = step;
            this.index = index;
            this.modifiers = modifiers;
            this.config = config;
            this.scattered = scattered;
            for (class_3124.class_5876 target : config.field_29063) {
                this.produces.add(target.field_29069.method_26204());
            }
        }

        /** Whether a block the client sees now could be this ore's host (or the ore itself). */
        boolean couldHold(class_2680 state) {
            if (state.method_26215()) {
                return false;
            }
            if (this.produces.contains(state.method_26204())) {
                return true;
            }
            for (class_3124.class_5876 target : this.config.field_29063) {
                if (target.field_29068.method_16768(state, DUMMY_RANDOM)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Ore features of one dimension. */
    static final class Dimension {
        final class_2794 generator;
        final List<Spec> specs;
        final Map<class_5321<class_1959>, Set<Spec>> allowed;
        final Set<class_2248> ores;

        Dimension(class_2794 generator, List<Spec> specs, Map<class_5321<class_1959>, Set<Spec>> allowed) {
            this.generator = generator;
            this.specs = specs;
            this.allowed = allowed;
            Set<class_2248> all = new HashSet<>();
            for (Spec spec : specs) {
                all.addAll(spec.produces);
            }
            this.ores = all;
        }
    }

    private static final class_6677 DUMMY_RANDOM = new class_6677(0L);
    private static final Field COUNT = field(class_6793.class, class_6017.class);
    private static final Field HEIGHT = field(class_6795.class, class_6122.class);
    private static final Field CHANCE = field(class_6799.class, int.class);

    private static volatile CompletableFuture<Map<class_5321<class_1937>, Dimension>> loading;
    private static volatile String error;

    private OreSim() {
    }

    /** Null until the worldgen data has loaded in the background (a second or two, once). */
    static Dimension get(class_5321<class_1937> world) {
        CompletableFuture<Map<class_5321<class_1937>, Dimension>> future = loading;
        if (future == null) {
            synchronized (OreSim.class) {
                if (loading == null) {
                    loading = CompletableFuture.supplyAsync(OreSim::load);
                }
                future = loading;
            }
        }
        if (!future.isDone()) {
            return null;
        }
        try {
            return future.join().get(world);
        } catch (Throwable t) {
            error = String.valueOf(t.getCause() != null ? t.getCause() : t);
            return null;
        }
    }

    static boolean isLoading() {
        CompletableFuture<?> future = loading;
        return future == null || !future.isDone();
    }

    static String error() {
        return error;
    }

    private static Map<class_5321<class_1937>, Dimension> load() {
        class_7225.class_7874 lookup = class_7887.method_46817();
        class_7145 preset = lookup.method_46762(class_7924.field_41250).method_46747(class_5317.field_25050).comp_349();
        Map<class_5321<class_5363>, class_5363> options = preset.method_45546().comp_1014();
        Map<class_5321<class_1937>, Dimension> out = new HashMap<>();
        build(out, class_1937.field_25179, options.get(class_5363.field_25412));
        build(out, class_1937.field_25180, options.get(class_5363.field_25413));
        build(out, class_1937.field_25181, options.get(class_5363.field_25414));
        return out;
    }

    private static void build(Map<class_5321<class_1937>, Dimension> out, class_5321<class_1937> key, class_5363 option) {
        if (option == null) {
            return;
        }
        class_2794 generator = option.comp_1013();
        // Same biome list, same indexer, same order as ChunkGenerator, so the
        // indices match the ones the server seeded each feature with.
        List<class_6880<class_1959>> biomes = List.copyOf(generator.method_12098().method_28443());
        List<class_7510.class_6827> indexed = class_7510.method_44210(biomes, b -> b.comp_349().method_30970().method_30983(), true);

        Map<class_6796, Spec> byPlaced = new IdentityHashMap<>();
        List<Spec> specs = new ArrayList<>();
        for (int step = 0; step < indexed.size(); step++) {
            class_7510.class_6827 features = indexed.get(step);
            for (class_6796 placed : features.comp_303()) {
                class_2975<?, ?> configured = placed.comp_334().comp_349();
                Object feature = configured.comp_332();
                boolean scattered = feature instanceof class_5875;
                if (!(feature instanceof class_3122) && !scattered) {
                    continue;
                }
                if (!(configured.comp_333() instanceof class_3124 config) || !supported(placed.comp_335())) {
                    continue;
                }
                Spec spec = new Spec(step, features.comp_304().applyAsInt(placed), placed.comp_335(), config, scattered);
                specs.add(spec);
                byPlaced.put(placed, spec);
            }
        }
        specs.sort(Comparator.<Spec>comparingInt(s -> s.step).thenComparingInt(s -> s.index));

        Map<class_5321<class_1959>, Set<Spec>> allowed = new HashMap<>();
        for (class_6880<class_1959> biome : biomes) {
            Optional<class_5321<class_1959>> biomeKey = biome.method_40230();
            if (biomeKey.isEmpty()) {
                continue;
            }
            Set<Spec> set = new HashSet<>();
            for (class_6885<class_6796> list : biome.comp_349().method_30970().method_30983()) {
                list.method_40239().forEach(entry -> {
                    Spec spec = byPlaced.get(entry.comp_349());
                    if (spec != null) {
                        set.add(spec);
                    }
                });
            }
            allowed.put(biomeKey.get(), set);
        }
        out.put(key, new Dimension(generator, specs, allowed));
    }

    /** Only the modifiers ore features use; anything else and the feature is skipped, not guessed. */
    private static boolean supported(List<class_6797> modifiers) {
        for (class_6797 m : modifiers) {
            if (!(m instanceof class_6793 || m instanceof class_5450 || m instanceof class_6795
                    || m instanceof class_6799 || m instanceof class_6792)) {
                return false;
            }
        }
        return true;
    }

    private static Field field(Class<?> owner, Class<?> type) {
        for (Field f : owner.getDeclaredFields()) {
            if (f.getType() == type || (!type.isPrimitive() && type.isAssignableFrom(f.getType()))) {
                f.setAccessible(true);
                return f;
            }
        }
        throw new IllegalStateException("no " + type.getSimpleName() + " field in " + owner.getName());
    }

    /**
     * Ore the generator placed while decorating chunk (cx, cz), for the
     * picked blocks only, as position -> the feature that placed it.
     */
    static Long2ObjectOpenHashMap<Spec> simulate(class_638 world, Dimension dim, long seed, int cx, int cz, Set<class_2248> wanted) {
        Long2ObjectOpenHashMap<Spec> out = new Long2ObjectOpenHashMap<>();
        // Features run when any biome in the surrounding 3x3 chunks has them,
        // read straight from the section biome storage as vanilla does.
        Set<class_5321<class_1959>> biomes = new HashSet<>();
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                if (!world.method_8393(cx + ox, cz + oz)) {
                    continue;
                }
                class_2818 chunk = world.method_8497(cx + ox, cz + oz);
                for (class_2826 section : chunk.method_12006()) {
                    if (section != null) {
                        section.method_38294().method_39793(entry -> entry.method_40230().ifPresent(biomes::add));
                    }
                }
            }
        }
        Set<Spec> present = new HashSet<>();
        for (class_5321<class_1959> biome : biomes) {
            Set<Spec> set = dim.allowed.get(biome);
            if (set != null) {
                present.addAll(set);
            }
        }

        Run run = new Run(world, dim, out, wanted);
        long population = run.random.method_12661(seed, cx << 4, cz << 4);
        int originY = world.method_31607();
        // All ore features in generation order, not just the picked ones:
        // earlier ore occupies blocks a later vein would otherwise test as stone.
        for (Spec spec : dim.specs) {
            if (!present.contains(spec)) {
                continue;
            }
            run.random.method_12664(population, spec.index, spec.step);
            run.spec = spec;
            try {
                run.place(0, cx << 4, originY, cz << 4);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        return out;
    }

    private static final class Run {
        final class_638 world;
        final Dimension dim;
        final Long2ObjectOpenHashMap<Spec> out;
        final Set<class_2248> wanted;
        final class_2919 random = new class_2919(new class_6677(0L));
        final class_5868 heights;
        /** Ore this run already placed, which the world view does not have. */
        final Long2ObjectOpenHashMap<class_2680> overlay = new Long2ObjectOpenHashMap<>();
        final class_2338.class_2339 probe = new class_2338.class_2339();
        final class_2680 stone = class_2246.field_10340.method_9564();
        Spec spec;

        Run(class_638 world, Dimension dim, Long2ObjectOpenHashMap<Spec> out, Set<class_2248> wanted) {
            this.world = world;
            this.dim = dim;
            this.out = out;
            this.wanted = wanted;
            this.heights = new class_5868(dim.generator, world);
        }

        /** The placement modifiers, depth-first, with the vein generated at each leaf, as the feature stream does. */
        void place(int i, int x, int y, int z) throws ReflectiveOperationException {
            List<class_6797> mods = this.spec.modifiers;
            if (i == mods.size()) {
                if (this.spec.scattered) {
                    this.scattered(x, y, z);
                } else {
                    this.ore(x, y, z);
                }
                return;
            }
            class_6797 m = mods.get(i);
            if (m instanceof class_6793) {
                int n = ((class_6017) COUNT.get(m)).method_35008(this.random);
                for (int k = 0; k < n; k++) {
                    this.place(i + 1, x, y, z);
                }
            } else if (m instanceof class_5450) {
                int nx = this.random.method_43048(16) + x;
                int nz = this.random.method_43048(16) + z;
                this.place(i + 1, nx, y, nz);
            } else if (m instanceof class_6795) {
                int ny = ((class_6122) HEIGHT.get(m)).method_35391(this.random, this.heights);
                this.place(i + 1, x, ny, z);
            } else if (m instanceof class_6799) {
                if (this.random.method_43057() < 1.0F / (float) CHANCE.getInt(m)) {
                    this.place(i + 1, x, y, z);
                }
            } else if (m instanceof class_6792) {
                Optional<class_5321<class_1959>> biome = this.world.method_23753(this.probe.method_10103(x, y, z)).method_40230();
                Set<Spec> set = biome.isPresent() ? this.dim.allowed.get(biome.get()) : null;
                if (set != null && set.contains(this.spec)) {
                    this.place(i + 1, x, y, z);
                }
            }
        }

        /** OreFeature.generate. */
        void ore(int ox, int oy, int oz) {
            int size = this.spec.config.field_13723;
            float f = this.random.method_43057() * 3.1415927F;
            float g = (float) size / 8.0F;
            int i = class_3532.method_15386(((float) size / 16.0F * 2.0F + 1.0F) / 2.0F);
            double d = (double) ox + Math.sin((double) f) * (double) g;
            double e = (double) ox - Math.sin((double) f) * (double) g;
            double h = (double) oz + Math.cos((double) f) * (double) g;
            double j = (double) oz - Math.cos((double) f) * (double) g;
            double l = (double) (oy + this.random.method_43048(3) - 2);
            double m = (double) (oy + this.random.method_43048(3) - 2);
            int n = ox - class_3532.method_15386(g) - i;
            int o = oy - 2 - i;
            int p = oz - class_3532.method_15386(g) - i;
            int q = 2 * (class_3532.method_15386(g) + i);
            int r = 2 * (2 + i);
            for (int s = n; s <= n + q; s++) {
                for (int t = p; t <= p + q; t++) {
                    if (o <= this.surface(s, t)) {
                        this.veinPart(size, d, e, h, j, l, m, n, o, p, q, r);
                        return;
                    }
                }
            }
        }

        /** OreFeature.generateVeinPart. */
        void veinPart(int size, double startX, double endX, double startZ, double endZ, double startY, double endY,
                      int x, int y, int z, int horizontalSize, int verticalSize) {
            BitSet bitSet = new BitSet(horizontalSize * verticalSize * horizontalSize);
            double[] ds = new double[size * 4];
            for (int k = 0; k < size; k++) {
                float f = (float) k / (float) size;
                double px = class_3532.method_16436((double) f, startX, endX);
                double py = class_3532.method_16436((double) f, startY, endY);
                double pz = class_3532.method_16436((double) f, startZ, endZ);
                double s = this.random.method_43058() * (double) size / 16.0;
                double radius = ((double) (class_3532.method_15374((double) (3.1415927F * f)) + 1.0F) * s + 1.0) / 2.0;
                ds[k * 4] = px;
                ds[k * 4 + 1] = py;
                ds[k * 4 + 2] = pz;
                ds[k * 4 + 3] = radius;
            }
            for (int k = 0; k < size - 1; k++) {
                if (!(ds[k * 4 + 3] <= 0.0)) {
                    for (int o = k + 1; o < size; o++) {
                        if (!(ds[o * 4 + 3] <= 0.0)) {
                            double dx = ds[k * 4] - ds[o * 4];
                            double dy = ds[k * 4 + 1] - ds[o * 4 + 1];
                            double dz = ds[k * 4 + 2] - ds[o * 4 + 2];
                            double dr = ds[k * 4 + 3] - ds[o * 4 + 3];
                            if (dr * dr > dx * dx + dy * dy + dz * dz) {
                                if (dr > 0.0) {
                                    ds[o * 4 + 3] = -1.0;
                                } else {
                                    ds[k * 4 + 3] = -1.0;
                                }
                            }
                        }
                    }
                }
            }
            for (int k = 0; k < size; k++) {
                double u = ds[k * 4 + 3];
                if (u < 0.0) {
                    continue;
                }
                double v = ds[k * 4];
                double w = ds[k * 4 + 1];
                double aa = ds[k * 4 + 2];
                int ab = Math.max(class_3532.method_15357(v - u), x);
                int ac = Math.max(class_3532.method_15357(w - u), y);
                int ad = Math.max(class_3532.method_15357(aa - u), z);
                int ae = Math.max(class_3532.method_15357(v + u), ab);
                int af = Math.max(class_3532.method_15357(w + u), ac);
                int ag = Math.max(class_3532.method_15357(aa + u), ad);
                for (int ah = ab; ah <= ae; ah++) {
                    double ai = ((double) ah + 0.5 - v) / u;
                    if (!(ai * ai < 1.0)) {
                        continue;
                    }
                    for (int aj = ac; aj <= af; aj++) {
                        double ak = ((double) aj + 0.5 - w) / u;
                        if (!(ai * ai + ak * ak < 1.0)) {
                            continue;
                        }
                        for (int al = ad; al <= ag; al++) {
                            double am = ((double) al + 0.5 - aa) / u;
                            if (!(ai * ai + ak * ak + am * am < 1.0) || this.world.method_31601(aj)) {
                                continue;
                            }
                            int an = ah - x + (aj - y) * horizontalSize + (al - z) * horizontalSize * verticalSize;
                            if (bitSet.get(an)) {
                                continue;
                            }
                            bitSet.set(an);
                            this.tryPlace(ah, aj, al);
                        }
                    }
                }
            }
        }

        /** ScatteredOreFeature.generate. */
        void scattered(int ox, int oy, int oz) {
            int count = this.random.method_43048(this.spec.config.field_13723 + 1);
            for (int j = 0; j < count; j++) {
                int spread = Math.min(j, 7);
                int dx = this.spread(spread);
                int dy = this.spread(spread);
                int dz = this.spread(spread);
                this.tryPlace(ox + dx, oy + dy, oz + dz);
            }
        }

        int spread(int spread) {
            return Math.round((this.random.method_43057() - this.random.method_43057()) * (float) spread);
        }

        /** The target loop: first target whose rule accepts the block wins. */
        void tryPlace(int x, int y, int z) {
            class_2680 state = this.state(x, y, z);
            for (class_3124.class_5876 target : this.spec.config.field_29063) {
                if (this.shouldPlace(state, target, x, y, z)) {
                    long key = BlockESP.pack(x, y, z);
                    this.overlay.put(key, target.field_29069);
                    if (this.wanted.contains(target.field_29069.method_26204())) {
                        this.out.put(key, this.spec);
                    }
                    return;
                }
            }
        }

        /** OreFeature.shouldPlace with shouldNotDiscard inlined, random calls exactly as vanilla. */
        boolean shouldPlace(class_2680 state, class_3124.class_5876 target, int x, int y, int z) {
            if (!target.field_29068.method_16768(state, this.random)) {
                return false;
            }
            float chance = this.spec.config.field_29064;
            boolean keep = chance <= 0.0F || (chance < 1.0F && this.random.method_43057() >= chance);
            return keep || !this.exposed(x, y, z);
        }

        boolean exposed(int x, int y, int z) {
            return this.state(x + 1, y, z).method_26215() || this.state(x - 1, y, z).method_26215()
                    || this.state(x, y + 1, z).method_26215() || this.state(x, y - 1, z).method_26215()
                    || this.state(x, y, z + 1).method_26215() || this.state(x, y, z - 1).method_26215();
        }

        class_2680 state(int x, int y, int z) {
            class_2680 placed = this.overlay.get(BlockESP.pack(x, y, z));
            if (placed != null) {
                return placed;
            }
            if (!this.world.method_8393(x >> 4, z >> 4)) {
                return this.stone; // not sent to us: the common case underground
            }
            return this.world.method_8320(this.probe.method_10103(x, y, z));
        }

        /**
         * Stands in for the generator's OCEAN_FLOOR_WG height, which clients
         * never get. Only decides veins that start above the terrain.
         */
        int surface(int x, int z) {
            if (!this.world.method_8393(x >> 4, z >> 4)) {
                return Integer.MAX_VALUE;
            }
            return this.world.method_8624(class_2902.class_2903.field_13203, x, z);
        }
    }
}
