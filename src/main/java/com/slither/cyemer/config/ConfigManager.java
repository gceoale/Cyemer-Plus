package com.slither.cyemer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.slither.cyemer.Cyemer;
import com.slither.cyemer.gui.new_ui.ClickGUI;
import com.slither.cyemer.gui.new_ui.FriendlistPanel;
import com.slither.cyemer.gui.new_ui.old.Panel;
import com.slither.cyemer.hud.HUDElement;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.ColorSetting;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.Setting;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.module.StringSetting;
import com.slither.cyemer.theme.ThemeManager;
import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public class ConfigManager {
    private static final int CONFIG_AUTH_GATE_SALT = 1094866261;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File configsDir;
    private Map<String, ConfigManager.PanelPosition> panelPositions = new HashMap<>();
    private static final ConfigManager INSTANCE = new ConfigManager();

    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    public ConfigManager() {
        this.configsDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "cyemer/configs");
        if (!this.configsDir.exists()) {
            this.configsDir.mkdirs();
        }
    }

    public void load(String name) {
        File configFile = new File(this.configsDir, name + ".json");
        if (!configFile.exists()) {
            if ("default".equals(name)) {
                ThemeManager.getInstance().setCurrentTheme("dark");
                this.save("default", new ArrayList<>());
            }
        } else {
            try {
                try (FileReader reader = new FileReader(configFile)) {
                    ConfigManager.ConfigData data = (ConfigManager.ConfigData)this.gson.fromJson(reader, ConfigManager.ConfigData.class);
                    if (data != null) {
                        this.applyConfig(data);
                        this.panelPositions = (Map<String, ConfigManager.PanelPosition>)(data.panelPositions != null ? data.panelPositions : new HashMap<>());
                        return;
                    }
                }
            } catch (Exception var8) {
            }
        }
    }

    public void loadModule(String configName, Module module) {
        if (configName != null && !configName.isEmpty() && module != null) {
            if (!"Booster".equalsIgnoreCase(module.getName())) {
                File configFile = new File(this.configsDir, configName + ".json");
                if (configFile.exists()) {
                    try {
                        try (FileReader reader = new FileReader(configFile)) {
                            ConfigManager.ConfigData data = (ConfigManager.ConfigData)this.gson.fromJson(reader, ConfigManager.ConfigData.class);
                            if (data == null || data.modules == null) {
                                return;
                            }

                            Map<String, ConfigManager.ModuleData> moduleLookup = this.buildModuleLookup(data.modules);
                            ConfigManager.ModuleData moduleData = this.resolveModuleData(data.modules, moduleLookup, module.getName());
                            if (moduleData != null) {
                                module.setEnabled(moduleData.enabled);
                                module.setKeyCode(moduleData.keyCode);
                                if (moduleData.settings == null) {
                                    return;
                                }

                                for (Setting setting : module.getSettings()) {
                                    this.loadSetting(moduleData.settings, setting);
                                }

                                return;
                            }
                        }
                    } catch (Exception var12) {
                    }
                }
            }
        }
    }

    public void save(String name, List<Panel> panels) {
        File configFile = new File(this.configsDir, name + ".json");
        ConfigManager.ConfigData data = new ConfigManager.ConfigData();
        data.theme = ThemeManager.getInstance().getCurrentTheme().name;
        data.hudElements = new HashMap<>();
        if (Cyemer.INSTANCE != null && Cyemer.INSTANCE.getHudManager() != null) {
            for (HUDElement element : Cyemer.INSTANCE.getHudManager().getElements()) {
                ConfigManager.HUDElementData elementData = new ConfigManager.HUDElementData(element.getX(), element.getY(), element.isEnabled());
                elementData.settings = new JsonObject();
                List<Setting> settings = element.getSettings();
                if (settings != null) {
                    for (Setting setting : settings) {
                        this.saveSetting(elementData.settings, setting);
                    }
                }

                data.hudElements.put(element.getName(), elementData);
            }
        }

        data.modules = new HashMap<>();

        for (Module module : Cyemer.INSTANCE.getModuleManager().getModules()) {
            if (!"Booster".equalsIgnoreCase(module.getName())) {
                ConfigManager.ModuleData moduleData = new ConfigManager.ModuleData();
                moduleData.enabled = module.isEnabledRaw();
                moduleData.keyCode = module.getKeyCode();
                moduleData.settings = new JsonObject();

                for (Setting setting : module.getSettings()) {
                    this.saveSetting(moduleData.settings, setting);
                }

                data.modules.put(module.getName(), moduleData);
            }
        }

        if (panels != null) {
            data.panelPositions = new HashMap<>();

            for (Panel var18 : panels) {
                ;
            }
        } else {
            data.panelPositions = this.panelPositions;
        }

        data.cameraX = ClickGUI.cameraX;
        data.cameraY = ClickGUI.cameraY;
        data.zoom = ClickGUI.zoom;
        this.panelPositions = data.panelPositions;

        try (FileWriter writer = new FileWriter(configFile)) {
            this.gson.toJson(data, writer);
        } catch (IOException var13) {
        }
    }

    public void save(String name, List<Panel> panels, FriendlistPanel friendlistPanel) {
        File configFile = new File(this.configsDir, name + ".json");
        ConfigManager.ConfigData data = new ConfigManager.ConfigData();
        data.theme = ThemeManager.getInstance().getCurrentTheme().name;
        data.hudElements = new HashMap<>();
        if (Cyemer.INSTANCE != null && Cyemer.INSTANCE.getHudManager() != null) {
            for (HUDElement element : Cyemer.INSTANCE.getHudManager().getElements()) {
                ConfigManager.HUDElementData elementData = new ConfigManager.HUDElementData(element.getX(), element.getY(), element.isEnabled());
                elementData.settings = new JsonObject();
                List<Setting> settings = element.getSettings();
                if (settings != null) {
                    for (Setting setting : settings) {
                        this.saveSetting(elementData.settings, setting);
                    }
                }

                data.hudElements.put(element.getName(), elementData);
            }
        }

        data.modules = new HashMap<>();

        for (Module module : Cyemer.INSTANCE.getModuleManager().getModules()) {
            if (!"Booster".equalsIgnoreCase(module.getName())) {
                ConfigManager.ModuleData moduleData = new ConfigManager.ModuleData();
                moduleData.enabled = module.isEnabledRaw();
                moduleData.keyCode = module.getKeyCode();
                moduleData.settings = new JsonObject();

                for (Setting setting : module.getSettings()) {
                    this.saveSetting(moduleData.settings, setting);
                }

                data.modules.put(module.getName(), moduleData);
            }
        }

        if (panels != null) {
            data.panelPositions = new HashMap<>();

            for (Panel var19 : panels) {
                ;
            }

            if (friendlistPanel != null) {
                data.panelPositions.put("FRIENDLIST", new ConfigManager.PanelPosition(friendlistPanel.getX(), friendlistPanel.getY()));
            }
        } else {
            data.panelPositions = this.panelPositions;
        }

        data.cameraX = ClickGUI.cameraX;
        data.cameraY = ClickGUI.cameraY;
        data.zoom = ClickGUI.zoom;
        this.panelPositions = data.panelPositions;

        try (FileWriter writer = new FileWriter(configFile)) {
            this.gson.toJson(data, writer);
        } catch (IOException var14) {
        }
    }

    private void saveSetting(JsonObject jsonObject, Setting setting) {
        if (setting instanceof ModeSetting s) {
            jsonObject.addProperty(s.getName(), s.getCurrentMode());
        } else if (setting instanceof SliderSetting s) {
            jsonObject.addProperty(s.getName(), s.getPreciseValue());
        } else if (setting instanceof ColorSetting s) {
            jsonObject.addProperty(s.getName(), s.getValue().getRGB());
        } else if (setting instanceof BooleanSetting s) {
            jsonObject.addProperty(s.getName(), s.isEnabled());
        } else if (setting instanceof StringSetting s) {
            jsonObject.addProperty(s.getName(), s.getValue());
        }
    }

    private void loadSetting(JsonObject jsonObject, Setting setting) {
        String settingKey = this.resolveSettingKey(jsonObject, setting.getName());
        if (settingKey != null) {
            try {
                if (setting instanceof ModeSetting s) {
                    s.setCurrentMode(jsonObject.get(settingKey).getAsString());
                } else if (setting instanceof SliderSetting s) {
                    s.setValue(jsonObject.get(settingKey).getAsDouble());
                } else if (setting instanceof ColorSetting s) {
                    s.setValue(new Color(jsonObject.get(settingKey).getAsInt(), true));
                } else if (setting instanceof BooleanSetting s) {
                    s.setEnabled(jsonObject.get(settingKey).getAsBoolean());
                } else if (setting instanceof StringSetting s) {
                    s.setValue(jsonObject.get(settingKey).getAsString());
                }
            } catch (Exception var9) {
            }
        }
    }

    private void applyConfig(ConfigManager.ConfigData data) {
        if (data.theme != null) {
            ThemeManager.getInstance().setCurrentTheme(data.theme);
        }

        if (data.cameraX != null) {
            ClickGUI.cameraX = data.cameraX;
        }

        if (data.cameraY != null) {
            ClickGUI.cameraY = data.cameraY;
        }

        if (data.zoom != null) {
            ClickGUI.zoom = data.zoom;
        }

        if (data.hudElements != null && Cyemer.INSTANCE != null && Cyemer.INSTANCE.getHudManager() != null) {
            for (Entry<String, ConfigManager.HUDElementData> entry : data.hudElements.entrySet()) {
                HUDElement element = Cyemer.INSTANCE.getHudManager().getElement(entry.getKey());
                if (element != null) {
                    ConfigManager.HUDElementData elementData = entry.getValue();
                    element.setX(elementData.x);
                    element.setY(elementData.y);
                    element.setEnabled(elementData.enabled);
                    if (elementData.settings != null) {
                        List<Setting> settings = element.getSettings();
                        if (settings != null) {
                            for (Setting setting : settings) {
                                this.loadSetting(elementData.settings, setting);
                            }
                        }
                    }
                }
            }
        }

        if (data.modules != null) {
            Map<String, ConfigManager.ModuleData> moduleLookup = this.buildModuleLookup(data.modules);

            for (Module module : new ArrayList<>(Cyemer.INSTANCE.getModuleManager().getModules())) {
                if (!"Booster".equalsIgnoreCase(module.getName())) {
                    ConfigManager.ModuleData moduleData = this.resolveModuleData(data.modules, moduleLookup, module.getName());
                    if (moduleData != null) {
                        module.setEnabled(moduleData.enabled);
                        module.setKeyCode(moduleData.keyCode);
                        if (moduleData.settings != null) {
                            for (Setting setting : module.getSettings()) {
                                this.loadSetting(moduleData.settings, setting);
                            }
                        }
                    }
                }
            }
        }
    }

    private Map<String, ConfigManager.ModuleData> buildModuleLookup(Map<String, ConfigManager.ModuleData> modules) {
        Map<String, ConfigManager.ModuleData> lookup = new HashMap<>();
        if (modules == null) {
            return lookup;
        } else {
            for (Entry<String, ConfigManager.ModuleData> entry : modules.entrySet()) {
                String key = entry.getKey();
                ConfigManager.ModuleData value = entry.getValue();
                if (key != null && !key.isBlank() && value != null) {
                    this.putIfAbsent(lookup, this.normalizeLookupKey(key), value);
                    this.putIfAbsent(lookup, this.normalizeAliasLookupKey(key), value);
                }
            }

            return lookup;
        }
    }

    private ConfigManager.ModuleData resolveModuleData(
        Map<String, ConfigManager.ModuleData> modules, Map<String, ConfigManager.ModuleData> lookup, String moduleName
    ) {
        if (modules != null && moduleName != null && !moduleName.isBlank()) {
            ConfigManager.ModuleData exact = modules.get(moduleName);
            if (exact != null) {
                return exact;
            } else if (lookup != null && !lookup.isEmpty()) {
                ConfigManager.ModuleData normalized = lookup.get(this.normalizeLookupKey(moduleName));
                return normalized != null ? normalized : lookup.get(this.normalizeAliasLookupKey(moduleName));
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private String resolveSettingKey(JsonObject jsonObject, String settingName) {
        if (jsonObject == null || settingName == null || settingName.isBlank()) {
            return null;
        } else if (jsonObject.has(settingName)) {
            return settingName;
        } else if ("Separator".equals(settingName) && jsonObject.has("Seperator")) {
            return "Seperator";
        } else {
            String normalizedTarget = this.normalizeLookupKey(settingName);
            String aliasTarget = this.normalizeAliasLookupKey(settingName);

            for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                String key = entry.getKey();
                if (key != null
                    && !key.isBlank()
                    && (this.normalizeLookupKey(key).equals(normalizedTarget) || this.normalizeAliasLookupKey(key).equals(aliasTarget))) {
                    return key;
                }
            }

            return null;
        }
    }

    private void putIfAbsent(Map<String, ConfigManager.ModuleData> map, String key, ConfigManager.ModuleData value) {
        if (key != null && !key.isBlank() && value != null) {
            map.putIfAbsent(key, value);
        }
    }

    private String normalizeLookupKey(String value) {
        return value == null ? "" : value.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private String normalizeAliasLookupKey(String value) {
        String normalized = this.normalizeLookupKey(value);
        return normalized.startsWith("auto") && normalized.length() > 4 ? normalized.substring(4) : normalized;
    }

    public void save(String name) {
        this.save(name, null);
    }

    public synchronized String exportConfigJson(String name) {
        String safeName = this.sanitizeConfigName(name);
        File configFile = new File(this.configsDir, safeName + ".json");
        if (!configFile.exists()) {
            return null;
        } else {
            try {
                return Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
            } catch (IOException var5) {
                return null;
            }
        }
    }

    public synchronized boolean importConfigJson(String name, String rawJson, boolean applyNow) {
        if (rawJson != null && !rawJson.isBlank()) {
            String safeName = this.sanitizeConfigName(name);

            JsonElement parsed;
            ConfigManager.ConfigData data;
            try {
                parsed = JsonParser.parseString(rawJson);
                data = (ConfigManager.ConfigData)this.gson.fromJson(parsed, ConfigManager.ConfigData.class);
            } catch (Exception var14) {
                return false;
            }

            if (data == null) {
                return false;
            } else {
                File configFile = new File(this.configsDir, safeName + ".json");

                try (FileWriter writer = new FileWriter(configFile)) {
                    this.gson.toJson(parsed, writer);
                } catch (IOException var13) {
                    return false;
                }

                if (applyNow) {
                    this.applyConfig(data);
                    this.panelPositions = (Map<String, ConfigManager.PanelPosition>)(data.panelPositions != null ? data.panelPositions : new HashMap<>());
                }

                return true;
            }
        } else {
            return false;
        }
    }

    private String sanitizeConfigName(String name) {
        if (name != null && !name.isBlank()) {
            String normalized = name.trim().replaceAll("[^A-Za-z0-9_.-]", "_");
            if (normalized.isBlank()) {
                return "default";
            } else {
                if (normalized.length() > 64) {
                    normalized = normalized.substring(0, 64);
                }

                return normalized;
            }
        } else {
            return "default";
        }
    }

    public boolean delete(String name) {
        if (name != null && !name.isEmpty() && !"default".equalsIgnoreCase(name)) {
            File configFile = new File(this.configsDir, name + ".json");
            if (configFile.exists()) {
                try {
                    return Files.deleteIfExists(configFile.toPath());
                } catch (IOException var4) {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public List<String> getAvailableConfigs() {
        File[] files = this.configsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        return (List<String>)(files == null
            ? new ArrayList<>()
            : Arrays.stream(files).map(file -> file.getName().substring(0, file.getName().length() - 5)).collect(Collectors.toList()));
    }

    public Map<String, ConfigManager.PanelPosition> getPanelPositions() {
        return this.panelPositions;
    }

    @Environment(EnvType.CLIENT)
    private static class ConfigData {
        public String theme;
        public Map<String, ConfigManager.HUDElementData> hudElements = new HashMap<>();
        public Map<String, ConfigManager.ModuleData> modules = new HashMap<>();
        public Map<String, ConfigManager.PanelPosition> panelPositions = new HashMap<>();
        public Double cameraX;
        public Double cameraY;
        public Double zoom;
    }

    @Environment(EnvType.CLIENT)
    private static class HUDElementData {
        public double x;
        public double y;
        public boolean enabled;
        public JsonObject settings;

        public HUDElementData(double x, double y, boolean enabled) {
            this.x = x;
            this.y = y;
            this.enabled = enabled;
        }
    }

    @Environment(EnvType.CLIENT)
    private static class ModuleData {
        public boolean enabled;
        public int keyCode;
        public JsonObject settings = new JsonObject();
    }

    @Environment(EnvType.CLIENT)
    public static class PanelPosition {
        public double x;
        public double y;

        public PanelPosition(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
