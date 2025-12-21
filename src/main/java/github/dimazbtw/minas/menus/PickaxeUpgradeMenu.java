package github.dimazbtw.minas.menus;

import github.dimazbtw.lib.inventories.InventoryGUI;
import github.dimazbtw.lib.inventories.ItemButton;
import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.EnchantConfig;
import github.dimazbtw.minas.data.PickaxeData;
import github.dimazbtw.minas.managers.EnchantManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class PickaxeUpgradeMenu {

    private final Main plugin;
    private final Player player;
    private final EnchantManager enchantManager;
    private InventoryGUI gui;

    private final Map<String, Long> clickCooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 200; // 200ms entre cliques

    public PickaxeUpgradeMenu(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.enchantManager = plugin.getEnchantManager();
        createGUI();
    }

    private void createGUI() {
        this.gui = new InventoryGUI(enchantManager.getMenuTitle(), enchantManager.getMenuSize());
        this.gui.setDefaultAllCancell(true);
        setup();
    }

    private void setup() {
        // Filler (borda)
        if (enchantManager.isFillerEnabled()) {
            ItemStack fillerStack = enchantManager.createFillerItem();
            hideAttributes(fillerStack);
            ItemButton filler = new ItemButton(fillerStack);
            for (int i = 0; i < gui.getInventory().getSize(); i++) {
                gui.setButton(i, filler);
            }
        }

        // Info da picareta
        ItemStack pickaxeInfoStack = enchantManager.createPickaxeInfoItem(player);
        hideAttributes(pickaxeInfoStack);
        ItemButton pickaxeInfo = new ItemButton(pickaxeInfoStack);
        gui.setButton(enchantManager.getPickaxeInfoSlot(), pickaxeInfo);

        // XP do jogador
        ItemStack playerXpStack = enchantManager.createPlayerXpItem(player);
        hideAttributes(playerXpStack);
        ItemButton playerXp = new ItemButton(playerXpStack);
        gui.setButton(enchantManager.getPlayerXpSlot(), playerXp);

        // Encantamentos
        PickaxeData data = plugin.getDatabaseManager().getPickaxeData(player.getUniqueId());
        int pickaxeLevel = data.getLevel();

        for (EnchantConfig enchant : enchantManager.getAllEnchants()) {
            int currentLevel = enchantManager.getEnchantLevel(player, enchant.getId());
            boolean canAfford = enchantManager.canAfford(player, enchant.getId());

            ItemStack itemStack = enchant.createMenuItem(currentLevel, canAfford, pickaxeLevel);
            hideAttributes(itemStack);
            ItemButton button = new ItemButton(itemStack);

            // Adicionar ações
            setupEnchantActions(button, enchant.getId());

            gui.setButton(enchant.getSlot(), button);
        }
    }

    /**
     * Esconde todos os atributos e flags de um ItemStack
     */
    private void hideAttributes(ItemStack item) {
        if (item == null || item.getType().isAir()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Esconder todos os atributos
        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_PLACED_ON,
                ItemFlag.HIDE_POTION_EFFECTS
        );

        item.setItemMeta(meta);
    }

    private boolean isOnCooldown(String enchantId) {
        Long lastClick = clickCooldowns.get(enchantId);
        if (lastClick == null) return false;

        long timeSince = System.currentTimeMillis() - lastClick;
        return timeSince < COOLDOWN_MS;
    }

    private void setCooldown(String enchantId) {
        clickCooldowns.put(enchantId, System.currentTimeMillis());
    }

    private void setupEnchantActions(ItemButton button, String enchantId) {
        EnchantConfig enchant = enchantManager.getEnchant(enchantId);
        if (enchant == null) return;

        // LEFT: Upgrade 1 nível
        button.addAction(ClickType.LEFT, e -> {
            if (isOnCooldown(enchantId)) {
                plugin.getLogger().warning("Duplicate click prevented for " + enchantId);
                return;
            }
            setCooldown(enchantId);
            handleUpgrade(enchantId, 1);
        });

        button.addAction(ClickType.SHIFT_LEFT, e -> {
            if (isOnCooldown(enchantId)) return;
            setCooldown(enchantId);
            handleUpgrade(enchantId, 1);
        });

        // RIGHT: Seleção de quantidade
        button.addAction(ClickType.RIGHT, e -> {
            if (isOnCooldown(enchantId)) return;
            setCooldown(enchantId);
            openAmountSelection(enchantId);
        });

        button.addAction(ClickType.SHIFT_RIGHT, e -> {
            if (isOnCooldown(enchantId)) return;
            setCooldown(enchantId);
            openAmountSelection(enchantId);
        });

        // DROP: Upgrade máximo
        button.addAction(ClickType.DROP, e -> {
            if (isOnCooldown(enchantId)) return;
            setCooldown(enchantId);

            int maxUpgrades = enchantManager.calculateMaxUpgrades(player, enchantId);
            if (maxUpgrades <= 0) {
                player.sendMessage(enchantManager.getMessage("upgrade-no-resources"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            handleUpgrade(enchantId, maxUpgrades);
        });
    }

    private void handleUpgrade(String enchantId, int levels) {
        EnchantConfig enchant = enchantManager.getEnchant(enchantId);
        if (enchant == null) return;

        if (!enchantManager.isEnchantUnlocked(player, enchantId)) {
            player.sendMessage(enchantManager.getMessage("upgrade-locked")
                    .replace("{level}", String.valueOf(enchant.getRequiredLevel())));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        int currentLevel = enchantManager.getEnchantLevel(player, enchantId);
        if (currentLevel >= enchant.getMaxLevel()) {
            player.sendMessage(enchantManager.getMessage("upgrade-max"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        boolean success = enchantManager.processUpgrade(player, enchantId, levels);

        if (success) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);

            // Limpar cooldowns ao recriar GUI
            clickCooldowns.clear();
            createGUI();
            open();
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    private void openAmountSelection(String enchantId) {
        player.closeInventory();

        plugin.getInputManager().requestInput(player, enchantId, (amount) -> {
            try {
                int levels = Integer.parseInt(amount);
                if (levels <= 0) {
                    player.sendMessage(enchantManager.getMessage("select-amount-invalid"));
                    return;
                }

                boolean success = enchantManager.processUpgrade(player, enchantId, levels);

                if (success) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }

                // Reabrir menu após o upgrade
                Bukkit.getScheduler().runTask(plugin, () -> {
                    new PickaxeUpgradeMenu(plugin, player).open(); // ✅ CORRIGIDO
                });

            } catch (NumberFormatException e) {
                player.sendMessage(enchantManager.getMessage("select-amount-invalid"));
            }
        });

        player.sendMessage(enchantManager.getMessage("select-amount-message"));
    }

    public void open() {
        gui.show(player);
    }
}