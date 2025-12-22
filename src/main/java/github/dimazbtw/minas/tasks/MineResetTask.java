// MineResetTask.java - Otimizado
package github.dimazbtw.minas.tasks;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.managers.MineManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class MineResetTask extends BukkitRunnable {

    private final MineManager mineManager;
    private final Main plugin;

    public MineResetTask(MineManager mineManager) {
        this.mineManager = mineManager;
        this.plugin = Main.getInstance();
    }

    @Override
    public void run() {
        // Coletar minas que precisam resetar
        List<Mine> minesToReset = new ArrayList<>();

        for (Mine mine : mineManager.getAllMines()) {
            if (!mine.isConfigured()) continue;

            // Verificar se precisa resetar
            if (mine.getTimeUntilReset() <= 0) {
                minesToReset.add(mine);
            }
        }

        // Se não há minas para resetar, retornar
        if (minesToReset.isEmpty()) return;

        // Resetar cada mina em ticks separados para evitar lag
        final int[] index = {0};

        new BukkitRunnable() {
            @Override
            public void run() {
                if (index[0] >= minesToReset.size()) {
                    this.cancel();
                    return;
                }

                Mine mine = minesToReset.get(index[0]);

                // Resetar de forma assíncrona se possível
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        // Preparação assíncrona
                        mine.reset();
                    } catch (Exception e) {
                        plugin.getLogger().severe("Erro ao resetar mina " + mine.getId() + ": " + e.getMessage());
                    }
                });

                index[0]++;
            }
        }.runTaskTimer(plugin, 0L, 5L); // 5 ticks (0.25s) entre cada reset
    }
}