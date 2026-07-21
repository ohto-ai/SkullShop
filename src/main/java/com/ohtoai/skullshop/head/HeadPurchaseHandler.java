package com.ohtoai.skullshop.head;

import com.ohtoai.skullshop.SkullShopPlugin;
import com.ohtoai.skullshop.config.Messages;
import com.ohtoai.skullshop.economy.EconomyProvider;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Coordinates direct (non-GUI) head purchases.
 * For GUI purchases, see {@link com.ohtoai.skullshop.gui.QuantityGui}.
 */
public class HeadPurchaseHandler {

    private final SkullShopPlugin plugin;
    private final Map<String, EconomyProvider> economyProviders;
    private final HeadFactory headFactory;

    public HeadPurchaseHandler(SkullShopPlugin plugin, Map<String, EconomyProvider> economyProviders) {
        this.plugin = plugin;
        this.economyProviders = economyProviders;
        this.headFactory = new HeadFactory(plugin);
    }

    /**
     * Direct purchase via command (no GUI).
     *
     * @param buyer      the player making the purchase
     * @param targetName the target player name (null = own head)
     * @param economyKey the economy to use (required for direct purchase)
     * @param quantity   number of heads to buy (1, 8, or 64)
     * @return true if successful
     */
    public boolean purchaseDirect(Player buyer, String targetName, String economyKey, int quantity) {
        Messages messages = plugin.getMessages();

        // Validate other-players setting
        if (targetName != null && !targetName.isEmpty()
                && !targetName.equalsIgnoreCase(buyer.getName())) {
            if (!plugin.getConfigManager().isAllowOtherPlayers()) {
                buyer.sendMessage(messages.get("purchase.other_players_disabled"));
                return false;
            }
        }

        // Get economy provider
        EconomyProvider provider = economyProviders.get(economyKey);
        if (provider == null) {
            buyer.sendMessage(messages.get("purchase.economy_disabled",
                    "{name}", economyKey));
            return false;
        }

        // Calculate price
        double unitPrice = plugin.getConfigManager().getPrice(economyKey);
        double totalPrice = unitPrice * quantity;

        // Check balance
        if (!provider.hasBalance(buyer, totalPrice)) {
            buyer.playSound(buyer.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
            buyer.sendMessage(messages.get("purchase.insufficient",
                    "{price}", formatPrice(economyKey, totalPrice),
                    "{balance}", "?",
                    "{name}", provider.getDisplayName()));
            return false;
        }

        // Create the head
        ItemStack head;
        String effectiveName = targetName;
        if (effectiveName == null || effectiveName.isEmpty()) {
            head = headFactory.createOwnHead(buyer);
            effectiveName = buyer.getName();
        } else {
            head = headFactory.createPlayerHead(targetName);
            if (head == null) {
                buyer.sendMessage(messages.get("purchase.player_not_found",
                        "{player}", targetName));
                return false;
            }
        }

        // Withdraw
        if (!provider.withdraw(buyer, totalPrice)) {
            buyer.sendMessage(messages.get("purchase.insufficient",
                    "{price}", formatPrice(economyKey, totalPrice),
                    "{balance}", "?",
                    "{name}", provider.getDisplayName()));
            return false;
        }

        // Give heads
        head.setAmount(1);
        int given = 0;
        for (int i = 0; i < quantity; i++) {
            ItemStack single = head.clone();
            var overflow = buyer.getInventory().addItem(single);
            if (!overflow.isEmpty()) {
                for (ItemStack drop : overflow.values()) {
                    buyer.getWorld().dropItemNaturally(buyer.getLocation(), drop);
                }
            }
            given++;
        }

        // Success
        buyer.playSound(buyer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
        buyer.sendMessage(messages.get("gui.purchase_success",
                "{player}", effectiveName,
                "{amount}", String.valueOf(given)));

        return true;
    }

    /**
     * Purchase a single head (legacy method for backward compat).
     */
    public boolean purchase(Player buyer, String targetName, String economyKey) {
        return purchaseDirect(buyer, targetName, economyKey, 1);
    }

    private String formatPrice(String economyKey, double price) {
        return switch (economyKey) {
            case "vault" -> String.format("%.2f", price);
            case "player_points" -> String.valueOf((int) price);
            case "item" -> (int) price + " × " + plugin.getConfigManager().getItemDisplayName();
            default -> String.valueOf(price);
        };
    }
}
