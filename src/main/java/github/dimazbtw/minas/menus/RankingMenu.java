package github.dimazbtw.minas.menus;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.PickaxeData;
import github.dimazbtw.minas.data.RankingEntry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class RankingMenu implements InventoryHolder {

    private final Main plugin;
    private final Player player;
    private final Inventory inventory;
    private RankingType currentType;
    private List<RankingEntry> currentRanking;
    private boolean loading;

    public enum RankingType {
        BLOCKS("Blocos Minerados", Material.DIAMOND_PICKAXE),
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
        this.inventory = Bukkit.createInventory(this, 54,
                ChatColor.translateAlternateColorCodes('&', "&8⛏ Ranking - " + type.getDisplayName()));
        setupLoading();
        loadRanking();
    }

    private void setupLoading() {
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, glass);
        }

        inventory.setItem(22, createItem(Material.CLOCK, "&e&lCarregando...",
                "",
                "&7A obter dados do ranking..."));

        setupNavigationButtons();
    }

    private void setupNavigationButtons() {
        inventory.setItem(45, createItem(Material.ARROW, "&c&lVoltar",
                "",
                "&eClique para voltar"));

        RankingType otherType = currentType == RankingType.BLOCKS ? RankingType.EXP : RankingType.BLOCKS;
        inventory.setItem(49, createItem(otherType.getIcon(), "&6&lVer: " + otherType.getDisplayName(),
                "",
                "&7Clique para ver o ranking de",
                "&7" + otherType.getDisplayName().toLowerCase() + ".",
                "",
                "&eClique para trocar!"));

        PickaxeData data = plugin.getDatabaseManager().getPickaxeData(player.getUniqueId());
        inventory.setItem(53, createPlayerHead(player, "&a&lTuas Estatísticas",
                "",
                "&7Blocos Minerados: &f" + formatNumber(data.getBlocksMined()),
                "&7Nível: &f" + data.getLevel(),
                "&7EXP: &f" + data.getExp() + "/" + data.getExpForNextLevel()));
    }

    private void loadRanking() {
        if (currentType == RankingType.BLOCKS) {
            plugin.getRankingManager().getBlocksRanking(10).thenAccept(ranking -> {
                currentRanking = ranking;
                loading = false;
                Bukkit.getScheduler().runTask(plugin, this::setupRanking);
            });
        } else {
            plugin.getRankingManager().getExpRanking(10).thenAccept(ranking -> {
                currentRanking = ranking;
                loading = false;
                Bukkit.getScheduler().runTask(plugin, this::setupRanking);
            });
        }
    }

    private void setupRanking() {
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, glass);
        }

        inventory.setItem(4, createItem(currentType.getIcon(), "&6&l" + currentType.getDisplayName(),
                "",
                "&7Top 10 jogadores"));

        if (currentRanking.isEmpty()) {
            inventory.setItem(22, createItem(Material.BARRIER, "&c&lSem Dados",
                    "",
                    "&7Ainda não há jogadores no ranking."));
            return;
        }

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
        String[] colors = {"&6", "&f", "&c", "&7", "&7", "&7", "&7", "&7", "&7", "&7"};
        String[] prefixes = {"&6&l1º", "&f&l2º", "&c&l3º", "&74º", "&75º", "&76º", "&77º", "&78º", "&79º", "&710º"};

        for (int i = 0; i < Math.min(currentRanking.size(), 10); i++) {
            RankingEntry entry = currentRanking.get(i);
            inventory.setItem(slots[i], createRankingItem(entry, prefixes[i], colors[i], i + 1));
        }

        setupNavigationButtons();
    }

    private ItemStack createRankingItem(RankingEntry entry, String prefix, String color, int position) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        try {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.getUuid()));
        } catch (Exception ignored) {}

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                prefix + " " + color + entry.getPlayerName()));

        List<String> lore = new ArrayList<>();
        lore.add("");

        if (currentType == RankingType.BLOCKS) {
            lore.add(color("&7Blocos Minerados: &f" + formatNumber(entry.getBlocksMined())));
            lore.add(color("&7Nível: &f" + entry.getLevel()));
        } else {
            lore.add(color("&7Nível: &f" + entry.getLevel()));
            lore.add(color("&7EXP: &f" + entry.getExp()));
            lore.add(color("&7Blocos: &f" + formatNumber(entry.getBlocksMined())));
        }

        if (entry.getUuid().equals(player.getUniqueId())) {
            lore.add("");
            lore.add(color("&a✓ Este és tu!"));
        }

        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));

        if (lore.length > 0) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(color(line));
            }
            meta.setLore(coloredLore);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPlayerHead(Player player, String name, String... lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(color(name));

        if (lore.length > 0) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(color(line));
            }
            meta.setLore(coloredLore);
        }

        head.setItemMeta(meta);
        return head;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private String formatNumber(int number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        }
        return String.valueOf(number);
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void handleClick(int slot, ClickType clickType) {
        if (loading) return;

        switch (slot) {
            case 45:
                new MinasMainMenu(plugin, player).open();
                break;

            case 49:
                RankingType newType = currentType == RankingType.BLOCKS ? RankingType.EXP : RankingType.BLOCKS;
                new RankingMenu(plugin, player, newType).open();
                break;
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}