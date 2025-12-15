package github.dimazbtw.minas.commands;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.data.PickaxeSkin;
import github.dimazbtw.minas.menus.MineEditMenu;
import github.dimazbtw.minas.menus.MineListMenu;
import github.dimazbtw.minas.utils.MapBuilder;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.annotation.Optional;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.target.CommandTarget;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class MinaAdminCommand {

    private final Main plugin;

    public MinaAdminCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Command(
            name = "minaadmin",
            aliases = {"mineadmin", "madmin"},
            description = "Menu administrativo de minas",
            permission = "minas.admin",
            target = CommandTarget.PLAYER
    )
    public void minaAdminCommand(Context<Player> context) {
        Player player = context.getSender();
        new MineListMenu(plugin, player).open();
    }

    @Command(
            name = "minaadmin.criar",
            aliases = {"minaadmin.create", "minaadmin.new"},
            description = "Cria uma nova mina",
            permission = "minas.admin",
            target = CommandTarget.PLAYER,
            usage = "minaadmin criar <id>"
    )
    public void criarCommand(Context<Player> context, String id) {
        Player player = context.getSender();

        if (!id.matches("^[a-zA-Z0-9_-]+$")) {
            plugin.getLanguageManager().sendMessage(player, "mine.invalid-id");
            return;
        }

        if (plugin.getMineManager().mineExists(id)) {
            plugin.getLanguageManager().sendMessage(player, "mine.already-exists",
                    MapBuilder.of("mine", id));
            return;
        }

        Mine mine = plugin.getMineManager().createMine(id);
        if (mine == null) {
            plugin.getLanguageManager().sendMessage(player, "mine.create-error");
            return;
        }

        plugin.getLanguageManager().sendMessage(player, "mine.created",
                MapBuilder.of("mine", id));

        plugin.getSelectionManager().startSelection(player, id);
    }

    @Command(
            name = "minaadmin.deletar",
            aliases = {"minaadmin.delete", "minaadmin.remover"},
            description = "Deleta uma mina",
            permission = "minas.admin",
            target = CommandTarget.ALL,
            usage = "minaadmin deletar <id>"
    )
    public void deletarCommand(Context<CommandSender> context, String id) {
        CommandSender sender = context.getSender();

        if (!plugin.getMineManager().mineExists(id)) {
            plugin.getLanguageManager().sendMessage(sender, "mine.not-found");
            return;
        }

        if (plugin.getMineManager().deleteMine(id)) {
            plugin.getLanguageManager().sendMessage(sender, "mine.deleted",
                    MapBuilder.of("mine", id));
        } else {
            plugin.getLanguageManager().sendMessage(sender, "mine.delete-error");
        }
    }

    @Command(
            name = "minaadmin.editar",
            aliases = {"minaadmin.edit"},
            description = "Edita uma mina",
            permission = "minas.admin",
            target = CommandTarget.PLAYER,
            usage = "minaadmin editar <id>"
    )
    public void editarCommand(Context<Player> context, String id) {
        Player player = context.getSender();

        Mine mine = plugin.getMineManager().getMine(id);
        if (mine == null) {
            plugin.getLanguageManager().sendMessage(player, "mine.not-found");
            return;
        }

        new MineEditMenu(plugin, player, mine).open();
    }

    @Command(
            name = "minaadmin.confirmar",
            aliases = {"minaadmin.confirm"},
            description = "Confirma a seleção da região",
            permission = "minas.admin",
            target = CommandTarget.PLAYER
    )
    public void confirmarCommand(Context<Player> context) {
        Player player = context.getSender();
        plugin.getSelectionManager().confirmSelection(player);
    }

    @Command(
            name = "minaadmin.cancelar",
            aliases = {"minaadmin.cancel"},
            description = "Cancela a seleção atual",
            permission = "minas.admin",
            target = CommandTarget.PLAYER
    )
    public void cancelarCommand(Context<Player> context) {
        Player player = context.getSender();

        if (!plugin.getSelectionManager().isSelecting(player)) {
            plugin.getLanguageManager().sendMessage(player, "selection.not-selecting");
            return;
        }

        plugin.getSelectionManager().cancelSelection(player);
    }

    @Command(
            name = "minaadmin.reload",
            description = "Recarrega o plugin",
            permission = "minas.admin",
            target = CommandTarget.ALL
    )
    public void reloadCommand(Context<CommandSender> context) {
        CommandSender sender = context.getSender();
        plugin.reload();
        plugin.getLanguageManager().sendMessage(sender, "general.reloaded");
    }

    @Command(
            name = "minaadmin.reset",
            description = "Reseta os dados da picareta de um jogador",
            permission = "minas.admin",
            target = CommandTarget.ALL,
            usage = "minaadmin reset <jogador>"
    )
    public void resetCommand(Context<CommandSender> context, Player player) {
        CommandSender sender = context.getSender();

        if (player == null) {
            sender.sendMessage("§cJogador inválido ou offline.");
            return;
        }

        UUID uuid = player.getUniqueId();

        plugin.getDatabaseManager().resetPickaxeData(uuid);
        plugin.getPickaxeManager().updatePickaxe(player);

        sender.sendMessage("§aDados da picareta de " + player.getName() + " resetados com sucesso.");
    }

    @Command(
            name = "minaadmin.giveskin",
            aliases = {"minaadmin.darskin"},
            description = "Dá um item de ativação de skin a um jogador",
            permission = "minas.admin",
            target = CommandTarget.ALL,
            usage = "minaadmin giveskin <jogador> <ordem> [quantidade]"
    )
    public void giveSkinCommand(Context<CommandSender> context, Player target, int ordem, @Optional Integer quantidade) {
        CommandSender sender = context.getSender();

        if (target == null) {
            sender.sendMessage("§cJogador não encontrado ou offline.");
            return;
        }

        PickaxeSkin skin = plugin.getSkinManager().getSkinByOrder(ordem);
        if (skin == null) {
            sender.sendMessage("§cSkin com ordem " + ordem + " não encontrada!");
            sender.sendMessage("§7Skins disponíveis:");
            for (PickaxeSkin s : plugin.getSkinManager().getAllSkins()) {
                sender.sendMessage("§7 - Ordem " + s.getOrder() + ": " + s.getDisplay());
            }
            return;
        }

        int qtd = quantidade != null ? quantidade : 1;
        if (qtd < 1) qtd = 1;
        if (qtd > 64) qtd = 64;

        ItemStack skinItem = plugin.getSkinManager().createSkinActivationItem(ordem);
        if (skinItem == null) {
            sender.sendMessage("§cErro ao criar item de skin!");
            return;
        }

        skinItem.setAmount(qtd);

        target.getInventory().addItem(skinItem);

        sender.sendMessage("§aItem de skin §f" + skin.getDisplay() + " §a(x" + qtd + ") dado a §f" + target.getName() + "§a!");
        target.sendMessage("§aRecebeste um item de ativação de skin: " + skin.getDisplay() + "§a!");
    }

    @Command(
            name = "minaadmin.skins",
            aliases = {"minaadmin.listskins"},
            description = "Lista todas as skins disponíveis",
            permission = "minas.admin",
            target = CommandTarget.ALL
    )
    public void listSkinsCommand(Context<CommandSender> context) {
        CommandSender sender = context.getSender();

        sender.sendMessage("§6§l━━━━ Skins Disponíveis ━━━━");
        sender.sendMessage("");

        for (PickaxeSkin skin : plugin.getSkinManager().getAllSkins()) {
            double bonusPercent = (skin.getBonus() - 1.0) * 100;
            sender.sendMessage("§e Ordem " + skin.getOrder() + ": " + skin.getDisplay());
            sender.sendMessage("§7   Material: §f" + skin.getMaterial().name());
            sender.sendMessage("§7   Bónus: §a+" + String.format("%.0f", bonusPercent) + "%");
            sender.sendMessage("");
        }

        sender.sendMessage("§7Use: §f/minaadmin giveskin <jogador> <ordem> [quantidade]");
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Command(
            name = "minaadmin.setskin",
            description = "Define diretamente a skin de um jogador",
            permission = "minas.admin",
            target = CommandTarget.ALL,
            usage = "minaadmin setskin <jogador> <ordem>"
    )
    public void setSkinCommand(Context<CommandSender> context, Player target, int ordem) {
        CommandSender sender = context.getSender();

        if (target == null) {
            sender.sendMessage("§cJogador não encontrado ou offline.");
            return;
        }

        PickaxeSkin skin = plugin.getSkinManager().getSkinByOrder(ordem);
        if (skin == null) {
            sender.sendMessage("§cSkin com ordem " + ordem + " não encontrada!");
            return;
        }

        plugin.getSkinManager().activateSkin(target, ordem);
        sender.sendMessage("§aSkin §f" + skin.getDisplay() + " §adefinida para §f" + target.getName() + "§a!");
    }
}