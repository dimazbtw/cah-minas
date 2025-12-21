package github.dimazbtw.minas.managers;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.data.PlayerSession;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gerencia as sessões dos jogadores nas minas
 */
public class SessionManager {

    private final Main plugin;
    private final Map<UUID, PlayerSession> sessions;

    public SessionManager(Main plugin) {
        this.plugin = plugin;
        this.sessions = new HashMap<>();
    }

    /**
     * Cria ou atualiza a sessão de um jogador
     */
    public void createSession(Player player, Mine mine) {
        PlayerSession session = sessions.get(player.getUniqueId());

        if (session == null) {
            sessions.put(player.getUniqueId(), new PlayerSession(player.getUniqueId(), mine));
        } else {
            session.setCurrentMine(mine);
        }

        // Dar picareta de mina ao jogador
        plugin.getPickaxeManager().givePickaxe(player);

        // Mostrar scoreboard
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().showScoreboard(player, mine);
        }
    }

    /**
     * Remove a sessão de um jogador
     */
    public void removeSession(Player player) {
        sessions.remove(player.getUniqueId());

        // Remover picareta de mina
        plugin.getPickaxeManager().removePickaxes(player);

        // Remover scoreboard
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().removeScoreboard(player);
        }
    }

    public void removeAllSessions() {
        for (UUID uuid : sessions.keySet()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                plugin.getPickaxeManager().removePickaxes(player);
                if (plugin.getScoreboardManager() != null) {
                    plugin.getScoreboardManager().removeScoreboard(player);
                }
            }
        }
        sessions.clear();
    }

    /**
     * Obtém a sessão de um jogador
     */
    public PlayerSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    /**
     * Verifica se o jogador tem uma sessão ativa
     */
    public boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    /**
     * Obtém a mina atual do jogador
     */
    public Mine getCurrentMine(Player player) {
        PlayerSession session = sessions.get(player.getUniqueId());
        return session != null ? session.getCurrentMine() : null;
    }

    /**
     * Incrementa o contador de blocos minerados
     */
    public void incrementBlocksMined(Player player, int amount) {
        PlayerSession session = sessions.get(player.getUniqueId());
        if (session != null) {
            for (int i = 0; i < amount; i++) {
                session.incrementBlocksMined();
            }
        }
    }

    /**
     * Limpa todas as sessões
     */
    public void clearAll() {
        sessions.clear();
    }

    /**
     * Obtém todas as sessões ativas
     */
    public Map<UUID, PlayerSession> getSessions() {
        return sessions;
    }
}