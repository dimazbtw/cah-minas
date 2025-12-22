// SessionManager.java - Corrigido para remover bossbar ao sair
package github.dimazbtw.minas.managers;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.data.PlayerSession;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SessionManager {

    private final Main plugin;
    private final Map<UUID, PlayerSession> sessions;

    public SessionManager(Main plugin) {
        this.plugin = plugin;
        this.sessions = new HashMap<>();
    }

    public void createSession(Player player, Mine mine) {
        PlayerSession session = sessions.get(player.getUniqueId());

        if (session == null) {
            sessions.put(player.getUniqueId(), new PlayerSession(player.getUniqueId(), mine));
        } else {
            session.setCurrentMine(mine);
        }

        plugin.getPickaxeManager().givePickaxe(player);

        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().showScoreboard(player, mine);
        }
    }

    public void removeSession(Player player) {
        sessions.remove(player.getUniqueId());

        plugin.getPickaxeManager().removePickaxes(player);

        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().removeScoreboard(player);
        }

        // Remover bossbar ao sair da mina
        if (plugin.getPickaxeBossBarManager() != null) {
            plugin.getPickaxeBossBarManager().removeBossBar(player);
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

                if (plugin.getPickaxeBossBarManager() != null) {
                    plugin.getPickaxeBossBarManager().removeBossBar(player);
                }
            }
        }
        sessions.clear();
    }

    public PlayerSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public Mine getCurrentMine(Player player) {
        PlayerSession session = sessions.get(player.getUniqueId());
        return session != null ? session.getCurrentMine() : null;
    }

    public void incrementBlocksMined(Player player, int amount) {
        PlayerSession session = sessions.get(player.getUniqueId());
        if (session != null) {
            for (int i = 0; i < amount; i++) {
                session.incrementBlocksMined();
            }
        }
    }

    public void clearAll() {
        sessions.clear();
    }

    public Map<UUID, PlayerSession> getSessions() {
        return sessions;
    }
}