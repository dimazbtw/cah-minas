package github.dimazbtw.minas.menus;

import github.dimazbtw.lib.inventories.InventoryGUI;
import github.dimazbtw.lib.inventories.InventorySize;
import github.dimazbtw.lib.inventories.ItemButton;
import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.PickaxeData;
import github.dimazbtw.minas.data.RankingEntry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RankingMenu {

    private final Main plugin;
    private final Player player;
    private RankingType currentType;
    private List<RankingEntry> currentRanking;
    private boolean loading;

    public enum RankingType {
        BLOCKS("Blocos Minerados", Material.COBBLESTONE),
        EXP("Nível de Picareta", Material.EXPERIENCE_BOTTLE);

        private final String displayName;
        private final Material icon;

        RankingType(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getIcon() {
            return icon;
        }
    }

    public RankingMenu(Main plugin, Player player) {
        this(plugin, player, RankingType.BLOCKS);
    }

    public RankingMenu(Main plugin, Player player, RankingType type) {
        this.plugin = plugin;
        this.player = player;
        this.currentType = type;
        this.currentRanking = new ArrayList<>();
        this.loading = true;
    }

    public void open() { // ✅ CORRIGIDO - Método open() agora inicia o processo
        openLoadingMenu();
        loadRanking();
    }

    private void openLoadingMenu() {
        InventoryGUI gui = new InventoryGUI(
                ChatColor.translateAlternateColorCodes('§', "§8⛏ Ranking - " + currentType.getDisplayName()),
                InventorySize.FIVE_ROWS);
        gui.setDefaultAllCancell(true);

        // Mensagem de carregamento
        ItemButton loadingBtn = new ItemButton(Material.CLOCK, "§e§lCarregando...",
                "",
                "§7A obter dados do ranking...");
        gui.setButton(22, loadingBtn);

        setupNavigationButtons(gui);
        gui.show(player);
    }

    private void loadRanking() {
        if (currentType == RankingType.BLOCKS) {
            plugin.getRankingManager().getBlocksRanking(10).thenAccept(ranking -> {
                currentRanking = ranking;
                loading = false;
                Bukkit.getScheduler().runTask(plugin, this::openRankingMenu);
            });
        } else {
            plugin.getRankingManager().getExpRanking(10).thenAccept(ranking -> {
                currentRanking = ranking;
                loading = false;
                Bukkit.getScheduler().runTask(plugin, this::openRankingMenu);
            });
        }
    }

    private void openRankingMenu() {
        InventoryGUI gui = new InventoryGUI(
                ChatColor.translateAlternateColorCodes('§', "§8⛏ Ranking - " + currentType.getDisplayName()),
                InventorySize.FIVE_ROWS);
        gui.setDefaultAllCancell(true);

        if (currentRanking.isEmpty()) {
            ItemButton emptyBtn = new ItemButton(Material.BARRIER, "§c§lSem Dados",
                    "",
                    "§7Ainda não há jogadores no ranking.");
            gui.setButton(22, emptyBtn);
        } else {
            int[] slots = {10, 11, 12, 13, 14, 15, 16, 21, 22, 23};
            String[] colors = {"§a", "§e", "§f", "§7", "§7", "§7", "§7", "§7", "§7", "§7"};
            String[] prefixes = {"§6§l1º", "§f§l2º", "§c§l3º", "§74º", "§75º", "§76º", "§77º", "§78º", "§79º", "§710º"};

            for (int i = 0; i < Math.min(currentRanking.size(), 10); i++) {
                RankingEntry entry = currentRanking.get(i);
                gui.setButton(slots[i], createRankingButton(entry, prefixes[i], colors[i]));
            }
        }

        setupNavigationButtons(gui);
        gui.show(player);
    }

    private void setupNavigationButtons(InventoryGUI gui) {
        // Voltar
        ItemButton backBtn = new ItemButton(Material.ARROW, "§c§lVoltar",
                "",
                "§eClique para voltar");
        backBtn.setDefaultAction(e -> new MinasMainMenu(plugin, player).open()); // ✅ CORRIGIDO
        gui.setButton(36, backBtn);

        // Trocar tipo de ranking
        RankingType otherType = currentType == RankingType.BLOCKS ? RankingType.EXP : RankingType.BLOCKS;
        ItemButton switchBtn = new ItemButton(otherType.getIcon(),
                "§6§lVer: " + otherType.getDisplayName(),
                "",
                "§7Clique para ver o ranking de",
                "§7" + otherType.getDisplayName().toLowerCase() + ".",
                "",
                "§eClique para trocar!");
        switchBtn.setDefaultAction(e -> {
            if (!loading) {
                new RankingMenu(plugin, player, otherType).open(); // ✅ CORRIGIDO
            }
        });
        gui.setButton(40, switchBtn);

        // Estatísticas do jogador
        PickaxeData data = plugin.getDatabaseManager().getPickaxeData(player.getUniqueId());
        ItemButton statsBtn = new ItemButton(Material.PLAYER_HEAD, "§a§lTuas Estatísticas",
                "",
                "§7Blocos Minerados: §f" + formatNumber(data.getBlocksMined()),
                "§7Nível: §f" + data.getLevel(),
                "§7EXP: §f" + data.getExp() + "/" + data.getExpForNextLevel());
        statsBtn.setHead(player.getName());
        gui.setButton(44, statsBtn);
    }

    private ItemButton createRankingButton(RankingEntry entry, String prefix, String color) {
        ItemButton button = new ItemButton(Material.PLAYER_HEAD,
                color(prefix + " " + color + entry.getPlayerName()));

        List<String> lore = new ArrayList<>();
        lore.add("");

        if (currentType == RankingType.BLOCKS) {
            lore.add(color("§7Blocos Minerados: §f" + formatNumber(entry.getBlocksMined())));
            lore.add(color("§7Nível: §f" + entry.getLevel()));
        } else {
            lore.add(color("§7Nível: §f" + entry.getLevel()));
            lore.add(color("§7EXP: §f" + entry.getExp()));
            lore.add(color("§7Blocos: §f" + formatNumber(entry.getBlocksMined())));
        }

        if (entry.getUuid().equals(player.getUniqueId())) {
            lore.add("");
            lore.add(color("§a✓ Este és tu!"));
        }

        button.setLore(lore);

        try {
            button.setHead(Bukkit.getOfflinePlayer(entry.getUuid()).getName());
        } catch (Exception ignored) {}

        return button;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('§', text);
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