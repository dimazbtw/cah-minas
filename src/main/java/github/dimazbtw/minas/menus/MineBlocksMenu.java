package github.dimazbtw.minas.menus;

import github.dimazbtw.lib.inventories.PaginatedGUI;
import github.dimazbtw.lib.inventories.PaginatedGUIBuilder;
import github.dimazbtw.lib.inventories.ItemButton;
import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.data.MineBlock;
import github.dimazbtw.minas.utils.MapBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MineBlocksMenu {

    private final Main plugin;
    private final Player player;
    private final Mine mine;

    public MineBlocksMenu(Main plugin, Player player, Mine mine) {
        this.plugin = plugin;
        this.player = player;
        this.mine = mine;
    }

    public void open() {
        List<MineBlock> blockList = new ArrayList<>(mine.getBlocks().values());

        PaginatedGUIBuilder builder = new PaginatedGUIBuilder(
                "§8Blocos: " + mine.getDisplayName() + " - Página {page}",
                "xxxxxxxxx" +
                        "xxxxxxxxx" +
                        "xxxxxxxxx" +
                        "xxxxxxxxx" +
                        "<#######>");

        builder.setDefaultAllCancell(true);


        // Botões de navegação
        builder.setPreviousPageItem(Material.ARROW, 1, "§ePágina Anterior");
        builder.setNextPageItem(Material.ARROW, 1, "§ePróxima Página");

        // Botão de adicionar bloco
        ItemButton addButton = new ItemButton(Material.EMERALD, "§a§lAdicionar Bloco",
                "", "§7Clique para adicionar um novo bloco",
                "§7O bloco que você estiver segurando será adicionado");
        addButton.setDefaultAction(e -> {
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
            open();
        });
        builder.setHotbarButton((byte) 4, addButton);

        // Botão de info
        ItemButton infoButton = new ItemButton(Material.BOOK, "§6§lInformações",
                "", "§7Total de blocos: §f" + blockList.size());
        builder.setHotbarButton((byte) 2, infoButton);

        // Botão de voltar
        ItemButton backButton = new ItemButton(Material.BARRIER, "§c§lVoltar",
                "", "§eClique para voltar");
        backButton.setDefaultAction(e -> new MineEditMenu(plugin, player, mine).open()); // ✅ CORRIGIDO
        builder.setHotbarButton((byte) 6, backButton);

        // Conteúdo - blocos
        List<ItemButton> content = new ArrayList<>();
        for (MineBlock block : blockList) {
            content.add(createBlockButton(block));
        }

        builder.setContent(content);

        PaginatedGUI gui = builder.build();
        gui.show(player);
    }

    private ItemButton createBlockButton(MineBlock block) {
        Material material = block.getMaterial();

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Chance: §f" + block.getChance() + "%");
        lore.add("§7XP: §f" + block.getExp());
        lore.add("§7Dinheiro: §f$" + block.getMoney());

        if (!block.getRewards().isEmpty()) {
            lore.add("");
            lore.add("§7Rewards: §f" + block.getRewards().size());
        }

        lore.add("");
        lore.add("§e⬥ Esquerdo §8- §fEditar");
        lore.add("§e⬥ Direito §8- §fRemover");

        ItemButton button = new ItemButton(material, "§f" + formatMaterialName(material),
                lore.toArray(new String[0]));

        // Editar bloco
        button.addAction(ClickType.LEFT, e -> new BlockEditMenu(plugin, player, mine, block).open()); // ✅ CORRIGIDO

        // Remover bloco
        button.addAction(ClickType.RIGHT, e -> {
            mine.removeBlock(block.getMaterial());
            mine.save();
            plugin.getLanguageManager().sendMessage(player, "blocks.removed",
                    MapBuilder.of("block", formatMaterialName(block.getMaterial())));
            open();
        });

        return button;
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
}