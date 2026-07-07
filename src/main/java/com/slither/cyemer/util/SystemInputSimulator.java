package com.slither.cyemer.util;

import com.slither.cyemer.mixin.KeyBindingAccessor;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.win32.W32APIOptions;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3675;
import net.minecraft.class_3675.class_306;
import net.minecraft.class_3675.class_307;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class SystemInputSimulator {
    private static final SystemInputSimulator.IInputSimulator SIMULATOR;
    private static final boolean USE_NATIVE;

    private SystemInputSimulator() {
    }

    private static boolean isAndroidOrPojav() {
        String jvmArg = System.getProperty("cyemer.force.vanilla");
        if ("true".equalsIgnoreCase(jvmArg)) {
            return true;
        } else {
            try {
                File runDir = new File(".");
                if (new File(runDir, "cyemer_force_vanilla").exists() || new File(runDir, "cyemer_force_vanilla.txt").exists()) {
                    return true;
                }
            } catch (Throwable var6) {
            }

            if (System.getenv("POJAVEXEC_EGL") != null || System.getenv("POJAV_RENDERER") != null) {
                return true;
            } else if (Platform.isAndroid()) {
                return true;
            } else {
                String vendor = System.getProperty("java.vendor", "").toLowerCase();
                String vmName = System.getProperty("java.vm.name", "").toLowerCase();
                String userHome = System.getProperty("user.home", "");
                if (vendor.contains("android")) {
                    return true;
                } else if (vmName.contains("dalvik") || vmName.contains("art")) {
                    return true;
                } else if (!userHome.contains("/storage/emulated") && !userHome.contains("/data/user") && !userHome.contains("/data/data")) {
                    String os = System.getProperty("os.name", "").toLowerCase();
                    String arch = System.getProperty("os.arch", "").toLowerCase();
                    return os.contains("linux") && (arch.equals("aarch64") || arch.startsWith("arm"))
                        ? System.getProperty("android.os.Build.VERSION") != null
                        : false;
                } else {
                    return true;
                }
            }
        }
    }

    public static void tapKey(class_304 key) {
        SIMULATOR.simulatePress(key);
        SIMULATOR.simulateRelease(key);
    }

    public static void pressAttack() {
        SIMULATOR.simulatePress(getClient().field_1690.field_1886);
    }

    public static void releaseAttack() {
        SIMULATOR.simulateRelease(getClient().field_1690.field_1886);
    }

    public static boolean tapAttack() {
        pressAttack();
        releaseAttack();
        return true;
    }

    public static void pressUse() {
        SIMULATOR.simulatePress(getClient().field_1690.field_1904);
    }

    public static void releaseUse() {
        SIMULATOR.simulateRelease(getClient().field_1690.field_1904);
    }

    public static boolean tapUse() {
        pressUse();
        releaseUse();
        return true;
    }

    public static void pressForward() {
        SIMULATOR.simulatePress(getClient().field_1690.field_1894);
    }

    public static void releaseForward() {
        SIMULATOR.simulateRelease(getClient().field_1690.field_1894);
    }

    public static void pressBack() {
        SIMULATOR.simulatePress(getClient().field_1690.field_1881);
    }

    public static void releaseBack() {
        SIMULATOR.simulateRelease(getClient().field_1690.field_1881);
    }

    public static void pressSneak() {
        SIMULATOR.simulatePress(getClient().field_1690.field_1832);
    }

    public static void releaseSneak() {
        SIMULATOR.simulateRelease(getClient().field_1690.field_1832);
    }

    public static void pressJump() {
        SIMULATOR.simulatePress(getClient().field_1690.field_1903);
    }

    public static void releaseJump() {
        SIMULATOR.simulateRelease(getClient().field_1690.field_1903);
    }

    public static boolean tapJump() {
        pressJump();
        releaseJump();
        return true;
    }

    public static void tapSwapHands() {
        class_304 key = getClient().field_1690.field_1831;
        if (key != null) {
            if (key instanceof KeyBindingAccessor accessor) {
                accessor.setTimesPressed(accessor.getTimesPressed() + 1);
            }
        }
    }

    public static double getScaledCursorX(int scaledWidth) {
        class_310 mc = getClient();
        if (mc != null && mc.method_22683() != null && scaledWidth > 0) {
            long handle = mc.method_22683().method_4490();
            double[] x = new double[1];
            double[] y = new double[1];
            GLFW.glfwGetCursorPos(handle, x, y);
            return x[0] * ((double)scaledWidth / Math.max(1, mc.method_22683().method_4480()));
        } else {
            return 0.0;
        }
    }

    public static double getScaledCursorY(int scaledHeight) {
        class_310 mc = getClient();
        if (mc != null && mc.method_22683() != null && scaledHeight > 0) {
            long handle = mc.method_22683().method_4490();
            double[] x = new double[1];
            double[] y = new double[1];
            GLFW.glfwGetCursorPos(handle, x, y);
            return y[0] * ((double)scaledHeight / Math.max(1, mc.method_22683().method_4507()));
        } else {
            return 0.0;
        }
    }

    public static boolean moveCursorToScaled(int scaledWidth, int scaledHeight, double targetScaledX, double targetScaledY, double speedPerTick, String mode) {
        class_310 mc = getClient();
        if (mc != null && mc.method_22683() != null && scaledWidth > 0 && scaledHeight > 0) {
            long handle = mc.method_22683().method_4490();
            int windowWidth = Math.max(1, mc.method_22683().method_4480());
            int windowHeight = Math.max(1, mc.method_22683().method_4507());
            double targetWindowX = targetScaledX * ((double)windowWidth / scaledWidth);
            double targetWindowY = targetScaledY * ((double)windowHeight / scaledHeight);
            double[] currentX = new double[1];
            double[] currentY = new double[1];
            GLFW.glfwGetCursorPos(handle, currentX, currentY);
            double dx = targetWindowX - currentX[0];
            double dy = targetWindowY - currentY[0];
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance <= 1.0) {
                GLFW.glfwSetCursorPos(handle, targetWindowX, targetWindowY);
                return true;
            } else {
                String resolvedMode = mode == null ? "Smooth" : mode;
                if ("Instant".equalsIgnoreCase(resolvedMode)) {
                    GLFW.glfwSetCursorPos(handle, targetWindowX, targetWindowY);
                    return true;
                } else {
                    double speed = Math.max(1.0, speedPerTick);
                    if ("Heuristic".equalsIgnoreCase(resolvedMode)) {
                        if (distance > 200.0) {
                            speed *= 2.3;
                        } else if (distance > 90.0) {
                            speed *= 1.7;
                        } else if (distance > 40.0) {
                            speed *= 1.25;
                        }
                    }

                    double step = Math.min(distance, speed);
                    double nextX = currentX[0] + dx / distance * step;
                    double nextY = currentY[0] + dy / distance * step;
                    GLFW.glfwSetCursorPos(handle, nextX, nextY);
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public static boolean isUsingNative() {
        return USE_NATIVE;
    }

    private static class_310 getClient() {
        return class_310.method_1551();
    }

    static {
        boolean useNative = true;

        SystemInputSimulator.IInputSimulator impl;
        try {
            if (isAndroidOrPojav()) {
                impl = new SystemInputSimulator.InternalSimulator();
                useNative = false;
            } else if (Platform.isWindows()) {
                impl = new SystemInputSimulator.WindowsSimulator();
            } else if (Platform.isMac()) {
                impl = new SystemInputSimulator.MacSimulator();
            } else if (Platform.isLinux()) {
                impl = new SystemInputSimulator.LinuxSimulator();
            } else {
                impl = new SystemInputSimulator.InternalSimulator();
                useNative = false;
            }
        } catch (Throwable var3) {
            impl = new SystemInputSimulator.InternalSimulator();
            useNative = false;
        }

        SIMULATOR = impl;
        USE_NATIVE = useNative;
    }

    @Environment(EnvType.CLIENT)
    private interface IInputSimulator {
        void simulatePress(class_304 var1);

        void simulateRelease(class_304 var1);
    }

    @Environment(EnvType.CLIENT)
    private static class InternalSimulator implements SystemInputSimulator.IInputSimulator {
        @Override
        public void simulatePress(class_304 keyBinding) {
            try {
                keyBinding.method_23481(true);
                class_310 mc = SystemInputSimulator.getClient();
                if ((keyBinding == mc.field_1690.field_1886 || keyBinding == mc.field_1690.field_1904) && keyBinding instanceof KeyBindingAccessor accessor) {
                    accessor.setTimesPressed(accessor.getTimesPressed() + 1);
                }
            } catch (Exception var4) {
            }
        }

        @Override
        public void simulateRelease(class_304 keyBinding) {
            try {
                keyBinding.method_23481(false);
            } catch (Exception var3) {
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private static class LinuxSimulator implements SystemInputSimulator.IInputSimulator {
        private static final Map<Integer, Long> LWJGL_TO_X11 = new HashMap<>();
        private final Pointer display = SystemInputSimulator.LinuxSimulator.X11.INSTANCE.XOpenDisplay(null);

        public LinuxSimulator() {
            if (this.display == null) {
                throw new RuntimeException("Could not open X11 display");
            } else {
                LWJGL_TO_X11.put(87, 119L);
                LWJGL_TO_X11.put(65, 97L);
                LWJGL_TO_X11.put(83, 115L);
                LWJGL_TO_X11.put(68, 100L);
                LWJGL_TO_X11.put(340, 65505L);
                LWJGL_TO_X11.put(344, 65506L);
                LWJGL_TO_X11.put(32, 32L);
                LWJGL_TO_X11.put(341, 65507L);
                LWJGL_TO_X11.put(345, 65508L);
            }
        }

        @Override
        public void simulatePress(class_304 kb) {
            kb.method_23481(true);
            this.handle(kb, true);
        }

        @Override
        public void simulateRelease(class_304 kb) {
            kb.method_23481(false);
            this.handle(kb, false);
        }

        private void handle(class_304 kb, boolean press) {
            if (this.display != null) {
                class_306 k = class_3675.method_15981(kb.method_1428());

                try {
                    if (k.method_1442() == class_307.field_1672) {
                        int button = 0;
                        if (k.method_1444() == 0) {
                            button = 1;
                        } else if (k.method_1444() == 1) {
                            button = 3;
                        }

                        if (button != 0) {
                            SystemInputSimulator.LinuxSimulator.XTest.INSTANCE.XTestFakeButtonEvent(this.display, button, press, 0L);
                            SystemInputSimulator.LinuxSimulator.X11.INSTANCE.XFlush(this.display);
                        }
                    } else if (k.method_1442() == class_307.field_1668) {
                        Long keysym = LWJGL_TO_X11.get(k.method_1444());
                        if (keysym != null) {
                            int keycode = SystemInputSimulator.LinuxSimulator.X11.INSTANCE.XKeysymToKeycode(this.display, keysym);
                            if (keycode != 0) {
                                SystemInputSimulator.LinuxSimulator.XTest.INSTANCE.XTestFakeKeyEvent(this.display, keycode, press, 0L);
                                SystemInputSimulator.LinuxSimulator.X11.INSTANCE.XFlush(this.display);
                            }
                        }
                    }
                } catch (Exception var6) {
                }
            }
        }

        @Environment(EnvType.CLIENT)
        private interface X11 extends Library {
            SystemInputSimulator.LinuxSimulator.X11 INSTANCE = (SystemInputSimulator.LinuxSimulator.X11)Native.load(
                "X11", SystemInputSimulator.LinuxSimulator.X11.class
            );

            Pointer XOpenDisplay(String var1);

            int XKeysymToKeycode(Pointer var1, long var2);

            int XFlush(Pointer var1);
        }

        @Environment(EnvType.CLIENT)
        private interface XTest extends Library {
            SystemInputSimulator.LinuxSimulator.XTest INSTANCE = (SystemInputSimulator.LinuxSimulator.XTest)Native.load(
                "Xtst", SystemInputSimulator.LinuxSimulator.XTest.class
            );

            void XTestFakeKeyEvent(Pointer var1, int var2, boolean var3, long var4);

            void XTestFakeButtonEvent(Pointer var1, int var2, boolean var3, long var4);
        }
    }

    @Environment(EnvType.CLIENT)
    private static class MacSimulator implements SystemInputSimulator.IInputSimulator {
        private static final Map<Integer, Short> LWJGL_TO_MAC = new HashMap<>();

        public MacSimulator() {
            LWJGL_TO_MAC.put(87, (short)13);
            LWJGL_TO_MAC.put(65, (short)0);
            LWJGL_TO_MAC.put(83, (short)1);
            LWJGL_TO_MAC.put(68, (short)2);
            LWJGL_TO_MAC.put(340, (short)56);
            LWJGL_TO_MAC.put(344, (short)60);
            LWJGL_TO_MAC.put(32, (short)49);
            LWJGL_TO_MAC.put(341, (short)59);
            LWJGL_TO_MAC.put(345, (short)62);
        }

        @Override
        public void simulatePress(class_304 kb) {
            kb.method_23481(true);
            this.handle(kb, true);
        }

        @Override
        public void simulateRelease(class_304 kb) {
            kb.method_23481(false);
            this.handle(kb, false);
        }

        private void handle(class_304 kb, boolean press) {
            class_306 k = class_3675.method_15981(kb.method_1428());
            Pointer event = null;

            try {
                if (k.method_1442() == class_307.field_1672) {
                    int mouseType = 0;
                    int mouseButton = 0;
                    if (k.method_1444() == 0) {
                        mouseType = press ? 1 : 2;
                        mouseButton = 0;
                    } else if (k.method_1444() == 1) {
                        mouseType = press ? 3 : 4;
                        mouseButton = 1;
                    }

                    event = SystemInputSimulator.MacSimulator.CoreGraphics.INSTANCE.CGEventCreateMouseEvent(null, mouseType, null, mouseButton);
                } else if (k.method_1442() == class_307.field_1668) {
                    Short virtualKey = LWJGL_TO_MAC.get(k.method_1444());
                    if (virtualKey != null) {
                        event = SystemInputSimulator.MacSimulator.CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, virtualKey, press);
                    }
                }

                if (event != null) {
                    SystemInputSimulator.MacSimulator.CoreGraphics.INSTANCE.CGEventPost(0, event);
                    SystemInputSimulator.MacSimulator.CoreGraphics.INSTANCE.CFRelease(event);
                }
            } catch (Exception var7) {
            }
        }

        @Environment(EnvType.CLIENT)
        private interface CoreGraphics extends Library {
            SystemInputSimulator.MacSimulator.CoreGraphics INSTANCE = (SystemInputSimulator.MacSimulator.CoreGraphics)Native.load(
                "CoreGraphics", SystemInputSimulator.MacSimulator.CoreGraphics.class
            );

            Pointer CGEventCreateMouseEvent(Pointer var1, int var2, Pointer var3, int var4);

            Pointer CGEventCreateKeyboardEvent(Pointer var1, short var2, boolean var3);

            void CGEventPost(int var1, Pointer var2);

            void CFRelease(Pointer var1);
        }
    }

    @Environment(EnvType.CLIENT)
    private static class WindowsSimulator implements SystemInputSimulator.IInputSimulator {
        private static final Map<String, Integer> KEY_NAME_TO_CODE_MAP = new HashMap<>();

        @Override
        public void simulatePress(class_304 keyBinding) {
            class_306 key = class_3675.method_15981(keyBinding.method_1428());
            keyBinding.method_23481(true);
            if (key.method_1442() == class_307.field_1672) {
                if (key.method_1444() == 0) {
                    SystemInputSimulator.WindowsSimulator.NativeUser32.INSTANCE.mouse_event(2, 0, 0, 0, 0);
                } else if (key.method_1444() == 1) {
                    SystemInputSimulator.WindowsSimulator.NativeUser32.INSTANCE.mouse_event(8, 0, 0, 0, 0);
                }
            } else if (key.method_1442() == class_307.field_1668) {
                int vk = this.getVirtualKeyCode(key);
                if (vk != 0) {
                    SystemInputSimulator.WindowsSimulator.NativeUser32.INSTANCE.keybd_event((byte)vk, (byte)0, 0, 0);
                }
            }
        }

        @Override
        public void simulateRelease(class_304 keyBinding) {
            class_306 key = class_3675.method_15981(keyBinding.method_1428());
            keyBinding.method_23481(false);
            if (key.method_1442() == class_307.field_1672) {
                if (key.method_1444() == 0) {
                    SystemInputSimulator.WindowsSimulator.NativeUser32.INSTANCE.mouse_event(4, 0, 0, 0, 0);
                } else if (key.method_1444() == 1) {
                    SystemInputSimulator.WindowsSimulator.NativeUser32.INSTANCE.mouse_event(16, 0, 0, 0, 0);
                }
            } else if (key.method_1442() == class_307.field_1668) {
                int vk = this.getVirtualKeyCode(key);
                if (vk != 0) {
                    SystemInputSimulator.WindowsSimulator.NativeUser32.INSTANCE.keybd_event((byte)vk, (byte)0, 2, 0);
                }
            }
        }

        private int getVirtualKeyCode(class_306 key) {
            String translationKey = key.method_1441();
            String name = translationKey.substring(translationKey.lastIndexOf(46) + 1);
            if (KEY_NAME_TO_CODE_MAP.containsKey(name)) {
                return KEY_NAME_TO_CODE_MAP.get(name);
            } else {
                return name.length() == 1 ? KeyEvent.getExtendedKeyCodeForChar(name.toUpperCase().charAt(0)) : 0;
            }
        }

        private static void initializeKeyMap() {
            KEY_NAME_TO_CODE_MAP.put("left.shift", 16);
            KEY_NAME_TO_CODE_MAP.put("right.shift", 16);
            KEY_NAME_TO_CODE_MAP.put("left.control", 17);
            KEY_NAME_TO_CODE_MAP.put("right.control", 17);
            KEY_NAME_TO_CODE_MAP.put("left.alt", 18);
            KEY_NAME_TO_CODE_MAP.put("right.alt", 65406);
            KEY_NAME_TO_CODE_MAP.put("space", 32);
            KEY_NAME_TO_CODE_MAP.put("enter", 10);
        }

        static {
            initializeKeyMap();
        }

        @Environment(EnvType.CLIENT)
        private interface NativeUser32 extends User32 {
            SystemInputSimulator.WindowsSimulator.NativeUser32 INSTANCE = (SystemInputSimulator.WindowsSimulator.NativeUser32)Native.load(
                "user32", SystemInputSimulator.WindowsSimulator.NativeUser32.class, W32APIOptions.DEFAULT_OPTIONS
            );
            int MOUSEEVENTF_LEFTDOWN = 2;
            int MOUSEEVENTF_LEFTUP = 4;
            int MOUSEEVENTF_RIGHTDOWN = 8;
            int MOUSEEVENTF_RIGHTUP = 16;
            int KEYEVENTF_KEYDOWN = 0;
            int KEYEVENTF_KEYUP = 2;

            void mouse_event(int var1, int var2, int var3, int var4, int var5);

            void keybd_event(byte var1, byte var2, int var3, int var4);
        }
    }
}
