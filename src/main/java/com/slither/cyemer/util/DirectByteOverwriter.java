package com.slither.cyemer.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class DirectByteOverwriter {
    public static boolean overwriteWithBytes(String downloadUrl, File targetFile) {
        HttpURLConnection connection = null;

        boolean var9;
        try {
            URL url = new URL(downloadUrl);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestProperty("User-Agent", "Cyemer-Client-Byte-Overwriter/1.0");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(15000);
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return false;
            }

            try (
                InputStream inputStream = connection.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(targetFile);
            ) {
                byte[] buffer = new byte[8192];

                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                var9 = true;
            }
        } catch (Exception var21) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return var9;
    }
}
