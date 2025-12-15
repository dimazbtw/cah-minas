package github.dimazbtw.minas.listeners;

import github.dimazbtw.minas.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class InputListener implements Listener {

    private final Main plugin;

    public InputListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Verificar se tem input pendente
        if (!plugin.getInputManager().hasPendingInput(player)) {
            return;
        }

        // Cancelar mensagem no chat
        event.setCancelled(true);

        // Processar input na thread principal
        String message = event.getMessage();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getInputManager().processInput(player, message);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Limpar input pendente
        plugin.getInputManager().cancelInput(event.getPlayer());
    }
}