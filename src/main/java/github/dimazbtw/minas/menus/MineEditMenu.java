package github.dimazbtw.minas.menus;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.utils.MapBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MineEditMenu implements InventoryHolder {

    private final Main plugin;
    private final Player player;
    private final Mine mine;
    private final Inventory inventory;

    public MineEditMenu(Main plugin, Player player, Mine mine) {
        this.plugin = plugin;
        this.player = player;
        this.mine = mine;
        this.inventory = org.bukkit.Bukkit.createInventory(this, 54,
                ChatColor.translateAlternateColorCodes('&', "&8Editar: " + mine.getDisplayName()));
        setup();
    }

    private void setup() {
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, glass);
        }

        // Info central
        inventory.setItem(4, createInfoItem());

        // Spawn
        inventory.setItem(19, createItem(Material.ENDER_PEARL, "&a&lDefinir Spawn",
                "", "&7Atual: " + formatLocation(mine.getSpawn()), "",
                "&eClique para definir na sua posição"));

        // Exit
        inventory.setItem(20, createItem(Material.IRON_DOOR, "&a&lDefinir Saída",
                "", "&7Atual: " + formatLocation(mine.getExit()), "",
                "&eClique para definir na sua posição"));

        // Região
        inventory.setItem(21, createItem(Material.GOLDEN_AXE, "&a&lDefinir Região",
                "", "&7Pos1: " + formatLocation(mine.getPos1()),
                "&7Pos2: " + formatLocation(mine.getPos2()),
                "&7Blocos: &f" + mine.getTotalBlocks(), "",
                "&eClique para iniciar seleção"));

        // Reset Time
        inventory.setItem(22, createItem(Material.CLOCK, "&e&lTempo de Reset",
                "", "&7Atual: &f" + mine.getResetTime() + "s", "",
                "&eEsquerdo: +30s", "&eDireito: -30s", "&eShift: ±300s"));

        // Reset Percentage
        inventory.setItem(23, createItem(Material.HOPPER, "&e&lPorcentagem de Reset",
                "", "&7Atual: &f" + mine.getResetPercentage() + "%", "",
                "&eEsquerdo: +5%", "&eDireito: -5%"));

        // Ordem
        inventory.setItem(24, createItem(Material.COMPARATOR, "&6&lOrdem de Prioridade",
                "", "&7Atual: &f" + mine.getOrder(), "",
                "&7Define a prioridade da mina.",
                "&7Maior valor = maior prioridade", "",
                "&eEsquerdo: +1", "&eDireito: -1", "&eShift: ±10"));

        // PvP
        String pvpStatus = mine.isPvpEnabled() ? "&aAtivado" : "&cDesativado";
        inventory.setItem(25, createItem(Material.DIAMOND_SWORD, "&c&lPvP",
                "", "&7Status: " + pvpStatus, "",
                "&eClique para alternar"));

        // Blocos
        inventory.setItem(31, createItem(Material.CHEST, "&6&lEditar Blocos",
                "", "&7Blocos configurados: &f" + mine.getBlocks().size(), "",
                "&eClique para abrir editor de blocos"));

        // Resetar mina
        inventory.setItem(37, createItem(Material.TNT, "&c&lResetar Mina",
                "", "&7Preenche a mina com blocos", "",
                "&eClique para resetar agora"));

        // Deletar mina
        inventory.setItem(43, createItem(Material.BARRIER, "&4&lDeletar Mina",
                "", "&cAtenção: Esta ação é irreversível!", "",
                "&eShift + Clique para deletar"));

        // Voltar
        inventory.setItem(49, createItem(Material.ARROW, "&c&lVoltar",
                "", "&eClique para voltar à lista"));
    }

    private ItemStack createInfoItem() {
        String status = mine.isConfigured() ? "&aConfigurada" : "&cNão configurada";
        return createItem(Material.BOOK, "&6&l" + mine.getDisplayName(),
                "", "&7ID: &f" + mine.getId(),
                "&7Status: " + status,
                "&7Permissão: &f" + mine.getPermission(),
                "&7Ordem: &f" + mine.getOrder(),
                "", "&7Blocos restantes: &f" + mine.getCurrentBlocks() + "/" + mine.getTotalBlocks(),
                "&7Porcentagem: &f" + String.format("%.1f%%", mine.getPercentageRemaining()));
    }

    private String formatLocation(Location loc) {
        if (loc == null) return "&cNão definido";
        return String.format("&f%d, %d, %d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
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
        switch (slot) {
            case 19: // Spawn
                mine.setSpawn(player.getLocation());
                mine.save();
                plugin.getLanguageManager().sendMessage(player, "mine.spawn-set", MapBuilder.of("mine", mine.getId()));
                setup();
                break;

            case 20: // Exit
                mine.setExit(player.getLocation());
                mine.save();
                plugin.getLanguageManager().sendMessage(player, "mine.exit-set", MapBuilder.of("mine", mine.getId()));
                setup();
                break;

            case 21: // Região
                player.closeInventory();
                plugin.getSelectionManager().startSelection(player, mine.getId());
                break;

            case 22: // Reset Time
                int timeChange = clickType.isShiftClick() ? 300 : 30;
                if (clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT) {
                    mine.setResetTime(mine.getResetTime() + timeChange);
                } else {
                    mine.setResetTime(Math.max(30, mine.getResetTime() - timeChange));
                }
                mine.save();
                setup();
                break;

            case 23: // Reset Percentage
                if (clickType == ClickType.LEFT) {
                    mine.setResetPercentage(Math.min(100, mine.getResetPercentage() + 5));
                } else {
                    mine.setResetPercentage(Math.max(0, mine.getResetPercentage() - 5));
                }
                mine.save();
                setup();
                break;

            case 24: // Ordem
                int orderChange = clickType.isShiftClick() ? 10 : 1;
                if (clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT) {
                    mine.setOrder(mine.getOrder() + orderChange);
                } else {
                    mine.setOrder(Math.max(0, mine.getOrder() - orderChange));
                }
                mine.save();
                setup();
                break;

            case 25: // PvP
                mine.setPvpEnabled(!mine.isPvpEnabled());
                mine.save();
                setup();
                break;

            case 31: // Blocos
                new MineBlocksMenu(plugin, player, mine).open();
                break;

            case 37: // Resetar
                if (mine.isConfigured()) {
                    mine.reset();
                    plugin.getLanguageManager().sendMessage(player, "mine.reset-success",
                            MapBuilder.of("mine", mine.getDisplayName()));
                }
                break;

            case 43: // Deletar
                if (clickType.isShiftClick()) {
                    player.closeInventory();
                    plugin.getMineManager().deleteMine(mine.getId());
                    plugin.getLanguageManager().sendMessage(player, "mine.deleted", MapBuilder.of("mine", mine.getId()));
                }
                break;

            case 49: // Voltar
                new MineListMenu(plugin, player).open();
                break;
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}