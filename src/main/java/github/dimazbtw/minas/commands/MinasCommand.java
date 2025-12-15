package github.dimazbtw.minas.commands;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.menus.MinasMainMenu;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.target.CommandTarget;
import org.bukkit.entity.Player;

public class MinasCommand {

    private final Main plugin;

    public MinasCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Command(
            name = "minas",
            description = "Menu de minas para jogadores",
            target = CommandTarget.PLAYER
    )
    public void minasCommand(Context<Player> context) {
        Player player = context.getSender();
        new MinasMainMenu(plugin, player).open();
    }
}