package github.dimazbtw.minas.listeners;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

/**
 * Listener que protege blocos quando o jogador está em sessão de mina
 *
 * Jogadores em sessão só podem quebrar/colocar blocos dentro da mina atual
 */
public class SessionProtectionListener implements Listener {

    private final Main plugin;

    public SessionProtectionListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Verificar se tem sessão ativa
        if (!plugin.getSessionManager().hasSession(player)) {
            return;
        }

        // Obter mina da sessão
        Mine sessionMine = plugin.getSessionManager().getCurrentMine(player);
        if (sessionMine == null) {
            return;
        }

        // Verificar se está quebrando dentro da mina da sessão
        if (!sessionMine.isInMine(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Verificar se tem sessão ativa
        if (!plugin.getSessionManager().hasSession(player)) {
            return;
        }

        // Obter mina da sessão
        Mine sessionMine = plugin.getSessionManager().getCurrentMine(player);
        if (sessionMine == null) {
            return;
        }

        // Bloquear colocação de blocos se estiver em sessão
        // (jogadores em sessão não devem colocar blocos)
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        // Verificar se está em sessão
        if (!plugin.getSessionManager().hasSession(player)) {
            return;
        }

        // Admins podem usar qualquer comando
        if (player.hasPermission("minas.admin")) {
            return;
        }

        String message = event.getMessage().toLowerCase();
        String command = message.split(" ")[0].substring(1); // Remove a /

        // Verificar whitelist
        List<String> whitelist = plugin.getConfig().getStringList("command-whitelist");

        for (String allowed : whitelist) {
            String allowedLower = allowed.toLowerCase();

            // Verificar comando exato ou se começa com o comando (para subcomandos)
            if (command.equals(allowedLower) || command.startsWith(allowedLower + " ")) {
                return;
            }

            // Verificar também com a mensagem completa (sem a /)
            String fullCommand = message.substring(1);
            if (fullCommand.equals(allowedLower) || fullCommand.startsWith(allowedLower + " ")) {
                return;
            }
        }

        // Comando não permitido
        event.setCancelled(true);
        plugin.getLanguageManager().sendMessage(player, "session.command-blocked");
    }
}