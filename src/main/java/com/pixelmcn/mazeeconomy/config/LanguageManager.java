package com.pixelmcn.mazeeconomy.config;

import com.pixelmcn.mazeeconomy.MazeEconomy;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

        InputStream defStream = plugin.getResource("lang/" + locale + ".yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration
                    .loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));

            int currentVersion = defConfig.getInt("config-version", 1);
            boolean updateNeeded = !langConfig.contains("config-version")
                    || langConfig.getInt("config-version") < currentVersion;

            if (updateNeeded) {
                if (!langConfig.contains("config-version")) {
                    plugin.getLogger()
                            .info("Updating " + locale + ".yml (missing version) to version " + currentVersion + "...");
                } else {
                    plugin.getLogger().info("Updating " + locale + ".yml from version "
                            + langConfig.getInt("config-version") + " to " + currentVersion + "...");
                }

                for (String key : langConfig.getKeys(true)) {
                    if (!langConfig.isConfigurationSection(key) && defConfig.contains(key)) {
                        defConfig.set(key, langConfig.get(key));
                    }
                }

                defConfig.set("config-version", currentVersion);

                try {
                    int oldVer = langConfig.contains("config-version") ? langConfig.getInt("config-version") : 0;
                    File backupFile = new File(plugin.getDataFolder(), "lang/" + locale + "-old-v" + oldVer + ".yml");
                    if (backupFile.exists())
                        backupFile.delete();
                    java.nio.file.Files.copy(langFile.toPath(), backupFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                    defConfig.save(langFile);
                    plugin.getLogger().info(locale + ".yml successfully updated (comments preserved). Backup saved as "
                            + backupFile.getName());
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not save " + locale + ".yml: " + e.getMessage());
                }

                langConfig = YamlConfiguration.loadConfiguration(langFile);
            }
        }
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
