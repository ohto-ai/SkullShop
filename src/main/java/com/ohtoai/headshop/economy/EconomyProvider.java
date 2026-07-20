package com.ohtoai.headshop.economy;

import org.bukkit.entity.Player;

/**
 * Abstract economy provider interface.
 * Each implementation handles a different payment method.
 */
public interface EconomyProvider {

    /**
     * Unique key identifying this provider type.
     * @return "vault", "player_points", or "item"
     */
    String getKey();

    /**
     * Human-readable display name shown in messages.
     */
    String getDisplayName();

    /**
     * Whether this provider is available and ready to use.
     */
    boolean isAvailable();

    /**
     * If unavailable, returns a human-readable reason.
     * e.g. "Vault 插件未安装", "未设置参考物品"
     */
    default String getUnavailableReason() {
        return "未知原因";
    }

    boolean hasBalance(Player player, double amount);
    boolean withdraw(Player player, double amount);
    boolean deposit(Player player, double amount);
}
