package github.dimazbtw.minas.listeners;

import github.dimazbtw.minas.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class WorldLoadListener implements Listener {

    private final Main plugin;

    public WorldLoadListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        String worldName = event.getWorld().getName();

        // Recarregar minas deste mundo
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getMineManager().reloadMinasFromWorld(worldName);
        }, 20L); // 1 segundo de delay para garantir que o mundo está pronto
    }
}