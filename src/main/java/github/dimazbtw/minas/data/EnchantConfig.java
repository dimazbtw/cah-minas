package github.dimazbtw.minas.data;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Representa a configuração de um encantamento
 */
public class EnchantConfig {

    private final String id;
    private String name;
    private String description;
    private int slot;
    private int defaultLevel;
    private int maxLevel;
    private int requiredLevel;

    // Efeito
    private String effectType;
    private double valuePerLevel;

    // Preços
    private final Map<String, PriceEntry> defaultPrices;
    private final Map<String, PriceEntry> perLevelPrices;

    // Displays
    private DisplayConfig canDisplay;
    private DisplayConfig cantDisplay;
    private DisplayConfig maxedDisplay;
    private DisplayConfig lockedDisplay;

    public EnchantConfig(String id) {
        this.id = id;
        this.defaultPrices = new HashMap<>();
        this.perLevelPrices = new HashMap<>();
        this.requiredLevel = 0;
    }

    /**
     * Carrega a configuração de uma seção YAML
     */
    public static EnchantConfig fromConfig(String id, ConfigurationSection section) {
        EnchantConfig config = new EnchantConfig(id);

        config.name = section.getString("name", id);
        config.description = section.getString("description", "");
        config.slot = section.getInt("slot", 0);

        // Níveis
        ConfigurationSection levelSection = section.getConfigurationSection("level");
        if (levelSection != null) {
            config.defaultLevel = levelSection.getInt("default", 0);
            config.maxLevel = levelSection.getInt("max", 100);
            config.requiredLevel = levelSection.getInt("required", 0);
        }

        // Efeito
        ConfigurationSection effectSection = section.getConfigurationSection("effect");
        if (effectSection != null) {
            config.effectType = effectSection.getString("type", "none");
            config.valuePerLevel = effectSection.getDouble("value-per-level", 1.0);
        }

        // Preços padrão
        ConfigurationSection defaultPricesSection = section.getConfigurationSection("prices-default");
        if (defaultPricesSection != null) {
            for (String key : defaultPricesSection.getKeys(false)) {
                ConfigurationSection priceSection = defaultPricesSection.getConfigurationSection(key);
                if (priceSection != null) {
                    PriceEntry entry = new PriceEntry(
                            priceSection.getString("provider", "exp"),
                            priceSection.getDouble("price", 0)
                    );
                    config.defaultPrices.put(key, entry);
                }
            }
        }

        // Preços por nível
        ConfigurationSection perLevelPricesSection = section.getConfigurationSection("prices-per-level");
        if (perLevelPricesSection != null) {
            for (String key : perLevelPricesSection.getKeys(false)) {
                ConfigurationSection priceSection = perLevelPricesSection.getConfigurationSection(key);
                if (priceSection != null) {
                    PriceEntry entry = new PriceEntry(
                            priceSection.getString("provider", "exp"),
                            priceSection.getDouble("price", 0)
                    );
                    config.perLevelPrices.put(key, entry);
                }
            }
        }

        // Displays
        ConfigurationSection displaysSection = section.getConfigurationSection("displays");
        if (displaysSection != null) {
            config.canDisplay = DisplayConfig.fromConfig(displaysSection.getConfigurationSection("can"));
            config.cantDisplay = DisplayConfig.fromConfig(displaysSection.getConfigurationSection("cant"));
            config.maxedDisplay = DisplayConfig.fromConfig(displaysSection.getConfigurationSection("maxed"));
            config.lockedDisplay = DisplayConfig.fromConfig(displaysSection.getConfigurationSection("locked"));
        }

        return config;
    }

    /**
     * Calcula o custo total para evoluir do nível atual para o próximo
     */
    public Map<String, Double> calculateCost(int currentLevel) {
        Map<String, Double> costs = new HashMap<>();

        for (PriceEntry entry : defaultPrices.values()) {
            costs.merge(entry.getProvider(), entry.getPrice(), Double::sum);
        }

        for (PriceEntry entry : perLevelPrices.values()) {
            double levelCost = entry.getPrice() * (currentLevel + 1);
            costs.merge(entry.getProvider(), levelCost, Double::sum);
        }

        return costs;
    }

