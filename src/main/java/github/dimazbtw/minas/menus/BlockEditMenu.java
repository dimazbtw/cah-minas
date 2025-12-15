package github.dimazbtw.minas.menus;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.data.MineBlock;
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

public class BlockEditMenu implements InventoryHolder {

    private final Main plugin;
    private final Player player;
    private final Mine mine;
    private final MineBlock block;
    private final Inventory inventory;

    public BlockEditMenu(Main plugin, Player player, Mine mine, MineBlock block) {
        this.plugin = plugin;
        this.player = player;
        this.mine = mine;
        this.block = block;
        this.inventory = org.bukkit.Bukkit.createInventory(this, 45,
                ChatColor.translateAlternateColorCodes('&', "&8Editar: " + formatMaterialName(block.getMaterial())));
        setup();
    }

    private void setup() {
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, glass);
        }

        // Bloco central (info)
        inventory.setItem(4, createItem(block.getMaterial(), "&6&l" + formatMaterialName(block.getMaterial()),
                "", "&7Chance: &f" + block.getChance() + "%",
                "&7XP: &f" + block.getExp(),
                "&7Dinheiro: &f$" + block.getMoney(),
                "&7Rewards: &f" + block.getRewards().size()));

        // Chance
        inventory.setItem(20, createItem(Material.GOLD_INGOT, "&e&lChance",
                "", "&7Atual: &f" + block.getChance() + "%", "",
                "&eEsquerdo: +5%", "&eDireito: -5%",
                "&eShift: ±1%"));

        // XP
        inventory.setItem(22, createItem(Material.EXPERIENCE_BOTTLE, "&a&lXP",
                "", "&7Atual: &f" + block.getExp(), "",
                "&eEsquerdo: +1", "&eDireito: -1",
                "&eShift: ±10"));

        // Dinheiro
        inventory.setItem(24, createItem(Material.EMERALD, "&2&lDinheiro",
                "", "&7Atual: &f$" + block.getMoney(), "",
                "&eEsquerdo: +10", "&eDireito: -10",
                "&eShift: ±100"));

        // Rewards
        inventory.setItem(31, createItem(Material.CHEST, "&d&lRewards",
                "", "&7Configurados: &f" + block.getRewards().size(), "",
                "&7Edite via config para adicionar rewards",
                "&7Formato: comando:comando2, chance"));

        // Voltar
        inventory.setItem(40, createItem(Material.ARROW, "&c&lVoltar",
                "", "&eClique para voltar"));
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        StringBuilder result = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1)).append(" ");
            }
        }
        return result.toString().trim();
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
        boolean shift = clickType.isShiftClick();
        boolean left = clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT;

        switch (slot) {
            case 20: // Chance
                double chanceChange = shift ? 1 : 5;
                if (left) {
                    block.setChance(Math.min(100, block.getChance() + chanceChange));
                } else {
                    block.setChance(Math.max(0, block.getChance() - chanceChange));
                }
                mine.save();
                setup();
                break;

            case 22: // XP
                int xpChange = shift ? 10 : 1;
                if (left) {
                    block.setExp(block.getExp() + xpChange);
                } else {
                    block.setExp(Math.max(0, block.getExp() - xpChange));
                }
                mine.save();
                setup();
                break;

            case 24: // Dinheiro
                double moneyChange = shift ? 100 : 10;
                if (left) {
                    block.setMoney(block.getMoney() + moneyChange);
                } else {
                    block.setMoney(Math.max(0, block.getMoney() - moneyChange));
                }
                mine.save();
                setup();
                break;

            case 40: // Voltar
                new MineBlocksMenu(plugin, player, mine).open();
                break;
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}