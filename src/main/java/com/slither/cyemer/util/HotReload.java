package com.slither.cyemer.util;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.config.ConfigManager;
import com.slither.cyemer.event.EventBus;
import com.slither.cyemer.manager.ModuleManager;
import com.slither.cyemer.module.Module;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Loads a new build of Cyemer into the running game.
 *
 * A JVM cannot unload or (without an agent) change a class it has loaded,
 * so the build is split in two. The core is everything the mixins and the
 * entry points can reach: those classes are woven into Minecraft's own
 * code and stay exactly as they were. Everything else (the modules and what
 * only they use) is loaded again from the new jar by a class loader of its
 * own, one per reload, which asks the game's loader for the core and serves
 * the rest itself. New module instances take over from the old ones with
 * the settings, keybinds and on/off state they had.
 *
 * Classes from two loaders share a package name but not package access, so
 * the new code is checked first for any package-private reach into the
 * core; a build that has one is refused rather than left to fail mid-game.
 *
 * If a core class changed (a mixin, a manager, the GUI, a setting type) the
 * reload is refused: only a restart can apply that. The core is found from
 * the bytecode itself, so nothing here needs updating as the client grows.
 */
@Environment(EnvType.CLIENT)
public final class HotReload {
    private static final String ROOT = "com/slither/cyemer/";
    private static final Pattern REF = Pattern.compile("com/slither/cyemer/[A-Za-z0-9_/$]+");
    /** Kept even though the core never names them: they register Fabric callbacks or threads that cannot be taken back. */
    private static final Set<String> KEEP = Set.of(
        ROOT + "module/implementation/Fakelag",
        ROOT + "module/implementation/Blink",
        ROOT + "module/implementation/Interface",
        ROOT + "module/implementation/SelfDestruct");

    private static Path jar;
    private static final Map<String, Long> startCrc = new HashMap<>();
    private static long loadedTime;
    private static long loadedSize;
    private static int generation;
    private static volatile boolean requested;

    private HotReload() {
    }

