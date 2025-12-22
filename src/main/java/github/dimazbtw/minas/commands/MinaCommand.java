package github.dimazbtw.minas.commands;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.utils.MapBuilder;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.annotation.Optional;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.target.CommandTarget;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

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
    public void minaCommand(Context<Player> context, @Optional String minaId) {
        Player player = context.getSender();

        // Verificar se já está em uma mina
        if (plugin.getSessionManager().hasSession(player)) {
            plugin.getLanguageManager().sendMessage(player, "mine.already-in-mine");
            return;
        }

        Mine targetMine;

        // Se especificou uma mina, tentar ir para ela
        if (minaId != null && !minaId.isEmpty()) {
            targetMine = plugin.getMineManager().getMine(minaId);

            // Verificar se a mina existe
            if (targetMine == null) {
                plugin.getLanguageManager().sendMessage(player, "mine.not-found");
                suggestAvailableMines(player);
                return;
            }

            // Verificar se tem permissão
            if (!player.hasPermission("minas.admin") && !player.hasPermission(targetMine.getPermission())) {
                plugin.getLanguageManager().sendMessage(player, "mine.no-permission-specific",
                        MapBuilder.of("mine", targetMine.getDisplayName()));
                suggestAvailableMines(player);
                return;
            }

            // Verificar se está configurada
            if (!targetMine.isConfigured()) {
                plugin.getLanguageManager().sendMessage(player, "mine.not-configured");
                return;
            }

            // Verificar se tem spawn
            if (targetMine.getSpawn() == null) {
                plugin.getLanguageManager().sendMessage(player, "mine.no-spawn");
                return;
            }

        } else {
            // Não especificou mina, ir para a melhor disponível
            targetMine = plugin.getMineManager().getBestMineForPlayer(player);

            if (targetMine == null) {
                plugin.getLanguageManager().sendMessage(player, "mine.no-permission-any");
                return;
            }

            if (targetMine.getSpawn() == null) {
                plugin.getLanguageManager().sendMessage(player, "mine.no-spawn");
                return;
            }
        }

        // Teleportar para a mina
        player.teleport(targetMine.getSpawn());
        plugin.getSessionManager().createSession(player, targetMine);
        plugin.getLanguageManager().sendMessage(player, "mine.teleported",
                MapBuilder.of("mine", targetMine.getDisplayName()));
    }

    @Command(
            name = "mina.ir",
            aliases = "mine.go",
            description = "Ir para a mina",
            target = CommandTarget.PLAYER
    )
    public void irCommand(Context<Player> context, @Optional String mina) {
        // Mesmo comportamento do comando base
        minaCommand(context, mina);
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

    private void suggestAvailableMines(Player player) {
        List<Mine> availableMines = plugin.getMineManager().getMinesForPlayer(player);

        if (availableMines.isEmpty()) {
            return;
        }

        // Limitar a 5 sugestões
        String suggestions = availableMines.stream()
                .limit(5)
                .map(Mine::getId)
                .collect(Collectors.joining("§7, §f"));

        player.sendMessage("");
        player.sendMessage("§7Minas disponíveis: §f" + suggestions);

        if (availableMines.size() > 5) {
            player.sendMessage("§7Use §f/mina lista §7para ver todas.");
        }
    }
}