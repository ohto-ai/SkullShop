package com.ohtoai.headshop.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Economy provider using Vault (coins/money).
 * Uses lazy resolution — the Economy provider may register after Vault itself loads.
 */
public class VaultEconomyProvider implements EconomyProvider {

    private final String displayName;
    private Economy economy;

    public VaultEconomyProvider(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Lazily resolves the Vault economy provider.
     * Called every time isAvailable() or any transaction method is used,
     * so EssentialsX (or other economy plugins) that register after Vault
     * will still be discovered.
     */
    private Economy resolveEconomy() {
        if (economy != null && economy.isEnabled()) return economy;
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return null;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
        }
        return economy;
    }

    @Override
    public String getKey() {
        return "vault";
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean isAvailable() {
        Economy econ = resolveEconomy();
        return econ != null && econ.isEnabled();
    }

    @Override
    public String getUnavailableReason() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return "Vault 插件未安装";
        return "未找到经济插件 (EssentialsX等)";
    }

    @Override
    public boolean hasBalance(Player player, double amount) {
        Economy econ = resolveEconomy();
        if (econ == null) return false;
        return econ.has(player, amount);
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        Economy econ = resolveEconomy();
        if (econ == null) return false;
        return econ.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public boolean deposit(Player player, double amount) {
        Economy econ = resolveEconomy();
        if (econ == null) return false;
        return econ.depositPlayer(player, amount).transactionSuccess();
    }

    /**
     * Get a player's current balance.
     */
    public double getBalance(Player player) {
        Economy econ = resolveEconomy();
        if (econ == null) return 0;
        return econ.getBalance(player);
    }

    /**
     * Format a balance amount using the economy's currency format.
     */
    public String format(double amount) {
        Economy econ = resolveEconomy();
        if (econ != null) {
            return econ.format(amount);
        }
        return String.format("%.2f", amount);
    }
}
