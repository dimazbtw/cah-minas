package github.dimazbtw.minas.listeners;

import github.dimazbtw.minas.Main;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class SkinActivationListener implements Listener {

    private final Main plugin;

    public SkinActivationListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Verificar se é item de ativação de skin
        if (!plugin.getSkinManager().isSkinActivationItem(item)) return;

        // Verificar se é clique direito
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // Ignorar offhand
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        // Cancelar evento
        event.setCancelled(true);

        // Obter ordem da skin
        int skinOrder = plugin.getSkinManager().getSkinOrderFromItem(item);
        if (skinOrder < 0) {
            player.sendMessage("§cItem de skin inválido!");
            return;
        }

        // Ativar skin
        boolean success = plugin.getSkinManager().activateSkin(player, skinOrder);

        if (success) {
            // Remover item da mão
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }

            player.sendMessage("§aSkin ativada com sucesso!");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
    }
}