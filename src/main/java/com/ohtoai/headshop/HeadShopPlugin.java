package com.ohtoai.headshop;

import com.ohtoai.headshop.command.HeadShopCommand;
import com.ohtoai.headshop.command.HeadShopTabCompleter;
import com.ohtoai.headshop.config.ConfigManager;
import com.ohtoai.headshop.config.Messages;
import com.ohtoai.headshop.economy.*;
import com.ohtoai.headshop.gui.PaymentGui;
import com.ohtoai.headshop.gui.QuantityGui;
import com.ohtoai.headshop.head.HeadPurchaseHandler;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class HeadShopPlugin extends JavaPlugin implements Listener {

    private static HeadShopPlugin instance;

    private ConfigManager configManager;
    private Messages messages;
    private final Map<String, EconomyProvider> economyProviders = new LinkedHashMap<>();
    private HeadPurchaseHandler headPurchaseHandler;

    // Track active GUIs by inventory (avoids race conditions when switching GUIs)
    private final Map<Inventory, Object> activeGuis = new ConcurrentHashMap<>();

    public static HeadShopPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // 1. Load configuration
        try {
            configManager = new ConfigManager(this);
            configManager.load();
            messages = new Messages(this);
            getLogger().info(messages.getRaw("plugin.config_loaded"));
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to load config!", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 2. Initialize economy providers
        initEconomyProviders();

        // 3. Initialize purchase handler
        headPurchaseHandler = new HeadPurchaseHandler(this, economyProviders);

        // 4. Register commands
        Objects.requireNonNull(getCommand("headshop")).setExecutor(
                new HeadShopCommand(this));
        Objects.requireNonNull(getCommand("headshop")).setTabCompleter(
                new HeadShopTabCompleter(this));

        // 5. Register GUI listeners
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info(stripColors(messages.get("plugin.enabled", "{version}", getPluginMeta().getVersion())));
    }

    @Override
    public void onDisable() {
        activeGuis.clear();
        economyProviders.clear();
        getLogger().info(stripColors(messages.getRaw("plugin.disabled")));
    }

    // ==================== GUI Listener ====================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Object gui = activeGuis.get(event.getInventory());
        if (gui == null) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;

        if (gui instanceof PaymentGui paymentGui) {
            paymentGui.handleClick(event);
        } else if (gui instanceof QuantityGui quantityGui) {
            quantityGui.handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        activeGuis.remove(event.getInventory());
    }

    // ==================== GUI Registration ====================

    public void registerGui(Inventory inventory, Object gui) {
        activeGuis.put(inventory, gui);
    }

    // ==================== Economy Init ====================

    private void initEconomyProviders() {
        economyProviders.clear();

        // Always register all three providers — config "enabled" only controls UI visibility.
        // This allows /hs currency <type> enable to work even when the provider is currently
        // disabled but available (e.g., right after /hs setitem).

        VaultEconomyProvider vault = new VaultEconomyProvider(configManager.getVaultDisplayName());
        economyProviders.put("vault", vault);
        getLogger().info("Economy provider: Vault"
                + (vault.isAvailable() ? " ✓" : " (" + vault.getUnavailableReason() + ")"));

        PlayerPointsProvider points = new PlayerPointsProvider(configManager.getPlayerPointsDisplayName());
        economyProviders.put("player_points", points);
        getLogger().info("Economy provider: PlayerPoints"
                + (points.isAvailable() ? " ✓" : " (" + points.getUnavailableReason() + ")"));

        ItemEconomyProvider item = new ItemEconomyProvider(this, configManager.getItemDisplayName());
        economyProviders.put("item", item);
        getLogger().info("Economy provider: Item"
                + (item.isAvailable() ? " ✓ (" + item.getDescription() + ")"
                : " (" + item.getUnavailableReason() + ")"));
    }

    // --- Getters ---

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public Messages getMessages() {
        return messages;
    }

    public Map<String, EconomyProvider> getEconomyProviders() {
        return economyProviders;
    }

    /** Only providers that are enabled in config AND actually usable right now. */
    public Map<String, EconomyProvider> getAvailableProviders() {
        Map<String, EconomyProvider> available = new LinkedHashMap<>();
        for (var entry : economyProviders.entrySet()) {
            if (configManager.isEconomyEnabled(entry.getKey()) && entry.getValue().isAvailable()) {
                available.put(entry.getKey(), entry.getValue());
            }
        }
        return available;
    }

    public EconomyProvider getEconomyProvider(String key) {
        return economyProviders.get(key);
    }

    public HeadPurchaseHandler getHeadPurchaseHandler() {
        return headPurchaseHandler;
    }

    /** Reload economy providers after config change */
    public void reloadEconomyProviders() {
        initEconomyProviders();
    }

    /** Strip Minecraft color codes for clean console output. */
    private static String stripColors(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', text));
    }
}
