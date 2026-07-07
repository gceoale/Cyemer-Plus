package com.slither.cyemer.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class JarUpdater {
    public static File getCurrentJarFile() {
        try {
            return new File(JarUpdater.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException var1) {
            return null;
        }
    }

    public static String downloadFile(String downloadUrl, File targetFile) {
        HttpURLConnection connection = null;

        Object var6;
        try {
            if (targetFile.exists()) {
                return "The replacement file '" + targetFile.getName() + "' already exists in the mods folder.";
            }

            URL url = new URL(downloadUrl);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestProperty("User-Agent", "Cyemer-Client-Updater/1.0");
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return "Download failed. Server responded with code: " + responseCode;
            }

            try (InputStream inputStream = connection.getInputStream()) {
                Files.copy(inputStream, targetFile.toPath());
                var6 = null;
            }
        } catch (IOException var15) {
            return "An I/O error occurred during download. Check console.";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return (String)var6;
    }
}
