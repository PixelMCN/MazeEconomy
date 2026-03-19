package com.pixelmcn.mazeeconomy.config;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public class LanguageManager {

    private final MazeEconomy plugin;
    private YamlConfiguration langConfig;

    public LanguageManager(MazeEconomy plugin) {
        this.plugin = plugin;
    }

    public void load() {
        String locale = plugin.getConfig().getString("locale", "en_US");
        File langFile = new File(plugin.getDataFolder(), "lang/" + locale + ".yml");

        if (!langFile.exists()) {
            langFile.getParentFile().mkdirs();
            try {
                plugin.saveResource("lang/" + locale + ".yml", false);
            } catch (Exception e) {
                plugin.getLogger().warning("Language file " + locale + " not found natively. Creating empty file.");
                try {
                    langFile.createNewFile();
                } catch (Exception ignored) {
                }
            }
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public void reload() {
        load();
    }

    public String getPrefix() {
        return langConfig.getString("prefix", "<gray>[<#C9A55A>MazeEconomy</#C9A55A>]</gray> ");
    }

    public String getMessage(String path) {
        return langConfig.getString(path, "<red>Missing string: " + path + "</red>");
    }

    public String getString(String path, String def) {
        return langConfig.getString(path, def);
    }

    public List<String> getStringList(String path) {
        return langConfig.getStringList(path);
    }
}
