package com.slither.cyemer.gui.new_ui;

import com.slither.cyemer.util.Renderer;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_2960;
import net.minecraft.class_310;

@Environment(EnvType.CLIENT)
public final class ClickGUIIconCache {
    static final Map<String, Integer> globalIconIds = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> fetchInProgress = new ConcurrentHashMap<>();
    private static final String[] ALL_ICONS = new String[]{"client.png", "misc.png", "render.png", "combat.png", "movement.png", "player.png", "friend.png"};

    private ClickGUIIconCache() {
    }

    public static void preloadAllIcons() {
        for (String file : ALL_ICONS) {
            fetchIconAsync(file);
        }
    }

    public static void retryFailedIcons() {
        for (String file : ALL_ICONS) {
            Integer cached = globalIconIds.get(file);
            if (cached == null || cached == -1) {
                fetchIconAsync(file);
            }
        }
    }

    public static void fetchIconAsync(String fileName) {
        if (!globalIconIds.containsKey(fileName) && !fetchInProgress.containsKey(fileName)) {
            fetchInProgress.put(fileName, true);
            CompletableFuture.runAsync(() -> {
                try {
                    String url = "https://raw.githubusercontent.com/Cyemer/cyemer.github.io/main/icons/" + fileName;
                    HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

                    class_1011 scaled;
                    try (InputStream stream = conn.getInputStream()) {
                        class_1011 original = class_1011.method_4309(stream);

                        try {
                            scaled = CategoryTab.scaleImageStatic(original, 64);
                        } catch (Throwable var10) {
                            if (original != null) {
                                try {
                                    original.close();
                                } catch (Throwable var9) {
                                    var10.addSuppressed(var9);
                                }
                            }

                            throw var10;
                        }

                        if (original != null) {
                            original.close();
                        }
                    }

                    class_310.method_1551().execute(() -> {
                        try {
                            class_2960 texId = class_2960.method_60655("dynamic_fps", "textures/gui/web/" + fileName);
                            class_1043 tex = new class_1043(() -> "cyemer_icon_" + fileName, scaled);
                            class_310.method_1551().method_1531().method_4616(texId, tex);
                            int id = Renderer.get().createImageFromFile(texId.toString());
                            globalIconIds.put(fileName, id);
                        } catch (Exception var8) {
                            globalIconIds.put(fileName, -1);
                        } finally {
                            fetchInProgress.remove(fileName);
                        }
                    });
                } catch (Exception var12) {
                    globalIconIds.put(fileName, -1);
                    fetchInProgress.remove(fileName);
                }
            });
        }
    }

    public static int getIconId(String fileName) {
        return globalIconIds.getOrDefault(fileName, -1);
    }
}
