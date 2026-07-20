package com.ohtoai.headshop.gui;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.ohtoai.headshop.HeadShopPlugin;
import com.ohtoai.headshop.config.Messages;
import com.ohtoai.headshop.economy.EconomyProvider;
import com.ohtoai.headshop.economy.ItemEconomyProvider;
import com.ohtoai.headshop.head.HeadFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GUI for selecting a payment method.
 * Shows target head preview at top (slot 4) and payment options (slots 11/13/15).
 */
public class PaymentGui {

    private static final int PREVIEW_SLOT = 4;
    private static final int[] PAYMENT_SLOTS = {11, 13, 15};

    private final Player player;
    private final String targetName;
    private final Inventory inventory;
    private final HeadShopPlugin plugin;
    private final HeadFactory headFactory;

    public PaymentGui(HeadShopPlugin plugin, Player player, String targetName) {
        this.plugin = plugin;
        this.player = player;
        this.targetName = targetName;
        this.headFactory = new HeadFactory(plugin);
        this.inventory = Bukkit.createInventory(player, 27,
                plugin.getMessages().getNoPrefix("gui.payment_title"));
        build();
        startProfileLoad();
    }

    public void open() {
        plugin.registerGui(inventory, this);
        player.openInventory(inventory);
    }

    private void build() {
        Messages messages = plugin.getMessages();
        inventory.clear();

        // Preview head (loading placeholder)
        ItemStack placeholder = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) placeholder.getItemMeta();
        sm.setDisplayName("§e" + targetName + " §7(加载中...)");
        placeholder.setItemMeta(sm);
        inventory.setItem(PREVIEW_SLOT, placeholder);

        // Payment options
        Map<String, EconomyProvider> providers = plugin.getAvailableProviders();
        int i = 0;
        for (Map.Entry<String, EconomyProvider> entry : providers.entrySet()) {
            String key = entry.getKey();
            EconomyProvider provider = entry.getValue();
            double price = plugin.getConfigManager().getPrice(key);
            String priceStr = formatPrice(key, price);

            Material icon = getIcon(key);
            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§r" + provider.getDisplayName());

            List<String> lore = new ArrayList<>();
            lore.add(messages.getNoPrefix("gui.price_lore", "{price}", priceStr));
            lore.add("");
            lore.add(messages.getNoPrefix("gui.click_to_select"));
            meta.setLore(lore);
            item.setItemMeta(meta);

            inventory.setItem(PAYMENT_SLOTS[i], item);
            i++;
        }
    }

    private void startProfileLoad() {
        if (targetName.equals(player.getName())) {
            // Own head — immediate
            updatePreview(player.getPlayerProfile());
            return;
        }

        headFactory.resolveProfileAsync(targetName).thenAccept(profile -> {
            if (profile != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.getOpenInventory().getTopInventory().equals(inventory)) {
                        updatePreview(profile);
                    }
                });
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    ItemStack error = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta sm = (SkullMeta) error.getItemMeta();
                    sm.setDisplayName("§c" + targetName + " §7(未找到)");
                    error.setItemMeta(sm);
                    inventory.setItem(PREVIEW_SLOT, error);
                });
            }
        });
    }

    private void updatePreview(PlayerProfile profile) {
        ItemStack preview = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) preview.getItemMeta();
        sm.setPlayerProfile(profile);
        sm.setDisplayName("§e" + targetName);
        preview.setItemMeta(sm);
        inventory.setItem(PREVIEW_SLOT, preview);
    }

    private Material getIcon(String key) {
        return switch (key) {
            case "vault" -> Material.GOLD_INGOT;
            case "player_points" -> Material.EMERALD;
            case "item" -> {
                EconomyProvider p = plugin.getEconomyProvider("item");
                if (p instanceof ItemEconomyProvider item) {
                    ItemStack ref = item.getReferenceItem();
                    if (ref != null) yield ref.getType();
                }
                yield Material.DIAMOND;
            }
            default -> Material.PAPER;
        };
    }

    private String formatPrice(String key, double price) {
        return switch (key) {
            case "vault" -> String.format("%.2f", price);
            case "player_points" -> String.valueOf((int) price);
            case "item" -> (int) price + " × " + plugin.getConfigManager().getItemDisplayName();
            default -> String.valueOf(price);
        };
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        Map<String, EconomyProvider> providers = plugin.getAvailableProviders();
        int i = 0;
        for (String key : providers.keySet()) {
            if (slot == PAYMENT_SLOTS[i]) {
                QuantityGui quantityGui = new QuantityGui(plugin, player, targetName, key, headFactory, this);
                quantityGui.open();
                return;
            }
            i++;
        }
    }
}
