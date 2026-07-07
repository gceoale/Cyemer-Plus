package com.slither.cyemer.config.hub;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public class ConfigHubManager {
    private static final String REPO_BASE_URL = "https://raw.githubusercontent.com/Cyemer/Cyemer-ConfigHub/main/";
    private static final String MANIFEST_URL = "https://raw.githubusercontent.com/Cyemer/Cyemer-ConfigHub/main/manifest.json";
    private static final String CONFIGS_URL = "https://raw.githubusercontent.com/Cyemer/Cyemer-ConfigHub/main/configs/";
    private final Gson gson = new Gson();
    private final List<RemoteConfig> cachedConfigs = new ArrayList<>();

    public void fetchConfigs(Consumer<List<RemoteConfig>> onSuccess, Consumer<String> onError) {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL("https://raw.githubusercontent.com/Cyemer/Cyemer-ConfigHub/main/manifest.json");
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                if (connection.getResponseCode() == 200) {
                    InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                    List<RemoteConfig> configs = (List<RemoteConfig>)this.gson.fromJson(reader, (new TypeToken<List<RemoteConfig>>() {}).getType());
                    synchronized (this.cachedConfigs) {
                        this.cachedConfigs.clear();
                        this.cachedConfigs.addAll(configs);
                    }

                    onSuccess.accept(configs);
                } else {
                    onError.accept("HTTP Error: " + connection.getResponseCode());
                }
            } catch (Exception var10) {
                onError.accept("Connection failed");
            }
        });
    }

    private static boolean isSafeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        } else if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return false;
        } else if (fileName.contains("\u0000")) {
            return false;
        } else if (fileName.startsWith(".")) {
            return false;
        } else {
            return fileName.length() > 255 ? false : fileName.matches("^[A-Za-z0-9_\\-][A-Za-z0-9_.\\-]*\\.json$");
        }
    }

    public void downloadConfig(RemoteConfig config, Runnable onSuccess, Consumer<String> onError) {
        CompletableFuture.runAsync(() -> {
            try {
                if (!isSafeFileName(config.fileName)) {
                    onError.accept("Download failed: invalid config file name");
                    return;
                }

                URL url = new URL("https://raw.githubusercontent.com/Cyemer/Cyemer-ConfigHub/main/configs/" + config.fileName);
                Path configsPath = FabricLoader.getInstance().getConfigDir().resolve("cyemer/configs");
                Files.createDirectories(configsPath);
                Path outputPath = configsPath.resolve(config.fileName).normalize();
                if (!outputPath.startsWith(configsPath)) {
                    onError.accept("Download failed: invalid config file path");
                    return;
                }

                File outputFile = outputPath.toFile();

                try (
                    BufferedInputStream in = new BufferedInputStream(url.openStream());
                    FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                ) {
                    byte[] dataBuffer = new byte[1024];

                    int bytesRead;
                    while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                        fileOutputStream.write(dataBuffer, 0, bytesRead);
                    }
                }

                onSuccess.run();
            } catch (Exception var15) {
                onError.accept("Download failed");
            }
        });
    }

    public List<RemoteConfig> getCachedConfigs() {
        return this.cachedConfigs;
    }
}
