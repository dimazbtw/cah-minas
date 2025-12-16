package github.dimazbtw.minas.managers;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.EnchantConfig;
import github.dimazbtw.minas.data.PickaxeData;
import github.dimazbtw.minas.data.PickaxeSkin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PickaxeManager {

    private final Main plugin;
    private final NamespacedKey pickaxeKey;

    public PickaxeManager(Main plugin) {
        this.plugin = plugin;
        this.pickaxeKey = new NamespacedKey(plugin, "mine_pickaxe");
    }

    public ItemStack createPickaxe(Player player) {
        PickaxeData data = plugin.getDatabaseManager().getPickaxeData(player.getUniqueId());
        return createPickaxeItem(player, data);
    }

    public ItemStack createPickaxeItem(Player player, PickaxeData data) {
        // Obter skin atual baseada no skinOrder salvo
        PickaxeSkin currentSkin = plugin.getSkinManager().getCurrentSkin(data.getSkinOrder());

        Material material = currentSkin != null ? currentSkin.getMaterial() : Material.WOODEN_PICKAXE;
        String skinDisplay = currentSkin != null ? currentSkin.getDisplay() : "&6Picareta de Madeira";
        double skinBonus = currentSkin != null ? currentSkin.getBonus() : 1.0;
        double skinPercent = (skinBonus - 1.0) * 100;

        // Bónus de permissões
        double permissionPercent = plugin.getBonusManager().getPermissionBonusPercent(player);
        double totalMultiplier = plugin.getBonusManager().getTotalMultiplier(player, data.getSkinOrder());
        double totalPercent = (totalMultiplier - 1.0) * 100;

        ItemStack pickaxe = new ItemStack(material);
        ItemMeta meta = pickaxe.getItemMeta();

        // Nome com skin e blocos minerados
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                skinDisplay + " &7[" + formatNumber(data.getBlocksMined()) + "]"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(color("&f Nível: &e" + data.getLevel()));
        lore.add(color("&f EXP: &e" + data.getExp() + "&f/&e" + data.getExpForNextLevel()));
        lore.add("");

        // Info da skin atual
        lore.add(color("&d&lSkin Atual:"));
        lore.add(color("&f " + skinDisplay));

        // Bónus de ganhos
        lore.add("");
        lore.add(color("&a&lBónus de Ganhos:"));
        lore.add(color("&f Skin: &a+" + formatPercent(skinPercent) + "%"));

        // Listar bónus de permissões
        Map<String, Double> allBonus = plugin.getBonusManager().getAllBonus();
        for (Map.Entry<String, Double> entry : allBonus.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                lore.add(color("&f " + formatPermissionName(entry.getKey()) + ": &a+" + formatPercent(entry.getValue()) + "%"));
            }
        }

        lore.add(color("&f Total: &a+" + formatPercent(totalPercent) + "% &7(" + String.format("%.2fx", totalMultiplier) + ")"));

        lore.add("");
        lore.add(color("&e&lEncantamentos:"));
        lore.add(color("&f ⚡ Eficiência: &b" + data.getEfficiency()));
        lore.add(color("&f 💎 Fortuna: &b" + data.getFortune() + " &7(" + String.format("%.1fx", data.getFortuneMultiplier()) + ")"));
        lore.add(color("&f 💥 Explosão: &b" + data.getExplosion() + " &7(" + String.format("%.1f%%", data.getExplosionChance()) + " chance)"));
        lore.add(color("&f ✨ Multiplicador: &b" + data.getMultiplier() + " &7(+" + String.format("%.0f%%", (data.getMultiplierBonus() - 1) * 100) + " XP)"));
        lore.add(color("&f 📚 Experiente: &b" + data.getExperienced() + " &7(+" + String.format("%.0f%%", (data.getExperiencedBonus() - 1) * 100) + " EXP)"));
        lore.add(color("&f 🔨 Destruidor: &b" + data.getDestroyer() + " &7(" + String.format("%.1f%%", data.getDestroyerChance()) + " chance)"));
        lore.add("");
        lore.add(color("&7Clique direito para melhorar"));

        meta.setLore(lore);

        if (data.getEfficiency() > 0) {
            int vanillaEfficiency = Math.min(data.getEfficiency(), 1000);
            meta.addEnchant(Enchantment.DIG_SPEED, vanillaEfficiency, true);
        }

        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

        meta.getPersistentDataContainer().set(pickaxeKey, PersistentDataType.STRING,
                data.getPlayerUUID().toString());

        pickaxe.setItemMeta(meta);
        return pickaxe;
    }

    private String formatPermissionName(String permission) {
        // Formata "vip.bonus" para "VIP" ou "rank.deus" para "Deus"
        String[] parts = permission.split("\\.");
        if (parts.length > 1) {
            String name = parts[parts.length - 1];
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return permission;
    }

    private String formatPercent(double value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        }
        return String.format("%.1f", value);
    }

    private int getMaxLevel(String enchantId) {
        if (plugin.getEnchantManager() == null) return 100;
        EnchantConfig config = plugin.getEnchantManager().getEnchant(enchantId);
        return config != null ? config.getMaxLevel() : 100;
    }

    public boolean isMinePickaxe(ItemStack item) {
        if (item == null || !item.getType().name().endsWith("_PICKAXE")) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(pickaxeKey, PersistentDataType.STRING);
    }

    public UUID getPickaxeOwner(ItemStack item) {
        if (!isMinePickaxe(item)) return null;
        ItemMeta meta = item.getItemMeta();
        String uuidStr = meta.getPersistentDataContainer().get(pickaxeKey, PersistentDataType.STRING);
        if (uuidStr == null) return null;
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isOwner(Player player, ItemStack item) {
        UUID owner = getPickaxeOwner(item);
        return owner != null && owner.equals(player.getUniqueId());
    }

    public void givePickaxe(Player player) {
        removePickaxes(player);

        ItemStack pickaxe = createPickaxe(player);

        ItemStack currentItem = player.getInventory().getItem(0);
        if (currentItem == null || currentItem.getType() == Material.AIR) {
            player.getInventory().setItem(0, pickaxe);
        } else {
            player.getInventory().addItem(pickaxe);
        }
    }

    public void removePickaxes(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isMinePickaxe(item)) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    public void updatePickaxe(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isMinePickaxe(item) && isOwner(player, item)) {
                ItemStack updated = createPickaxe(player);
                player.getInventory().setItem(i, updated);
                return;
            }
        }
    }

    public boolean hasPickaxe(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isMinePickaxe(item) && isOwner(player, item)) {
                return true;
            }
        }
        return false;
    }

    private String formatNumber(int number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        }
        return String.valueOf(number);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}