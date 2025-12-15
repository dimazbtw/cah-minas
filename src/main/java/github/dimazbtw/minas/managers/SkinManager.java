package github.dimazbtw.minas.managers;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.PickaxeData;
import github.dimazbtw.minas.data.PickaxeSkin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;

public class SkinManager {

    private final Main plugin;
    private final Map<String, PickaxeSkin> skins;
    private final List<PickaxeSkin> sortedSkins;
    private final NamespacedKey skinItemKey;

    public SkinManager(Main plugin) {
        this.plugin = plugin;
        this.skins = new LinkedHashMap<>();
        this.sortedSkins = new ArrayList<>();
        this.skinItemKey = new NamespacedKey(plugin, "skin_activation_item");
        loadSkins();
    }

    public void loadSkins() {
        skins.clear();
        sortedSkins.clear();

        File file = new File(plugin.getDataFolder(), "skins.yml");
        if (!file.exists()) {
            plugin.saveResource("skins.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection skinsSection = config.getConfigurationSection("skins");

        if (skinsSection != null) {
            for (String skinId : skinsSection.getKeys(false)) {
                ConfigurationSection skinSection = skinsSection.getConfigurationSection(skinId);
                if (skinSection != null) {
                    PickaxeSkin skin = PickaxeSkin.fromConfig(skinId, skinSection);
                    skins.put(skinId.toLowerCase(), skin);
                    sortedSkins.add(skin);
                    plugin.getLogger().info("Skin carregada: " + skinId + " (Ordem " + skin.getOrder() + ", Bónus +" + ((skin.getBonus() - 1) * 100) + "%)");
                }
            }
        }

        // Ordenar por order
        sortedSkins.sort(Comparator.comparingInt(PickaxeSkin::getOrder));

        plugin.getLogger().info("Total de skins carregadas: " + skins.size());
    }

    public void reload() {
        loadSkins();
    }

    /**
     * Obtém a skin atual do jogador baseado no skinOrder salvo
     */
    public PickaxeSkin getCurrentSkin(int skinOrder) {
        for (PickaxeSkin skin : sortedSkins) {
            if (skin.getOrder() == skinOrder) {
                return skin;
            }
        }
        // Retorna a primeira skin (default) se não encontrar
        return sortedSkins.isEmpty() ? null : sortedSkins.get(0);
    }

    /**
     * Obtém skin por ordem
     */
    public PickaxeSkin getSkinByOrder(int order) {
        for (PickaxeSkin skin : sortedSkins) {
            if (skin.getOrder() == order) {
                return skin;
            }
        }
        return null;
    }

    /**
     * Obtém o bónus de ganhos da skin atual
     */
    public double getSkinBonus(int skinOrder) {
        PickaxeSkin skin = getCurrentSkin(skinOrder);
        return skin != null ? skin.getBonus() : 1.0;
    }

    /**
     * Cria o item de ativação de skin
     */
    public ItemStack createSkinActivationItem(int skinOrder) {
        PickaxeSkin skin = getSkinByOrder(skinOrder);
        if (skin == null) {
            return null;
        }

        ItemStack item = new ItemStack(skin.getMaterial());
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                "&d&l✦ Skin de Picareta &d✦"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(color("&fSkin: " + skin.getDisplay()));
        lore.add(color("&fBónus: &a+" + formatPercent(skin.getBonus()) + "% ganhos"));
        lore.add("");
        lore.add(color("&7Clique direito para ativar"));
        lore.add(color("&7esta skin na sua picareta!"));
        lore.add("");
        lore.add(color("&8ID: " + skin.getOrder()));

        meta.setLore(lore);

        // Adicionar encantamento visual
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

        // Guardar ordem da skin no item
        meta.getPersistentDataContainer().set(skinItemKey, PersistentDataType.INTEGER, skinOrder);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Verifica se um item é um item de ativação de skin
     */
    public boolean isSkinActivationItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(skinItemKey, PersistentDataType.INTEGER);
    }

    /**
     * Obtém a ordem da skin de um item de ativação
     */
    public int getSkinOrderFromItem(ItemStack item) {
        if (!isSkinActivationItem(item)) return -1;
        Integer order = item.getItemMeta().getPersistentDataContainer().get(skinItemKey, PersistentDataType.INTEGER);
        return order != null ? order : -1;
    }

    /**
     * Ativa uma skin para o jogador
     */
    public boolean activateSkin(Player player, int skinOrder) {
        PickaxeSkin skin = getSkinByOrder(skinOrder);
        if (skin == null) {
            player.sendMessage(color("&cSkin não encontrada!"));
            return false;
        }

        PickaxeData data = plugin.getDatabaseManager().getPickaxeData(player.getUniqueId());

        // Verificar se já tem esta skin
        if (data.getSkinOrder() == skinOrder) {
            player.sendMessage(color("&cJá tens esta skin ativada!"));
            return false;
        }

        // Ativar skin
        data.setSkinOrder(skinOrder);
        plugin.getDatabaseManager().savePickaxeData(data);

        // Atualizar picareta no inventário
        plugin.getPickaxeManager().updatePickaxe(player);

        // Enviar título
        sendNewSkinTitle(player, skin);

        return true;
    }

    /**
     * Envia título de nova skin ativada
     */
    public void sendNewSkinTitle(Player player, PickaxeSkin skin) {
        String title = ChatColor.translateAlternateColorCodes('&', "&d&lNOVA SKIN ATIVADA!");
        String subtitle = ChatColor.translateAlternateColorCodes('&', skin.getDisplay());

        player.sendTitle(title, subtitle, 10, 70, 20);
    }

    /**
     * Obtém uma skin por ID
     */
    public PickaxeSkin getSkin(String id) {
        return skins.get(id.toLowerCase());
    }

    /**
     * Obtém todas as skins ordenadas
     */
    public List<PickaxeSkin> getAllSkins() {
        return Collections.unmodifiableList(sortedSkins);
    }

    private String formatPercent(double bonus) {
        double percent = (bonus - 1.0) * 100;
        if (percent == (int) percent) {
            return String.valueOf((int) percent);
        }
        return String.format("%.1f", percent);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}