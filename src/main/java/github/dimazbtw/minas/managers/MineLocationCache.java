// MineLocationCache.java - Corrigido
package github.dimazbtw.minas.managers;

import github.dimazbtw.minas.data.Mine;
import org.bukkit.Location;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MineLocationCache {

    private final Map<String, Mine> locationCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 5000; // 5 segundos
    private final Map<String, Long> cacheTimestamp = new ConcurrentHashMap<>();

    public Mine getCachedMine(Location location, MineManager mineManager) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        String key = getLocationKey(location);
        Long timestamp = cacheTimestamp.get(key);

        // Verificar se cache é válido
        if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_DURATION) {
            return locationCache.get(key);
        }

        // Cache inválido ou inexistente, buscar novamente
        Mine mine = mineManager.getMineAt(location);

        // IMPORTANTE: Só adicionar ao cache se não for null
        // ConcurrentHashMap não aceita valores null
        if (mine != null) {
            locationCache.put(key, mine);
            cacheTimestamp.put(key, System.currentTimeMillis());
        } else {
            // Remover do cache se existia mas agora é null
            locationCache.remove(key);
            cacheTimestamp.remove(key);
        }

        return mine;
    }

    private String getLocationKey(Location loc) {
        return loc.getWorld().getName() + ":" +
                (loc.getBlockX() >> 4) + ":" +
                (loc.getBlockZ() >> 4);
    }

    public void invalidateCache() {
        locationCache.clear();
        cacheTimestamp.clear();
    }

    public void invalidateMine(Mine mine) {
        if (mine == null) return;
        locationCache.entrySet().removeIf(entry -> entry.getValue() == mine);
    }

    /**
     * Invalida cache de uma região específica
     */
    public void invalidateRegion(Location location) {
        if (location == null || location.getWorld() == null) return;

        String key = getLocationKey(location);
        locationCache.remove(key);
        cacheTimestamp.remove(key);
    }

    /**
     * Obtém estatísticas do cache (para debug)
     */
    public String getCacheStats() {
        return String.format("Cache: %d entradas, %d timestamps",
                locationCache.size(),
                cacheTimestamp.size());
    }
}