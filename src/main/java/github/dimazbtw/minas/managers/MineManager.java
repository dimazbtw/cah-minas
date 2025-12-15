package github.dimazbtw.minas.managers;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.tasks.MineResetTask;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class MineManager {

    private final Main plugin;
    private final Map<String, Mine> mines;
    private BukkitTask resetTask;

    public MineManager(Main plugin) {
        this.plugin = plugin;
        this.mines = new LinkedHashMap<>();
    }

    /**
     * Carrega todas as minas da pasta minas/
     */
    public void loadAllMines() {
        mines.clear();

        File minasFolder = new File(plugin.getDataFolder(), "minas");
        if (!minasFolder.exists()) {
            minasFolder.mkdirs();
            return;
        }

        File[] files = minasFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                Mine mine = Mine.fromFile(file);
                mines.put(mine.getId().toLowerCase(), mine);
                plugin.getLogger().info("Mina carregada: " + mine.getId());
            } catch (Exception e) {
                plugin.getLogger().severe("Erro ao carregar mina " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Total de minas carregadas: " + mines.size());

        // Iniciar task de reset
        startResetTask();
    }

    /**
     * Salva todas as minas
     */
    public void saveAllMines() {
        for (Mine mine : mines.values()) {
            try {
                mine.save();
            } catch (Exception e) {
                plugin.getLogger().severe("Erro ao salvar mina " + mine.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Recarrega todas as minas
     */
    public void reloadAllMines() {
        // Parar task atual
        if (resetTask != null) {
            resetTask.cancel();
        }

        // Salvar antes de recarregar
        saveAllMines();

        // Recarregar
        loadAllMines();
    }

    /**
     * Cria uma nova mina
     */
    public Mine createMine(String id) {
        if (mines.containsKey(id.toLowerCase())) {
            return null;
        }

        Mine mine = new Mine(id);
        mines.put(id.toLowerCase(), mine);
        mine.save();

        return mine;
    }

    /**
     * Remove uma mina
     */
    public boolean deleteMine(String id) {
        Mine mine = mines.remove(id.toLowerCase());
        if (mine == null) return false;

        // Deletar arquivo
        File file = new File(plugin.getDataFolder(), "minas/" + id + ".yml");
        if (file.exists()) {
            file.delete();
        }

        return true;
    }

    /**
     * Obtém uma mina pelo ID
     */
    public Mine getMine(String id) {
        return mines.get(id.toLowerCase());
    }

    /**
     * Obtém a mina em uma localização
     */
    public Mine getMineAt(Location location) {
        for (Mine mine : mines.values()) {
            if (mine.isInMine(location)) {
                return mine;
            }
        }
        return null;
    }

    /**
     * Obtém todas as minas
     */
    public Collection<Mine> getAllMines() {
        return Collections.unmodifiableCollection(mines.values());
    }

    /**
     * Obtém a melhor mina para um jogador (maior ordem que ele tem permissão)
     */
    public Mine getBestMineForPlayer(Player player) {
        return mines.values().stream()
                .filter(mine -> mine.isConfigured())
                .filter(mine -> mine.getSpawn() != null)
                .filter(mine -> player.hasPermission("minas.admin") || player.hasPermission(mine.getPermission()))
                .max(Comparator.comparingInt(Mine::getOrder))
                .orElse(null);
    }

    /**
     * Obtém todas as minas que o jogador tem permissão, ordenadas
     */
    public List<Mine> getMinesForPlayer(Player player) {
        return mines.values().stream()
                .filter(mine -> mine.isConfigured())
                .filter(mine -> mine.getSpawn() != null)
                .filter(mine -> player.hasPermission("minas.admin") || player.hasPermission(mine.getPermission()))
                .sorted(Comparator.comparingInt(Mine::getOrder).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Verifica se uma mina existe
     */
    public boolean mineExists(String id) {
        return mines.containsKey(id.toLowerCase());
    }

    /**
     * Obtém a quantidade de minas
     */
    public int getMineCount() {
        return mines.size();
    }

    /**
     * Inicia a task de reset automático
     */
    private void startResetTask() {
        if (resetTask != null) {
            resetTask.cancel();
        }

        // Task roda a cada segundo para verificar resets
        resetTask = new MineResetTask(this).runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Verifica e reseta minas que precisam
     */
    public void checkResets() {
        for (Mine mine : mines.values()) {
            if (!mine.isConfigured()) continue;

            if (mine.getTimeUntilReset() <= 0) {
                mine.reset();
            }
        }
    }
}