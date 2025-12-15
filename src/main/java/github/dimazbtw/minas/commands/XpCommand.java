package github.dimazbtw.minas.commands;

import github.dimazbtw.minas.Main;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.annotation.Optional;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.target.CommandTarget;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class XpCommand {

    private final Main plugin;

    public XpCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Command(
            name = "xp",
            description = "Comando principal de XP",
            permission = "minas.xp",
            target = CommandTarget.ALL
    )
    public void xpCommand(Context<CommandSender> context) {
        CommandSender sender = context.getSender();

        sender.sendMessage("§6§l━━━━ Sistema de XP ━━━━");
        sender.sendMessage("");
        sender.sendMessage("§e/xp add <jogador> <quantidade> §8- §7Adiciona XP");
        sender.sendMessage("§e/xp remove <jogador> <quantidade> §8- §7Remove XP");
        sender.sendMessage("§e/xp set <jogador> <quantidade> §8- §7Define XP");
        sender.sendMessage("§e/xp ver [jogador] §8- §7Vê o XP de um jogador");
        sender.sendMessage("");
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━");
    }

    @Command(
            name = "xp.add",
            aliases = {"xp.give", "xp.dar"},
            description = "Adiciona XP a um jogador",
            permission = "minas.xp.admin",
            target = CommandTarget.ALL,
            usage = "xp add <jogador> <quantidade>"
    )
    public void addCommand(Context<CommandSender> context, Player target, int amount) {
        CommandSender sender = context.getSender();

        if (target == null) {
            sender.sendMessage("§cJogador não encontrado ou offline.");
            return;
        }

        if (amount <= 0) {
            sender.sendMessage("§cA quantidade deve ser maior que 0.");
            return;
        }

        target.giveExp(amount);

        sender.sendMessage("§aAdicionado §f" + formatNumber(amount) + " XP §aa §f" + target.getName() + "§a.");
        target.sendMessage("§aRecebeste §f" + formatNumber(amount) + " XP§a!");
    }

    @Command(
            name = "xp.remove",
            aliases = {"xp.take", "xp.tirar"},
            description = "Remove XP de um jogador",
            permission = "minas.xp.admin",
            target = CommandTarget.ALL,
            usage = "xp remove <jogador> <quantidade>"
    )
    public void removeCommand(Context<CommandSender> context, Player target, int amount) {
        CommandSender sender = context.getSender();

        if (target == null) {
            sender.sendMessage("§cJogador não encontrado ou offline.");
            return;
        }

        if (amount <= 0) {
            sender.sendMessage("§cA quantidade deve ser maior que 0.");
            return;
        }

        int currentXp = target.getTotalExperience();
        int newXp = Math.max(0, currentXp - amount);

        target.setTotalExperience(0);
        target.setExp(0);
        target.setLevel(0);
        if (newXp > 0) {
            target.giveExp(newXp);
        }

        int removed = currentXp - newXp;
        sender.sendMessage("§cRemovido §f" + formatNumber(removed) + " XP §cde §f" + target.getName() + "§c.");
        target.sendMessage("§cForam removidos §f" + formatNumber(removed) + " XP§c!");
    }

    @Command(
            name = "xp.set",
            aliases = {"xp.definir"},
            description = "Define o XP de um jogador",
            permission = "minas.xp.admin",
            target = CommandTarget.ALL,
            usage = "xp set <jogador> <quantidade>"
    )
    public void setCommand(Context<CommandSender> context, Player target, int amount) {
        CommandSender sender = context.getSender();

        if (target == null) {
            sender.sendMessage("§cJogador não encontrado ou offline.");
            return;
        }

        if (amount < 0) {
            sender.sendMessage("§cA quantidade não pode ser negativa.");
            return;
        }

        target.setTotalExperience(0);
        target.setExp(0);
        target.setLevel(0);
        if (amount > 0) {
            target.giveExp(amount);
        }

        sender.sendMessage("§aDefinido o XP de §f" + target.getName() + " §apara §f" + formatNumber(amount) + "§a.");
        target.sendMessage("§aO teu XP foi definido para §f" + formatNumber(amount) + "§a!");
    }

    @Command(
            name = "xp.ver",
            aliases = {"xp.see", "xp.check"},
            description = "Vê o XP de um jogador",
            permission = "minas.xp",
            target = CommandTarget.ALL,
            usage = "xp ver [jogador]"
    )
    public void verCommand(Context<CommandSender> context, @Optional Player target) {
        CommandSender sender = context.getSender();

        if (target == null) {
            if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cEspecifica um jogador.");
                return;
            }
        }

        int totalXp = target.getTotalExperience();
        int level = target.getLevel();
        float progress = target.getExp();

        sender.sendMessage("§6§l━━━━ XP de " + target.getName() + " ━━━━");
        sender.sendMessage("");
        sender.sendMessage("§7XP Total: §f" + formatNumber(totalXp));
        sender.sendMessage("§7Nível: §f" + level);
        sender.sendMessage("§7Progresso: §f" + String.format("%.1f%%", progress * 100));
        sender.sendMessage("");
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Command(
            name = "xp.setlevel",
            aliases = {"xp.setlvl"},
            description = "Define o nível de um jogador",
            permission = "minas.xp.admin",
            target = CommandTarget.ALL,
            usage = "xp setlevel <jogador> <nível>"
    )
    public void setLevelCommand(Context<CommandSender> context, Player target, int level) {
        CommandSender sender = context.getSender();

        if (target == null) {
            sender.sendMessage("§cJogador não encontrado ou offline.");
            return;
        }

        if (level < 0) {
            sender.sendMessage("§cO nível não pode ser negativo.");
            return;
        }

        target.setLevel(level);
        target.setExp(0);

        sender.sendMessage("§aDefinido o nível de §f" + target.getName() + " §apara §f" + level + "§a.");
        target.sendMessage("§aO teu nível foi definido para §f" + level + "§a!");
    }

    private String formatNumber(int number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        }
        return String.valueOf(number);
    }
}