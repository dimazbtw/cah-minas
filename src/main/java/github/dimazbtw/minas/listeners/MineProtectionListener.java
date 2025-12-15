package github.dimazbtw.minas.listeners;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class MineProtectionListener implements Listener {

    private final Main plugin;

    public MineProtectionListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Verificar se é jogador atacando jogador
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        // Verificar se estão em uma mina
        Mine victimMine = plugin.getMineManager().getMineAt(victim.getLocation());
        Mine attackerMine = plugin.getMineManager().getMineAt(attacker.getLocation());

        // Se algum está em mina com PvP desabilitado
        if (victimMine != null && !victimMine.isPvpEnabled()) {
            event.setCancelled(true);
            plugin.getLanguageManager().sendMessage(attacker, "mine.pvp-disabled");
            return;
        }

        if (attackerMine != null && !attackerMine.isPvpEnabled()) {
            event.setCancelled(true);
            plugin.getLanguageManager().sendMessage(attacker, "mine.pvp-disabled");
        }
    }
}