    /** At startup, once the modules exist: remembers the jar and what the running classes are. */
    public static void snapshot() {
        try {
            jar = locateJar();
            if (jar == null || !Files.isRegularFile(jar)) {
                jar = null;
                return; // a dev run from class folders: nothing to reload from
            }
            try (ZipFile zip = new ZipFile(jar.toFile())) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    if (isOurClass(e.getName())) {
                        startCrc.put(internalName(e.getName()), e.getCrc());
                    }
                }
            }
            loadedTime = Files.getLastModifiedTime(jar).toMillis();
            loadedSize = Files.size(jar);
        } catch (Exception e) {
            jar = null;
        }
    }

    private static Path locateJar() {
        try {
            ModContainer container = FabricLoader.getInstance().getModContainer(Cyemer.MOD_ID).orElse(null);
            if (container != null && container.getOrigin().getKind() == net.fabricmc.loader.api.metadata.ModOrigin.Kind.PATH) {
                for (Path p : container.getOrigin().getPaths()) {
                    if (p.toString().endsWith(".jar")) {
                        return p;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        try {
            return Paths.get(Cyemer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean available() {
        return jar != null && !Cyemer.selfDestructed;
    }

    /** The installed jar is a different build from the one running, and has finished being written. */
    public static boolean newBuildReady() {
        if (!available()) {
            return false;
        }
        try {
            long time = Files.getLastModifiedTime(jar).toMillis();
            long size = Files.size(jar);
            return (time != loadedTime || size != loadedSize) && System.currentTimeMillis() - time > 1500L;
        } catch (Exception e) {
            return false;
        }
    }

    /** Reload at the start of the next tick, outside any module code. */
    public static void request() {
        requested = true;
    }

    /** From MinecraftClientMixin, before the modules tick. */
    public static void tick() {
        if (!requested) {
            return;
        }
        requested = false;
        say(reload());
    }

    private static String reload() {
        if (!available()) {
            return "§cHotReload needs Cyemer running from its jar";
        }
        try {
            Map<String, byte[]> classes = new HashMap<>();
            Map<String, Long> crcs = new HashMap<>();
            long time = Files.getLastModifiedTime(jar).toMillis();
            long size = Files.size(jar);
            try (ZipFile zip = new ZipFile(jar.toFile())) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    if (isOurClass(e.getName())) {
                        try (InputStream in = zip.getInputStream(e)) {
                            classes.put(internalName(e.getName()), in.readAllBytes());
                        }
                        crcs.put(internalName(e.getName()), e.getCrc());
                    }
                }
            }
            loadedTime = time; // whatever happens, do not retry this same build on every check
            loadedSize = size;

            Set<String> core = core(classes);
            List<String> changed = new ArrayList<>();
            for (String n : core) {
                if (!Objects.equals(crcs.get(n), startCrc.get(n))) {
                    changed.add(n);
                }
            }
            if (!changed.isEmpty()) {
                return "§eThis build changed core code, so it needs a game restart:§r §7" + shortNames(changed);
            }
            String registry = ModuleManager.REGISTRY.replace('.', '/');
            if (core.contains(registry) || !classes.containsKey(registry)) {
                return "§cHotReload can't find a reloadable module list in this build";
            }

            String problem = crossLoaderProblem(classes, core);
            if (problem != null) {
                return "§eThis build can't be hot reloaded (" + problem + "), restart the game for it";
            }
            int gen = generation + 1;
            Generation loader = new Generation(gen, Cyemer.class.getClassLoader(), classes, core);
            loader.linkAll(); // load and verify every class now, so a broken build fails here and not mid-game
            generation = gen;
            int count = swap(loader, core);
            return "§aReloaded§r §7" + count + " modules from the new build (reload #" + gen + ")";
        } catch (Throwable t) {
            return "§cReload failed§r §7(" + t + "), carrying on with the build already running";
        }
    }

    /**
     * One reload's classes. The core comes from the game's loader as always;
     * every other Cyemer class is defined here from the new jar's bytes,
     * on first use, so classes that need each other while being verified
     * simply load each other.
     */
    static final class Generation extends ClassLoader {
        private final Map<String, byte[]> bytes = new HashMap<>();

        Generation(int number, ClassLoader parent, Map<String, byte[]> classes, Set<String> core) {
            super("cyemer-reload-" + number, parent);
            for (Map.Entry<String, byte[]> e : classes.entrySet()) {
                if (!core.contains(e.getKey())) {
                    this.bytes.put(e.getKey().replace('/', '.'), e.getValue());
                }
            }
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!this.bytes.containsKey(name)) {
                return super.loadClass(name, resolve); // the core, Minecraft, libraries: the game's own
            }
            synchronized (this.getClassLoadingLock(name)) {
                Class<?> c = this.findLoadedClass(name);
                if (c == null) {
                    byte[] b = this.bytes.get(name);
                    c = this.defineClass(name, b, 0, b.length, Cyemer.class.getProtectionDomain());
                }
                if (resolve) {
                    this.resolveClass(c);
                }
                return c;
            }
        }

        void linkAll() throws ClassNotFoundException {
            for (String name : new ArrayList<>(this.bytes.keySet())) {
                Class<?> c = this.loadClass(name, false);
                c.getDeclaredMethods(); // links it: verification runs, and with it any missing class shows up
                c.getDeclaredFields();
            }
        }
    }

    /**
     * Code in the new loader cannot use package-private members, or
     * non-public classes, of core classes in the same package, nor
     * override their package-private methods. Returns what breaks that
     * rule, or null.
     */
    static String crossLoaderProblem(Map<String, byte[]> classes, Set<String> core) {
        Map<String, ClassNode> coreNodes = new HashMap<>();
        for (String n : core) {
            ClassNode node = new ClassNode();
            new ClassReader(classes.get(n)).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            coreNodes.put(n, node);
        }
        for (Map.Entry<String, byte[]> e : classes.entrySet()) {
            String name = e.getKey();
            if (core.contains(name)) {
                continue;
            }
            ClassNode node = new ClassNode();
            new ClassReader(e.getValue()).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            String pkg = packageOf(name);
            // Types it names
            List<String> types = new ArrayList<>();
            if (node.superName != null) {
                types.add(node.superName);
            }
            types.addAll(node.interfaces);
            for (MethodNode m : node.methods) {
                for (AbstractInsnNode insn : m.instructions) {
                    if (insn instanceof TypeInsnNode t) {
                        types.add(Type.getObjectType(t.desc).getSort() == Type.ARRAY ? Type.getObjectType(t.desc).getElementType().getInternalName() : t.desc);
                    } else if (insn instanceof FieldInsnNode f) {
                        String why = memberProblem(coreNodes, node, pkg, f.owner, f.name, f.desc, false);
                        if (why != null) {
                            return shortName(name) + " uses " + why;
                        }
                    } else if (insn instanceof MethodInsnNode mi) {
                        String why = memberProblem(coreNodes, node, pkg, mi.owner, mi.name, mi.desc, true);
                        if (why != null) {
                            return shortName(name) + " uses " + why;
                        }
                    }
                }
            }
            for (String t : types) {
                ClassNode target = coreNodes.get(t);
                if (target != null && packageOf(t).equals(pkg) && (target.access & Opcodes.ACC_PUBLIC) == 0) {
                    return shortName(name) + " uses the non-public class " + shortName(t);
                }
            }
            // Overriding a package-private method of a core superclass does not work across loaders.
            for (String sup = node.superName; sup != null && coreNodes.containsKey(sup); sup = coreNodes.get(sup).superName) {
                ClassNode parent = coreNodes.get(sup);
                if (!packageOf(sup).equals(pkg)) {
                    continue;
                }
                for (MethodNode pm : parent.methods) {
                    if (isPackagePrivate(pm.access) && !pm.name.startsWith("<")) {
                        for (MethodNode m : node.methods) {
                            if (m.name.equals(pm.name) && m.desc.equals(pm.desc)) {
                                return shortName(name) + " overrides package-private " + shortName(sup) + "." + pm.name;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static String memberProblem(Map<String, ClassNode> coreNodes, ClassNode from, String pkg, String owner, String name, String desc, boolean method) {
        for (String c = owner; c != null && coreNodes.containsKey(c); c = coreNodes.get(c).superName) {
            ClassNode cls = coreNodes.get(c);
            int access = -1;
            if (method) {
                for (MethodNode m : cls.methods) {
                    if (m.name.equals(name) && m.desc.equals(desc)) {
                        access = m.access;
                    }
                }
            } else {
                for (FieldNode f : cls.fields) {
                    if (f.name.equals(name) && f.desc.equals(desc)) {
                        access = f.access;
                    }
                }
            }
            if (access == -1) {
                continue; // inherited from further up
            }
            if (!packageOf(c).equals(pkg)) {
                return null; // another package: the usual rules, same as before
            }
            boolean subclass = isSubclass(coreNodes, from, c);
            if (isPackagePrivate(access) || ((access & Opcodes.ACC_PROTECTED) != 0 && !subclass)) {
                return "package-private " + shortName(c) + "." + name;
            }
            return null;
        }
        return null;
    }

    private static boolean isSubclass(Map<String, ClassNode> coreNodes, ClassNode from, String target) {
        for (String c = from.superName; c != null; c = coreNodes.containsKey(c) ? coreNodes.get(c).superName : null) {
            if (c.equals(target)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPackagePrivate(int access) {
        return (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE)) == 0;
    }

    private static String shortName(String internal) {
        return internal.substring(internal.lastIndexOf('/') + 1);
    }

    /**
     * The core: the mixins, the entry points, KEEP, and every Cyemer class
     * any of them names, transitively. A class name appears verbatim in the
     * class file, so reading it as text and matching names finds every
     * reference (types, fields, methods, signatures, inner classes).
     */
    private static Set<String> core(Map<String, byte[]> classes) {
        Deque<String> todo = new ArrayDeque<>();
        for (String n : classes.keySet()) {
            if (n.startsWith(ROOT + "mixin/") || n.equals(ROOT + "Cyemer") || n.equals(ROOT + "CyemerClient") || KEEP.contains(topLevel(n))) {
                todo.add(n);
            }
        }
        Set<String> core = new HashSet<>();
        while (!todo.isEmpty()) {
            String n = todo.poll();
            if (!core.add(n)) {
                continue;
            }
            Matcher m = REF.matcher(new String(classes.get(n), StandardCharsets.ISO_8859_1));
            while (m.find()) {
                String ref = m.group();
                if (classes.containsKey(ref) && !core.contains(ref)) {
                    todo.add(ref);
                }
            }
        }
        return core;
    }

    /** Replaces every reloaded module with a new instance from the new build; keeps the core's own. */
    private static int swap(Generation loader, Set<String> core) {
        Cyemer cyemer = Cyemer.getInstance();
        ModuleManager manager = cyemer.getModuleManager();
        ConfigManager config = cyemer.getConfigManager();
        List<Module> old = new ArrayList<>(manager.getModules());
        config.save("default"); // what is set now is what the new modules start with
        Map<Class<?>, Module> keep = new HashMap<>();
        for (Module m : old) {
            if (m.getClass().getClassLoader() == Cyemer.class.getClassLoader() && core.contains(m.getClass().getName().replace('.', '/'))) {
                keep.put(m.getClass(), m);
            }
        }
        // Build the new modules before touching the running ones: if one fails
        // to construct, this throws and every running module is left as it was.
        List<Module> fresh = ModuleManager.createModules(ModuleManager.REGISTRY, loader, keep::get);
        Module.quiet = true;
        int swapped = 0;
        try {
            for (Module m : old) {
                if (keep.get(m.getClass()) == m) {
                    continue;
                }
                try {
                    if (m.isEnabled()) {
                        m.setEnabled(false); // its own cleanup: keys, rotations, packets held
                    }
                } catch (Throwable ignored) {
                }
                EventBus.unregister(m);
                swapped++;
            }
            RotationManager.stop();
            manager.replaceModules(fresh);
            config.load("default");
        } finally {
            Module.quiet = false;
        }
        class_310 mc = class_310.method_1551();
        if (mc.field_1755 != null && mc.field_1755.getClass().getName().startsWith("com.slither.cyemer.")) {
            mc.method_1507(null); // a Cyemer screen holds the old modules; reopen it for the new ones
        }
        return swapped;
    }

    private static boolean isOurClass(String entry) {
        return entry.startsWith(ROOT) && entry.endsWith(".class");
    }

    private static String internalName(String entry) {
        return entry.substring(0, entry.length() - ".class".length());
    }

    private static String topLevel(String n) {
        int i = n.indexOf('$');
        return i < 0 ? n : n.substring(0, i);
    }

    private static String packageOf(String internal) {
        int i = internal.lastIndexOf('/');
        return i < 0 ? "" : internal.substring(0, i);
    }

    private static String shortNames(List<String> names) {
        List<String> out = new ArrayList<>();
        for (String n : names) {
            String s = topLevel(n).substring(topLevel(n).lastIndexOf('/') + 1);
            if (!out.contains(s)) {
                out.add(s);
            }
        }
        out.sort(null);
        return out.size() > 5 ? String.join(", ", out.subList(0, 5)) + " and " + (out.size() - 5) + " more" : String.join(", ", out);
    }

    private static void say(String message) {
        System.out.println("[Cyemer] HotReload: " + message.replaceAll("§.", ""));
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 != null) {
            mc.field_1724.method_7353(class_2561.method_43470("§b[Cyemer]§r " + message), false);
        }
    }
}
