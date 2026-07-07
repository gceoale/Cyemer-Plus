package com.slither.cyemer.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_1060;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_12079.class_10726;

@Environment(EnvType.CLIENT)
public class CapeTextureManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_CAPE_NAME = "cyemer";
    private static final String CUSTOM_CAPE_NAME = "custom";
    private static final class_2960 CUSTOM_CAPE_TEXTURE_ID = class_2960.method_60655("dynamic_fps", "generated/custom_cape");
    private static final int TARGET_CAPE_WIDTH = 64;
    private static final int TARGET_CAPE_HEIGHT = 32;
    private static final int LIBRARY_VERSION = 1;
    private static final Path CUSTOM_CAPE_DIRECTORY = FabricLoader.getInstance().getConfigDir().resolve("cyemer").resolve("custom_capes");
    private static final Path CUSTOM_CAPE_LIBRARY_FILE = CUSTOM_CAPE_DIRECTORY.resolve("library.json");
    private static final Path CUSTOM_CAPE_STATE_FILE = FabricLoader.getInstance().getConfigDir().resolve("cyemer").resolve("custom_cape.path");
    private static volatile String loadedCapeName;
    private static volatile class_2960 loadedCapeTextureIdentifier;
    private static volatile String customCapeSourcePath;
    private static volatile class_2960 customCapeTextureIdentifier;
    private static volatile int customCapeWidth = 64;
    private static volatile int customCapeHeight = 32;
    private static class_1043 customCapeTexture;
    private static volatile boolean persistedPathLoaded;
    private static volatile String persistedCustomCapePath;
    private static final List<CapeTextureManager.CustomCapeEntry> customCapeLibrary = new ArrayList<>();
    private static volatile boolean customCapeLibraryLoaded;
    private static volatile String activeCustomCapeId;

    public static void loadCapeTexture(String capeName) {
        ensureCapeTexture(capeName);
    }

    public static synchronized CapeTextureManager.ImportResult importCustomCape(
        Path sourcePath, String requestedName, CapeTextureManager.FrameMode frameMode, float frameX, float frameY
    ) {
        if (sourcePath != null && Files.isRegularFile(sourcePath)) {
            loadCustomCapeLibrary();
            Path normalizedSource = sourcePath.toAbsolutePath().normalize();
            CapeTextureManager.FrameMode resolvedMode = frameMode == null ? CapeTextureManager.FrameMode.FILL_CROP : frameMode;
            float resolvedFrameX = clamp01(frameX);
            float resolvedFrameY = clamp01(frameY);

            class_1011 sourceImage;
            try (InputStream in = Files.newInputStream(normalizedSource)) {
                sourceImage = class_1011.method_4309(in);
            } catch (Exception var41) {
                return CapeTextureManager.ImportResult.error("Unable to read PNG file.");
            }

            if (sourceImage != null && sourceImage.method_4307() > 0 && sourceImage.method_4323() > 0) {
                String baseName = sanitizeDisplayName(requestedName);
                if (baseName.isBlank()) {
                    baseName = sanitizeDisplayName(stripFileExtension(normalizedSource.getFileName().toString()));
                }

                if (baseName.isBlank()) {
                    baseName = "Custom Cape";
                }

                long now = System.currentTimeMillis();
                String id = generateEntryId(baseName);
                String fileName = id + ".png";
                Path outputPath = CUSTOM_CAPE_DIRECTORY.resolve(fileName);
                class_1011 converted = null;
                int convertedWidth = 64;
                int convertedHeight = 32;

                label168: {
                    CapeTextureManager.ImportResult var20;
                    try {
                        Files.createDirectories(CUSTOM_CAPE_DIRECTORY);
                        converted = convertToCapeTexture(sourceImage, resolvedMode, resolvedFrameX, resolvedFrameY);
                        convertedWidth = Math.max(1, converted.method_4307());
                        convertedHeight = Math.max(1, converted.method_4323());
                        converted.method_4314(outputPath);
                        break label168;
                    } catch (Exception var42) {
                        var20 = CapeTextureManager.ImportResult.error("Failed to convert PNG to cape format.");
                    } finally {
                        try {
                            sourceImage.close();
                        } catch (Exception var37) {
                        }

                        if (converted != null) {
                            try {
                                converted.close();
                            } catch (Exception var36) {
                            }
                        }
                    }

                    return var20;
                }

                CapeTextureManager.CustomCapeEntry entry = new CapeTextureManager.CustomCapeEntry();
                entry.id = id;
                entry.name = baseName;
                entry.fileName = fileName;
                entry.sourcePath = normalizedSource.toString();
                entry.frameMode = resolvedMode.name();
                entry.frameX = resolvedFrameX;
                entry.frameY = resolvedFrameY;
                entry.width = convertedWidth;
                entry.height = convertedHeight;
                entry.createdAt = now;
                entry.updatedAt = now;
                customCapeLibrary.add(entry);
                sortLibraryByNewest();
                activeCustomCapeId = entry.id;
                persistCustomCapeLibrary();
                return !loadCustomCapeTextureFromLibraryEntry(entry)
                    ? CapeTextureManager.ImportResult.error("Cape saved, but texture upload failed.")
                    : CapeTextureManager.ImportResult.success(toView(entry));
            } else {
                if (sourceImage != null) {
                    try {
                        sourceImage.close();
                    } catch (Exception var39) {
                    }
                }

                return CapeTextureManager.ImportResult.error("PNG dimensions are invalid.");
            }
        } else {
            return CapeTextureManager.ImportResult.error("Selected file does not exist.");
        }
    }

    public static synchronized boolean setCustomCapeFromFile(Path path) {
        String requestedName = path != null ? stripFileExtension(path.getFileName().toString()) : "Custom Cape";
        return importCustomCape(path, requestedName, CapeTextureManager.FrameMode.FILL_CROP, 0.5F, 0.5F).success;
    }

    public static synchronized List<CapeTextureManager.CustomCapeEntryView> getCustomCapeLibrary() {
        loadCustomCapeLibrary();
        List<CapeTextureManager.CustomCapeEntryView> result = new ArrayList<>(customCapeLibrary.size());

        for (CapeTextureManager.CustomCapeEntry entry : customCapeLibrary) {
            result.add(toView(entry));
        }

        result.sort(Comparator.comparingLong(CapeTextureManager.CustomCapeEntryView::updatedAt).reversed());
        return result;
    }

    public static synchronized String getActiveCustomCapeId() {
        loadCustomCapeLibrary();
        return activeCustomCapeId;
    }

    public static synchronized boolean selectCustomCape(String entryId) {
        loadCustomCapeLibrary();
        CapeTextureManager.CustomCapeEntry entry = findEntry(entryId);
        if (entry == null) {
            return false;
        } else if (!loadCustomCapeTextureFromLibraryEntry(entry)) {
            return false;
        } else {
            activeCustomCapeId = entry.id;
            entry.updatedAt = System.currentTimeMillis();
            sortLibraryByNewest();
            persistCustomCapeLibrary();
            return true;
        }
    }

    public static synchronized boolean deleteCustomCape(String entryId) {
        loadCustomCapeLibrary();
        CapeTextureManager.CustomCapeEntry entry = findEntry(entryId);
        if (entry == null) {
            return false;
        } else {
            Path filePath = CUSTOM_CAPE_DIRECTORY.resolve(entry.fileName);

            try {
                Files.deleteIfExists(filePath);
            } catch (Exception var4) {
            }

            customCapeLibrary.removeIf(e -> Objects.equals(e.id, entryId));
            if (Objects.equals(activeCustomCapeId, entryId)) {
                activeCustomCapeId = null;
                customCapeSourcePath = null;
                if (!customCapeLibrary.isEmpty()) {
                    sortLibraryByNewest();
                    CapeTextureManager.CustomCapeEntry fallback = customCapeLibrary.get(0);
                    if (loadCustomCapeTextureFromLibraryEntry(fallback)) {
                        activeCustomCapeId = fallback.id;
                    } else {
                        clearCustomCapeTexture();
                    }
                } else {
                    clearCustomCapeTexture();
                }
            }

            persistCustomCapeLibrary();
            return true;
        }
    }

    public static synchronized CapeTextureManager.CustomCapeEntryView getActiveCustomCape() {
        loadCustomCapeLibrary();
        CapeTextureManager.CustomCapeEntry entry = findEntry(activeCustomCapeId);
        return entry != null ? toView(entry) : null;
    }

    public static boolean hasCustomCapeTexture() {
        return customCapeTextureIdentifier != null;
    }

    public static String getCustomCapeSourcePath() {
        CapeTextureManager.CustomCapeEntryView active = getActiveCustomCape();
        if (active != null && active.sourcePath() != null && !active.sourcePath().isBlank()) {
            return active.sourcePath();
        } else {
            return customCapeSourcePath != null && !customCapeSourcePath.isBlank() ? customCapeSourcePath : readPersistedCustomCapePath();
        }
    }

    public static class_2960 getPreviewTextureIdentifier(String capeName) {
        String normalized = normalizeCapeName(capeName);
        if ("custom".equals(normalized) && customCapeTextureIdentifier != null) {
            return customCapeTextureIdentifier;
        } else {
            if ("custom".equals(normalized)) {
                restorePersistedCustomCape();
                if (customCapeTextureIdentifier != null) {
                    return customCapeTextureIdentifier;
                }
            }

            return resolveCapeTextureIdentifier(normalized);
        }
    }

    public static int getCustomCapeWidth() {
        return customCapeWidth;
    }

    public static int getCustomCapeHeight() {
        return customCapeHeight;
    }

    public static boolean ensureCapeTexture(String capeName) {
        if (capeName != null && !capeName.isBlank()) {
            String normalized = normalizeCapeName(capeName);
            if ("custom".equals(normalized) && customCapeTextureIdentifier == null) {
                restorePersistedCustomCape();
            }

            if ("custom".equals(normalized) && customCapeTextureIdentifier == null) {
                loadCustomCapeLibrary();
                if (activeCustomCapeId != null) {
                    selectCustomCape(activeCustomCapeId);
                }
            }

            class_2960 resolved = resolveCapeTextureIdentifier(normalized);
            if (resolved == null) {
                loadedCapeName = null;
                loadedCapeTextureIdentifier = null;
                return false;
            } else {
                loadedCapeName = normalized;
                loadedCapeTextureIdentifier = resolved;
                return true;
            }
        } else {
            return false;
        }
    }

    public static boolean hasCustomCapeLoaded() {
        return loadedCapeTextureIdentifier != null;
    }

    public static class_10726 getCapeAssetInfo(String capeName) {
        if (!ensureCapeTexture(capeName)) {
            return null;
        } else {
            String token = sanitizePathToken(loadedCapeName != null ? loadedCapeName : "cyemer");
            class_2960 infoId = class_2960.method_60655("dynamic_fps", "cape/" + token);
            return new class_10726(infoId, loadedCapeTextureIdentifier);
        }
    }

    public static class_2960 getLoadedCapeTextureIdentifier() {
        return loadedCapeTextureIdentifier;
    }

    private static class_2960 resolveCapeTextureIdentifier(String capeName) {
        if ("custom".equals(capeName) && customCapeTextureIdentifier != null) {
            return customCapeTextureIdentifier;
        } else {
            class_2960 selected = buildCapeTextureIdentifier(capeName);
            if (resourceExists(selected)) {
                return selected;
            } else {
                class_2960 fallback = buildCapeTextureIdentifier("cyemer");
                return resourceExists(fallback) ? fallback : null;
            }
        }
    }

    private static class_2960 buildCapeTextureIdentifier(String capeName) {
        return class_2960.method_60655("dynamic_fps", "textures/" + sanitizePathToken(capeName) + ".png");
    }

    private static boolean resourceExists(class_2960 resourceId) {
        try {
            class_310 client = class_310.method_1551();
            return client != null && client.method_1478() != null ? client.method_1478().method_14486(resourceId).isPresent() : true;
        } catch (Throwable var2) {
            return false;
        }
    }

    private static String normalizeCapeName(String capeName) {
        String normalized = capeName.trim().toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".png")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }

        return normalized.isEmpty() ? "cyemer" : normalized;
    }

    private static String sanitizePathToken(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        String sanitized = lower.replaceAll("[^a-z0-9/_\\-.]", "_");
        return sanitized.isBlank() ? "cyemer" : sanitized;
    }

    public static synchronized boolean restorePersistedCustomCape() {
        if (customCapeTextureIdentifier != null) {
            return true;
        } else {
            loadCustomCapeLibrary();
            if (activeCustomCapeId != null && selectCustomCape(activeCustomCapeId)) {
                return true;
            } else if (!customCapeLibrary.isEmpty() && selectCustomCape(customCapeLibrary.get(0).id)) {
                return true;
            } else {
                String persistedPath = readPersistedCustomCapePath();
                if (persistedPath != null && !persistedPath.isBlank()) {
                    try {
                        return importCustomCape(Path.of(persistedPath), "Custom Cape", CapeTextureManager.FrameMode.FILL_CROP, 0.5F, 0.5F).success;
                    } catch (Exception var2) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
    }

    private static synchronized void loadCustomCapeLibrary() {
        if (!customCapeLibraryLoaded) {
            customCapeLibraryLoaded = true;
            customCapeLibrary.clear();
            activeCustomCapeId = null;

            try {
                Files.createDirectories(CUSTOM_CAPE_DIRECTORY);
            } catch (Exception var4) {
            }

            try {
                if (Files.isRegularFile(CUSTOM_CAPE_LIBRARY_FILE)) {
                    String raw = Files.readString(CUSTOM_CAPE_LIBRARY_FILE, StandardCharsets.UTF_8);
                    CapeTextureManager.CustomCapeLibraryState state = (CapeTextureManager.CustomCapeLibraryState)GSON.fromJson(
                        raw, (new TypeToken<CapeTextureManager.CustomCapeLibraryState>() {}).getType()
                    );
                    if (state != null && state.entries != null) {
                        for (CapeTextureManager.CustomCapeEntry entry : state.entries) {
                            if (isValidEntry(entry)) {
                                customCapeLibrary.add(entry);
                            }
                        }

                        activeCustomCapeId = state.activeId;
                    }
                }
            } catch (Exception var5) {
            }

            sortLibraryByNewest();
            if (activeCustomCapeId != null && findEntry(activeCustomCapeId) == null) {
                activeCustomCapeId = null;
            }
        }
    }

    private static synchronized void persistCustomCapeLibrary() {
        try {
            Files.createDirectories(CUSTOM_CAPE_DIRECTORY);
            CapeTextureManager.CustomCapeLibraryState state = new CapeTextureManager.CustomCapeLibraryState();
            state.version = 1;
            state.activeId = activeCustomCapeId;
            state.entries = new ArrayList<>(customCapeLibrary);
            String json = GSON.toJson(state);
            Files.writeString(
                CUSTOM_CAPE_LIBRARY_FILE,
                json,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
        } catch (Exception var2) {
        }
    }

    private static boolean isValidEntry(CapeTextureManager.CustomCapeEntry entry) {
        if (entry != null && entry.id != null && !entry.id.isBlank() && entry.fileName != null && !entry.fileName.isBlank()) {
            Path filePath = CUSTOM_CAPE_DIRECTORY.resolve(entry.fileName);
            if (!Files.isRegularFile(filePath)) {
                return false;
            } else {
                if (entry.name == null || entry.name.isBlank()) {
                    entry.name = "Custom Cape";
                }

                if (entry.frameMode == null || entry.frameMode.isBlank()) {
                    entry.frameMode = CapeTextureManager.FrameMode.FILL_CROP.name();
                }

                entry.frameX = clamp01(entry.frameX);
                entry.frameY = clamp01(entry.frameY);
                if (entry.width <= 0) {
                    entry.width = 64;
                }

                if (entry.height <= 0) {
                    entry.height = 32;
                }

                return true;
            }
        } else {
            return false;
        }
    }

    private static CapeTextureManager.CustomCapeEntry findEntry(String entryId) {
        if (entryId != null && !entryId.isBlank()) {
            for (CapeTextureManager.CustomCapeEntry entry : customCapeLibrary) {
                if (entryId.equals(entry.id)) {
                    return entry;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    private static void sortLibraryByNewest() {
        customCapeLibrary.sort(Comparator.<CapeTextureManager.CustomCapeEntry>comparingLong(e -> e.updatedAt).reversed());
    }

    private static boolean loadCustomCapeTextureFromLibraryEntry(CapeTextureManager.CustomCapeEntry entry) {
        if (entry == null) {
            return false;
        } else {
            Path filePath = CUSTOM_CAPE_DIRECTORY.resolve(entry.fileName);
            if (!Files.isRegularFile(filePath)) {
                return false;
            } else if (!loadCustomCapeTextureFromPath(filePath, entry.sourcePath)) {
                return false;
            } else {
                activeCustomCapeId = entry.id;
                return true;
            }
        }
    }

    private static boolean loadCustomCapeTextureFromPath(Path path, String sourcePath) {
        class_1011 image;
        try (InputStream in = Files.newInputStream(path)) {
            image = class_1011.method_4309(in);
        } catch (Exception var14) {
            return false;
        }

        if (image != null && image.method_4307() > 0 && image.method_4323() > 0) {
            class_1043 texture = null;

            try {
                texture = new class_1043(() -> "cyemer_custom_cape", image);
                texture.method_4524();
                class_310 client = class_310.method_1551();
                if (client == null) {
                    texture.close();
                    return false;
                } else {
                    class_1060 textureManager = client.method_1531();
                    textureManager.method_4616(CUSTOM_CAPE_TEXTURE_ID, texture);
                    if (customCapeTexture != null) {
                        try {
                            customCapeTexture.close();
                        } catch (Exception var10) {
                        }
                    }

                    customCapeTexture = texture;
                    customCapeTextureIdentifier = CUSTOM_CAPE_TEXTURE_ID;
                    customCapeSourcePath = sourcePath;
                    customCapeWidth = image.method_4307();
                    customCapeHeight = image.method_4323();
                    writePersistedCustomCapePath(sourcePath);
                    loadedCapeName = "custom";
                    loadedCapeTextureIdentifier = CUSTOM_CAPE_TEXTURE_ID;
                    return true;
                }
            } catch (Exception var12) {
                if (texture != null) {
                    try {
                        texture.close();
                    } catch (Exception var8) {
                    }
                } else {
                    try {
                        image.close();
                    } catch (Exception var7) {
                    }
                }

                return false;
            }
        } else {
            if (image != null) {
                try {
                    image.close();
                } catch (Exception var11) {
                }
            }

            return false;
        }
    }

    private static void clearCustomCapeTexture() {
        if (customCapeTexture != null) {
            try {
                customCapeTexture.close();
            } catch (Exception var1) {
            }
        }

        customCapeTexture = null;
        customCapeTextureIdentifier = null;
        customCapeWidth = 64;
        customCapeHeight = 32;
        customCapeSourcePath = null;
    }

    private static class_1011 convertToCapeTexture(class_1011 source, CapeTextureManager.FrameMode frameMode, float frameX, float frameY) {
        int sourceWidth = source.method_4307();
        int sourceHeight = source.method_4323();
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return createTransparentImage(64, 32);
        } else if (frameMode == CapeTextureManager.FrameMode.FIT) {
            int outputHeight = Math.max(sourceHeight, (int)Math.ceil(sourceWidth / 2.0));
            int outputWidth = Math.max(sourceWidth, outputHeight * 2);
            if ((outputWidth & 1) != 0) {
                outputWidth++;
            }

            outputHeight = Math.max(outputHeight, outputWidth / 2);
            class_1011 out = createTransparentImage(outputWidth, outputHeight);
            int offsetX = (outputWidth - sourceWidth) / 2;
            int offsetY = (outputHeight - sourceHeight) / 2;
            copyPixels(source, 0, 0, sourceWidth, sourceHeight, out, offsetX, offsetY);
            return out;
        } else {
            float sourceAspect = (float)sourceWidth / sourceHeight;
            float targetAspect = 2.0F;
            int cropWidth = sourceWidth;
            int cropHeight = sourceHeight;
            if (sourceAspect > targetAspect) {
                cropWidth = Math.max(1, Math.round(sourceHeight * targetAspect));
            } else {
                cropHeight = Math.max(1, Math.round(sourceWidth / targetAspect));
            }

            int maxOffsetX = Math.max(0, sourceWidth - cropWidth);
            int maxOffsetY = Math.max(0, sourceHeight - cropHeight);
            int offsetX = Math.max(0, Math.min(maxOffsetX, Math.round(maxOffsetX * clamp01(frameX))));
            int offsetY = Math.max(0, Math.min(maxOffsetY, Math.round(maxOffsetY * clamp01(frameY))));
            class_1011 out = createTransparentImage(cropWidth, cropHeight);
            copyPixels(source, offsetX, offsetY, cropWidth, cropHeight, out, 0, 0);
            return out;
        }
    }

    private static class_1011 createTransparentImage(int width, int height) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        class_1011 out = new class_1011(safeWidth, safeHeight, true);

        for (int y = 0; y < safeHeight; y++) {
            for (int x = 0; x < safeWidth; x++) {
                out.method_61941(x, y, 0);
            }
        }

        return out;
    }

    private static void copyPixels(class_1011 source, int sourceX, int sourceY, int copyWidth, int copyHeight, class_1011 destination, int destX, int destY) {
        int srcWidth = source.method_4307();
        int srcHeight = source.method_4323();
        int dstWidth = destination.method_4307();
        int dstHeight = destination.method_4323();

        for (int y = 0; y < copyHeight; y++) {
            int sy = sourceY + y;
            int dy = destY + y;
            if (sy >= 0 && sy < srcHeight && dy >= 0 && dy < dstHeight) {
                for (int x = 0; x < copyWidth; x++) {
                    int sx = sourceX + x;
                    int dx = destX + x;
                    if (sx >= 0 && sx < srcWidth && dx >= 0 && dx < dstWidth) {
                        destination.method_61941(dx, dy, source.method_61940(sx, sy));
                    }
                }
            }
        }
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static String stripFileExtension(String value) {
        if (value != null && !value.isBlank()) {
            int index = value.lastIndexOf(46);
            return index > 0 ? value.substring(0, index) : value;
        } else {
            return "";
        }
    }

    private static String sanitizeDisplayName(String value) {
        if (value == null) {
            return "";
        } else {
            String cleaned = value.trim().replaceAll("\\s+", " ");
            cleaned = cleaned.replaceAll("[^A-Za-z0-9 _\\-\\.]", "");
            if (cleaned.length() > 40) {
                cleaned = cleaned.substring(0, 40);
            }

            return cleaned;
        }
    }

    private static String generateEntryId(String baseName) {
        String token = sanitizePathToken(baseName).replace("/", "_").replace(".", "_");
        if (token.isBlank()) {
            token = "custom_cape";
        }

        return token + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static CapeTextureManager.CustomCapeEntryView toView(CapeTextureManager.CustomCapeEntry entry) {
        return new CapeTextureManager.CustomCapeEntryView(
            entry.id,
            entry.name,
            entry.sourcePath,
            parseFrameMode(entry.frameMode),
            clamp01(entry.frameX),
            clamp01(entry.frameY),
            entry.width,
            entry.height,
            entry.createdAt,
            entry.updatedAt
        );
    }

    private static CapeTextureManager.FrameMode parseFrameMode(String value) {
        if (value != null && !value.isBlank()) {
            try {
                return CapeTextureManager.FrameMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (Exception var2) {
                return CapeTextureManager.FrameMode.FILL_CROP;
            }
        } else {
            return CapeTextureManager.FrameMode.FILL_CROP;
        }
    }

    private static synchronized String readPersistedCustomCapePath() {
        if (persistedPathLoaded) {
            return persistedCustomCapePath;
        } else {
            persistedPathLoaded = true;

            try {
                if (Files.isRegularFile(CUSTOM_CAPE_STATE_FILE)) {
                    String raw = Files.readString(CUSTOM_CAPE_STATE_FILE, StandardCharsets.UTF_8).trim();
                    if (!raw.isBlank()) {
                        persistedCustomCapePath = raw;
                        if (customCapeSourcePath == null || customCapeSourcePath.isBlank()) {
                            customCapeSourcePath = raw;
                        }
                    }
                }
            } catch (Exception var1) {
            }

            return persistedCustomCapePath;
        }
    }

    private static synchronized void writePersistedCustomCapePath(String absolutePath) {
        if (absolutePath != null && !absolutePath.isBlank()) {
            persistedCustomCapePath = absolutePath;
            persistedPathLoaded = true;

            try {
                Files.createDirectories(CUSTOM_CAPE_STATE_FILE.getParent());
                Files.writeString(
                    CUSTOM_CAPE_STATE_FILE,
                    absolutePath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
                );
            } catch (Exception var2) {
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class CustomCapeEntry {
        String id;
        String name;
        String fileName;
        String sourcePath;
        String frameMode;
        float frameX = 0.5F;
        float frameY = 0.5F;
        int width = 64;
        int height = 32;
        long createdAt;
        long updatedAt;
    }

    @Environment(EnvType.CLIENT)
    public record CustomCapeEntryView(
        String id,
        String name,
        String sourcePath,
        CapeTextureManager.FrameMode frameMode,
        float frameX,
        float frameY,
        int width,
        int height,
        long createdAt,
        long updatedAt
    ) {
    }

    @Environment(EnvType.CLIENT)
    private static final class CustomCapeLibraryState {
        int version = 1;
        String activeId;
        List<CapeTextureManager.CustomCapeEntry> entries = new ArrayList<>();
    }

    @Environment(EnvType.CLIENT)
    public static enum FrameMode {
        FILL_CROP,
        FIT;
    }

    @Environment(EnvType.CLIENT)
    public record ImportResult(boolean success, String message, CapeTextureManager.CustomCapeEntryView entry) {
        static CapeTextureManager.ImportResult success(CapeTextureManager.CustomCapeEntryView entry) {
            return new CapeTextureManager.ImportResult(true, "ok", entry);
        }

        static CapeTextureManager.ImportResult error(String message) {
            return new CapeTextureManager.ImportResult(false, message == null ? "failed" : message, null);
        }
    }
}
