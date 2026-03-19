package com.pixelmcn.mazeeconomy.config;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

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
