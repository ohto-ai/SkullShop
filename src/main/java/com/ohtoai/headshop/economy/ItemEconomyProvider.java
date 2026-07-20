package com.ohtoai.headshop.economy;

import com.ohtoai.headshop.HeadShopPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

/**
 * Economy provider using custom NBT items as currency.
 * Items are exactly matched via ItemStack.isSimilar().
 */
public class ItemEconomyProvider implements EconomyProvider {

    private final HeadShopPlugin plugin;
    private final String displayName;
    private ItemStack referenceItem;

    public ItemEconomyProvider(HeadShopPlugin plugin, String displayName) {
        this.plugin = plugin;
        this.displayName = displayName;
        loadReferenceItem();
    }

    /**
     * Load the reference item from config.
     */
    public void loadReferenceItem() {
        Map<String, Object> serialized = plugin.getConfigManager().getReferenceItem();
        if (serialized != null && !serialized.isEmpty()) {
            try {
                this.referenceItem = ItemStack.deserialize(serialized);
                plugin.getLogger().info("Item reference loaded: " + getDescription());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to deserialize reference item: " + e.getMessage()
                        + " — data keys: " + serialized.keySet());
                this.referenceItem = null;
            }
        } else {
            plugin.getLogger().info("Item reference: not set (map is "
                    + (serialized == null ? "null" : "empty") + ")");
            this.referenceItem = null;
        }
    }

    @Override
    public String getKey() {
        return "item";
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean isAvailable() {
        return referenceItem != null && referenceItem.getType() != Material.AIR;
    }

    @Override
    public String getUnavailableReason() {
        return "未设置参考物品 (请使用 /hs setitem)";
    }

    /**
     * Get a human-readable description of the reference item.
     */
    public String getDescription() {
        if (referenceItem == null) return "none";
        if (referenceItem.hasItemMeta() && referenceItem.getItemMeta().hasDisplayName()) {
            return referenceItem.getItemMeta().getDisplayName();
        }
        return referenceItem.getType().name();
    }

    /**
     * Get the reference item for display in GUIs.
     */
    public ItemStack getReferenceItem() {
        return referenceItem;
    }

    @Override
    public boolean hasBalance(Player player, double amount) {
        if (!isAvailable()) return false;
        int needed = (int) amount;
        int count = countMatchingItems(player);
        return count >= needed;
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        if (!isAvailable()) return false;
        int remaining = (int) amount;
        PlayerInventory inv = player.getInventory();

        for (int i = 0; i < inv.getSize() && remaining > 0; i++) {
            ItemStack item = inv.getItem(i);
            if (matches(item)) {
                int take = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - take);
                remaining -= take;
                if (item.getAmount() <= 0) {
                    inv.setItem(i, null);
                }
            }
        }

        return remaining <= 0;
    }

    @Override
    public boolean deposit(Player player, double amount) {
        if (!isAvailable()) return false;
        int give = (int) amount;
        ItemStack toGive = referenceItem.clone();
        toGive.setAmount(1);

        int given = 0;
        for (int i = 0; i < give; i++) {
            ItemStack single = referenceItem.clone();
            single.setAmount(1);
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(single);
            if (overflow.isEmpty()) {
                given++;
            } else {
                // Drop overflow on the ground
                for (ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                given++;
            }
        }
        return given > 0;
    }

    /**
     * Count the total number of matching items in a player's inventory.
     */
    public int countMatchingItems(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (matches(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Check if an ItemStack exactly matches the reference item via ItemStack.isSimilar().
     * This compares Material, durability, and all ItemMeta (display name, lore, enchantments,
     * custom model data, all NBT tags). Amount is NOT compared.
     */
    private boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (referenceItem == null) return false;
        return referenceItem.isSimilar(item);
    }
}
