package github.dimazbtw.minas.managers;

import github.dimazbtw.minas.Main;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultManager {

    private final Main plugin;
    private Economy economy;
    private boolean economyEnabled;

    public VaultManager(Main plugin) {
        this.plugin = plugin;
        this.economyEnabled = false;
        setupEconomy();
    }

    /**
     * Configura o hook com o Vault
     */
    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault não encontrado! Sistema de economia desabilitado.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("Nenhum plugin de economia encontrado! Sistema de economia desabilitado.");
            return;
        }

        economy = rsp.getProvider();
        economyEnabled = true;
        plugin.getLogger().info("§aVault conectado com sucesso! Economia: " + economy.getName());
    }

    /**
     * Verifica se a economia está habilitada
     */
    public boolean isEconomyEnabled() {
        return economyEnabled && economy != null;
    }

    /**
     * Obtém o saldo de um jogador
     */
    public double getBalance(Player player) {
        if (!isEconomyEnabled()) return 0.0;
        return economy.getBalance(player);
    }

    /**
     * Adiciona dinheiro a um jogador
     */
    public boolean depositPlayer(Player player, double amount) {
        if (!isEconomyEnabled() || amount <= 0) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    /**
     * Remove dinheiro de um jogador
     */
    public boolean withdrawPlayer(Player player, double amount) {
        if (!isEconomyEnabled() || amount <= 0) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    /**
     * Verifica se o jogador tem saldo suficiente
     */
    public boolean hasBalance(Player player, double amount) {
        if (!isEconomyEnabled()) return false;
        return economy.has(player, amount);
    }

    /**
     * Define o saldo de um jogador
     */
    public boolean setBalance(Player player, double amount) {
        if (!isEconomyEnabled()) return false;

        double current = getBalance(player);
        if (current > amount) {
            return withdrawPlayer(player, current - amount);
        } else {
            return depositPlayer(player, amount - current);
        }
    }

    /**
     * Formata um valor monetário
     */
    public String format(double amount) {
        if (!isEconomyEnabled()) return String.format("%.2f", amount);
        return economy.format(amount);
    }

    /**
     * Obtém o nome da moeda (plural)
     */
    public String getCurrencyNamePlural() {
        if (!isEconomyEnabled()) return "moedas";
        return economy.currencyNamePlural();
    }

    /**
     * Obtém o nome da moeda (singular)
     */
    public String getCurrencyNameSingular() {
        if (!isEconomyEnabled()) return "moeda";
        return economy.currencyNameSingular();
    }

    /**
     * Adiciona experiência a um jogador
     */
    public void giveExp(Player player, int amount) {
        if (amount <= 0) return;
        player.giveExp(amount);
    }

    /**
     * Remove experiência de um jogador
     */
    public void takeExp(Player player, int amount) {
        if (amount <= 0) return;

        int current = player.getTotalExperience();
        player.setTotalExperience(Math.max(0, current - amount));
    }

    /**
     * Obtém a experiência total de um jogador
     */
    public int getTotalExp(Player player) {
        return player.getTotalExperience();
    }

    /**
     * Define a experiência total de um jogador
     */
    public void setTotalExp(Player player, int amount) {
        player.setTotalExperience(Math.max(0, amount));
    }
}