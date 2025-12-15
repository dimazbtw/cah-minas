package github.dimazbtw.minas.actionbar;

import github.dimazbtw.minas.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ActionBarManager {

    private final Main plugin;
    private final Map<UUID, ActionBarEarnings> earningsMap = new ConcurrentHashMap<>();

    public ActionBarManager(Main plugin) {
        this.plugin = plugin;
        startTask();
    }

    public void addEarnings(Player player, double money, int xp) {
        ActionBarEarnings data = earningsMap.computeIfAbsent(
                player.getUniqueId(), k -> new ActionBarEarnings()
        );

        data.addMoney(money);
        data.addXp(xp);
    }

    private void startTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : earningsMap.keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    earningsMap.remove(uuid);
                    continue;
                }

                ActionBarEarnings data = earningsMap.get(uuid);
                if (data == null || !data.hasEarnings()) continue;

                String message = ChatColor.translateAlternateColorCodes('&',
                        "&a+ " + formatMoney(data.getMoney()) +
                                " &7| &b+ " + formatXp(data.getXp()) + " XP"
                );

                player.spigot().sendMessage(
                        net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(message)
                );

                data.reset();
            }
        }, 100L, 100L); // 5 segundos
    }

    private String formatMoney(double value) {
        if (value >= 1_000_000)
            return String.format("%.2fM", value / 1_000_000);
        if (value >= 1_000)
            return String.format("%.1fK", value / 1_000);
        return String.format("%.0f", value);
    }

    private String formatXp(int xp) {
        if (xp >= 1_000_000)
            return String.format("%.1fM", xp / 1_000_000.0);
        if (xp >= 1_000)
            return String.format("%.1fK", xp / 1_000.0);
        return String.valueOf(xp);
    }
}