    /**
     * Calcula o custo total para evoluir múltiplos níveis
     */
    public Map<String, Double> calculateCostForLevels(int currentLevel, int levels) {
        Map<String, Double> totalCosts = new HashMap<>();

        for (int i = 0; i < levels; i++) {
            Map<String, Double> cost = calculateCost(currentLevel + i);
            for (Map.Entry<String, Double> entry : cost.entrySet()) {
                totalCosts.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        return totalCosts;
    }

    /**
     * Formata o custo para exibição
     */
    public String formatCost(int currentLevel) {
        Map<String, Double> costs = calculateCost(currentLevel);
        List<String> parts = new ArrayList<>();

        for (Map.Entry<String, Double> entry : costs.entrySet()) {
            String provider = entry.getKey();
            double amount = entry.getValue();

            if (provider.equals("exp")) {
                parts.add(formatNumber(amount) + " XP");
            } else if (provider.equals("coins")) {
                parts.add("$" + formatNumber(amount));
            } else {
                parts.add(formatNumber(amount) + " " + provider);
            }
        }

        return String.join(" &f+ ", parts);
    }

    /**
     * Verifica se o jogador tem o nível necessário para desbloquear
     */
    public boolean isUnlocked(int pickaxeLevel) {
        return pickaxeLevel >= requiredLevel;
    }

    /**
     * Cria o ItemStack para o menu
     */
    public ItemStack createMenuItem(int currentLevel, boolean canAfford, int pickaxeLevel) {
        DisplayConfig display;

        if (!isUnlocked(pickaxeLevel)) {
            display = lockedDisplay != null ? lockedDisplay : cantDisplay;
            if (display == null) display = canDisplay;
            return display.createItem(this, currentLevel, pickaxeLevel);
        }

        if (currentLevel >= maxLevel) {
            display = maxedDisplay;
        } else if (canAfford) {
            display = canDisplay;
        } else {
            display = cantDisplay;
        }

        if (display == null) {
            display = canDisplay;
        }

        return display.createItem(this, currentLevel, pickaxeLevel);
    }

    /**
     * Calcula o valor do efeito para um nível
     */
    public double getEffectValue(int level) {
        return level * valuePerLevel;
    }

    private String formatNumber(double number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        }
        return String.format("%.0f", number);
    }

    // ============ GETTERS ============

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getSlot() { return slot; }
    public int getDefaultLevel() { return defaultLevel; }
    public int getMaxLevel() { return maxLevel; }
    public int getRequiredLevel() { return requiredLevel; }
    public String getEffectType() { return effectType; }
    public double getValuePerLevel() { return valuePerLevel; }

    // ============ CLASSES INTERNAS ============

    public static class PriceEntry {
        private final String provider;
        private final double price;

        public PriceEntry(String provider, double price) {
            this.provider = provider;
            this.price = price;
        }

        public String getProvider() { return provider; }
        public double getPrice() { return price; }
    }

    public static class DisplayConfig {
        private Material material;
        private String name;
        private List<String> lore;

        public static DisplayConfig fromConfig(ConfigurationSection section) {
            if (section == null) return null;

            DisplayConfig config = new DisplayConfig();

            try {
                config.material = Material.valueOf(section.getString("material", "STONE").toUpperCase());
            } catch (IllegalArgumentException e) {
                config.material = Material.STONE;
            }

            config.name = section.getString("name", "&fItem");
            config.lore = section.getStringList("lore");

            return config;
        }

        public ItemStack createItem(EnchantConfig enchant, int currentLevel, int pickaxeLevel) {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            String processedName = processPlaceholders(name, enchant, currentLevel, pickaxeLevel);
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', processedName));

            List<String> processedLore = new ArrayList<>();
            for (String line : lore) {
                String processed = processPlaceholders(line, enchant, currentLevel, pickaxeLevel);
                processedLore.add(ChatColor.translateAlternateColorCodes('&', processed));
            }
            meta.setLore(processedLore);

            item.setItemMeta(meta);
            return item;
        }

        private String processPlaceholders(String text, EnchantConfig enchant, int currentLevel, int pickaxeLevel) {
            int nextLevel = Math.min(currentLevel + 1, enchant.getMaxLevel());

            text = text.replace("{actual}", String.valueOf(currentLevel));
            text = text.replace("{next}", String.valueOf(nextLevel));
            text = text.replace("{maximum}", String.valueOf(enchant.getMaxLevel()));
            text = text.replace("{cost}", enchant.formatCost(currentLevel));
            text = text.replace("{required_level}", String.valueOf(enchant.getRequiredLevel()));
            text = text.replace("{pickaxe_level}", String.valueOf(pickaxeLevel));

            // Multiplicador de fortuna
            if (enchant.getEffectType() != null && enchant.getEffectType().equals("reward_multiplier")) {
                double multiplier = 1.0 + (currentLevel * enchant.getValuePerLevel());
                text = text.replace("{multiplier}", String.format("%.2f", multiplier));
            }

            // Chance de explosão
            if (enchant.getEffectType() != null && enchant.getEffectType().equals("explosion_chance")) {
                double chance = currentLevel * enchant.getValuePerLevel();
                text = text.replace("{chance}", String.format("%.1f%%", chance));
            }

            // Multiplicador de XP para encantamentos
            if (enchant.getEffectType() != null && enchant.getEffectType().equals("enchant_xp_multiplier")) {
                double bonus = 1.0 + (currentLevel * enchant.getValuePerLevel());
                text = text.replace("{bonus}", String.format("%.0f%%", (bonus - 1) * 100));
                text = text.replace("{multiplier}", String.format("%.2fx", bonus));
            }

            // Bónus de EXP da picareta
            if (enchant.getEffectType() != null && enchant.getEffectType().equals("pickaxe_exp_bonus")) {
                double bonus = 1.0 + (currentLevel * enchant.getValuePerLevel());
                text = text.replace("{bonus}", String.format("%.0f%%", (bonus - 1) * 100));
                text = text.replace("{multiplier}", String.format("%.2fx", bonus));
            }

            // Chance de destruidor
            if (enchant.getEffectType() != null && enchant.getEffectType().equals("layer_destroy_chance")) {
                double chance = currentLevel * enchant.getValuePerLevel();
                text = text.replace("{chance}", String.format("%.1f%%", chance));
            }

            return text;
        }
    }
}