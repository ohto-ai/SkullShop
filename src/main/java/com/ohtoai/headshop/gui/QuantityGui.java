package com.ohtoai.headshop.gui;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.ohtoai.headshop.HeadShopPlugin;
import com.ohtoai.headshop.config.Messages;
import com.ohtoai.headshop.economy.EconomyProvider;
import com.ohtoai.headshop.economy.ItemEconomyProvider;
import com.ohtoai.headshop.economy.PlayerPointsProvider;
import com.ohtoai.headshop.economy.VaultEconomyProvider;
import com.ohtoai.headshop.head.HeadFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for selecting purchase quantity (x1, x8, x64).
 * Shows currency type indicator at top, quantity buttons with target head skin,
 * and a back button to return to payment selection.
 * GUI stays open after purchase for continuous buying.
 */
public class QuantityGui {

    private static final int CURRENCY_SLOT = 4;
    private static final int[] QUANTITY_SLOTS = {11, 13, 15};
    private static final int[] QUANTITIES = {1, 8, 64};
    private static final int BACK_SLOT = 22;

    private final HeadShopPlugin plugin;
    private final Player player;
    private final String targetName;
    private final String economyKey;
    private final HeadFactory headFactory;
    private final PaymentGui parent;
    private final Inventory inventory;
    private PlayerProfile resolvedProfile;

    public QuantityGui(HeadShopPlugin plugin, Player player, String targetName,
                       String economyKey, HeadFactory headFactory, PaymentGui parent) {
        this.plugin = plugin;
        this.player = player;
        this.targetName = targetName;
        this.economyKey = economyKey;
        this.headFactory = headFactory;
        this.parent = parent;

        String title = plugin.getMessages().getNoPrefix("gui.quantity_title",
                "{player}", targetName);
        if (title.length() > 32) title = title.substring(0, 32);
        this.inventory = Bukkit.createInventory(player, 27, title);

        buildStatic();
        startProfileLoad();
    }

    public void open() {
        plugin.registerGui(inventory, this);
        player.openInventory(inventory);
    }

    /**
     * Build static elements: currency indicator, back button.
     */
    private void buildStatic() {
        // Currency indicator at top
        EconomyProvider provider = plugin.getEconomyProvider(economyKey);
        double price = plugin.getConfigManager().getPrice(economyKey);

        Material icon = switch (economyKey) {
            case "vault" -> Material.GOLD_INGOT;
            case "player_points" -> Material.EMERALD;
            case "item" -> {
                if (provider instanceof ItemEconomyProvider ie) {
                    ItemStack ref = ie.getReferenceItem();
                    if (ref != null) yield ref.getType();
                }
                yield Material.DIAMOND;
            }
            default -> Material.PAPER;
        };

        ItemStack currencyItem = new ItemStack(icon);
        ItemMeta cm = currencyItem.getItemMeta();
        cm.setDisplayName("§e" + provider.getDisplayName() + " §7- " + formatPrice(price));
        currencyItem.setItemMeta(cm);
        inventory.setItem(CURRENCY_SLOT, currencyItem);

        // Back button
        ItemStack backBtn = new ItemStack(Material.ARROW);
        ItemMeta bm = backBtn.getItemMeta();
        bm.setDisplayName("§c◀ 返回");
        backBtn.setItemMeta(bm);
        inventory.setItem(BACK_SLOT, backBtn);
    }

