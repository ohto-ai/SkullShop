package com.ohtoai.headshop.config;

import com.ohtoai.headshop.HeadShopPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager {

    private final HeadShopPlugin plugin;
    private File configFile;
    private FileConfiguration config;

    public ConfigManager(HeadShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Merge defaults from jar
        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig);
            boolean needsSave = false;
            for (String key : defaultConfig.getKeys(true)) {
                if (!config.contains(key)) {
                    config.set(key, defaultConfig.get(key));
                    needsSave = true;
                }
            }
            if (needsSave) {
                save();
            }
        }
    }

    public void reload() {
        if (configFile == null) {
            load();
            return;
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream != null) {
            config.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8)));
        }
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config.yml!", e);
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    // ==================== General Settings ====================

    public boolean isAllowOtherPlayers() {
        return config.getBoolean("allow_other_players", true);
    }

    public void setAllowOtherPlayers(boolean allow) {
        config.set("allow_other_players", allow);
        save();
    }

    public String getLanguage() {
        return config.getString("language", "zh");
    }

    // ==================== Economy Settings ====================

    /**
     * Check if a specific economy type is enabled in config.
     */
    public boolean isEconomyEnabled(String economyKey) {
        return config.getBoolean("economy." + economyKey + ".enabled", false);
    }

    /**
     * Get the configured price for a specific economy type.
     */
    public double getPrice(String economyKey) {
        return config.getDouble("economy." + economyKey + ".price", 100.0);
    }

    // --- Vault ---

    public String getVaultDisplayName() {
        return config.getString("economy.vault.display_name", "金币");
    }

    // --- PlayerPoints ---

    public String getPlayerPointsDisplayName() {
        return config.getString("economy.player_points.display_name", "点券");
    }

    // --- Item Economy ---

    public String getItemDisplayName() {
        return config.getString("economy.item.display_name", "货币物品");
    }

    public int getItemAmount() {
        return config.getInt("economy.item.price", 1);
    }

    /**
     * Get the serialized reference item map from config.
     * Returns null if not set.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getReferenceItem() {
        Object val = config.get("economy.item.reference_item");
        if (val instanceof Map) {
            return (Map<String, Object>) val;
        }
        // Fallback: try as ConfigurationSection
        ConfigurationSection section = config.getConfigurationSection("economy.item.reference_item");
        if (section == null) return null;
        return section.getValues(true);
    }

    /**
     * Save a serialized item map as the reference item in config.
     */
    public void setReferenceItem(Map<String, Object> serialized) {
        config.set("economy.item.reference_item", serialized);
        save();
    }

    // ==================== Price Management ====================

    /**
     * Set the price for a specific economy type.
     */
    public void setPrice(String economyKey, double price) {
        config.set("economy." + economyKey + ".price", price);
        save();
    }

    /**
     * Enable or disable a specific economy type.
     */
    public void setEconomyEnabled(String economyKey, boolean enabled) {
        config.set("economy." + economyKey + ".enabled", enabled);
        save();
    }
}
