package github.dimazbtw.minas.menus;

import github.dimazbtw.lib.inventories.InventoryGUI;
import github.dimazbtw.lib.inventories.InventorySize;
import github.dimazbtw.lib.inventories.ItemButton;
import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.utils.MapBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class MinasMainMenu {

    private final Main plugin;
    private final Player player;
    private final InventoryGUI gui;

    public MinasMainMenu(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.gui = new InventoryGUI("§8⛏ Minas - Menu Principal", InventorySize.THREE_ROWS);
        this.gui.setDefaultAllCancell(true);
        setup();
    }

    private void setup() {

        Mine bestMine = plugin.getMineManager().getBestMineForPlayer(player);
        String mineName = bestMine != null ? bestMine.getDisplayName() : "§cNenhuma disponível";

        // Botão: Entrar na Mina
        ItemButton enterMine = new ItemButton(Material.DARK_OAK_DOOR, "§a§lEntrar na Mina",
                "",
                "§7Teleporta para a tua melhor mina",
                "§7disponível.",
                "",
                "§7Mina: " + mineName,
                "",
                "§eClique para teleportar!");

        enterMine.setDefaultAction(e -> {
            Mine mine = plugin.getMineManager().getBestMineForPlayer(player);
            if (mine == null) {
                plugin.getLanguageManager().sendMessage(player, "mine.no-permission-any");
                return;
            }
            if (mine.getSpawn() == null) {
                plugin.getLanguageManager().sendMessage(player, "mine.no-spawn");
                return;
            }
            player.closeInventory();
            player.teleport(mine.getSpawn());
            plugin.getSessionManager().createSession(player, mine);
            plugin.getLanguageManager().sendMessage(player, "mine.teleported",
                    MapBuilder.of("mine", mine.getDisplayName()));
        });

        int totalMinas = plugin.getMineManager().getMinesForPlayer(player).size();

        // Botão: Lista de Minas
        ItemButton listMines = new ItemButton(Material.CHEST, "§6§lLista de Minas",
                "",
                "§7Vê todas as minas que tens",
                "§7acesso.",
                "",
                "§7Minas disponíveis: §f" + totalMinas,
                "",
                "§eClique para abrir!");

        listMines.setDefaultAction(e -> new PlayerMinesMenu(plugin, player).open());

        // Botão: Ranking
        ItemButton ranking = new ItemButton(Material.PLAYER_HEAD, "§e§lRanking",
                "",
                "§7Vê os melhores mineradores",
                "§7do servidor!",
                "",
                "§7Categorias:",
                "§f • Blocos Minerados",
                "§f • Nível de Picareta",
                "",
                "§eClique para ver!");

        ranking.setHead(player.getName());
        ranking.setDefaultAction(e -> new RankingMenu(plugin, player).open()); // ✅ CORRIGIDO

        gui.setButton(13, enterMine);
        gui.setButton(11, listMines);
        gui.setButton(15, ranking);
    }

    public void open() {
        gui.show(player);
    }
}