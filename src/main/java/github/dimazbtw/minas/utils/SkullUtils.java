// SkullUtils.java - Usando authlib
package github.dimazbtw.minas.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class SkullUtils {

    /**
     * Cria uma cabeça customizada a partir de uma URL de textura
     * @param textureUrl URL da textura (ex: http://textures.minecraft.net/texture/...)
     * @return ItemStack da cabeça customizada
     */
    public static ItemStack createSkull(String textureUrl) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);

        if (textureUrl == null || textureUrl.isEmpty()) {
            return skull;
        }

        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) {
            return skull;
        }

        try {
            // Criar GameProfile com UUID aleatório
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);

            // Converter URL para base64 no formato correto
            String base64 = urlToBase64(textureUrl);

            // Adicionar propriedade de textura ao perfil
            profile.getProperties().put("textures", new Property("textures", base64));

            // Aplicar o GameProfile ao SkullMeta usando reflection
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);

            skull.setItemMeta(meta);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return skull;
    }

    /**
     * Converte uma URL de textura para o formato base64 necessário
     */
    private static String urlToBase64(String textureUrl) {
        // Formato JSON esperado pelo Minecraft
        String json = String.format("{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}", textureUrl);

        // Codificar em base64
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Verifica se uma string é uma URL válida de textura do Minecraft
     */
    public static boolean isValidTextureUrl(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        try {
            URL url = new URL(str);
            String host = url.getHost();

            // Verificar se é do domínio do Minecraft
            return host.equals("textures.minecraft.net") ||
                    host.endsWith(".minecraft.net") ||
                    str.startsWith("http://textures.minecraft.net/texture/") ||
                    str.startsWith("https://textures.minecraft.net/texture/");

        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * Extrai o hash da textura de uma URL completa
     * Exemplo: http://textures.minecraft.net/texture/abc123 -> abc123
     */
    public static String extractTextureHash(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        if (url.contains("/texture/")) {
            String[] parts = url.split("/texture/");
            if (parts.length > 1) {
                return parts[1];
            }
        }

        return url;
    }

    /**
     * Constrói uma URL completa a partir de um hash de textura
     */
    public static String buildTextureUrl(String hash) {
        if (hash == null || hash.isEmpty()) {
            return null;
        }

        // Se já é uma URL completa, retornar
        if (hash.startsWith("http://") || hash.startsWith("https://")) {
            return hash;
        }

        // Construir URL completa
        return "http://textures.minecraft.net/texture/" + hash;
    }

    /**
     * Valida e normaliza uma URL de textura
     * Aceita tanto URLs completas quanto apenas o hash
     */
    public static String normalizeTextureUrl(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        // Se já é uma URL válida, retornar
        if (isValidTextureUrl(input)) {
            return input;
        }

        // Tentar construir URL a partir do hash
        String builtUrl = buildTextureUrl(input);
        if (isValidTextureUrl(builtUrl)) {
            return builtUrl;
        }

        return null;
    }

    /**
     * Cria uma cabeça a partir de um nome de jogador
     * @param playerName Nome do jogador
     * @return ItemStack da cabeça
     */
    public static ItemStack createPlayerSkull(String playerName) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);

        if (playerName == null || playerName.isEmpty()) {
            return skull;
        }

        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) {
            return skull;
        }

        try {
            // Criar GameProfile com nome do jogador
            GameProfile profile = new GameProfile(UUID.randomUUID(), playerName);

            // Aplicar o GameProfile ao SkullMeta usando reflection
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);

            skull.setItemMeta(meta);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return skull;
    }

    /**
     * Obtém o GameProfile de um SkullMeta
     */
    public static GameProfile getProfile(SkullMeta meta) {
        try {
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            return (GameProfile) profileField.get(meta);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Obtém a URL de textura de uma cabeça
     */
    public static String getTextureUrl(ItemStack skull) {
        if (skull == null || skull.getType() != Material.PLAYER_HEAD) {
            return null;
        }

        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) {
            return null;
        }

        GameProfile profile = getProfile(meta);
        if (profile == null) {
            return null;
        }

        Property textures = profile.getProperties().get("textures").iterator().next();
        if (textures == null) {
            return null;
        }

        try {
            // Decodificar base64
            String decoded = new String(Base64.getDecoder().decode(textures.getValue()));

            // Extrair URL do JSON
            // Formato: {"textures":{"SKIN":{"url":"..."}}}
            int urlStart = decoded.indexOf("\"url\":\"") + 7;
            int urlEnd = decoded.indexOf("\"", urlStart);

            if (urlStart > 7 && urlEnd > urlStart) {
                return decoded.substring(urlStart, urlEnd);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Clona uma cabeça customizada
     */
    public static ItemStack cloneSkull(ItemStack skull) {
        if (skull == null || skull.getType() != Material.PLAYER_HEAD) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        String textureUrl = getTextureUrl(skull);
        if (textureUrl == null) {
            return skull.clone();
        }

        return createSkull(textureUrl);
    }

    /**
     * Verifica se uma ItemStack é uma cabeça customizada (com textura)
     */
    public static boolean isCustomSkull(ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) {
            return false;
        }

        return getTextureUrl(item) != null;
    }

    /**
     * Compara duas cabeças customizadas
     */
    public static boolean isSameSkull(ItemStack skull1, ItemStack skull2) {
        if (!isCustomSkull(skull1) || !isCustomSkull(skull2)) {
            return false;
        }

        String url1 = getTextureUrl(skull1);
        String url2 = getTextureUrl(skull2);

        return url1 != null && url1.equals(url2);
    }
}