    /**
     * Build quantity buttons with price info.
     */
    private void buildQuantityButtons() {
        Messages messages = plugin.getMessages();
        EconomyProvider provider = plugin.getEconomyProvider(economyKey);
        double unitPrice = plugin.getConfigManager().getPrice(economyKey);

        for (int i = 0; i < QUANTITY_SLOTS.length; i++) {
            int qty = QUANTITIES[i];
            double totalPrice = unitPrice * qty;
            boolean canAfford = provider != null && provider.hasBalance(player, totalPrice);

            ItemStack button;
            if (resolvedProfile != null) {
                button = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta sm = (SkullMeta) button.getItemMeta();
                sm.setPlayerProfile(resolvedProfile);
                sm.setDisplayName("§a×" + qty + " " + targetName + "'s Head");
                List<String> lore = new ArrayList<>();
                lore.add(messages.getNoPrefix("gui.price_lore",
                        "{price}", formatPrice(unitPrice)));
                lore.add(messages.getNoPrefix("gui.total_price_lore",
                        "{total}", formatPrice(totalPrice)));
                lore.add("");
                lore.add(messages.getNoPrefix("gui.balance_lore",
                        "{balance}", formatBalance(provider)));
                if (!canAfford) {
                    lore.add("");
                    lore.add(messages.getNoPrefix("gui.insufficient"));
                } else {
                    lore.add("");
                    lore.add(messages.getNoPrefix("gui.click_to_buy"));
                }
                sm.setLore(lore);
                button.setItemMeta(sm);
            } else {
                button = new ItemStack(canAfford ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
                ItemMeta meta = button.getItemMeta();
                meta.setDisplayName("§a×" + qty);
                List<String> lore = new ArrayList<>();
                lore.add(messages.getNoPrefix("gui.price_lore",
                        "{price}", formatPrice(unitPrice)));
                lore.add(messages.getNoPrefix("gui.total_price_lore",
                        "{total}", formatPrice(totalPrice)));
                lore.add("");
                lore.add("§7加载中...");
                meta.setLore(lore);
                button.setItemMeta(meta);
            }

            inventory.setItem(QUANTITY_SLOTS[i], button);
        }
    }

    private void startProfileLoad() {
        if (targetName.equals(player.getName())) {
            resolvedProfile = player.getPlayerProfile();
            buildQuantityButtons();
            return;
        }

        headFactory.resolveProfileAsync(targetName).thenAccept(profile -> {
            if (profile != null) {
                resolvedProfile = profile;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.getOpenInventory().getTopInventory().equals(inventory)) {
                        buildQuantityButtons();
                    }
                });
            }
        });
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        // Back button
        if (slot == BACK_SLOT) {
            parent.open();
            return;
        }

        // Quantity buttons
        int quantity = -1;
        for (int i = 0; i < QUANTITY_SLOTS.length; i++) {
            if (slot == QUANTITY_SLOTS[i]) {
                quantity = QUANTITIES[i];
                break;
            }
        }
        if (quantity < 0) return;

        // Perform purchase
        EconomyProvider provider = plugin.getEconomyProvider(economyKey);
        if (provider == null) {
            player.sendMessage(plugin.getMessages().get("purchase.economy_disabled",
                    "{name}", economyKey));
            return;
        }

        double unitPrice = plugin.getConfigManager().getPrice(economyKey);
        double totalPrice = unitPrice * quantity;

        if (!provider.hasBalance(player, totalPrice)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
            player.sendMessage(plugin.getMessages().get("purchase.insufficient",
                    "{price}", formatPrice(totalPrice),
                    "{balance}", formatBalance(provider),
                    "{name}", provider.getDisplayName()));
            return;
        }

        if (resolvedProfile == null) {
            player.sendMessage("§c头像数据还在加载中，请稍后再试。");
            return;
        }

        // Withdraw
        if (!provider.withdraw(player, totalPrice)) {
            player.sendMessage(plugin.getMessages().get("purchase.insufficient",
                    "{price}", formatPrice(totalPrice),
                    "{balance}", formatBalance(provider),
                    "{name}", provider.getDisplayName()));
            return;
        }

        // Give heads
        ItemStack head = headFactory.createHeadFromProfile(resolvedProfile, targetName);
        head.setAmount(1);
        int given = 0;
        for (int i = 0; i < quantity; i++) {
            ItemStack single = head.clone();
            var overflow = player.getInventory().addItem(single);
            if (!overflow.isEmpty()) {
                for (ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
            given++;
        }

        // Success feedback
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
        player.sendMessage(plugin.getMessages().get("gui.purchase_success",
                "{player}", targetName,
                "{amount}", String.valueOf(given)));

        // Refresh buttons with updated balance — GUI stays open
        buildQuantityButtons();
    }

    private String formatPrice(double price) {
        return switch (economyKey) {
            case "vault" -> String.format("%.2f", price);
            case "player_points" -> String.valueOf((int) price);
            case "item" -> (int) price + " × " + plugin.getConfigManager().getItemDisplayName();
            default -> String.valueOf(price);
        };
    }

    private String formatBalance(EconomyProvider provider) {
        if (provider instanceof VaultEconomyProvider vault) {
            return vault.format(vault.getBalance(player));
        } else if (provider instanceof PlayerPointsProvider points) {
            return String.valueOf(points.look(player));
        } else if (provider instanceof ItemEconomyProvider item) {
            return String.valueOf(item.countMatchingItems(player));
        }
        return "?";
    }
}
