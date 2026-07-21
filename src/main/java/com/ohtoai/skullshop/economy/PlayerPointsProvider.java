package com.ohtoai.skullshop.economy;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Economy provider using PlayerPoints (coupons/points).
 * Uses lazy resolution for robustness.
 */
public class PlayerPointsProvider implements EconomyProvider {

    private final String displayName;
    private PlayerPointsAPI api;

    public PlayerPointsProvider(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Lazily resolves the PlayerPoints API.
     */
    private PlayerPointsAPI resolveApi() {
        if (api != null) return api;
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (plugin instanceof PlayerPoints) {
            api = ((PlayerPoints) plugin).getAPI();
        }
        return api;
    }

    @Override
    public String getKey() {
        return "player_points";
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean isAvailable() {
        return resolveApi() != null;
    }

    @Override
    public String getUnavailableReason() {
        return "PlayerPoints 插件未安装";
    }

    @Override
    public boolean hasBalance(Player player, double amount) {
        PlayerPointsAPI resolved = resolveApi();
        if (resolved == null) return false;
        return resolved.look(player.getUniqueId()) >= (int) amount;
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        PlayerPointsAPI resolved = resolveApi();
        if (resolved == null) return false;
        return resolved.take(player.getUniqueId(), (int) amount);
    }

    @Override
    public boolean deposit(Player player, double amount) {
        PlayerPointsAPI resolved = resolveApi();
        if (resolved == null) return false;
        return resolved.give(player.getUniqueId(), (int) amount);
    }

    /**
     * Get the player's current points balance.
     */
    public int look(Player player) {
        PlayerPointsAPI resolved = resolveApi();
        if (resolved == null) return 0;
        return resolved.look(player.getUniqueId());
    }
}
