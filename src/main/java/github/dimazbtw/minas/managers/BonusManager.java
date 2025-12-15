package github.dimazbtw.minas.managers;

import github.dimazbtw.minas.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;

public class BonusManager {

    private final Main plugin;
    private final Map<String, Double> permissionBonus;

    public BonusManager(Main plugin) {
        this.plugin = plugin;
        this.permissionBonus = new LinkedHashMap<>();
        loadBonus();
    }

    public void loadBonus() {
        permissionBonus.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("bonus");
        if (section != null) {
            for (String permission : section.getKeys(false)) {
                double bonus = section.getDouble(permission, 0);
                permissionBonus.put(permission, bonus);
                plugin.getLogger().info("Bónus carregado: " + permission + " = +" + bonus + "%");
            }
        }

        plugin.getLogger().info("Total de bónus por permissão: " + permissionBonus.size());
    }

    public void reload() {
        loadBonus();
    }

    /**
     * Calcula o bónus total de permissões do jogador (em percentagem)
     * Exemplo: Se tem vip.bonus (5%) e rank.deus (20%), retorna 25.0
     */
    public double getPermissionBonusPercent(Player player) {
        double totalBonus = 0;

        for (Map.Entry<String, Double> entry : permissionBonus.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                totalBonus += entry.getValue();
            }
        }

        return totalBonus;
    }

    /**
     * Calcula o multiplicador de permissões do jogador
     * Exemplo: Se tem 25% de bónus, retorna 1.25
     */
    public double getPermissionMultiplier(Player player) {
        return 1.0 + (getPermissionBonusPercent(player) / 100.0);
    }

    /**
     * Calcula o multiplicador total (skin + permissões)
     * @param player Jogador
     * @param skinOrder Ordem da skin ativa
     * @return Multiplicador total
     */
    public double getTotalMultiplier(Player player, int skinOrder) {
        // Bónus da skin (já é um multiplicador, ex: 1.5 = +50%)
        double skinBonus = plugin.getSkinManager().getSkinBonus(skinOrder);

        // Bónus de permissões (em percentagem convertido para multiplicador)
        double permissionMultiplier = getPermissionMultiplier(player);

        // Acumula: (skinBonus - 1) + (permissionMultiplier - 1) + 1
        // Exemplo: skin 1.5 (+50%) + permissões 1.25 (+25%) = 1.75 (+75%)
        return 1.0 + (skinBonus - 1.0) + (permissionMultiplier - 1.0);
    }

    /**
     * Obtém detalhes do bónus para exibição
     */
    public String getBonusDetails(Player player, int skinOrder) {
        StringBuilder details = new StringBuilder();

        // Skin
        double skinBonus = plugin.getSkinManager().getSkinBonus(skinOrder);
        double skinPercent = (skinBonus - 1.0) * 100;
        if (skinPercent > 0) {
            details.append("&d Skin: &a+").append(formatPercent(skinPercent)).append("%\n");
        }

        // Permissões
        for (Map.Entry<String, Double> entry : permissionBonus.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                details.append("&e ").append(entry.getKey()).append(": &a+").append(formatPercent(entry.getValue())).append("%\n");
            }
        }

        // Total
        double total = getTotalMultiplier(player, skinOrder);
        double totalPercent = (total - 1.0) * 100;
        details.append("&f Total: &a+").append(formatPercent(totalPercent)).append("%");

        return details.toString();
    }

    private String formatPercent(double value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        }
        return String.format("%.1f", value);
    }

    /**
     * Obtém todos os bónus configurados
     */
    public Map<String, Double> getAllBonus() {
        return new LinkedHashMap<>(permissionBonus);
    }
}