package github.dimazbtw.minas.menus;

import github.dimazbtw.lib.inventories.PaginatedGUI;
import github.dimazbtw.lib.inventories.PaginatedGUIBuilder;
import github.dimazbtw.lib.inventories.ItemButton;
import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.utils.MapBuilder;
import github.dimazbtw.minas.utils.SkullUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PlayerMinesMenu {

    private final Main plugin;
    private final Player player;
    private final List<Mine> availableMines;

    public PlayerMinesMenu(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.availableMines = plugin.getMineManager().getMinesForPlayer(player);
        // Ordenar do menor para o maior (1 a 9)
        this.availableMines.sort(Comparator.comparingInt(Mine::getOrder));
    }

    public void open() {
        PaginatedGUIBuilder builder = new PaginatedGUIBuilder("§8Lista de Minas - Página {page}",
                "xxxxxxxxx" +
                        "x#######x" +
                        "x#######x" +
                        "x#######x" +
                        "<#######>");

        builder.setDefaultAllCancell(true);

        // Botões de navegação
        builder.setPreviousPageItem(Material.ARROW, 1, "§ePágina Anterior");
        builder.setNextPageItem(Material.ARROW, 1, "§ePróxima Página");

        // Botão de voltar
        ItemButton backButton = new ItemButton(Material.BARRIER, "§c§lVoltar",
                "",
                "§eClique para voltar ao menu principal");
        backButton.setDefaultAction(e -> new MinasMainMenu(plugin, player).open()); // ✅ CORRIGIDO
        builder.setHotbarButton((byte) 4, backButton);

        // Conteúdo - minas
        List<ItemButton> content = new ArrayList<>();
        for (int i = 0; i < availableMines.size(); i++) {
            Mine mine = availableMines.get(i);
            boolean isBest = i == 0;
            content.add(createMineButton(mine, isBest));
        }

        builder.setContent(content);

        PaginatedGUI gui = builder.build();
        gui.show(player);
    }

    // PlayerMinesMenu.java - Atualizado para usar material da mina
    private ItemButton createMineButton(Mine mine, boolean isBest) {
        List<String> lore = new ArrayList<>();

        if (isBest) {
            lore.add("");
            lore.add("§a§l★ Sua melhor mina!");
        }

        lore.add("");
        lore.add("§eClique para teleportar!");

        String displayName = isBest ? "§6§l" + mine.getDisplayName() : "§f" + mine.getDisplayName() + " §7[" + String.format("%.1f%%", mine.getPercentageRemaining()) + "]";

        // Criar ItemStack com o material da mina
        org.bukkit.inventory.ItemStack item;

        if (mine.isBase64Head()) {
            // Usar cabeça customizada
            item = SkullUtils.createSkull(mine.getBase64Texture());
        } else {
            // Usar material normal
            item = new org.bukkit.inventory.ItemStack(mine.getMaterial());
        }

        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);

            meta.addItemFlags(
                    org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                    org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS,
                    org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE,
                    org.bukkit.inventory.ItemFlag.HIDE_DESTROYS,
                    org.bukkit.inventory.ItemFlag.HIDE_PLACED_ON,
                    org.bukkit.inventory.ItemFlag.HIDE_POTION_EFFECTS
            );

            item.setItemMeta(meta);
        }

        ItemButton button = new ItemButton(item);

        button.setDefaultAction(e -> {
            if (mine.getSpawn() != null) {
                player.closeInventory();
                player.teleport(mine.getSpawn());
                plugin.getSessionManager().createSession(player, mine);
                plugin.getLanguageManager().sendMessage(player, "mine.teleported",
                        MapBuilder.of("mine", mine.getDisplayName()));
            } else {
                plugin.getLanguageManager().sendMessage(player, "mine.no-spawn");
            }
        });

        return button;
    }
}