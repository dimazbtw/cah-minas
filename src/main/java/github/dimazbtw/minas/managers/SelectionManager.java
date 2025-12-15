package github.dimazbtw.minas.managers;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.PlayerSelection;
import github.dimazbtw.minas.utils.MapBuilder;
import lombok.var;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class SelectionManager {

    private final Main plugin;
    private final Map<UUID, PlayerSelection> selections;

    // Material do item de seleção
    private static final Material SELECTION_TOOL = Material.GOLDEN_AXE;
    private static final NamespacedKey KEY = new NamespacedKey(Main.getInstance(), "selection_tool");

    public SelectionManager(Main plugin) {
        this.plugin = plugin;
        this.selections = new HashMap<>();
    }

    /**
     * Inicia o modo de seleção para um jogador
     */
    public void startSelection(Player player, String mineId) {
        // Remover seleção anterior se existir
        cancelSelection(player);

        // Criar nova seleção
        PlayerSelection selection = new PlayerSelection(player.getUniqueId(), mineId);
        selections.put(player.getUniqueId(), selection);

        // Dar o item de seleção
        giveSelectionTool(player);

        // Mensagem
        plugin.getLanguageManager().sendMessage(player, "selection.started",
                MapBuilder.of("mine", mineId));
    }

    /**
     * Cancela a seleção de um jogador
     */
    public void cancelSelection(Player player) {
        PlayerSelection selection = selections.remove(player.getUniqueId());
        if (selection != null) {
            // Remover item de seleção
            removeSelectionTool(player);
            plugin.getLanguageManager().sendMessage(player, "selection.cancelled");
        }
    }

    /**
     * Confirma a seleção e aplica na mina
     */
    public boolean confirmSelection(Player player) {
        PlayerSelection selection = selections.get(player.getUniqueId());
        if (selection == null) {
            plugin.getLanguageManager().sendMessage(player, "selection.not-selecting");
            return false;
        }

        if (!selection.isComplete()) {
            plugin.getLanguageManager().sendMessage(player, "selection.incomplete");
            return false;
        }

        // Obter mina
        var mine = plugin.getMineManager().getMine(selection.getMineId());
        if (mine == null) {
            plugin.getLanguageManager().sendMessage(player, "mine.not-found");
            cancelSelection(player);
            return false;
        }

        // Aplicar posições
        mine.setPos1(selection.getPos1());
        mine.setPos2(selection.getPos2());
        mine.save();

        // Limpar seleção
        selections.remove(player.getUniqueId());
        removeSelectionTool(player);

        plugin.getLanguageManager().sendMessage(player, "selection.confirmed",
                MapBuilder.of("mine", mine.getId(),
                       "blocks", String.valueOf(mine.getTotalBlocks())));

        return true;
    }

    /**
     * Obtém a seleção de um jogador
     */
    public PlayerSelection getSelection(Player player) {
        return selections.get(player.getUniqueId());
    }

    /**
     * Verifica se o jogador está em modo de seleção
     */
    public boolean isSelecting(Player player) {
        return selections.containsKey(player.getUniqueId());
    }

    /**
     * Dá o item de seleção ao jogador
     */
    private void giveSelectionTool(Player player) {
        ItemStack tool = createSelectionTool();
        player.getInventory().addItem(tool);
    }

    /**
     * Remove o item de seleção do jogador
     */
    private void removeSelectionTool(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isSelectionTool(item)) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    /**
     * Cria o item de seleção
     */
    public ItemStack createSelectionTool() {
        ItemStack item = new ItemStack(SELECTION_TOOL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Selecionador de Mina");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Use para selecionar a região da mina:");
        lore.add("");
        lore.add("§e⬥ Clique Esquerdo §8- §fDefine Posição 1");
        lore.add("§e⬥ Clique Direito §8- §fDefine Posição 2");
        lore.add("");
        lore.add("§7Após selecionar, use §e/mina confirmar");
        lore.add("");

        meta.setLore(lore);

        // TAG REAL
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);

        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Verifica se um item é o item de seleção
     */
    public boolean isSelectionTool(ItemStack item) {
        if (item == null || item.getType() != SELECTION_TOOL) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return meta.getPersistentDataContainer().has(KEY, PersistentDataType.BYTE);
    }

    /**
     * Limpa seleções expiradas
     */
    public void cleanupExpired() {
        Iterator<Map.Entry<UUID, PlayerSelection>> iterator = selections.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PlayerSelection> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
            }
        }
    }
}
