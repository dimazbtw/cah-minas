package github.dimazbtw.minas.listeners;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.menus.MineBlocksMenu;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Listener responsável por detectar quando jogadores entram/saem de minas
 */
public class PlayerMoveListener implements Listener {

    private final Main plugin;

    public PlayerMoveListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        // Verificar se mudou de bloco
        if (to == null || (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ())) {
            return;
        }

        // Atualizar localização
        plugin.getLocationTracker().updatePlayerLocation(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Atualizar após teleporte (com delay para garantir que chegou)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                plugin.getLocationTracker().updatePlayerLocation(event.getPlayer());
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getLocationTracker().removePlayer(event.getPlayer());
        plugin.getPickaxeBossBarManager().onPlayerQuit(event.getPlayer());

        MineBlocksMenu.cleanupPlayer(event.getPlayer().getUniqueId());
    }
}