package github.dimazbtw.minas.managers;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.EnchantConfig;
import github.dimazbtw.minas.data.PickaxeData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class EnchantManager {

    private final Main plugin;
    private YamlConfiguration enchantConfig;
    private final Map<String, EnchantConfig> enchants;

    private String menuTitle;
    private int menuSize;
    private boolean fillerEnabled;
    private Material fillerMaterial;
    private String fillerName;
    private int pickaxeInfoSlot;
    private int playerXpSlot;

    public EnchantManager(Main plugin) {
        this.plugin = plugin;
        this.enchants = new LinkedHashMap<>();
        loadConfig();
    }

    public void loadConfig() {
        File file = new File(plugin.getDataFolder(), "enchants.yml");
        if (!file.exists()) {
            plugin.saveResource("enchants.yml", false);
        }

        enchantConfig = YamlConfiguration.loadConfiguration(file);
        enchants.clear();

        ConfigurationSection menuSection = enchantConfig.getConfigurationSection("menu");
        if (menuSection != null) {
            menuTitle = menuSection.getString("title", "&8⛏ Melhorar Picareta");
            menuSize = menuSection.getInt("size", 54);

            ConfigurationSection fillerSection = menuSection.getConfigurationSection("filler");
            if (fillerSection != null) {
                fillerEnabled = fillerSection.getBoolean("enabled", true);
                try {
                    fillerMaterial = Material.valueOf(fillerSection.getString("material", "GRAY_STAINED_GLASS_PANE"));
                } catch (IllegalArgumentException e) {
                    fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;
                }
                fillerName = fillerSection.getString("name", " ");
            }

            ConfigurationSection pickaxeSection = menuSection.getConfigurationSection("pickaxe-info");
            if (pickaxeSection != null) {
                pickaxeInfoSlot = pickaxeSection.getInt("slot", 4);
            }

            ConfigurationSection xpSection = menuSection.getConfigurationSection("player-xp");
            if (xpSection != null) {
                playerXpSlot = xpSection.getInt("slot", 49);
            }
        }

        ConfigurationSection enchantsSection = enchantConfig.getConfigurationSection("enchants");
        if (enchantsSection != null) {
            for (String enchantId : enchantsSection.getKeys(false)) {
                ConfigurationSection enchantSection = enchantsSection.getConfigurationSection(enchantId);
                if (enchantSection != null) {
                    EnchantConfig config = EnchantConfig.fromConfig(enchantId, enchantSection);
                    enchants.put(enchantId, config);
                    plugin.getLogger().info("Encantamento carregado: " + enchantId +
                            (config.getRequiredLevel() > 0 ? " (Req. Nível " + config.getRequiredLevel() + ")" : ""));
                }
            }
        }

        plugin.getLogger().info("Total de encantamentos carregados: " + enchants.size());
    }

    public void reload() {
        loadConfig();
    }

    public EnchantConfig getEnchant(String id) {
        return enchants.get(id);
    }

    public Collection<EnchantConfig> getAllEnchants() {
        return enchants.values();
    }

    public int getEnchantLevel(Player player, String enchantId) {
        PickaxeData data = plugin.getDatabaseManager().getPickaxeData(player.getUniqueId());

        switch (enchantId) {
            case "efficiency": return data.getEfficiency();
            case "fortune": return data.getFortune();
            case "explosion": return data.getExplosion();
            case "multiplier": return data.getMultiplier();
            case "experienced": return data.getExperienced();
            case "destroyer": return data.getDestroyer();
            default: return 0;
        }
    }

    public void setEnchantLevel(Player player, String enchantId, int level) {
        PickaxeData data = plugin.getDatabaseManager().getPickaxeData(player.getUniqueId());

        switch (enchantId) {
            case "efficiency": data.setEfficiency(level); break;
            case "fortune": data.setFortune(level); break;
            case "explosion": data.setExplosion(level); break;
            case "multiplier": data.setMultiplier(level); break;
            case "experienced": data.setExperienced(level); break;
            case "destroyer": data.setDestroyer(level); break;
        }

        plugin.getDatabaseManager().savePickaxeData(data);
    }

    public boolean isEnchantUnlocked(Player player, String enchantId) {
        EnchantConfig config = enchants.get(enchantId);
        if (config == null) return false;

        PickaxeData data = plugin.getDatabaseManager().getPickaxeData(player.getUniqueId());
        return config.isUnlocked(data.getLevel());
    }

    public boolean canAfford(Player player, String enchantId) {
        EnchantConfig config = enchants.get(enchantId);
        if (config == null) return false;

        int currentLevel = getEnchantLevel(player, enchantId);
        if (currentLevel >= config.getMaxLevel()) return false;

        if (!isEnchantUnlocked(player, enchantId)) return false;

        Map<String, Double> costs = config.calculateCost(currentLevel);

        for (Map.Entry<String, Double> entry : costs.entrySet()) {
            String provider = entry.getKey();
            double amount = entry.getValue();

            if (provider.equals("exp")) {
                if (player.getTotalExperience() < amount) return false;
            } else if (provider.equals("coins")) {
                if (!plugin.getVaultManager().hasBalance(player, amount)) return false;
            }
        }

        return true;
    }

    public int calculateMaxUpgrades(Player player, String enchantId) {
        EnchantConfig config = enchants.get(enchantId);
        if (config == null) return 0;

        if (!isEnchantUnlocked(player, enchantId)) return 0;

        int currentLevel = getEnchantLevel(player, enchantId);
        int maxPossible = config.getMaxLevel() - currentLevel;

        if (maxPossible <= 0) return 0;

        int upgrades = 0;
        double tempExp = player.getTotalExperience();
        double tempCoins = plugin.getVaultManager().isEconomyEnabled() ?
                plugin.getVaultManager().getBalance(player) : Double.MAX_VALUE;

        for (int i = 0; i < maxPossible; i++) {
            Map<String, Double> costs = config.calculateCost(currentLevel + i);

            boolean canAfford = true;
            for (Map.Entry<String, Double> entry : costs.entrySet()) {
                String provider = entry.getKey();
                double amount = entry.getValue();

                if (provider.equals("exp") && tempExp < amount) {
                    canAfford = false;
                    break;
                } else if (provider.equals("coins") && tempCoins < amount) {
                    canAfford = false;
                    break;
                }
            }

            if (!canAfford) break;

            for (Map.Entry<String, Double> entry : costs.entrySet()) {
                if (entry.getKey().equals("exp")) {
                    tempExp -= entry.getValue();
                } else if (entry.getKey().equals("coins")) {
                    tempCoins -= entry.getValue();
                }
            }

            upgrades++;
        }

        return upgrades;
    }

    public boolean processUpgrade(Player player, String enchantId, int levels) {
        EnchantConfig config = enchants.get(enchantId);
        if (config == null) return false;

        PickaxeData data = plugin.getDatabaseManager().getPickaxeData(player.getUniqueId());

        if (!config.isUnlocked(data.getLevel())) {
            player.sendMessage(getMessage("upgrade-locked")
                    .replace("{level}", String.valueOf(config.getRequiredLevel())));
            return false;
        }

        int currentLevel = getEnchantLevel(player, enchantId);

        if (currentLevel >= config.getMaxLevel()) {
            player.sendMessage(getMessage("upgrade-max"));
            return false;
        }

        levels = Math.min(levels, config.getMaxLevel() - currentLevel);
        if (levels <= 0) return false;

        Map<String, Double> totalCosts = config.calculateCostForLevels(currentLevel, levels);

        for (Map.Entry<String, Double> entry : totalCosts.entrySet()) {
            String provider = entry.getKey();
            double amount = entry.getValue();

            if (provider.equals("exp")) {
                if (player.getTotalExperience() < amount) {
                    player.sendMessage(getMessage("upgrade-no-resources"));
                    return false;
                }
            } else if (provider.equals("coins")) {
                if (!plugin.getVaultManager().hasBalance(player, amount)) {
                    player.sendMessage(getMessage("upgrade-no-resources"));
                    return false;
                }
            }
        }

        for (Map.Entry<String, Double> entry : totalCosts.entrySet()) {
            String provider = entry.getKey();
            double amount = entry.getValue();

            if (provider.equals("exp")) {
                removePlayerXp(player, (int) amount);
            } else if (provider.equals("coins")) {
                plugin.getVaultManager().withdrawPlayer(player, amount);
            }
        }

        int newLevel = currentLevel + levels;
        setEnchantLevel(player, enchantId, newLevel);

        plugin.getPickaxeManager().updatePickaxe(player);

        if (levels == 1) {
            player.sendMessage(getMessage("upgrade-success")
                    .replace("{enchant}", config.getName())
                    .replace("{level}", String.valueOf(newLevel)));
        } else {
            player.sendMessage(getMessage("upgrade-multiple")
                    .replace("{enchant}", config.getName())
                    .replace("{amount}", String.valueOf(levels))
                    .replace("{level}", String.valueOf(newLevel)));
        }

        return true;
    }

    private void removePlayerXp(Player player, int amount) {
        int current = player.getTotalExperience();
        player.setTotalExperience(0);
        player.setExp(0);
        player.setLevel(0);
        player.giveExp(Math.max(0, current - amount));
    }

    public String getMessage(String key) {
        String message = enchantConfig.getString("messages." + key, "&cMensagem não encontrada: " + key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public ItemStack createFillerItem() {
        ItemStack item = new ItemStack(fillerMaterial);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', fillerName));
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createPickaxeInfoItem(Player player) {
        ConfigurationSection section = enchantConfig.getConfigurationSection("menu.pickaxe-info");
        if (section == null) return new ItemStack(Material.DIAMOND_PICKAXE);

        Material material;
        try {
            material = Material.valueOf(section.getString("material", "DIAMOND_PICKAXE"));
        } catch (IllegalArgumentException e) {
            material = Material.DIAMOND_PICKAXE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        PickaxeData data = plugin.getDatabaseManager().getPickaxeData(player.getUniqueId());

        String name = section.getString("name", "&6&l⛏ Picareta de Mina");
        meta.setDisplayName(color(replacePlaceholders(name, player, data)));

        List<String> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            lore.add(color(replacePlaceholders(line, player, data)));
        }
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createPlayerXpItem(Player player) {
        ConfigurationSection section = enchantConfig.getConfigurationSection("menu.player-xp");
        if (section == null) return new ItemStack(Material.EXPERIENCE_BOTTLE);

        Material material;
        try {
            material = Material.valueOf(section.getString("material", "EXPERIENCE_BOTTLE"));
        } catch (IllegalArgumentException e) {
            material = Material.EXPERIENCE_BOTTLE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        PickaxeData data = plugin.getDatabaseManager().getPickaxeData(player.getUniqueId());

        String name = section.getString("name", "&a&lSeu XP");
        meta.setDisplayName(color(replacePlaceholders(name, player, data)));

        List<String> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            lore.add(color(replacePlaceholders(line, player, data)));
        }
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private String replacePlaceholders(String text, Player player, PickaxeData data) {
        text = text.replace("{level}", String.valueOf(data.getLevel()));
        text = text.replace("{exp}", String.valueOf(data.getExp()));
        text = text.replace("{exp_next}", String.valueOf(data.getExpForNextLevel()));
        text = text.replace("{blocks_mined}", formatNumber(data.getBlocksMined()));
        text = text.replace("{fortune_multiplier}", String.format("%.2f", data.getFortuneMultiplier()));
        text = text.replace("{explosion_chance}", String.format("%.1f%%", data.getExplosionChance()));
        text = text.replace("{multiplier_bonus}", String.format("%.0f%%", (data.getMultiplierBonus() - 1) * 100));
        text = text.replace("{experienced_bonus}", String.format("%.0f%%", (data.getExperiencedBonus() - 1) * 100));
        text = text.replace("{destroyer_chance}", String.format("%.1f%%", data.getDestroyerChance()));
        text = text.replace("{player_levels}", String.valueOf(player.getLevel()));
        text = text.replace("{player_xp}", formatNumber(player.getTotalExperience()));

        return text;
    }

    public String formatNumber(long number) {
        if (number >= 1_000_000_000) {
            return String.format("%.2fB", number / 1_000_000_000.0);
        } else if (number >= 1_000_000) {
            return String.format("%.2fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.2fK", number / 1_000.0);
        } else {
            return String.valueOf(number);
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public String getMenuTitle() { return ChatColor.translateAlternateColorCodes('&', menuTitle); }
    public int getMenuSize() { return menuSize; }
    public boolean isFillerEnabled() { return fillerEnabled; }
    public int getPickaxeInfoSlot() { return pickaxeInfoSlot; }
    public int getPlayerXpSlot() { return playerXpSlot; }
}