package github.dimazbtw.minas.commands;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.utils.MapBuilder;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.target.CommandTarget;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class MinaCommand {

    private final Main plugin;

    public MinaCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Command(
            name = "mina",
            aliases = "mine",
            description = "Comando de mina para jogadores",
            target = CommandTarget.PLAYER
    )
    public void minaCommand(Context<Player> context) {
        Player player = context.getSender();

        // Teleportar para a melhor mina disponível
        Mine bestMine = plugin.getMineManager().getBestMineForPlayer(player);

        if (bestMine == null) {
            plugin.getLanguageManager().sendMessage(player, "mine.no-permission-any");
            return;
        }

        if (bestMine.getSpawn() == null) {
            plugin.getLanguageManager().sendMessage(player, "mine.no-spawn");
            return;
        }

        if (plugin.getSessionManager().hasSession(player)) {
            plugin.getLanguageManager().sendMessage(player, "mine.already-in-mine");
            return;
        }

        player.teleport(bestMine.getSpawn());
        plugin.getSessionManager().createSession(player, bestMine);
        plugin.getLanguageManager().sendMessage(player, "mine.teleported",
                MapBuilder.of("mine", bestMine.getDisplayName()));
    }

    @Command(
            name = "mina.ir",
            aliases = "mine.go",
            description = "Ir para a mina",
            target = CommandTarget.PLAYER
    )
    public void irCommand(Context<Player> context) {
        // Mesmo comportamento do comando base
        minaCommand(context);
    }

    @Command(
            name = "spawn",
            aliases = "sair",
            description = "Sair da sessão de mina",
            target = CommandTarget.PLAYER
    )
    public void sairCommand(Context<Player> context) {
        Player player = context.getSender();

        // Verificar se está em sessão
        if (!plugin.getSessionManager().hasSession(player)) {
            return;
        }

        Mine currentMine = plugin.getSessionManager().getCurrentMine(player);

        // Teleportar para saída ou spawn do mundo
        Location exit = currentMine.getExit();
        if (exit != null) {
            player.teleport(exit);
        } else {
            // Fallback: spawn do mundo
            player.teleport(player.getWorld().getSpawnLocation());
        }

        // Remover sessão
        plugin.getSessionManager().removeSession(player);
        plugin.getLanguageManager().sendMessage(player, "session.exited",
                MapBuilder.of("mine", currentMine.getDisplayName()));
    }
}