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
import java.util.Collection;
import java.util.List;

public class MineListMenu implements InventoryHolder {

    private final Main plugin;
    private final Player player;
    private final Inventory inventory;
    private final int page;

    private static final int ITEMS_PER_PAGE = 45;

    public MineListMenu(Main plugin, Player player) {
        this(plugin, player, 0);
    }

    public MineListMenu(Main plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = page;
        this.inventory = org.bukkit.Bukkit.createInventory(this, 54,
                ChatColor.translateAlternateColorCodes('&', "&8Lista de Minas"));
        setup();
    }

    private void setup() {
        Collection<Mine> allMines = plugin.getMineManager().getAllMines();
        List<Mine> mineList = new ArrayList<>(allMines);

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, mineList.size());

        // Preencher com minas
        int slot = 0;
        for (int i = startIndex; i < endIndex && slot < ITEMS_PER_PAGE; i++) {
            Mine mine = mineList.get(i);
            inventory.setItem(slot, createMineItem(mine));
            slot++;
        }

        // Barra inferior
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, glass);
        }

        // Botão de criar nova mina
        inventory.setItem(49, createItem(Material.EMERALD, "&a&lCriar Nova Mina",
                "&7Clique para criar uma nova mina"));

        // Navegação
        int totalPages = (int) Math.ceil((double) mineList.size() / ITEMS_PER_PAGE);

        if (page > 0) {
            inventory.setItem(45, createItem(Material.ARROW, "&ePágina Anterior"));
        }

        if (page < totalPages - 1) {
            inventory.setItem(53, createItem(Material.ARROW, "&ePróxima Página"));
        }

        // Info
        inventory.setItem(47, createItem(Material.BOOK, "&6&lInformações",
                "&7Total de minas: &f" + mineList.size(),
                "&7Página: &f" + (page + 1) + "/" + Math.max(1, totalPages)));
    }

    private ItemStack createMineItem(Mine mine) {
        Material material = mine.isConfigured() ? Material.DIAMOND_PICKAXE : Material.WOODEN_PICKAXE;
        String status = mine.isConfigured() ? "&a✓ Configurada" : "&c✗ Não configurada";

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7ID: &f" + mine.getId());
        lore.add("&7Status: " + status);

        if (mine.isConfigured()) {
            lore.add("&7Blocos: &f" + mine.getCurrentBlocks() + "/" + mine.getTotalBlocks());
            lore.add("&7Porcentagem: &f" + String.format("%.1f%%", mine.getPercentageRemaining()));
            lore.add("&7Reset em: &f" + formatTime(mine.getTimeUntilReset()));
        }

        lore.add("");
        lore.add("&e⬥ Clique Esquerdo &8- &fEditar");
        lore.add("&e⬥ Clique Direito &8- &fTeleportar");
        lore.add("&e⬥ Shift + Clique &8- &fResetar");

        return createItem(material, mine.getDisplayName(), lore.toArray(new String[0]));
    }

    private String formatTime(long seconds) {
        if (seconds <= 0) return "Resetando...";

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
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
        Collection<Mine> allMines = plugin.getMineManager().getAllMines();
        List<Mine> mineList = new ArrayList<>(allMines);

        int startIndex = page * ITEMS_PER_PAGE;
        int mineIndex = startIndex + slot;

        // Criar nova mina
        if (slot == 49) {
            player.closeInventory();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&eUse: &f/mina criar <id> &epara criar uma nova mina"));
            return;
        }

        // Navegação
        if (slot == 45 && page > 0) {
            new MineListMenu(plugin, player, page - 1).open();
            return;
        }

        int totalPages = (int) Math.ceil((double) mineList.size() / ITEMS_PER_PAGE);
        if (slot == 53 && page < totalPages - 1) {
            new MineListMenu(plugin, player, page + 1).open();
            return;
        }

        // Clique em mina
        if (slot < ITEMS_PER_PAGE && mineIndex < mineList.size()) {
            Mine mine = mineList.get(mineIndex);

            if (clickType.isShiftClick()) {
                // Resetar mina
                if (mine.isConfigured()) {
                    mine.reset();
                    plugin.getLanguageManager().sendMessage(player, "mine.reset-success",
                            MapBuilder.of("mine", mine.getDisplayName()));
                    new MineListMenu(plugin, player, page).open();
                }
            } else if (clickType == ClickType.LEFT) {
                // Editar mina
                new MineEditMenu(plugin, player, mine).open();
            } else if (clickType == ClickType.RIGHT) {
                // Teleportar
                if (mine.getSpawn() != null) {
                    player.closeInventory();
                    player.teleport(mine.getSpawn());
                    plugin.getLanguageManager().sendMessage(player, "mine.teleported",
                            MapBuilder.of("mine", mine.getDisplayName()));
                } else {
                    plugin.getLanguageManager().sendMessage(player, "mine.no-spawn");
                }
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
