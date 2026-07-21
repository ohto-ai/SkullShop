package com.ohtoai.skullshop.config;

import com.ohtoai.skullshop.SkullShopPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Messages {

    private final SkullShopPlugin plugin;
    private YamlConfiguration messagesConfig;
    private String prefix;

    public Messages(SkullShopPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        String lang = plugin.getConfigManager().getLanguage();
        String fileName = "messages_" + lang + ".yml";

        // Try the configured language first
        InputStream stream = plugin.getResource(fileName);
        if (stream == null) {
            // Fallback to English
            plugin.getLogger().warning("Language file '" + fileName + "' not found, falling back to messages_en.yml");
            stream = plugin.getResource("messages_en.yml");
        }
        if (stream == null) {
            plugin.getLogger().severe("No message file found in plugin jar!");
            messagesConfig = new YamlConfiguration();
            prefix = "&8[&6HeadShop&8] ";
            return;
        }

        messagesConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        prefix = ChatColor.translateAlternateColorCodes('&',
                messagesConfig.getString("prefix", "&8[&6HeadShop&8] "));
    }

    public void reload() {
        load();
    }

    /**
     * Get raw message without prefix (for logs/console).
     */
    public String getRaw(String path) {
        String value = messagesConfig.getString(path);
        if (value == null) {
            return "<<" + path + ">>";
        }
        return value;
    }

    /**
     * Get a colored message with prefix and placeholder replacements.
     * Replacements are in pairs: key1, value1, key2, value2, ...
     */
    public String get(String path, String... replacements) {
        String value = messagesConfig.getString(path);
        if (value == null) {
            return ChatColor.translateAlternateColorCodes('&', prefix + "&c<<" + path + ">>");
        }

        // Apply placeholder replacements
        for (int i = 0; i < replacements.length - 1; i += 2) {
            value = value.replace(replacements[i], replacements[i + 1]);
        }

        return ChatColor.translateAlternateColorCodes('&', prefix + value);
    }

    /**
     * Get a colored message without prefix, with placeholder replacements.
     */
    public String getNoPrefix(String path, String... replacements) {
        String value = messagesConfig.getString(path);
        if (value == null) {
            return ChatColor.translateAlternateColorCodes('&', "&c<<" + path + ">>");
        }

        for (int i = 0; i < replacements.length - 1; i += 2) {
            value = value.replace(replacements[i], replacements[i + 1]);
        }

        return ChatColor.translateAlternateColorCodes('&', value);
    }

    /**
     * Get the prefix string.
     */
    public String getPrefix() {
        return prefix;
    }
}
