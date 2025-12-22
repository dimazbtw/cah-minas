package github.dimazbtw.minas.menus;

import github.dimazbtw.lib.inventories.InventoryGUI;
import github.dimazbtw.lib.inventories.InventorySize;
import github.dimazbtw.lib.inventories.ItemButton;
import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.utils.MapBuilder;
import github.dimazbtw.minas.utils.SkullUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public class MineEditMenu {

    private final Main plugin;
    private final Player player;
    private final Mine mine;
    private InventoryGUI gui;

    public MineEditMenu(Main plugin, Player player, Mine mine) {
        this.plugin = plugin;
        this.player = player;
        this.mine = mine;
        createGUI();
    }

    private void createGUI() {
        this.gui = new InventoryGUI("§8Editar: " + mine.getDisplayName(), InventorySize.SIX_ROWS);
        this.gui.setDefaultAllCancell(true);
        setup();
    }

    private void setup() {

        // Info central
        gui.setButton(4, createInfoButton());

        // ✅ NOVO - Editar Display
        ItemButton displayBtn = new ItemButton(Material.NAME_TAG, "§e§lEditar Nome",
                "", "§7Atual: " + mine.getDisplayName(), "",
                "§eClique para alterar o nome de exibição");
        displayBtn.setDefaultAction(e -> {
            player.closeInventory();
            plugin.getInputManager().requestInput(player, "display", (input) -> {
                mine.setDisplayName(input.replace("&", "§"));
                mine.save();
                plugin.getLanguageManager().sendMessage(player, "mine.display-changed");

                Bukkit.getScheduler().runTask(plugin, () -> {
                    new MineEditMenu(plugin, player, mine).open();
                });
            });
            player.sendMessage("§aDigite o novo nome da mina (use & para cores):");
            player.sendMessage("§7Ou digite §ccancelar §7para cancelar.");
        });
        gui.setButton(30, displayBtn);

        // ✅ NOVO - Editar Material
        String currentMaterial = mine.isBase64Head() ? "§7Cabeça customizada" : "§f" + mine.getMaterial().name();
        ItemButton materialBtn = new ItemButton(mine.getMaterial(), "§6§lEditar Material/Ícone",
                "", "§7Atual: " + currentMaterial, "",
                "§7Digite o nome do material (ex: DIAMOND_PICKAXE)",
                "§7ou cole a URL/hash da textura de cabeça.",
                "",
                "§eClique para alterar");
        materialBtn.setDefaultAction(e -> {
            player.closeInventory();
            plugin.getInputManager().requestInput(player, "material", (input) -> {
                handleMaterialInput(input);
            });
            player.sendMessage("§6§l━━━━ Alterar Material da Mina ━━━━");
            player.sendMessage("");
            player.sendMessage("§aDigite o material, URL ou hash da textura:");
            player.sendMessage("§7Exemplos:");
            player.sendMessage("§f • DIAMOND_PICKAXE");
            player.sendMessage("§f • NETHERITE_PICKAXE");
            player.sendMessage("§f • b52446481d70471ba61177b51cbf8a098c70324e3786b97447c909f077669b7b");
            player.sendMessage("§f • http://textures.minecraft.net/texture/...");
            player.sendMessage("");
            player.sendMessage("§7Ou digite §ccancelar §7para cancelar.");
        });
        gui.setButton(32, materialBtn);

        // Spawn
        ItemButton spawnBtn = new ItemButton(Material.ENDER_PEARL, "§a§lDefinir Spawn",
                "", "§7Atual: " + formatLocation(mine.getSpawn()), "",
                "§eClique para definir na sua posição");
        spawnBtn.setDefaultAction(e -> {
            mine.setSpawn(player.getLocation());
            mine.save();
            plugin.getLanguageManager().sendMessage(player, "mine.spawn-set",
                    MapBuilder.of("mine", mine.getId()));
            createGUI();
            open();
        });
        gui.setButton(19, spawnBtn);

        // Exit
        ItemButton exitBtn = new ItemButton(Material.IRON_DOOR, "§a§lDefinir Saída",
                "", "§7Atual: " + formatLocation(mine.getExit()), "",
                "§eClique para definir na sua posição");
        exitBtn.setDefaultAction(e -> {
            mine.setExit(player.getLocation());
            mine.save();
            plugin.getLanguageManager().sendMessage(player, "mine.exit-set",
                    MapBuilder.of("mine", mine.getId()));
            createGUI();
            open();
        });
        gui.setButton(20, exitBtn);

        // Região
        ItemButton regionBtn = new ItemButton(Material.GOLDEN_AXE, "§a§lDefinir Região",
                "", "§7Pos1: " + formatLocation(mine.getPos1()),
                "§7Pos2: " + formatLocation(mine.getPos2()),
                "§7Blocos: §f" + mine.getTotalBlocks(), "",
                "§eClique para iniciar seleção");
        regionBtn.setDefaultAction(e -> {
            player.closeInventory();
            plugin.getSelectionManager().startSelection(player, mine.getId());
        });
        gui.setButton(21, regionBtn);

        // Reset Time
        ItemButton resetTimeBtn = new ItemButton(Material.CLOCK, "§e§lTempo de Reset",
                "", "§7Atual: §f" + mine.getResetTime() + "s", "",
                "§eEsquerdo: +30s", "§eDireito: -30s", "§eShift: ±300s");

        resetTimeBtn.addAction(ClickType.LEFT, e -> adjustResetTime(30));
        resetTimeBtn.addAction(ClickType.RIGHT, e -> adjustResetTime(-30));
        resetTimeBtn.addAction(ClickType.SHIFT_LEFT, e -> adjustResetTime(300));
        resetTimeBtn.addAction(ClickType.SHIFT_RIGHT, e -> adjustResetTime(-300));

        gui.setButton(22, resetTimeBtn);

        // Reset Percentage
        ItemButton resetPercentBtn = new ItemButton(Material.HOPPER, "§e§lPorcentagem de Reset",
                "", "§7Atual: §f" + mine.getResetPercentage() + "%", "",
                "§eEsquerdo: +5%", "§eDireito: -5%");

        resetPercentBtn.addAction(ClickType.LEFT, e -> adjustResetPercentage(5));
        resetPercentBtn.addAction(ClickType.RIGHT, e -> adjustResetPercentage(-5));

        gui.setButton(23, resetPercentBtn);

        // Ordem
        ItemButton orderBtn = new ItemButton(Material.COMPARATOR, "§6§lOrdem de Prioridade",
                "", "§7Atual: §f" + mine.getOrder(), "",
                "§7Define a prioridade da mina.",
                "§7Maior valor = maior prioridade", "",
                "§eEsquerdo: +1", "§eDireito: -1", "§eShift: ±10");

        orderBtn.addAction(ClickType.LEFT, e -> adjustOrder(1));
        orderBtn.addAction(ClickType.RIGHT, e -> adjustOrder(-1));
        orderBtn.addAction(ClickType.SHIFT_LEFT, e -> adjustOrder(10));
        orderBtn.addAction(ClickType.SHIFT_RIGHT, e -> adjustOrder(-10));

        gui.setButton(24, orderBtn);

        // PvP
        String pvpStatus = mine.isPvpEnabled() ? "§aAtivado" : "§cDesativado";
        ItemButton pvpBtn = new ItemButton(Material.DIAMOND_SWORD, "§c§lPvP",
                "", "§7Status: " + pvpStatus, "",
                "§eClique para alternar");
        pvpBtn.setDefaultAction(e -> {
            mine.setPvpEnabled(!mine.isPvpEnabled());
            mine.save();
            createGUI();
            open();
        });
        gui.setButton(25, pvpBtn);

        // Blocos
        ItemButton blocksBtn = new ItemButton(Material.CHEST, "§6§lEditar Blocos",
                "", "§7Blocos configurados: §f" + mine.getBlocks().size(), "",
                "§eClique para abrir editor de blocos");
        blocksBtn.setDefaultAction(e -> new MineBlocksMenu(plugin, player, mine).open());
        gui.setButton(31, blocksBtn);

        // Resetar mina
        ItemButton resetBtn = new ItemButton(Material.TNT, "§c§lResetar Mina",
                "", "§7Preenche a mina com blocos", "",
                "§eClique para resetar agora");
        resetBtn.setDefaultAction(e -> {
            if (mine.isConfigured()) {
                mine.reset();
                plugin.getLanguageManager().sendMessage(player, "mine.reset-success",
                        MapBuilder.of("mine", mine.getDisplayName()));
            }
        });
        gui.setButton(37, resetBtn);

        // Deletar mina
        ItemButton deleteBtn = new ItemButton(Material.BARRIER, "§4§lDeletar Mina",
                "", "§cAtenção: Esta ação é irreversível!", "",
                "§eShift + Clique para deletar");
        deleteBtn.addAction(ClickType.SHIFT_LEFT, e -> {
            player.closeInventory();
            plugin.getMineManager().deleteMine(mine.getId());
            plugin.getLanguageManager().sendMessage(player, "mine.deleted",
                    MapBuilder.of("mine", mine.getId()));
        });
        deleteBtn.addAction(ClickType.SHIFT_RIGHT, e -> {
            player.closeInventory();
            plugin.getMineManager().deleteMine(mine.getId());
            plugin.getLanguageManager().sendMessage(player, "mine.deleted",
                    MapBuilder.of("mine", mine.getId()));
        });
        gui.setButton(43, deleteBtn);

        // Voltar
        ItemButton backBtn = new ItemButton(Material.ARROW, "§c§lVoltar",
                "", "§eClique para voltar à lista");
        backBtn.setDefaultAction(e -> new MineListMenu(plugin, player).open());
        gui.setButton(49, backBtn);
    }

    // ✅ NOVO - Processar input de material
    private void handleMaterialInput(String input) {
        input = input.trim();

        // Verificar se é cancelamento
        if (input.equalsIgnoreCase("cancelar") || input.equalsIgnoreCase("cancel")) {
            player.sendMessage("§cAlteração cancelada.");
            Bukkit.getScheduler().runTask(plugin, () -> {
                new MineEditMenu(plugin, player, mine).open();
            });
            return;
        }

        // Verificar se é uma URL completa ou hash de textura
        if (input.startsWith("http://") || input.startsWith("https://") || isTextureHash(input)) {
            String normalizedUrl = SkullUtils.normalizeTextureUrl(input);

            if (normalizedUrl != null) {
                mine.setMaterialData(normalizedUrl);
                mine.save();
                player.sendMessage("§aMaterial alterado para cabeça customizada!");
                player.sendMessage("§7Hash: §f" + SkullUtils.extractTextureHash(normalizedUrl));
            } else {
                player.sendMessage("§cURL/Hash de textura inválido!");
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                new MineEditMenu(plugin, player, mine).open();
            });
            return;
        }

        // Tentar como material
        try {
            Material material = Material.valueOf(input.toUpperCase());
            mine.setMaterialData(material.name());
            mine.save();
            player.sendMessage("§aMaterial alterado para §f" + material.name() + "§a!");
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cMaterial inválido: §f" + input);
            player.sendMessage("§7Certifica-te de que digitaste corretamente.");
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            new MineEditMenu(plugin, player, mine).open();
        });
    }

    /**
     * Verifica se uma string parece ser um hash de textura (64 caracteres hexadecimais)
     */
    private boolean isTextureHash(String input) {
        // Hash de textura do Minecraft tem exatamente 64 caracteres hexadecimais
        return input != null && input.matches("^[a-fA-F0-9]{64}$");
    }

    private ItemButton createInfoButton() {
        String status = mine.isConfigured() ? "§aConfigurada" : "§cNão configurada";
        return new ItemButton(Material.BOOK, "§6§l" + mine.getDisplayName(),
                "", "§7ID: §f" + mine.getId(),
                "§7Status: " + status,
                "§7Permissão: §f" + mine.getPermission(),
                "§7Ordem: §f" + mine.getOrder(),
                "", "§7Blocos restantes: §f" + mine.getCurrentBlocks() + "/" + mine.getTotalBlocks(),
                "§7Porcentagem: §f" + String.format("%.1f%%", mine.getPercentageRemaining()));
    }

    private void adjustResetTime(int change) {
        mine.setResetTime(Math.max(30, mine.getResetTime() + change));
        mine.save();
        createGUI();
        open();
    }

    private void adjustResetPercentage(int change) {
        double newValue = mine.getResetPercentage() + change;
        mine.setResetPercentage(Math.max(0, Math.min(100, newValue)));
        mine.save();
        createGUI();
        open();
    }

    private void adjustOrder(int change) {
        mine.setOrder(Math.max(0, mine.getOrder() + change));
        mine.save();
        createGUI();
        open();
    }

    private String formatLocation(Location loc) {
        if (loc == null) return "§cNão definido";
        return String.format("§f%d, %d, %d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public void open() {
        gui.show(player);
    }
}