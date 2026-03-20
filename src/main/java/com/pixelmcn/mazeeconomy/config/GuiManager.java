package com.pixelmcn.mazeeconomy.config;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class GuiManager {

    private final MazeEconomy plugin;
    private YamlConfiguration guiConfig;

    public GuiManager(MazeEconomy plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File guiFile = new File(plugin.getDataFolder(), "gui.yml");

        if (!guiFile.exists()) {
            guiFile.getParentFile().mkdirs();
            try {
                plugin.saveResource("gui.yml", false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("gui.yml not found natively. Creating empty file.");
                try {
                    guiFile.createNewFile();
                } catch (Exception ignored) {
                }
            }
        }
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);

        InputStream defStream = plugin.getResource("gui.yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration
                    .loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));

            int currentVersion = defConfig.getInt("config-version", 1);
            boolean updateNeeded = !guiConfig.contains("config-version")
                    || guiConfig.getInt("config-version") < currentVersion;

            if (updateNeeded) {
                if (!guiConfig.contains("config-version")) {
                    plugin.getLogger().info("Updating gui.yml (missing version) to version " + currentVersion + "...");
                } else {
                    plugin.getLogger().info("Updating gui.yml from version " + guiConfig.getInt("config-version")
                            + " to " + currentVersion + "...");
                }

                for (String key : guiConfig.getKeys(true)) {
                    if (!guiConfig.isConfigurationSection(key) && defConfig.contains(key)) {
                        defConfig.set(key, guiConfig.get(key));
                    }
                }

                defConfig.set("config-version", currentVersion);

                try {
                    int oldVer = guiConfig.contains("config-version") ? guiConfig.getInt("config-version") : 0;
                    File backupFile = new File(plugin.getDataFolder(), "gui-old-v" + oldVer + ".yml");
                    if (backupFile.exists())
                        backupFile.delete();
                    java.nio.file.Files.copy(guiFile.toPath(), backupFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                    defConfig.save(guiFile);
                    plugin.getLogger().info("gui.yml successfully updated (comments preserved). Backup saved as "
                            + backupFile.getName());
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not save gui.yml: " + e.getMessage());
                }

                guiConfig = YamlConfiguration.loadConfiguration(guiFile);
            }
        }
    }

    public void reload() {
        load();
    }

    public Material getMaterial(String path, Material defaultMaterial) {
        String matStr = guiConfig.getString(path);
        if (matStr == null)
            return defaultMaterial;

        Material mat = Material.matchMaterial(matStr);
        return mat != null ? mat : defaultMaterial;
    }
}
