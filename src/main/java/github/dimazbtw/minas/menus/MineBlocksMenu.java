package github.dimazbtw.minas.menus;

import github.dimazbtw.lib.inventories.PaginatedGUI;
import github.dimazbtw.lib.inventories.PaginatedGUIBuilder;
import github.dimazbtw.lib.inventories.ItemButton;
import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.data.MineBlock;
import github.dimazbtw.minas.utils.MapBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MineBlocksMenu implements Listener {

    private final Main plugin;
    private final Player player;
    private final Mine mine;

    // Sistema de tracking de menus ativos
    private static final Map<UUID, MineBlocksMenu> activeMenus = new HashMap<>();

    private boolean addMode = false;
    private boolean isOpen = false;

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

        // ✅ Botão de modo adicionar
        String addModeText = addMode ? "§a§lMODO: Adicionar Bloco §a[ATIVO]" : "§e§lAdicionar Bloco";
        String[] addModeLore = addMode ?
                new String[]{"", "§a✓ Modo ativo!", "§7Clique em qualquer bloco do", "§7seu inventário para adicionar.", "", "§cClique para desativar"} :
                new String[]{"", "§7Clique para ativar o modo de", "§7adicionar blocos.", "", "§7Depois, clique em qualquer bloco", "§7do seu inventário para adicionar."};

        ItemButton addButton = new ItemButton(addMode ? Material.LIME_DYE : Material.EMERALD, addModeText, addModeLore);
        addButton.setDefaultAction(e -> {
            addMode = !addMode;

            if (addMode) {
                player.sendMessage("§a§lMODO ADICIONAR ATIVADO!");
                player.sendMessage("§7Clique em qualquer bloco do seu inventário para adicionar.");
                player.sendMessage("§7Clique no botão verde novamente para desativar.");
            } else {
                player.sendMessage("§c§lMODO ADICIONAR DESATIVADO!");
            }

            open(); // Reabrir para atualizar visual
        });
        builder.setHotbarButton((byte) 4, addButton);

        // Botão de info
        double totalChance = blockList.stream().mapToDouble(MineBlock::getChance).sum();
        ItemButton infoButton = new ItemButton(Material.BOOK, "§6§lInformações",
                "",
                "§7Total de blocos: §f" + blockList.size(),
                "§7Soma das chances: §f" + String.format("%.1f%%", totalChance),
                "",
                totalChance < 99.0 ? "§c⚠ Atenção: Total abaixo de 100%" :
                        totalChance > 101.0 ? "§c⚠ Atenção: Total acima de 100%" : "§a✓ Total OK");
        builder.setHotbarButton((byte) 2, infoButton);

        // Botão de voltar
        ItemButton backButton = new ItemButton(Material.BARRIER, "§c§lVoltar",
                "", "§eClique para voltar");
        backButton.setDefaultAction(e -> {
            close();
            new MineEditMenu(plugin, player, mine).open();
        });
        builder.setHotbarButton((byte) 6, backButton);

        // Conteúdo - blocos
        List<ItemButton> content = new ArrayList<>();
        for (MineBlock block : blockList) {
            content.add(createBlockButton(block));
        }

        builder.setContent(content);

        PaginatedGUI gui = builder.build();

        // Registrar listener
        if (!isOpen) {
            activeMenus.put(player.getUniqueId(), this);
            Bukkit.getPluginManager().registerEvents(this, plugin);
            isOpen = true;
        }

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
        button.addAction(ClickType.LEFT, e -> {
            close();
            new BlockEditMenu(plugin, player, mine, block).open();
        });

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

    // ✅ Listener para cliques no inventário do jogador
    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getWhoClicked().getUniqueId().equals(player.getUniqueId())) return;

        // Só processar se o modo adicionar estiver ativo
        if (!addMode) return;

        // Verificar se clicou no inventário do jogador (bottom inventory)
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().getType() != InventoryType.PLAYER) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        Material material = clickedItem.getType();

        // Verificar se é um bloco
        if (!material.isBlock()) {
            player.sendMessage("§c✗ Isso não é um bloco válido!");
            return;
        }

        // Verificar se já existe
        if (mine.getBlocks().containsKey(material)) {
            player.sendMessage("§c✗ Este bloco já está configurado!");
            return;
        }

        // Adicionar bloco
        MineBlock newBlock = new MineBlock(material);
        newBlock.setChance(50.0);
        mine.addBlock(newBlock);
        mine.save();

        player.sendMessage("§a§l✓ Bloco adicionado: §f" + formatMaterialName(material));

        // Desativar modo adicionar após adicionar
        addMode = false;

        // Reabrir menu
        Bukkit.getScheduler().runTask(plugin, this::open);
    }

    // ✅ Cleanup ao fechar
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        if (!isOpen) return;

        // Aguardar um tick para verificar se não abriu outro menu
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Se não tem inventário aberto ou não é um dos nossos menus
            if (player.getOpenInventory().getTopInventory().getHolder() == null ||
                    !(player.getOpenInventory().getTopInventory().getHolder() instanceof github.dimazbtw.lib.inventories.InventoryGUI)) {
                close();
            }
        }, 1L);
    }

    private void close() {
        if (!isOpen) return;

        addMode = false;
        isOpen = false;
        activeMenus.remove(player.getUniqueId());
        HandlerList.unregisterAll(this);
    }

    // ✅ Método estático para cleanup global
    public static void cleanupPlayer(UUID uuid) {
        MineBlocksMenu menu = activeMenus.remove(uuid);
        if (menu != null) {
            menu.isOpen = false;
            HandlerList.unregisterAll(menu);
        }
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