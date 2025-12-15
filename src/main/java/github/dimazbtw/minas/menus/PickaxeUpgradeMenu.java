package github.dimazbtw.minas.menus;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.EnchantConfig;
import github.dimazbtw.minas.data.PickaxeData;
import github.dimazbtw.minas.managers.EnchantManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class PickaxeUpgradeMenu implements InventoryHolder {

    private final Main plugin;
    private final Player player;
    private final Inventory inventory;
    private final EnchantManager enchantManager;
    private final Map<Integer, String> slotToEnchant;

    public PickaxeUpgradeMenu(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.enchantManager = plugin.getEnchantManager();
        this.slotToEnchant = new HashMap<>();
        this.inventory = Bukkit.createInventory(this, enchantManager.getMenuSize(), enchantManager.getMenuTitle());
        setup();
    }

    private void setup() {
        slotToEnchant.clear();

        if (enchantManager.isFillerEnabled()) {
            ItemStack filler = enchantManager.createFillerItem();
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, filler);
            }
        }

        inventory.setItem(enchantManager.getPickaxeInfoSlot(), enchantManager.createPickaxeInfoItem(player));
        inventory.setItem(enchantManager.getPlayerXpSlot(), enchantManager.createPlayerXpItem(player));

        PickaxeData data = plugin.getDatabaseManager().getPickaxeData(player.getUniqueId());
        int pickaxeLevel = data.getLevel();

        for (EnchantConfig enchant : enchantManager.getAllEnchants()) {
            int currentLevel = enchantManager.getEnchantLevel(player, enchant.getId());
            boolean canAfford = enchantManager.canAfford(player, enchant.getId());

            ItemStack item = enchant.createMenuItem(currentLevel, canAfford, pickaxeLevel);
            inventory.setItem(enchant.getSlot(), item);
            slotToEnchant.put(enchant.getSlot(), enchant.getId());
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void handleClick(int slot, ClickType clickType) {
        String enchantId = slotToEnchant.get(slot);
        if (enchantId == null) return;

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

        int upgradeLevels = 0;

        switch (clickType) {
            case LEFT:
            case SHIFT_LEFT:
                upgradeLevels = 1;
                break;

            case RIGHT:
            case SHIFT_RIGHT:
                openAmountSelection(enchantId);
                return;

            case DROP:
                upgradeLevels = enchantManager.calculateMaxUpgrades(player, enchantId);
                if (upgradeLevels <= 0) {
                    player.sendMessage(enchantManager.getMessage("upgrade-no-resources"));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }
                break;

            default:
                return;
        }

        if (upgradeLevels > 0) {
            boolean success = enchantManager.processUpgrade(player, enchantId, upgradeLevels);

            if (success) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                setup();
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
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

                Bukkit.getScheduler().runTask(plugin, () -> {
                    new PickaxeUpgradeMenu(plugin, player).open();
                });

            } catch (NumberFormatException e) {
                player.sendMessage(enchantManager.getMessage("select-amount-invalid"));
            }
        });

        player.sendMessage(enchantManager.getMessage("select-amount-message"));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}