// PlayerDeathListener.java - Corrigido
package github.dimazbtw.minas.listeners;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.data.PickaxeData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class PlayerDeathListener implements Listener {

    private final Main plugin;

    // Armazena jogadores que morreram em minas PvP para teleporte após respawn
    private final Map<UUID, Location> pendingRespawnTeleports;

    public PlayerDeathListener(Main plugin) {
        this.plugin = plugin;
        this.pendingRespawnTeleports = new HashMap<>();
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

        // Verificar se o jogador está em uma mina
        Mine victimMine = plugin.getSessionManager().getCurrentMine(victim);

        if (victimMine != null && victimMine.isPvpEnabled()) {
            // Morreu em mina com PvP ativado
            handlePvPDeath(victim, victimMine);
        }

        // Se foi morto por outro jogador, processar roubo de XP
        if (killer != null && killer != victim) {
            processXpSteal(victim, killer);
        }
    }

    /**
     * Trata morte em mina com PvP ativado
     */
    private void handlePvPDeath(Player victim, Mine mine) {
        plugin.getLogger().info("Jogador " + victim.getName() + " morreu em mina PvP: " + mine.getId());

        // Remover da sessão IMEDIATAMENTE
        plugin.getSessionManager().removeSession(victim);

        // Determinar localização de saída
        Location exitLocation = mine.getExit();
        if (exitLocation == null) {
            // Fallback: spawn do mundo
            exitLocation = victim.getWorld().getSpawnLocation();
            plugin.getLogger().warning("Mina " + mine.getId() + " não tem exit definido, usando spawn do mundo");
        }

        // Armazenar localização para teleporte após respawn
        pendingRespawnTeleports.put(victim.getUniqueId(), exitLocation);

        plugin.getLogger().info("Teleporte agendado para: " + exitLocation);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Verificar se tem teleporte pendente
        if (pendingRespawnTeleports.containsKey(uuid)) {
            Location exitLocation = pendingRespawnTeleports.remove(uuid);

            plugin.getLogger().info("Definindo respawn de " + player.getName() + " para: " + exitLocation);

            // Definir localização de respawn
            event.setRespawnLocation(exitLocation);

            // Enviar mensagem após 1 tick para garantir que o jogador já respawnou
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getLanguageManager().sendMessage(player, "mine.pvp-death-exit");
                }
            }, 1L);
        }
    }

    private void processXpSteal(Player victim, Player killer) {
        PickaxeData victimData = plugin.getDatabaseManager().getPickaxeData(victim.getUniqueId());

        if (victimData.getExp() <= 0 && victimData.getLevel() <= 1) {
            return;
        }

        int victimTotalXp = calculateTotalXp(victimData);

        if (victimTotalXp <= 0) {
            return;
        }

        double baseLossPercent = plugin.getConfig().getDouble("death.xp-loss-percent", 50.0);
        double lossReduction = calculateLossReduction(victim);
        double finalLossPercent = Math.max(0, baseLossPercent - lossReduction);

        int xpLost = (int) (victimTotalXp * (finalLossPercent / 100.0));

        if (xpLost <= 0) {
            return;
        }

        double stealPercent = plugin.getConfig().getDouble("death.xp-steal-percent", 50.0);
        int xpStolen = (int) (xpLost * (stealPercent / 100.0));

        removeXpFromPlayer(victimData, xpLost);
        plugin.getDatabaseManager().savePickaxeData(victimData);

        // Atualizar picareta com delay para garantir que o jogador respawnou
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (victim.isOnline()) {
                plugin.getPickaxeManager().updatePickaxe(victim);
            }
        }, 5L);

        if (xpStolen > 0) {
            PickaxeData killerData = plugin.getDatabaseManager().getPickaxeData(killer.getUniqueId());
            killerData.addExp(xpStolen);
            plugin.getDatabaseManager().savePickaxeData(killerData);
            plugin.getPickaxeManager().updatePickaxe(killer);

            String xpLostFormatted = formatNumber(xpLost);
            String xpStolenFormatted = formatNumber(xpStolen);

            // Enviar mensagens com delay
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (victim.isOnline()) {
                    victim.sendMessage(plugin.getLanguageManager().getMessage(victim, "death.xp-lost")
                            .replace("{amount}", xpLostFormatted)
                            .replace("{percent}", String.format("%.1f", finalLossPercent))
                            .replace("{killer}", killer.getName()));
                }

                if (killer.isOnline()) {
                    killer.sendMessage(plugin.getLanguageManager().getMessage(killer, "death.xp-stolen")
                            .replace("{amount}", xpStolenFormatted)
                            .replace("{victim}", victim.getName()));
                }
            }, 10L);
        }
    }

    private double calculateLossReduction(Player player) {
        double totalReduction = 0;

        for (String permission : plugin.getConfig().getStringList("death.loss-reduction-permissions")) {
            if (player.hasPermission(permission)) {
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

        return Math.min(100, totalReduction);
    }

    private int calculateTotalXp(PickaxeData data) {
        int total = data.getExp();

        for (int i = 1; i < data.getLevel(); i++) {
            if (i <= 100) {
                total += i * 100;
            } else if (i <= 500) {
                total += 10000 + ((i - 100) * 150);
            } else if (i <= 1000) {
                total += 70000 + ((i - 500) * 200);
            } else {
                total += 170000 + ((i - 1000) * 250);
            }
        }

        return total;
    }

    private void removeXpFromPlayer(PickaxeData data, int amount) {
        int currentXp = data.getExp();

        if (currentXp >= amount) {
            data.setExp(currentXp - amount);
            return;
        }

        int remaining = amount - currentXp;
        data.setExp(0);

        while (remaining > 0 && data.getLevel() > 1) {
            int previousLevel = data.getLevel() - 1;
            int xpForPreviousLevel;

            if (previousLevel <= 100) {
                xpForPreviousLevel = previousLevel * 100;
            } else if (previousLevel <= 500) {
                xpForPreviousLevel = 10000 + ((previousLevel - 100) * 150);
            } else if (previousLevel <= 1000) {
                xpForPreviousLevel = 70000 + ((previousLevel - 500) * 200);
            } else {
                xpForPreviousLevel = 170000 + ((previousLevel - 1000) * 250);
            }

            if (remaining >= xpForPreviousLevel) {
                remaining -= xpForPreviousLevel;
                data.setLevel(previousLevel);
            } else {
                data.setLevel(previousLevel);
                data.setExp(xpForPreviousLevel - remaining);
                remaining = 0;
            }
        }

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