package github.dimazbtw.minas.managers;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.events.PlayerJoinMineEvent;
import github.dimazbtw.minas.events.PlayerQuitMineEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gerencia o rastreamento de jogadores dentro das minas
 */
public class PlayerLocationTracker {

    private final Main plugin;
    private final Map<UUID, Mine> playersInMines;

    public PlayerLocationTracker(Main plugin) {
        this.plugin = plugin;
        this.playersInMines = new HashMap<>();
    }

    /**
     * Atualiza a localização de um jogador e dispara eventos se necessário
     */
    public void updatePlayerLocation(Player player) {
        UUID uuid = player.getUniqueId();
        Mine currentMine = plugin.getMineManager().getMineAt(player.getLocation());
        Mine previousMine = playersInMines.get(uuid);

        // Jogador saiu de uma mina
        if (previousMine != null && currentMine == null) {
            playersInMines.remove(uuid);

            PlayerQuitMineEvent event = new PlayerQuitMineEvent(player, previousMine);
            Bukkit.getPluginManager().callEvent(event);
        }
        // Jogador entrou em uma mina
        else if (previousMine == null && currentMine != null) {
            PlayerJoinMineEvent event = new PlayerJoinMineEvent(player, currentMine);
            Bukkit.getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                playersInMines.put(uuid, currentMine);
            }
        }
        // Jogador mudou de mina
        else if (previousMine != null && currentMine != null && !previousMine.equals(currentMine)) {
            PlayerQuitMineEvent quitEvent = new PlayerQuitMineEvent(player, previousMine);
            Bukkit.getPluginManager().callEvent(quitEvent);

            PlayerJoinMineEvent joinEvent = new PlayerJoinMineEvent(player, currentMine);
            Bukkit.getPluginManager().callEvent(joinEvent);

            if (!joinEvent.isCancelled()) {
                playersInMines.put(uuid, currentMine);
            } else {
                playersInMines.remove(uuid);
            }
        }
    }

    /**
     * Remove um jogador do rastreamento (usar quando sair do servidor)
     */
    public void removePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        Mine mine = playersInMines.remove(uuid);

        if (mine != null) {
            PlayerQuitMineEvent event = new PlayerQuitMineEvent(player, mine);
            Bukkit.getPluginManager().callEvent(event);
            plugin.getPickaxeBossBarManager().removeBossBar(player);
        }
    }

    /**
     * Obtém a mina em que o jogador está
     */
    public Mine getPlayerMine(Player player) {
        return playersInMines.get(player.getUniqueId());
    }

    /**
     * Verifica se o jogador está em uma mina
     */
    public boolean isPlayerInMine(Player player) {
        return playersInMines.containsKey(player.getUniqueId());
    }

    /**
     * Limpa todos os dados (usar no disable)
     */
    public void clear() {
        playersInMines.clear();
    }
}