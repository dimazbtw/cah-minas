package github.dimazbtw.minas.managers;

import github.dimazbtw.minas.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.Map;

public class LanguageManager {

    private final Main plugin;
    private YamlConfiguration langConfig;

    public LanguageManager(Main plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File langFile = new File(plugin.getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    /**
     * Obtém uma mensagem traduzida
     */
    public String getMessage(Player player, String key) {
        String message = langConfig.getString(key);
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Obtém uma mensagem com placeholders
     */
    public String getMessage(Player player, String key, Map<String, String> placeholders) {
        String message = getMessage(player, key);
        if (message.isEmpty()) return "";

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return message;
    }

    /**
     * Envia uma mensagem para um jogador
     */
    public void sendMessage(CommandSender sender, String key) {
        String message = getMessage(sender instanceof Player ? (Player) sender : null, key);
        if (!message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    /**
     * Envia uma mensagem com placeholders
     */
    public void sendMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        String message = getMessage(sender instanceof Player ? (Player) sender : null, key, placeholders);
        if (!message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    /**
     * Obtém uma lista de mensagens
     */
    public List<String> getMessageList(String key) {
        List<String> messages = langConfig.getStringList(key);
        messages.replaceAll(s -> ChatColor.translateAlternateColorCodes('&', s));
        return messages;
    }

    /**
     * Obtém o valor bruto de uma key
     */
    public String getRawValue(String key) {
        return langConfig.getString(key, "");
    }
}
