package github.dimazbtw.minas.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.PickaxeData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final Main plugin;
    private HikariDataSource dataSource;
    private final Map<UUID, PickaxeData> cache;
    private boolean isMySQL;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
        this.cache = new HashMap<>();
        setupDatabase();
    }

    private void setupDatabase() {
        String type = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();
        isMySQL = type.equals("mysql");

        HikariConfig config = new HikariConfig();

        if (isMySQL) {
            String host = plugin.getConfig().getString("database.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("database.mysql.port", 3306);
            String database = plugin.getConfig().getString("database.mysql.database", "minas");
            String username = plugin.getConfig().getString("database.mysql.username", "root");
            String password = plugin.getConfig().getString("database.mysql.password", "");

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true");
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            plugin.getLogger().info("§aConectando ao MySQL: " + host + ":" + port + "/" + database);
        } else {
            String dbPath = plugin.getDataFolder().getAbsolutePath() + "/data.db";
            config.setJdbcUrl("jdbc:sqlite:" + dbPath);
            config.setDriverClassName("org.sqlite.JDBC");

            plugin.getLogger().info("§aUsando SQLite: " + dbPath);
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(10000);
        config.setMaxLifetime(1800000);
        config.setPoolName("cah-minas-pool");

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        try {
            dataSource = new HikariDataSource(config);
            createTables();
            migrateDatabase();
            plugin.getLogger().info("§aConexão com banco de dados estabelecida!");
        } catch (Exception e) {
            plugin.getLogger().severe("§cErro ao conectar ao banco de dados: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS pickaxe_data (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "blocks_mined INT DEFAULT 0, " +
                "level INT DEFAULT 1, " +
                "exp INT DEFAULT 0, " +
                "efficiency INT DEFAULT 0, " +
                "fortune INT DEFAULT 0, " +
                "explosion INT DEFAULT 0, " +
                "multiplier INT DEFAULT 0, " +
                "experienced INT DEFAULT 0, " +
                "destroyer INT DEFAULT 0, " +
                "skin_order INT DEFAULT 0" +
                ")";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("§cErro ao criar tabelas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void migrateDatabase() {
        String[] newColumns = {"multiplier", "experienced", "destroyer", "skin_order"};

        for (String column : newColumns) {
            try (Connection conn = getConnection()) {
                ResultSet rs = conn.getMetaData().getColumns(null, null, "pickaxe_data", column);
                if (!rs.next()) {
                    String sql = "ALTER TABLE pickaxe_data ADD COLUMN " + column + " INT DEFAULT 0";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.executeUpdate();
                        plugin.getLogger().info("§aColuna '" + column + "' adicionada à tabela pickaxe_data");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("§eErro ao migrar coluna " + column + ": " + e.getMessage());
            }
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource não inicializado!");
        }
        return dataSource.getConnection();
    }

    public CompletableFuture<PickaxeData> loadPickaxeData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (cache.containsKey(uuid)) {
                return cache.get(uuid);
            }

            String sql = "SELECT * FROM pickaxe_data WHERE uuid = ?";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();

                PickaxeData data;
                if (rs.next()) {
                    data = new PickaxeData(
                            uuid,
                            rs.getInt("blocks_mined"),
                            rs.getInt("level"),
                            rs.getInt("exp"),
                            rs.getInt("efficiency"),
                            rs.getInt("fortune"),
                            rs.getInt("explosion"),
                            getColumnSafe(rs, "multiplier", 0),
                            getColumnSafe(rs, "experienced", 0),
                            getColumnSafe(rs, "destroyer", 0),
                            getColumnSafe(rs, "skin_order", 0)
                    );
                } else {
                    data = new PickaxeData(uuid);
                    savePickaxeData(data).join();
                }

                cache.put(uuid, data);
                return data;

            } catch (SQLException e) {
                plugin.getLogger().severe("§cErro ao carregar dados da picareta: " + e.getMessage());
                e.printStackTrace();
                return new PickaxeData(uuid);
            }
        });
    }

    private int getColumnSafe(ResultSet rs, String column, int defaultValue) {
        try {
            return rs.getInt(column);
        } catch (SQLException e) {
            return defaultValue;
        }
    }

    public CompletableFuture<Void> savePickaxeData(PickaxeData data) {
        return CompletableFuture.runAsync(() -> {
            String sql = isMySQL ?
                    "INSERT INTO pickaxe_data (uuid, blocks_mined, level, exp, efficiency, fortune, explosion, multiplier, experienced, destroyer, skin_order) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE " +
                            "blocks_mined = VALUES(blocks_mined), " +
                            "level = VALUES(level), " +
                            "exp = VALUES(exp), " +
                            "efficiency = VALUES(efficiency), " +
                            "fortune = VALUES(fortune), " +
                            "explosion = VALUES(explosion), " +
                            "multiplier = VALUES(multiplier), " +
                            "experienced = VALUES(experienced), " +
                            "destroyer = VALUES(destroyer), " +
                            "skin_order = VALUES(skin_order)"
                    :
                    "INSERT OR REPLACE INTO pickaxe_data (uuid, blocks_mined, level, exp, efficiency, fortune, explosion, multiplier, experienced, destroyer, skin_order) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, data.getPlayerUUID().toString());
                stmt.setInt(2, data.getBlocksMined());
                stmt.setInt(3, data.getLevel());
                stmt.setInt(4, data.getExp());
                stmt.setInt(5, data.getEfficiency());
                stmt.setInt(6, data.getFortune());
                stmt.setInt(7, data.getExplosion());
                stmt.setInt(8, data.getMultiplier());
                stmt.setInt(9, data.getExperienced());
                stmt.setInt(10, data.getDestroyer());
                stmt.setInt(11, data.getSkinOrder());

                stmt.executeUpdate();

            } catch (SQLException e) {
                plugin.getLogger().severe("§cErro ao salvar dados da picareta: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public PickaxeData getPickaxeData(UUID uuid) {
        if (cache.containsKey(uuid)) {
            return cache.get(uuid);
        }
        return loadPickaxeData(uuid).join();
    }

    public void updateCache(UUID uuid, PickaxeData data) {
        cache.put(uuid, data);
    }

    public void removeFromCache(UUID uuid) {
        PickaxeData data = cache.remove(uuid);
        if (data != null) {
            savePickaxeData(data);
        }
    }

    public void saveAll() {
        for (PickaxeData data : cache.values()) {
            savePickaxeData(data).join();
        }
    }

    public void close() {
        saveAll();
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public void resetPickaxeData(UUID uuid) {
        PickaxeData data = getPickaxeData(uuid);
        data.reset();
        updateCache(uuid, data);
        savePickaxeData(data);
    }
}