package github.dimazbtw.minas.menus;

import github.dimazbtw.lib.inventories.PaginatedGUI;
import github.dimazbtw.lib.inventories.PaginatedGUIBuilder;
import github.dimazbtw.lib.inventories.ItemButton;
import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.utils.MapBuilder;
import github.dimazbtw.minas.utils.SkullUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MineListMenu {

    private final Main plugin;
    private final Player player;

    public MineListMenu(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        Collection<Mine> allMines = plugin.getMineManager().getAllMines();
        List<Mine> mineList = new ArrayList<>(allMines);

        PaginatedGUIBuilder builder = new PaginatedGUIBuilder("§8Lista de Minas - Página {page}",
                "xxxxxxxxx" +
                        "x#######x" +
                        "x#######x" +
                        "x#######x" +
                        "<xxxxxxx>");

        builder.setDefaultAllCancell(true);

        // Botões de navegação
        builder.setPreviousPageItem(Material.ARROW, 1, "§ePágina Anterior");
        builder.setNextPageItem(Material.ARROW, 1, "§ePróxima Página");

        // Botão de criar nova mina
        ItemButton createButton = new ItemButton(Material.EMERALD, "§a§lCriar Nova Mina",
                "§7Clique para criar uma nova mina");
        createButton.setDefaultAction(e -> {
            player.closeInventory();
            player.sendMessage(ChatColor.translateAlternateColorCodes('§',
                    "§eUse: §f/mina criar <id> §epara criar uma nova mina"));
        });
        builder.setHotbarButton((byte) 4, createButton);

        // Botão de info
        ItemButton infoButton = new ItemButton(Material.BOOK, "§6§lInformações",
                "§7Total de minas: §f" + mineList.size());
        builder.setHotbarButton((byte) 2, infoButton);

        // Conteúdo - minas
        List<ItemButton> content = new ArrayList<>();
        for (Mine mine : mineList) {
            content.add(createMineButton(mine));
        }

        builder.setContent(content);

        PaginatedGUI gui = builder.build();
        gui.show(player);
    }

    // MineListMenu.java - Atualizado para usar material da mina
    private ItemButton createMineButton(Mine mine) {
        String status = mine.isConfigured() ? "§a✓ Configurada" : "§c✗ Não configurada";

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7ID: §f" + mine.getId());
        lore.add("§7Status: " + status);

        if (mine.isConfigured()) {
            lore.add("§7Blocos: §f" + mine.getCurrentBlocks() + "/" + mine.getTotalBlocks());
            lore.add("§7Porcentagem: §f" + String.format("%.1f%%", mine.getPercentageRemaining()));
            lore.add("§7Reset em: §f" + formatTime(mine.getTimeUntilReset()));
        }

        lore.add("");
        lore.add("§e⬥ Clique Esquerdo §8- §fEditar");
        lore.add("§e⬥ Clique Direito §8- §fTeleportar");
        lore.add("§e⬥ Shift + Clique §8- §fResetar");

        // Criar ItemStack com o material da mina
        ItemStack item;

        if (mine.isBase64Head()) {
            item = SkullUtils.createSkull(mine.getBase64Texture());
        } else {
            item = new ItemStack(mine.getMaterial());
        }

        ItemButton button = new ItemButton(item);
        button.setName(mine.getDisplayName());
        button.setLore(lore);

        // Editar mina
        button.addAction(ClickType.LEFT, e -> new MineEditMenu(plugin, player, mine).open());

        // Teleportar
        button.addAction(ClickType.RIGHT, e -> {
            if (mine.getSpawn() != null) {
                player.closeInventory();
                player.teleport(mine.getSpawn());
                plugin.getLanguageManager().sendMessage(player, "mine.teleported",
                        MapBuilder.of("mine", mine.getDisplayName()));
            } else {
                plugin.getLanguageManager().sendMessage(player, "mine.no-spawn");
            }
        });

        // Resetar mina
        button.addAction(ClickType.SHIFT_LEFT, e -> {
            if (mine.isConfigured()) {
                mine.reset();
                plugin.getLanguageManager().sendMessage(player, "mine.reset-success",
                        MapBuilder.of("mine", mine.getDisplayName()));
                open();
            }
        });

        button.addAction(ClickType.SHIFT_RIGHT, e -> {
            if (mine.isConfigured()) {
                mine.reset();
                plugin.getLanguageManager().sendMessage(player, "mine.reset-success",
                        MapBuilder.of("mine", mine.getDisplayName()));
                open();
            }
        });

        return button;
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
}