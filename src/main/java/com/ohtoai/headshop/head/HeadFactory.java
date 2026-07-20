package com.ohtoai.headshop.head;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.ohtoai.headshop.HeadShopPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class for creating player head ItemStacks using Paper API.
 * Supports both joined players and never-joined players (via Mojang API).
 */
public class HeadFactory {

    private final HeadShopPlugin plugin;

    public HeadFactory(HeadShopPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Resolve a PlayerProfile for any player name, including never-joined players.
     * Blocking call — uses Mojang API for offline/never-joined players.
     *
     * @param playerName the player name
     * @return PlayerProfile or null if resolution fails
     */
    public PlayerProfile resolveProfile(String playerName) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);

        // Online player — get complete profile directly
        if (offline.isOnline()) {
            return offline.getPlayer().getPlayerProfile();
        }

        // Has played before — create profile with known UUID
        if (offline.hasPlayedBefore()) {
            PlayerProfile p = Bukkit.createProfile(offline.getUniqueId(), playerName);
            if (p != null && p.complete(true, true)) {
                return p;
            }
            return null;
        }

        // Never joined — create profile with random UUID, fetch from Mojang
        PlayerProfile p = Bukkit.createProfile(UUID.randomUUID(), playerName);
        if (p != null && p.complete(true, true)) {
            return p;
        }
        return null;
    }

    /**
     * Asynchronously resolve a PlayerProfile for any player name.
     * Returns a CompletableFuture that completes with the profile or null.
     */
    public CompletableFuture<PlayerProfile> resolveProfileAsync(String playerName) {
        CompletableFuture<PlayerProfile> future = new CompletableFuture<>();

        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);

        // Online player — immediate result
        if (offline.isOnline()) {
            future.complete(offline.getPlayer().getPlayerProfile());
            return future;
        }

        // Offline or never-joined — resolve async
        UUID uuid = offline.hasPlayedBefore() ? offline.getUniqueId() : UUID.randomUUID();
        PlayerProfile profile = Bukkit.createProfile(uuid, playerName);
        if (profile == null) {
            future.complete(null);
            return future;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                boolean completed = profile.complete(true, false);
                if (!completed || !profile.isComplete()) {
                    Bukkit.getScheduler().runTask(plugin, () -> future.complete(null));
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, () -> future.complete(profile));
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to resolve profile for " + playerName + ": " + ex.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> future.complete(null));
            }
        });

        return future;
    }

    /**
     * Create a head item for the given online player (their own skin).
     */
    public ItemStack createOwnHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setPlayerProfile(player.getPlayerProfile());
        meta.setDisplayName("§r" + player.getName() + "'s Head");
        head.setItemMeta(meta);
        return head;
    }

    /**
     * Create a head item from a resolved PlayerProfile.
     */
    public ItemStack createHeadFromProfile(PlayerProfile profile, String displayName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setPlayerProfile(profile);
        meta.setDisplayName("§r" + displayName + "'s Head");
        head.setItemMeta(meta);
        return head;
    }

    /**
     * Create a head item for a player by name (blocking).
     *
     * @param playerName the target player name
     * @return the head ItemStack, or null if profile resolution fails
     */
    public ItemStack createPlayerHead(String playerName) {
        PlayerProfile profile = resolveProfile(playerName);
        if (profile == null) return null;
        return createHeadFromProfile(profile, playerName);
    }

    /**
     * Asynchronously create a head for a player by name.
     */
    public CompletableFuture<ItemStack> createPlayerHeadAsync(String playerName) {
        return resolveProfileAsync(playerName).thenApply(profile -> {
            if (profile == null) return null;
            return createHeadFromProfile(profile, playerName);
        });
    }
}
