package github.dimazbtw.minas.listeners;

import github.dimazbtw.minas.menus.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getCurrentItem() == null) return;

        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof MineListMenu) {
            event.setCancelled(true);
            ((MineListMenu) holder).handleClick(event.getRawSlot(), event.getClick());

        } else if (holder instanceof MineEditMenu) {
            event.setCancelled(true);
            ((MineEditMenu) holder).handleClick(event.getRawSlot(), event.getClick());

        } else if (holder instanceof MineBlocksMenu) {
            event.setCancelled(true);
            ((MineBlocksMenu) holder).handleClick(event.getRawSlot(), event.getClick());

        } else if (holder instanceof BlockEditMenu) {
            event.setCancelled(true);
            ((BlockEditMenu) holder).handleClick(event.getRawSlot(), event.getClick());

        } else if (holder instanceof PlayerMinesMenu) {
            event.setCancelled(true);
            ((PlayerMinesMenu) holder).handleClick(event.getRawSlot(), event.getClick());

        } else if (holder instanceof PickaxeUpgradeMenu) {
            event.setCancelled(true);
            ((PickaxeUpgradeMenu) holder).handleClick(event.getRawSlot(), event.getClick());

        } else if (holder instanceof MinasMainMenu) {
            event.setCancelled(true);
            ((MinasMainMenu) holder).handleClick(event.getRawSlot(), event.getClick());

        } else if (holder instanceof RankingMenu) {
            event.setCancelled(true);
            ((RankingMenu) holder).handleClick(event.getRawSlot(), event.getClick());
        }
    }
}