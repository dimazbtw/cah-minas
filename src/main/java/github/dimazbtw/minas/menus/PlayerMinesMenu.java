package github.dimazbtw.minas.menus;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.utils.MapBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PlayerMinesMenu implements InventoryHolder {

    private final Main plugin;
    private final Player player;
    private final Inventory inventory;
    private final int page;
    private final List<Mine> availableMines;

    private static final int ITEMS_PER_PAGE = 36;

    public PlayerMinesMenu(Main plugin, Player player) {
        this(plugin, player, 0);
    }

    public PlayerMinesMenu(Main plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = page;
        this.availableMines = plugin.getMineManager().getMinesForPlayer(player);
        this.inventory = org.bukkit.Bukkit.createInventory(this, 54,
                ChatColor.translateAlternateColorCodes('&', "&8Lista de Minas"));
        setup();
    }

    private void setup() {
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, glass);
        }

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, availableMines.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex && slot < ITEMS_PER_PAGE; i++) {
            Mine mine = availableMines.get(i);
            inventory.setItem(slot, createMineItem(mine, i == 0));
            slot++;
        }

        int totalPages = (int) Math.ceil((double) availableMines.size() / ITEMS_PER_PAGE);

        if (page > 0) {
            inventory.setItem(45, createItem(Material.ARROW, "&ePágina Anterior"));
        }

        if (page < totalPages - 1) {
            inventory.setItem(53, createItem(Material.ARROW, "&ePróxima Página"));
        }

        inventory.setItem(49, createItem(Material.BARRIER, "&c&lVoltar",
                "",
                "&eClique para voltar ao menu principal"));

        inventory.setItem(47, createItem(Material.BOOK, "&6&lSuas Minas",
                "", "&7Minas disponíveis: &f" + availableMines.size(),
                "&7Página: &f" + (page + 1) + "/" + Math.max(1, totalPages),
                "", "&eClique em uma mina para teleportar!"));
    }

    private ItemStack createMineItem(Mine mine, boolean isBest) {
        Material material = isBest ? Material.DIAMOND_PICKAXE : Material.IRON_PICKAXE;

        List<String> lore = new ArrayList<>();
        lore.add("");

        if (isBest) {
            lore.add("&a&l★ Sua melhor mina!");
            lore.add("");
        }

        lore.add("&7Ordem: &f" + mine.getOrder());

        if (mine.isConfigured()) {
            lore.add("&7Blocos: &f" + mine.getCurrentBlocks() + "/" + mine.getTotalBlocks());
            lore.add("&7Porcentagem: &f" + String.format("%.1f%%", mine.getPercentageRemaining()));
        }

        lore.add("");
        lore.add("&eClique para teleportar!");

        String displayName = isBest ? "&6&l" + mine.getDisplayName() : mine.getDisplayName();

        return createItem(material, displayName, lore.toArray(new String[0]));
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

        if (lore.length > 0) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(coloredLore);
        }

        item.setItemMeta(meta);
        return item;
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void handleClick(int slot, ClickType clickType) {
        if (slot == 49) {
            new MinasMainMenu(plugin, player).open();
            return;
        }

        if (slot == 45 && page > 0) {
            new PlayerMinesMenu(plugin, player, page - 1).open();
            return;
        }

        int totalPages = (int) Math.ceil((double) availableMines.size() / ITEMS_PER_PAGE);
        if (slot == 53 && page < totalPages - 1) {
            new PlayerMinesMenu(plugin, player, page + 1).open();
            return;
        }

        int startIndex = page * ITEMS_PER_PAGE;
        int mineIndex = startIndex + slot;

        if (slot < ITEMS_PER_PAGE && mineIndex < availableMines.size()) {
            Mine mine = availableMines.get(mineIndex);

            if (mine.getSpawn() != null) {
                player.closeInventory();
                player.teleport(mine.getSpawn());
                plugin.getSessionManager().createSession(player, mine);
                plugin.getLanguageManager().sendMessage(player, "mine.teleported",
                        MapBuilder.of("mine", mine.getDisplayName()));
            } else {
                plugin.getLanguageManager().sendMessage(player, "mine.no-spawn");
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}