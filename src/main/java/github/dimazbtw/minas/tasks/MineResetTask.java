package github.dimazbtw.minas.tasks;

import github.dimazbtw.minas.managers.MineManager;
import org.bukkit.scheduler.BukkitRunnable;

public class MineResetTask extends BukkitRunnable {

    private final MineManager mineManager;

    public MineResetTask(MineManager mineManager) {
        this.mineManager = mineManager;
    }

    @Override
    public void run() {
        mineManager.checkResets();
    }
}
