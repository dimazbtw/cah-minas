package github.dimazbtw.minas.listeners;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.PlayerSelection;
import github.dimazbtw.minas.utils.MapBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Map;

public class MineSelectionListener implements Listener {

    private final Main plugin;

    public MineSelectionListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Verificar se está em modo de seleção
        if (!plugin.getSelectionManager().isSelecting(player)) return;

        // Verificar se está usando o item de seleção
        if (!plugin.getSelectionManager().isSelectionTool(event.getItem())) return;

        // Ignorar interação com offhand
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        // Cancelar evento
        event.setCancelled(true);

        // Verificar ação
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) return;

        // Obter bloco clicado
        if (event.getClickedBlock() == null) return;

        Location clickedLoc = event.getClickedBlock().getLocation();
        PlayerSelection selection = plugin.getSelectionManager().getSelection(player);

        if (selection == null) return;

        // Verificar se é mesma posição
        if (action == Action.LEFT_CLICK_BLOCK) {
            // Posição 1
            if (selection.getPos2() != null && clickedLoc.equals(selection.getPos2())) {
                plugin.getLanguageManager().sendMessage(player, "selection.same-position");
                return;
            }

            selection.setPos1(clickedLoc);
            plugin.getLanguageManager().sendMessage(player, "selection.pos1-set",
                    MapBuilder.of("x", String.valueOf(clickedLoc.getBlockX()),
                           "y", String.valueOf(clickedLoc.getBlockY()),
                           "z", String.valueOf(clickedLoc.getBlockZ())));
        } else {
            // Posição 2
            if (selection.getPos1() != null && clickedLoc.equals(selection.getPos1())) {
                plugin.getLanguageManager().sendMessage(player, "selection.same-position");
                return;
            }

            // Verificar se é mesmo mundo
            if (selection.getPos1() != null && !selection.getPos1().getWorld().equals(clickedLoc.getWorld())) {
                plugin.getLanguageManager().sendMessage(player, "selection.different-world");
                return;
            }

            selection.setPos2(clickedLoc);
            plugin.getLanguageManager().sendMessage(player, "selection.pos2-set",
                    MapBuilder.of("x", String.valueOf(clickedLoc.getBlockX()),
                           "y", String.valueOf(clickedLoc.getBlockY()),
                           "z", String.valueOf(clickedLoc.getBlockZ())));
        }

        // Verificar se a seleção está completa
        if (selection.isComplete()) {
            int blocks = calculateBlocks(selection);
            plugin.getLanguageManager().sendMessage(player, "selection.complete",
                    MapBuilder.of("blocks", String.valueOf(blocks)));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Cancelar seleção ao sair
        if (plugin.getSelectionManager().isSelecting(event.getPlayer())) {
            plugin.getSelectionManager().cancelSelection(event.getPlayer());
        }
    }

    /**
     * Calcula a quantidade de blocos na seleção
     */
    private int calculateBlocks(PlayerSelection selection) {
        Location pos1 = selection.getPos1();
        Location pos2 = selection.getPos2();

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }
}
