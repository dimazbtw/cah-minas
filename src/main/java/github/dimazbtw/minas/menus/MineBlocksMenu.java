package github.dimazbtw.minas.menus;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.data.MineBlock;
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

public class MineBlocksMenu implements InventoryHolder {

    private final Main plugin;
    private final Player player;
    private final Mine mine;
    private final Inventory inventory;
    private final int page;

    private static final int ITEMS_PER_PAGE = 36;

    public MineBlocksMenu(Main plugin, Player player, Mine mine) {
        this(plugin, player, mine, 0);
    }

    public MineBlocksMenu(Main plugin, Player player, Mine mine, int page) {
        this.plugin = plugin;
        this.player = player;
        this.mine = mine;
        this.page = page;
        this.inventory = org.bukkit.Bukkit.createInventory(this, 54,
                ChatColor.translateAlternateColorCodes('&', "&8Blocos: " + mine.getDisplayName()));
        setup();
    }

    private void setup() {
        List<MineBlock> blockList = new ArrayList<>(mine.getBlocks().values());

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, blockList.size());

        // Preencher com blocos
        int slot = 0;
        for (int i = startIndex; i < endIndex && slot < ITEMS_PER_PAGE; i++) {
            MineBlock block = blockList.get(i);
            inventory.setItem(slot, createBlockItem(block));
            slot++;
        }

        // Barra inferior
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 36; i < 54; i++) {
            inventory.setItem(i, glass);
        }

        // Adicionar novo bloco
        inventory.setItem(49, createItem(Material.EMERALD, "&a&lAdicionar Bloco",
                "", "&7Clique para adicionar um novo bloco",
                "&7O bloco que você estiver segurando será adicionado"));

        // Navegação
        int totalPages = (int) Math.ceil((double) blockList.size() / ITEMS_PER_PAGE);

        if (page > 0) {
            inventory.setItem(45, createItem(Material.ARROW, "&ePágina Anterior"));
        }

        if (page < totalPages - 1) {
            inventory.setItem(53, createItem(Material.ARROW, "&ePróxima Página"));
        }

        // Info
        inventory.setItem(47, createItem(Material.BOOK, "&6&lInformações",
                "", "&7Total de blocos: &f" + blockList.size(),
                "&7Página: &f" + (page + 1) + "/" + Math.max(1, totalPages)));

        // Voltar
        inventory.setItem(51, createItem(Material.BARRIER, "&c&lVoltar",
                "", "&eClique para voltar"));
    }

    private ItemStack createBlockItem(MineBlock block) {
        Material material = block.getMaterial();

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7Chance: &f" + block.getChance() + "%");
        lore.add("&7XP: &f" + block.getExp());
        lore.add("&7Dinheiro: &f$" + block.getMoney());

        if (!block.getRewards().isEmpty()) {
            lore.add("");
            lore.add("&7Rewards: &f" + block.getRewards().size());
        }

        lore.add("");
        lore.add("&e⬥ Esquerdo &8- &fEditar");
        lore.add("&e⬥ Direito &8- &fRemover");

        return createItem(material, "&f" + formatMaterialName(material), lore.toArray(new String[0]));
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
        List<MineBlock> blockList = new ArrayList<>(mine.getBlocks().values());

        // Adicionar bloco
        if (slot == 49) {
            ItemStack handItem = player.getInventory().getItemInMainHand();
            if (handItem == null || handItem.getType() == Material.AIR) {
                plugin.getLanguageManager().sendMessage(player, "blocks.hold-item");
                return;
            }

            Material material = handItem.getType();
            if (!material.isBlock()) {
                plugin.getLanguageManager().sendMessage(player, "blocks.not-block");
                return;
            }

            if (mine.getBlocks().containsKey(material)) {
                plugin.getLanguageManager().sendMessage(player, "blocks.already-exists");
                return;
            }

            MineBlock newBlock = new MineBlock(material);
            newBlock.setChance(50.0);
            mine.addBlock(newBlock);
            mine.save();

            plugin.getLanguageManager().sendMessage(player, "blocks.added",
                    MapBuilder.of("block", formatMaterialName(material)));
            setup();
            return;
        }

        // Navegação
        if (slot == 45 && page > 0) {
            new MineBlocksMenu(plugin, player, mine, page - 1).open();
            return;
        }

        int totalPages = (int) Math.ceil((double) blockList.size() / ITEMS_PER_PAGE);
        if (slot == 53 && page < totalPages - 1) {
            new MineBlocksMenu(plugin, player, mine, page + 1).open();
            return;
        }

        // Voltar
        if (slot == 51) {
            new MineEditMenu(plugin, player, mine).open();
            return;
        }

        // Clique em bloco
        int startIndex = page * ITEMS_PER_PAGE;
        int blockIndex = startIndex + slot;

        if (slot < ITEMS_PER_PAGE && blockIndex < blockList.size()) {
            MineBlock block = blockList.get(blockIndex);

            if (clickType == ClickType.LEFT) {
                // Editar bloco
                new BlockEditMenu(plugin, player, mine, block).open();
            } else if (clickType == ClickType.RIGHT) {
                // Remover bloco
                mine.removeBlock(block.getMaterial());
                mine.save();
                plugin.getLanguageManager().sendMessage(player, "blocks.removed",
                        MapBuilder.of("block", formatMaterialName(block.getMaterial())));
                setup();
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}