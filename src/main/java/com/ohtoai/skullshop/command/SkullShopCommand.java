package com.ohtoai.skullshop.command;

import com.ohtoai.skullshop.SkullShopPlugin;
import com.ohtoai.skullshop.config.Messages;
import com.ohtoai.skullshop.economy.EconomyProvider;
import com.ohtoai.skullshop.economy.ItemEconomyProvider;
import com.ohtoai.skullshop.gui.PaymentGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class SkullShopCommand implements CommandExecutor {

    private final SkullShopPlugin plugin;

    public SkullShopCommand(SkullShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.getMessages();

        if (args.length == 0) {
            sendHelp(sender, messages);
            return true;
        }

        String sub = args[0].toLowerCase();

        return switch (sub) {
            case "buy" -> handleBuy(sender, args);
            case "price" -> handlePrice(sender, args);
            case "currency" -> handleCurrency(sender, args);
            case "allowother" -> handleAllowOther(sender, args);
            case "setitem" -> handleSetItem(sender);
            case "reload" -> handleReload(sender);
            case "help" -> { sendHelp(sender, messages); yield true; }
            default -> {
                sender.sendMessage(messages.get("general.unknown_subcommand", "{cmd}", sub));
                sendHelp(sender, messages);
                yield true;
            }
        };
    }

    // ==================== /ss buy ====================

    private boolean handleBuy(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("general.player_only"));
            return true;
        }

        if (!sender.hasPermission("skullshop.use")) {
            sender.sendMessage(messages.get("general.no_permission"));
            return true;
        }

        // Parse: /ss buy [id] [via <currency>] [1|8|64]
        String targetName = null;
        String economyKey = null;
        int quantity = 1;

        int i = 1;
        while (i < args.length) {
            String arg = args[i].toLowerCase();
            if (arg.equals("via")) {
                i++;
                if (i < args.length) {
                    economyKey = args[i].toLowerCase();
                }
            } else if (arg.equals("1") || arg.equals("8") || arg.equals("64")) {
                quantity = Integer.parseInt(arg);
            } else if (plugin.getEconomyProvider(arg) != null && economyKey == null) {
                // This is an economy key (for backward compat: /ss buy vault)
                economyKey = arg;
            } else {
                // Treat as player name
                targetName = args[i];
            }
            i++;
        }

        // Normalize: empty target = own head, empty after own-check = own
        if (targetName != null && targetName.equalsIgnoreCase(player.getName())) {
            targetName = null;
        }

        // Check allow_other_players before any purchase path
        if (targetName != null && !targetName.isEmpty()
                && !plugin.getConfigManager().isAllowOtherPlayers()) {
            sender.sendMessage(messages.get("purchase.other_players_disabled"));
            return true;
        }

        // If "via" is specified, do direct purchase
        if (economyKey != null) {
            plugin.getHeadPurchaseHandler().purchaseDirect(player, targetName, economyKey, quantity);
            return true;
        }

        // Otherwise, open GUI
        String displayName = (targetName != null && !targetName.isEmpty()) ? targetName : player.getName();
        PaymentGui gui = new PaymentGui(plugin, player, displayName);
        gui.open();
        return true;
    }

    // ==================== /ss price ====================

    private boolean handlePrice(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();

        if (args.length == 1) {
            // /ss price — show all
            sender.sendMessage(messages.getNoPrefix("price.header"));
            Map<String, EconomyProvider> providers = plugin.getAvailableProviders();
            if (providers.isEmpty()) {
                sender.sendMessage(messages.getNoPrefix("price.no_available"));
            }
            for (Map.Entry<String, EconomyProvider> entry : providers.entrySet()) {
                double price = plugin.getConfigManager().getPrice(entry.getKey());
                sender.sendMessage(messages.getNoPrefix("price.line",
                        "{name}", entry.getValue().getDisplayName(),
                        "{price}", formatPriceSimple(entry.getKey(), price)));
            }
            return true;
        }

        String key = args[1].toLowerCase();
        if (!isValidEconomyKey(key)) {
            sender.sendMessage(messages.get("general.unknown_currency", "{key}", key));
            return true;
        }

        if (args.length >= 3) {
            // /ss price <type> <number> — set (admin)
            if (!sender.hasPermission("skullshop.admin")) {
                sender.sendMessage(messages.get("general.no_permission"));
                return true;
            }
            try {
                double newPrice = Double.parseDouble(args[2]);
                if (newPrice < 0) {
                    sender.sendMessage(messages.get("general.negative_price"));
                    return true;
                }
                plugin.getConfigManager().setPrice(key, newPrice);

                String displayName = plugin.getConfigManager().getConfig()
                        .getString("economy." + key + ".display_name", key);
                sender.sendMessage(messages.get("price.set_success",
                        "{name}", displayName,
                        "{price}", formatPriceSimple(key, newPrice)));
            } catch (NumberFormatException e) {
                sender.sendMessage(messages.get("general.invalid_number"));
            }
        } else {
            // /ss price <type> — show specific
            boolean enabled = plugin.getConfigManager().isEconomyEnabled(key);
            double price = plugin.getConfigManager().getPrice(key);
            String displayName = plugin.getConfigManager().getConfig()
                    .getString("economy." + key + ".display_name", key);
            String status = enabled ? "§a已启用" : "§c已禁用";
            sender.sendMessage(messages.getNoPrefix("price.header"));
            sender.sendMessage(messages.getNoPrefix("price.line",
                    "{name}", displayName + " " + status,
                    "{price}", formatPriceSimple(key, price)));
        }
        return true;
    }

    // ==================== /ss currency ====================

    private boolean handleCurrency(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();

        if (!sender.hasPermission("skullshop.admin")) {
            sender.sendMessage(messages.get("general.no_permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(messages.getNoPrefix("currency.usage"));
            return true;
        }

        String key = args[1].toLowerCase();
        if (!isValidEconomyKey(key)) {
            sender.sendMessage(messages.get("general.unknown_currency", "{key}", key));
            return true;
        }

        String action = args[2].toLowerCase();
        boolean enable;
        if (action.equals("enable") || action.equals("on")) {
            enable = true;
        } else if (action.equals("disable") || action.equals("off")) {
            enable = false;
        } else {
            sender.sendMessage(messages.getNoPrefix("currency.usage"));
            return true;
        }

        // If enabling, check that the provider is actually usable
        if (enable) {
            EconomyProvider provider = plugin.getEconomyProvider(key);
            if (provider == null || !provider.isAvailable()) {
                String reason = (provider != null) ? provider.getUnavailableReason() : "未知";
                String name = plugin.getConfigManager().getConfig().getString("economy." + key + ".display_name", key);
                sender.sendMessage(messages.get("currency.cannot_enable",
                        "{name}", name, "{reason}", reason));
                return true;
            }
        }

        plugin.getConfigManager().setEconomyEnabled(key, enable);
        plugin.reloadEconomyProviders();

        String displayName = plugin.getConfigManager().getConfig()
                .getString("economy." + key + ".display_name", key);
        if (enable) {
            sender.sendMessage(messages.get("currency.enabled", "{name}", displayName));
        } else {
            sender.sendMessage(messages.get("currency.disabled", "{name}", displayName));
        }
        return true;
    }

    // ==================== /ss allowother ====================

    private boolean handleAllowOther(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();

        if (!sender.hasPermission("skullshop.admin")) {
            sender.sendMessage(messages.get("general.no_permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(messages.getNoPrefix("allowother.usage"));
            sender.sendMessage(messages.getNoPrefix("allowother.current",
                    "{status}", plugin.getConfigManager().isAllowOtherPlayers()
                            ? messages.getNoPrefix("allowother.on")
                            : messages.getNoPrefix("allowother.off")));
            return true;
        }

        String action = args[1].toLowerCase();
        boolean enable;
        if (action.equals("enable") || action.equals("on")) {
            enable = true;
        } else if (action.equals("disable") || action.equals("off")) {
            enable = false;
        } else {
            sender.sendMessage(messages.getNoPrefix("allowother.usage"));
            return true;
        }

        plugin.getConfigManager().setAllowOtherPlayers(enable);
        sender.sendMessage(messages.get("allowother.result",
                "{status}", enable
                        ? messages.getNoPrefix("allowother.on")
                        : messages.getNoPrefix("allowother.off")));
        return true;
    }

    // ==================== /ss setitem ====================

    private boolean handleSetItem(CommandSender sender) {
        Messages messages = plugin.getMessages();

        if (!sender.hasPermission("skullshop.admin")) {
            sender.sendMessage(messages.get("general.no_permission"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("general.player_only"));
            return true;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            sender.sendMessage(messages.get("setitem.hold_item"));
            return true;
        }

        Map<String, Object> serialized = held.serialize();
        plugin.getConfigManager().setReferenceItem(serialized);

        EconomyProvider itemProvider = plugin.getEconomyProvider("item");
        if (itemProvider instanceof ItemEconomyProvider itemEcon) {
            itemEcon.loadReferenceItem();
        }
        plugin.reloadEconomyProviders();

        String itemDesc = held.hasItemMeta() && held.getItemMeta().hasDisplayName()
                ? held.getItemMeta().getDisplayName()
                : held.getType().name();
        sender.sendMessage(messages.get("setitem.success", "{item}", itemDesc));
        return true;
    }

    // ==================== /ss reload ====================

    private boolean handleReload(CommandSender sender) {
        Messages messages = plugin.getMessages();

        if (!sender.hasPermission("skullshop.admin")) {
            sender.sendMessage(messages.get("general.no_permission"));
            return true;
        }

        plugin.getConfigManager().reload();
        plugin.getMessages().reload();
        plugin.reloadEconomyProviders();
        sender.sendMessage(messages.get("general.reload"));
        return true;
    }

    // ==================== Helpers ====================

    private void sendHelp(CommandSender sender, Messages messages) {
        sender.sendMessage(messages.getNoPrefix("general.help_header",
                "{version}", plugin.getPluginMeta().getVersion()));
        sender.sendMessage("");
        sender.sendMessage(messages.getNoPrefix("general.help_buy"));
        sender.sendMessage(messages.getNoPrefix("general.help_buy_via"));
        sender.sendMessage(messages.getNoPrefix("general.help_price"));
        sender.sendMessage(messages.getNoPrefix("general.help_price_set"));
        sender.sendMessage(messages.getNoPrefix("general.help_currency"));
        sender.sendMessage(messages.getNoPrefix("general.help_allowother"));
        sender.sendMessage(messages.getNoPrefix("general.help_setitem"));
        sender.sendMessage(messages.getNoPrefix("general.help_reload"));
        sender.sendMessage("");
        sender.sendMessage(messages.getNoPrefix("general.help_author"));

        // Show current economy status
        Map<String, EconomyProvider> providers = plugin.getAvailableProviders();
        if (!providers.isEmpty()) {
            sender.sendMessage("");
            sender.sendMessage(messages.getNoPrefix("general.help_available"));
            for (Map.Entry<String, EconomyProvider> entry : providers.entrySet()) {
                double price = plugin.getConfigManager().getPrice(entry.getKey());
                sender.sendMessage("  §e" + entry.getValue().getDisplayName()
                        + " §7- " + formatPriceSimple(entry.getKey(), price));
            }
        }
    }

    private boolean isValidEconomyKey(String key) {
        return key.equals("vault") || key.equals("player_points") || key.equals("item");
    }

    private String formatPriceSimple(String key, double price) {
        return switch (key) {
            case "vault" -> String.format("%.2f", price);
            case "player_points" -> String.valueOf((int) price);
            case "item" -> (int) price + " × " + plugin.getConfigManager().getItemDisplayName();
            default -> String.valueOf(price);
        };
    }
}
