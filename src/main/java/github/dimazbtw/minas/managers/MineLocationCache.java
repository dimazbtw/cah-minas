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
        String key = getLocationKey(location);
        Long timestamp = cacheTimestamp.get(key);

        // Verificar se cache é válido
        if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_DURATION) {
            return locationCache.get(key);
        }

        // Cache inválido, buscar novamente
        Mine mine = mineManager.getMineAt(location);
        locationCache.put(key, mine);
        cacheTimestamp.put(key, System.currentTimeMillis());

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
        locationCache.entrySet().removeIf(entry -> entry.getValue() == mine);
    }
}