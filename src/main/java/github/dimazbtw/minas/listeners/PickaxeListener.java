package github.dimazbtw.minas.listeners;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.menus.PickaxeUpgradeMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PickaxeListener implements Listener {

    private final Main plugin;

    public PickaxeListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Carrega dados da picareta quando o jogador entra
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Carregar dados assincronamente
        plugin.getDatabaseManager().loadPickaxeData(player.getUniqueId());
    }

    /**
     * Salva dados e remove do cache quando o jogador sai
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Remover picareta de mina do inventário
        plugin.getPickaxeManager().removePickaxes(player);

        // Salvar e remover do cache
        plugin.getDatabaseManager().removeFromCache(player.getUniqueId());
    }

    /**
     * Abre menu de upgrade ao clicar direito com a picareta
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Verificar se é picareta de mina
        if (!plugin.getPickaxeManager().isMinePickaxe(item)) return;

        // Verificar se é clique direito
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // Ignorar offhand
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        // Cancelar evento
        event.setCancelled(true);

        // Verificar se é o dono
        if (!plugin.getPickaxeManager().isOwner(player, item)) {
            player.sendMessage("§cEsta picareta não é sua!");
            return;
        }

        // Abrir menu de upgrade
        new PickaxeUpgradeMenu(plugin, player).open();
    }

    /**
     * Impede que jogadores dropem a picareta de mina
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        if (plugin.getPickaxeManager().isMinePickaxe(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cVocê não pode dropar a picareta de mina!");
        }
    }

    /**
     * Impede que jogadores movam a picareta para outros inventários
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        // Verificar se está tentando mover picareta de mina
        boolean isCurrentPickaxe = plugin.getPickaxeManager().isMinePickaxe(currentItem);
        boolean isCursorPickaxe = plugin.getPickaxeManager().isMinePickaxe(cursorItem);

        if (isCurrentPickaxe || isCursorPickaxe) {
            // Permitir apenas no inventário do jogador
            if (event.getClickedInventory() != event.getWhoClicked().getInventory()) {
                event.setCancelled(true);
            }
        }
    }
}