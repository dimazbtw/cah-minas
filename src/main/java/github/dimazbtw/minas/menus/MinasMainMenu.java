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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class MinasMainMenu implements InventoryHolder {

    private final Main plugin;
    private final Player player;
    private final Inventory inventory;

    public MinasMainMenu(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = org.bukkit.Bukkit.createInventory(this, 27,
                ChatColor.translateAlternateColorCodes('&', "&8⛏ Menu de Minas"));
        setup();
    }

    private void setup() {
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, glass);
        }

        Mine bestMine = plugin.getMineManager().getBestMineForPlayer(player);
        String mineName = bestMine != null ? bestMine.getDisplayName() : "&cNenhuma disponível";

        inventory.setItem(10, createItem(Material.DIAMOND_PICKAXE, "&a&lEntrar na Mina",
                "",
                "&7Teleporta para a tua melhor mina",
                "&7disponível.",
                "",
                "&7Mina: " + mineName,
                "",
                "&eClique para teleportar!"));

        int totalMinas = plugin.getMineManager().getMinesForPlayer(player).size();

        inventory.setItem(13, createItem(Material.CHEST, "&6&lLista de Minas",
                "",
                "&7Vê todas as minas que tens",
                "&7acesso.",
                "",
                "&7Minas disponíveis: &f" + totalMinas,
                "",
                "&eClique para abrir!"));

        inventory.setItem(16, createPlayerHead("&e&lRanking",
                "",
                "&7Vê os melhores mineradores",
                "&7do servidor!",
                "",
                "&7Categorias:",
                "&f • Blocos Minerados",
                "&f • Nível de Picareta",
                "",
                "&eClique para ver!"));
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

    private ItemStack createPlayerHead(String name, String... lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

        if (lore.length > 0) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(coloredLore);
        }

        head.setItemMeta(meta);
        return head;
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void handleClick(int slot, ClickType clickType) {
        switch (slot) {
            case 10:
                Mine bestMine = plugin.getMineManager().getBestMineForPlayer(player);
                if (bestMine == null) {
                    plugin.getLanguageManager().sendMessage(player, "mine.no-permission-any");
                    return;
                }
                if (bestMine.getSpawn() == null) {
                    plugin.getLanguageManager().sendMessage(player, "mine.no-spawn");
                    return;
                }
                player.closeInventory();
                player.teleport(bestMine.getSpawn());
                plugin.getSessionManager().createSession(player, bestMine);
                plugin.getLanguageManager().sendMessage(player, "mine.teleported",
                        MapBuilder.of("mine", bestMine.getDisplayName()));
                break;

            case 13:
                new PlayerMinesMenu(plugin, player).open();
                break;

            case 16:
                new RankingMenu(plugin, player).open();
                break;
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}