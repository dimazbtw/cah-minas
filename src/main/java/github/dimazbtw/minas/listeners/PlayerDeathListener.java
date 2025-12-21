package github.dimazbtw.minas.listeners;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.PickaxeData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;

public class PlayerDeathListener implements Listener {

    private final Main plugin;

    public PlayerDeathListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Remover picareta dos drops
        Iterator<ItemStack> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (plugin.getPickaxeManager().isMinePickaxe(item)) {
                iterator.remove();
            }
        }

        // Se foi morto por outro jogador, processar roubo de XP
        if (killer != null && killer != victim) {
            processXpSteal(victim, killer);
        }
    }

    private void processXpSteal(Player victim, Player killer) {
        PickaxeData victimData = plugin.getDatabaseManager().getPickaxeData(victim.getUniqueId());

        // Verificar se a vítima tem XP
        if (victimData.getExp() <= 0 && victimData.getLevel() <= 1) {
            return;
        }

        // Calcular XP total da vítima
        int victimTotalXp = calculateTotalXp(victimData);

        if (victimTotalXp <= 0) {
            return;
        }

        // Obter porcentagem base de perda (configurável)
        double baseLossPercent = plugin.getConfig().getDouble("death.xp-loss-percent", 50.0);

        // Calcular redução de perda baseado em permissões
        double lossReduction = calculateLossReduction(victim);

        // Porcentagem final de perda
        double finalLossPercent = Math.max(0, baseLossPercent - lossReduction);

        // Calcular XP perdido
        int xpLost = (int) (victimTotalXp * (finalLossPercent / 100.0));

        if (xpLost <= 0) {
            return;
        }

        // Obter porcentagem que o killer rouba (configurável)
        double stealPercent = plugin.getConfig().getDouble("death.xp-steal-percent", 50.0);
        int xpStolen = (int) (xpLost * (stealPercent / 100.0));

        // Remover XP da vítima
        removeXpFromPlayer(victimData, xpLost);
        plugin.getDatabaseManager().savePickaxeData(victimData);
        plugin.getPickaxeManager().updatePickaxe(victim);

        // Adicionar XP ao killer
        if (xpStolen > 0) {
            PickaxeData killerData = plugin.getDatabaseManager().getPickaxeData(killer.getUniqueId());
            killerData.addExp(xpStolen);
            plugin.getDatabaseManager().savePickaxeData(killerData);
            plugin.getPickaxeManager().updatePickaxe(killer);

            // Mensagens
            String xpLostFormatted = formatNumber(xpLost);
            String xpStolenFormatted = formatNumber(xpStolen);

            victim.sendMessage(plugin.getLanguageManager().getMessage(victim, "death.xp-lost")
                    .replace("{amount}", xpLostFormatted)
                    .replace("{percent}", String.format("%.1f", finalLossPercent))
                    .replace("{killer}", killer.getName()));

            killer.sendMessage(plugin.getLanguageManager().getMessage(killer, "death.xp-stolen")
                    .replace("{amount}", xpStolenFormatted)
                    .replace("{victim}", victim.getName()));
        }
    }

    /**
     * Calcula a redução de perda baseado em permissões
     * Formato: utils.vip:10 = 10% de redução
     */
    private double calculateLossReduction(Player player) {
        double totalReduction = 0;

        // Verificar permissões configuradas
        for (String permission : plugin.getConfig().getStringList("death.loss-reduction-permissions")) {
            if (player.hasPermission(permission)) {
                // Formato: "utils.vip:10"
                String[] parts = permission.split(":");
                if (parts.length == 2) {
                    try {
                        double reduction = Double.parseDouble(parts[1]);
                        totalReduction += reduction;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        // Limitar a 100% de redução (não pode perder nada)
        return Math.min(100, totalReduction);
    }

    /**
     * Calcula o XP total do jogador (considerando níveis)
     */
    private int calculateTotalXp(PickaxeData data) {
        int total = data.getExp();

        // Somar XP de todos os níveis anteriores
        for (int i = 1; i < data.getLevel(); i++) {
            total += i * 100; // Fórmula: level * 100
        }

        return total;
    }

    /**
     * Remove XP do jogador, podendo descer de nível
     */
    private void removeXpFromPlayer(PickaxeData data, int amount) {
        int currentXp = data.getExp();

        // Se o XP atual cobre a perda
        if (currentXp >= amount) {
            data.setExp(currentXp - amount);
            return;
        }

        // Precisa descer de nível
        int remaining = amount - currentXp;
        data.setExp(0);

        while (remaining > 0 && data.getLevel() > 1) {
            int previousLevel = data.getLevel() - 1;
            int xpForPreviousLevel = previousLevel * 100;

            if (remaining >= xpForPreviousLevel) {
                // Desce mais um nível
                remaining -= xpForPreviousLevel;
                data.setLevel(previousLevel);
            } else {
                // Para neste nível com XP restante
                data.setLevel(previousLevel);
                data.setExp(xpForPreviousLevel - remaining);
                remaining = 0;
            }
        }

        // Garantir que não fique negativo
        if (data.getLevel() < 1) {
            data.setLevel(1);
            data.setExp(0);
        }
    }

    private String formatNumber(int number) {
        if (number >= 1_000_000_000) {
            return String.format("%.1fB", number / 1_000_000_000.0);
        } else if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }
}