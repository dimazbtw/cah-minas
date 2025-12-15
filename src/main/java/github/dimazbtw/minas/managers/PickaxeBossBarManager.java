package github.dimazbtw.minas.managers;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.PickaxeData;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PickaxeBossBarManager {

    private final Main plugin;
    private final Map<UUID, BossBar> activeBossBars;
    private final Map<UUID, BukkitTask> updateTasks;
    private final Map<UUID, BukkitTask> hideTasks;

    // Configurações
    private boolean enabled;
    private int updateInterval; // em ticks
    private int hideDelay; // em segundos após parar de minerar
    private String format;
    private BarColor barColor;
    private BarStyle barStyle;

    public PickaxeBossBarManager(Main plugin) {
        this.plugin = plugin;
        this.activeBossBars = new HashMap<>();
        this.updateTasks = new HashMap<>();
        this.hideTasks = new HashMap<>();
        loadConfig();
    }

    private void loadConfig() {
        // Carregar configurações do config.yml
        enabled = plugin.getConfig().getBoolean("pickaxe-bossbar.enabled", true);
        updateInterval = plugin.getConfig().getInt("pickaxe-bossbar.update-interval", 10);
        hideDelay = plugin.getConfig().getInt("pickaxe-bossbar.hide-delay", 3);
        format = plugin.getConfig().getString("pickaxe-bossbar.format",
                "§6⛏ Picareta §7| §fNível {level} §8- §b{exp}§7/§b{exp_max} XP §8({percentage}%)");

        String colorName = plugin.getConfig().getString("pickaxe-bossbar.color", "YELLOW");
        String styleName = plugin.getConfig().getString("pickaxe-bossbar.style", "SOLID");

        try {
            barColor = BarColor.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            barColor = BarColor.YELLOW;
            plugin.getLogger().warning("Cor inválida para bossbar: " + colorName + ". Usando YELLOW.");
        }

        try {
            barStyle = BarStyle.valueOf(styleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            barStyle = BarStyle.SOLID;
            plugin.getLogger().warning("Estilo inválido para bossbar: " + styleName + ". Usando SOLID.");
        }
    }

    public void reload() {
        // Remover todas as bossbars ativas
        for (BossBar bossBar : activeBossBars.values()) {
            bossBar.removeAll();
        }
        activeBossBars.clear();

        // Cancelar todas as tasks
        for (BukkitTask task : updateTasks.values()) {
            task.cancel();
        }
        updateTasks.clear();

        for (BukkitTask task : hideTasks.values()) {
            task.cancel();
        }
        hideTasks.clear();

        // Recarregar configurações
        loadConfig();
    }

    public void showBossBar(Player player) {
        if (!enabled) return;

        UUID uuid = player.getUniqueId();

        // Cancelar task de esconder se existir
        if (hideTasks.containsKey(uuid)) {
            hideTasks.get(uuid).cancel();
            hideTasks.remove(uuid);
        }

        BossBar bossBar = activeBossBars.get(uuid);

        if (bossBar == null) {
            bossBar = Bukkit.createBossBar("", barColor, barStyle);
            bossBar.addPlayer(player);
            activeBossBars.put(uuid, bossBar);

            // Iniciar task de atualização
            startUpdateTask(player);
        }

        updateBossBar(player);
        bossBar.setVisible(true);
    }

    public void hideBossBar(Player player) {
        if (!enabled) return;

        UUID uuid = player.getUniqueId();

        // Cancelar task de esconder anterior se existir
        if (hideTasks.containsKey(uuid)) {
            hideTasks.get(uuid).cancel();
        }

        // Agendar para esconder após o delay
        BukkitTask hideTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            BossBar bossBar = activeBossBars.get(uuid);
            if (bossBar != null) {
                bossBar.setVisible(false);
            }
            hideTasks.remove(uuid);
        }, hideDelay * 20L);

        hideTasks.put(uuid, hideTask);
    }

    public void removeBossBar(Player player) {
        UUID uuid = player.getUniqueId();

        // Cancelar tasks
        if (updateTasks.containsKey(uuid)) {
            updateTasks.get(uuid).cancel();
            updateTasks.remove(uuid);
        }

        if (hideTasks.containsKey(uuid)) {
            hideTasks.get(uuid).cancel();
            hideTasks.remove(uuid);
        }

        // Remover bossbar
        BossBar bossBar = activeBossBars.remove(uuid);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    private void startUpdateTask(Player player) {
        UUID uuid = player.getUniqueId();

        // Cancelar task anterior se existir
        if (updateTasks.containsKey(uuid)) {
            updateTasks.get(uuid).cancel();
        }

        // Criar nova task de atualização
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                removeBossBar(player);
                return;
            }

            BossBar bossBar = activeBossBars.get(uuid);
            if (bossBar != null && bossBar.isVisible()) {
                updateBossBar(player);
            }
        }, 0L, updateInterval);

        updateTasks.put(uuid, task);
    }

    private void updateBossBar(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bossBar = activeBossBars.get(uuid);

        if (bossBar == null) return;

        PickaxeData pickaxeData = plugin.getDatabaseManager().getPickaxeData(uuid);

        if (pickaxeData == null) {
            bossBar.setTitle("§cDados não encontrados");
            bossBar.setProgress(0.0);
            return;
        }

        int level = pickaxeData.getLevel();
        int exp = pickaxeData.getExp();
        int expMax = pickaxeData.getExpForNextLevel();

        // Calcular progresso (0.0 a 1.0)
        double progress = expMax > 0 ? Math.min(1.0, (double) exp / expMax) : 1.0;

        // Calcular percentagem
        int percentage = (int) (progress * 100);

        // Formatar título
        String title = format
                .replace("{level}", String.valueOf(level))
                .replace("{exp}", formatNumber(exp))
                .replace("{exp_max}", formatNumber(expMax))
                .replace("{percentage}", String.valueOf(percentage));

        bossBar.setTitle(title);
        bossBar.setProgress(progress);

        // Mudar cor baseado no progresso
        updateBossBarColor(bossBar, progress);
    }

    private void updateBossBarColor(BossBar bossBar, double progress) {
        if (!plugin.getConfig().getBoolean("pickaxe-bossbar.dynamic-color", false)) {
            return;
        }

        BarColor newColor;

        if (progress >= 0.75) {
            newColor = BarColor.GREEN;
        } else if (progress >= 0.50) {
            newColor = BarColor.YELLOW;
        } else if (progress >= 0.25) {
            newColor = BarColor.RED;
        } else {
            newColor = BarColor.RED;
        }

        if (bossBar.getColor() != newColor) {
            bossBar.setColor(newColor);
        }
    }

    private String formatNumber(int number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }

    public void onPlayerQuit(Player player) {
        removeBossBar(player);
    }

    public void shutdown() {
        // Cancelar todas as tasks
        for (BukkitTask task : updateTasks.values()) {
            task.cancel();
        }
        updateTasks.clear();

        for (BukkitTask task : hideTasks.values()) {
            task.cancel();
        }
        hideTasks.clear();

        // Remover todas as bossbars
        for (BossBar bossBar : activeBossBars.values()) {
            bossBar.removeAll();
        }
        activeBossBars.clear();
    }

    public boolean isEnabled() {
        return enabled;
    }
}