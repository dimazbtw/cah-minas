package github.dimazbtw.minas.managers;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.RankingEntry;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RankingManager {

    private final Main plugin;
    private List<RankingEntry> cachedBlocksRanking;
    private List<RankingEntry> cachedExpRanking;
    private long lastUpdate;
    private static final long CACHE_DURATION = 60000; // 1 minuto

    public RankingManager(Main plugin) {
        this.plugin = plugin;
        this.cachedBlocksRanking = new ArrayList<>();
        this.cachedExpRanking = new ArrayList<>();
        this.lastUpdate = 0;
    }

    public CompletableFuture<List<RankingEntry>> getBlocksRanking(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            if (System.currentTimeMillis() - lastUpdate < CACHE_DURATION && !cachedBlocksRanking.isEmpty()) {
                return cachedBlocksRanking.subList(0, Math.min(limit, cachedBlocksRanking.size()));
            }

            List<RankingEntry> ranking = new ArrayList<>();
            String sql = "SELECT uuid, blocks_mined, level, exp FROM pickaxe_data ORDER BY blocks_mined DESC LIMIT ?";

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, limit);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String playerName = getPlayerName(uuid);
                    int blocksMined = rs.getInt("blocks_mined");
                    int level = rs.getInt("level");
                    int exp = rs.getInt("exp");

                    ranking.add(new RankingEntry(uuid, playerName, blocksMined, level, exp));
                }

                cachedBlocksRanking = new ArrayList<>(ranking);
                lastUpdate = System.currentTimeMillis();

            } catch (SQLException e) {
                plugin.getLogger().severe("Erro ao obter ranking de blocos: " + e.getMessage());
            }

            return ranking;
        });
    }

    public CompletableFuture<List<RankingEntry>> getExpRanking(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<RankingEntry> ranking = new ArrayList<>();
            String sql = "SELECT uuid, blocks_mined, level, exp FROM pickaxe_data ORDER BY (level * 1000 + exp) DESC LIMIT ?";

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, limit);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String playerName = getPlayerName(uuid);
                    int blocksMined = rs.getInt("blocks_mined");
                    int level = rs.getInt("level");
                    int exp = rs.getInt("exp");

                    ranking.add(new RankingEntry(uuid, playerName, blocksMined, level, exp));
                }

                cachedExpRanking = new ArrayList<>(ranking);

            } catch (SQLException e) {
                plugin.getLogger().severe("Erro ao obter ranking de XP: " + e.getMessage());
            }

            return ranking;
        });
    }

    public CompletableFuture<Integer> getBlocksPosition(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) + 1 as position FROM pickaxe_data WHERE blocks_mined > " +
                    "(SELECT blocks_mined FROM pickaxe_data WHERE uuid = ?)";

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return rs.getInt("position");
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("Erro ao obter posição no ranking: " + e.getMessage());
            }

            return -1;
        });
    }

    private String getPlayerName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() != null ? player.getName() : "Desconhecido";
    }

    public void clearCache() {
        cachedBlocksRanking.clear();
        cachedExpRanking.clear();
        lastUpdate = 0;
    }
}