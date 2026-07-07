package com.slither.cyemer.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public class LogCleaner {
    public static void clean() {
        Path logPath = FabricLoader.getInstance().getGameDir().resolve("logs").resolve("latest.log");
        File file = logPath.toFile();
        if (file.exists()) {
            String[] remove = new String[]{"cyemer", "nanovg"};

            try {
                List<String> lines = new ArrayList<>();

                String line;
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    while ((line = reader.readLine()) != null) {
                        boolean skip = false;

                        for (String keyword : remove) {
                            if (line.toLowerCase().contains(keyword.toLowerCase())) {
                                skip = true;
                                break;
                            }
                        }

                        if (!skip) {
                            lines.add(line);
                        }
                    }
                }

                try (BufferedWriter var16 = new BufferedWriter(new FileWriter(file))) {
                    for (String l : lines) {
                        var16.write(l);
                        var16.newLine();
                    }
                }
            } catch (IOException var15) {
            }
        }
    }
}
