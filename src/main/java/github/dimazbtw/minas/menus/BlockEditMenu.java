package github.dimazbtw.minas.menus;

import github.dimazbtw.lib.inventories.InventoryGUI;
import github.dimazbtw.lib.inventories.InventorySize;
import github.dimazbtw.lib.inventories.ItemButton;
import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.data.MineBlock;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public class BlockEditMenu {

    private final Main plugin;
    private final Player player;
    private final Mine mine;
    private final MineBlock block;
    private InventoryGUI gui;

    public BlockEditMenu(Main plugin, Player player, Mine mine, MineBlock block) {
        this.plugin = plugin;
        this.player = player;
        this.mine = mine;
        this.block = block;
        createGUI();
    }

    private void createGUI() {
        this.gui = new InventoryGUI("§8Editar: " + formatMaterialName(block.getMaterial()),
                InventorySize.FIVE_ROWS);
        this.gui.setDefaultAllCancell(true);
        setup();
    }

    private void setup() {

        // Bloco central (info)
        ItemButton infoBtn = new ItemButton(block.getMaterial(),
                "§6§l" + formatMaterialName(block.getMaterial()),
                "", "§7Chance: §f" + block.getChance() + "%",
                "§7XP: §f" + block.getExp(),
                "§7Dinheiro: §f$" + block.getMoney(),
                "§7Rewards: §f" + block.getRewards().size());
        gui.setButton(4, infoBtn);

        // Chance
        ItemButton chanceBtn = new ItemButton(Material.GOLD_INGOT, "§e§lChance",
                "", "§7Atual: §f" + block.getChance() + "%", "",
                "§eEsquerdo: +5%", "§eDireito: -5%",
                "§eShift: ±1%");

        chanceBtn.addAction(ClickType.LEFT, e -> adjustChance(5));
        chanceBtn.addAction(ClickType.RIGHT, e -> adjustChance(-5));
        chanceBtn.addAction(ClickType.SHIFT_LEFT, e -> adjustChance(1));
        chanceBtn.addAction(ClickType.SHIFT_RIGHT, e -> adjustChance(-1));

        gui.setButton(20, chanceBtn);

        // XP
        ItemButton xpBtn = new ItemButton(Material.EXPERIENCE_BOTTLE, "§a§lXP",
                "", "§7Atual: §f" + block.getExp(), "",
                "§eEsquerdo: +1", "§eDireito: -1",
                "§eShift: ±10");

        xpBtn.addAction(ClickType.LEFT, e -> adjustExp(1));
        xpBtn.addAction(ClickType.RIGHT, e -> adjustExp(-1));
        xpBtn.addAction(ClickType.SHIFT_LEFT, e -> adjustExp(10));
        xpBtn.addAction(ClickType.SHIFT_RIGHT, e -> adjustExp(-10));

        gui.setButton(22, xpBtn);

        // Dinheiro
        ItemButton moneyBtn = new ItemButton(Material.EMERALD, "§2§lDinheiro",
                "", "§7Atual: §f$" + block.getMoney(), "",
                "§eEsquerdo: +10", "§eDireito: -10",
                "§eShift: ±100");

        moneyBtn.addAction(ClickType.LEFT, e -> adjustMoney(10));
        moneyBtn.addAction(ClickType.RIGHT, e -> adjustMoney(-10));
        moneyBtn.addAction(ClickType.SHIFT_LEFT, e -> adjustMoney(100));
        moneyBtn.addAction(ClickType.SHIFT_RIGHT, e -> adjustMoney(-100));

        gui.setButton(24, moneyBtn);

        // Rewards
        ItemButton rewardsBtn = new ItemButton(Material.CHEST, "§d§lRewards",
                "", "§7Configurados: §f" + block.getRewards().size(), "",
                "§7Edite via config para adicionar rewards",
                "§7Formato: comando:comando2, chance");
        gui.setButton(31, rewardsBtn);

        // Voltar
        ItemButton backBtn = new ItemButton(Material.ARROW, "§c§lVoltar",
                "", "§eClique para voltar");
        backBtn.setDefaultAction(e -> new MineBlocksMenu(plugin, player, mine).open()); // ✅ CORRIGIDO
        gui.setButton(40, backBtn);
    }

    private void adjustChance(double change) {
        double newChance = block.getChance() + change;
        block.setChance(Math.max(0, Math.min(100, newChance)));
        mine.save();
        createGUI();
        open();
    }

    private void adjustExp(int change) {
        int newExp = block.getExp() + change;
        block.setExp(Math.max(0, newExp));
        mine.save();
        createGUI();
        open();
    }

    private void adjustMoney(double change) {
        double newMoney = block.getMoney() + change;
        block.setMoney(Math.max(0, newMoney));
        mine.save();
        createGUI();
        open();
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

    public void open() {
        gui.show(player);
    }
}