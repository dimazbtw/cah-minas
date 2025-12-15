package github.dimazbtw.minas.managers;

import github.dimazbtw.lib.utils.basics.ScoreboardUtils;
import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.data.PickaxeData;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MineScoreboardManager {

    private final Main plugin;
    private final Map<UUID, BukkitTask> updateTasks;
    private final boolean placeholderApiEnabled;

    // Cores para linhas vazias (evita duplicação)
    private static final String[] EMPTY_LINE_COLORS = {
            "§0§r", "§1§r", "§2§r", "§3§r", "§4§r", "§5§r",
            "§6§r", "§7§r", "§8§r", "§9§r",
            "§a§r", "§b§r", "§c§r", "§d§r", "§e§r", "§f§r",
            "§0§r§0", "§1§r§1", "§2§r§2", "§3§r§3"
    };


    // Configurações
    private String title;
    private List<String> lines;
    private int updateInterval;

    public MineScoreboardManager(Main plugin) {
        this.plugin = plugin;
        this.updateTasks = new ConcurrentHashMap<>();
        this.placeholderApiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        loadConfig();

        if (placeholderApiEnabled) {
            plugin.getLogger().info("§aPlaceholderAPI encontrado! Placeholders habilitados na scoreboard.");
        }
    }

    /**
     * Carrega configurações do config.yml
     */
    public void loadConfig() {
        this.title = plugin.getConfig().getString("scoreboard.title", "&6&l⛏ MINAS");
        this.lines = plugin.getConfig().getStringList("scoreboard.lines");
        this.updateInterval = plugin.getConfig().getInt("scoreboard.update-interval", 20);

        // Linhas padrão se não configuradas
        if (lines.isEmpty()) {
            lines = new ArrayList<>();
            lines.add("");
            lines.add("&fMina: &e{mine_name}");
            lines.add("");
            lines.add("&fNível: &b{level}");
            lines.add("&fEXP: &a{exp}&7/&a{exp_next}");
            lines.add("&fProgresso: {progress_bar}");
            lines.add("");
            lines.add("&fBlocos: &e{blocks_mined}");
            lines.add("&fFortuna: &d{fortune_multiplier}x");
            lines.add("");
            lines.add("&7play.seuservidor.com");
        }
    }

    /**
     * Mostra a scoreboard para um jogador
     */
    public void showScoreboard(Player player, Mine mine) {
        if (player == null || !player.isOnline()) return;

        // Verificar se está ativado
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;

        UUID uuid = player.getUniqueId();

        // Cancelar task anterior se existir
        removeScoreboard(player);

        // Atualizar imediatamente
        updateScoreboard(player, mine);

        // Iniciar task de atualização
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                removeScoreboard(player);
                return;
            }

            // Verificar se ainda está na mina
            Mine currentMine = plugin.getSessionManager().getCurrentMine(player);
            if (currentMine == null) {
                removeScoreboard(player);
                return;
            }

            updateScoreboard(player, currentMine);
        }, updateInterval, updateInterval);

        updateTasks.put(uuid, task);
    }

    /**
     * Atualiza a scoreboard do jogador
     */
    public void updateScoreboard(Player player, Mine mine) {
        if (player == null || !player.isOnline() || mine == null) return;

        PickaxeData data = plugin.getDatabaseManager().getPickaxeData(player.getUniqueId());

        List<String> processedLines = new ArrayList<>();
        int emptyLineIndex = 0;

        for (String line : lines) {
            String processed = replacePlaceholders(line, player, mine, data);

            // Verificar se é linha vazia e adicionar cor única
            if (isEmptyLine(processed)) {
                processed = EMPTY_LINE_COLORS[emptyLineIndex % EMPTY_LINE_COLORS.length];
                emptyLineIndex++;
            }

            processedLines.add(processed);
        }

        String processedTitle = replacePlaceholders(title, player, mine, data);
        ScoreboardUtils.setScoreboard(player, processedTitle, processedLines);
    }

    /**
     * Verifica se uma linha é considerada vazia
     */
    private boolean isEmptyLine(String line) {
        if (line == null) return true;

        // Remove códigos de cor e espaços
        String stripped = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', line));
        return stripped == null || stripped.trim().isEmpty();
    }

    /**
     * Remove a scoreboard do jogador
     */
    public void removeScoreboard(Player player) {
        if (player == null) return;

        UUID uuid = player.getUniqueId();

        // Cancelar task
        BukkitTask task = updateTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        // Remover scoreboard
        ScoreboardUtils.removeScoreboard(player);
    }

    /**
     * Remove todas as scoreboards
     */
    public void removeAll() {
        for (UUID uuid : updateTasks.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                removeScoreboard(player);
            }
        }
        updateTasks.clear();
    }

    /**
     * Substitui os placeholders internos e do PlaceholderAPI
     */
    private String replacePlaceholders(String text, Player player, Mine mine, PickaxeData data) {
        if (text == null) return "";

        // Placeholders internos - Mina
        text = text.replace("{mine_name}", mine.getDisplayName());
        text = text.replace("{mine_id}", mine.getId());
        text = text.replace("{mine_blocks_current}", formatNumber(mine.getCurrentBlocks()));
        text = text.replace("{mine_blocks_total}", formatNumber(mine.getTotalBlocks()));
        text = text.replace("{mine_percentage}", String.format("%.1f%%", mine.getPercentageRemaining()));
        text = text.replace("{mine_reset_time}", formatTime(mine.getTimeUntilReset()));

        // Placeholders internos - Picareta
        text = text.replace("{level}", String.valueOf(data.getLevel()));
        text = text.replace("{exp}", formatNumber(data.getExp()));
        text = text.replace("{exp_next}", formatNumber(data.getExpForNextLevel()));
        text = text.replace("{blocks_mined}", formatNumber(data.getBlocksMined()));
        text = text.replace("{fortune_multiplier}", String.format("%.1f", data.getFortuneMultiplier()));
        text = text.replace("{explosion_chance}", String.format("%.1f%%", data.getExplosionChance()));
        text = text.replace("{multiplier_bonus}", String.format("+%.0f%%", (data.getMultiplierBonus() - 1) * 100));
        text = text.replace("{experienced_bonus}", String.format("+%.0f%%", (data.getExperiencedBonus() - 1) * 100));
        text = text.replace("{destroyer_chance}", String.format("%.1f%%", data.getDestroyerChance()));

        // Encantamentos
        text = text.replace("{efficiency}", String.valueOf(data.getEfficiency()));
        text = text.replace("{fortune}", String.valueOf(data.getFortune()));
        text = text.replace("{explosion}", String.valueOf(data.getExplosion()));
        text = text.replace("{multiplier}", String.valueOf(data.getMultiplier()));
        text = text.replace("{experienced}", String.valueOf(data.getExperienced()));
        text = text.replace("{destroyer}", String.valueOf(data.getDestroyer()));

        // Barras de progresso
        text = text.replace("{progress_bar}", createProgressBar(data.getExp(), data.getExpForNextLevel()));
        text = text.replace("{progress_percent}", String.format("%.1f%%",
                (double) data.getExp() / data.getExpForNextLevel() * 100));

        // XP Vanilla do jogador
        text = text.replace("{xp}", formatNumber(player.getTotalExperience()));
        text = text.replace("{xp_level}", String.valueOf(player.getLevel()));
        text = text.replace("{xp_progress}", String.format("%.1f%%", player.getExp() * 100));
        text = text.replace("{xp_bar}", createXpProgressBar(player));

        // Sessão
        if (plugin.getSessionManager().hasSession(player)) {
            text = text.replace("{session_blocks}", formatNumber(
                    plugin.getSessionManager().getSession(player).getBlocksMined()));
            text = text.replace("{session_time}", formatTime(
                    plugin.getSessionManager().getSession(player).getSessionTime()));
        } else {
            text = text.replace("{session_blocks}", "0");
            text = text.replace("{session_time}", "0s");
        }

        // PlaceholderAPI
        if (placeholderApiEnabled) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Cria uma barra de progresso visual para a picareta
     */
    private String createProgressBar(int current, int max) {
        int totalBars = 10;
        int filledBars = max > 0 ? (int) ((double) current / max * totalBars) : 0;

        StringBuilder bar = new StringBuilder();
        bar.append("&a");
        for (int i = 0; i < filledBars; i++) {
            bar.append("▌");
        }
        bar.append("&7");
        for (int i = filledBars; i < totalBars; i++) {
            bar.append("▌");
        }

        return bar.toString();
    }

    /**
     * Cria uma barra de progresso para o XP vanilla do jogador
     */
    private String createXpProgressBar(Player player) {
        int totalBars = 10;
        int filledBars = (int) (player.getExp() * totalBars);

        StringBuilder bar = new StringBuilder();
        bar.append("&a");
        for (int i = 0; i < filledBars; i++) {
            bar.append("▌");
        }
        bar.append("&7");
        for (int i = filledBars; i < totalBars; i++) {
            bar.append("▌");
        }

        return bar.toString();
    }

    /**
     * Formata números grandes
     */
    private String formatNumber(int number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        }
        return String.valueOf(number);
    }

    /**
     * Formata tempo em segundos para formato legível
     */
    private String formatTime(long seconds) {
        if (seconds <= 0) return "0s";

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }

    /**
     * Recarrega configurações
     */
    public void reload() {
        loadConfig();
    }
}