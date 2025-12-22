// Mine.java - Adicionar campo material
package github.dimazbtw.minas.data;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import github.dimazbtw.minas.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Mine {

    private final String id;
    private String displayName;
    private String materialData; // Pode ser material ou base64

    // Localizações
    private Location spawn;
    private Location exit;
    private Location pos1;
    private Location pos2;

    // Opções
    private int resetTime;
    private double resetPercentage;
    private String permission;
    private boolean pvpEnabled;
    private int order;

    // Blocos
    private final Map<Material, MineBlock> blocks;

    // Estado
    private int totalBlocks;
    private int currentBlocks;
    private long lastReset;
    private boolean resetting;

    public Mine(String id) {
        this.id = id;
        this.displayName = "§7" + id;
        this.materialData = "DIAMOND_PICKAXE"; // Material padrão
        this.resetTime = 300;
        this.resetPercentage = 50.0;
        this.permission = "mina." + id;
        this.pvpEnabled = false;
        this.order = 0;
        this.blocks = new LinkedHashMap<>();
        this.totalBlocks = 0;
        this.currentBlocks = 0;
        this.lastReset = System.currentTimeMillis();
        this.resetting = false;
    }

    /**
     * Carrega uma mina de um arquivo YAML
     */
    public static Mine fromFile(File file) {
        String id = file.getName().replace(".yml", "");
        Mine mine = new Mine(id);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Carregar display - NÃO usar valor padrão se não existir
        if (config.contains("display")) {
            mine.displayName = config.getString("display");
        }

        // Carregar material - NÃO usar valor padrão se não existir
        if (config.contains("material")) {
            mine.materialData = config.getString("material");
        }

        // Carregar localizações
        mine.spawn = loadLocation(config, "locs.spawn");
        mine.exit = loadLocation(config, "locs.exit");
        mine.pos1 = loadLocation(config, "locs.pos1");
        mine.pos2 = loadLocation(config, "locs.pos2");

        // Carregar opções
        if (config.contains("options.reset-time")) {
            mine.resetTime = config.getInt("options.reset-time");
        }

        if (config.contains("options.percentage")) {
            mine.resetPercentage = config.getDouble("options.percentage");
        }

        if (config.contains("options.permission")) {
            mine.permission = config.getString("options.permission");
        }

        if (config.contains("options.pvp")) {
            mine.pvpEnabled = config.getBoolean("options.pvp");
        }

        if (config.contains("options.order")) {
            mine.order = config.getInt("options.order");
        }

        // Carregar blocos
        ConfigurationSection blocksSection = config.getConfigurationSection("blocks");
        if (blocksSection != null) {
            for (String materialName : blocksSection.getKeys(false)) {
                try {
                    Material material = Material.valueOf(materialName.toUpperCase());
                    ConfigurationSection blockSection = blocksSection.getConfigurationSection(materialName);

                    if (blockSection != null) {
                        MineBlock mineBlock = new MineBlock(material);
                        mineBlock.setChance(blockSection.getDouble("chance", 100.0));
                        mineBlock.setExp(blockSection.getInt("xp", 0));
                        mineBlock.setMoney(blockSection.getDouble("money", 0.0));

                        List<String> rewards = blockSection.getStringList("rewards");
                        mineBlock.setRewards(rewards);

                        mine.blocks.put(material, mineBlock);
                    }
                } catch (IllegalArgumentException e) {
                    Main.getInstance().getLogger().warning("Material inválido na mina " + id + ": " + materialName);
                }
            }
        }

        if (mine.isConfigured()) {
            mine.calculateTotalBlocks();
        }

        return mine;
    }

    /**
     * Salva a mina em um arquivo YAML
     */
    public void save() {
        File file = new File(Main.getInstance().getDataFolder(), "minas/" + id + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // ✅ SEMPRE salvar display e material (remover lógica condicional)
        config.set("display", displayName);
        config.set("material", materialData);

        // Localizações
        if (spawn != null) {
            saveLocation(config, "locs.spawn", spawn);
        }
        if (exit != null) {
            saveLocation(config, "locs.exit", exit);
        }
        if (pos1 != null) {
            saveLocation(config, "locs.pos1", pos1);
        }
        if (pos2 != null) {
            saveLocation(config, "locs.pos2", pos2);
        }

        // Opções - apenas salvar se já existe no config OU se mudou do padrão
        if (config.contains("options.reset-time") || resetTime != 300) {
            config.set("options.reset-time", resetTime);
        }

        if (config.contains("options.percentage") || resetPercentage != 50.0) {
            config.set("options.percentage", resetPercentage);
        }

        if (config.contains("options.permission") || !permission.equals("mina." + id)) {
            config.set("options.permission", permission);
        }

        if (config.contains("options.pvp") || pvpEnabled) {
            config.set("options.pvp", pvpEnabled);
        }

        if (config.contains("options.order") || order != 0) {
            config.set("options.order", order);
        }

        // Blocos
        if (!blocks.isEmpty()) {
            config.set("blocks", null);

            for (Map.Entry<Material, MineBlock> entry : blocks.entrySet()) {
                String path = "blocks." + entry.getKey().name();
                MineBlock block = entry.getValue();

                config.set(path + ".chance", block.getChance());
                config.set(path + ".xp", block.getExp());
                config.set(path + ".money", block.getMoney());
                config.set(path + ".rewards", block.getRewards());
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            Main.getInstance().getLogger().severe("Erro ao salvar mina " + id + ": " + e.getMessage());
        }
    }

    /**
     * Reseta a mina de forma otimizada usando FAWE
     */
    public void reset() {
        if (!isConfigured() || resetting) return;

        resetting = true;

        // Teleportar jogadores dependendo do modo PvP
        if (pvpEnabled) {
            teleportPlayersToTop();
        } else {
            if (spawn != null) {
                for (Player player : getPlayersInMine()) {
                    player.teleport(spawn);
                    Main.getInstance().getLanguageManager().sendMessage(player, "mine.reset-teleport");
                }
            }
        }

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(pos1.getWorld());

                com.sk89q.worldedit.regions.CuboidRegion region = new com.sk89q.worldedit.regions.CuboidRegion(
                        weWorld,
                        com.sk89q.worldedit.math.BlockVector3.at(pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ()),
                        com.sk89q.worldedit.math.BlockVector3.at(pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ())
                );

                com.sk89q.worldedit.function.pattern.Pattern pattern = createWeightedPattern();

                com.sk89q.worldedit.EditSession editSession = com.sk89q.worldedit.WorldEdit.getInstance()
                        .newEditSessionBuilder()
                        .world(weWorld)
                        .build();

                editSession.setBlocks(region, pattern);
                editSession.close();

                currentBlocks = totalBlocks;
                lastReset = System.currentTimeMillis();
                resetting = false;

                Main.getInstance().getLogger().info("§aMina " + id + " resetada com sucesso!");

            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Erro ao resetar mina " + id + " com FAWE: " + e.getMessage());
                e.printStackTrace();
                resetting = false;

                Bukkit.getScheduler().runTask(Main.getInstance(), this::resetLegacy);
            }
        });
    }

    /**
     * Cria um pattern ponderado do WorldEdit baseado nas chances dos blocos
     */
    private com.sk89q.worldedit.function.pattern.Pattern createWeightedPattern() {
        com.sk89q.worldedit.function.pattern.RandomPattern randomPattern =
                new com.sk89q.worldedit.function.pattern.RandomPattern();

        for (Map.Entry<Material, MineBlock> entry : blocks.entrySet()) {
            Material material = entry.getKey();
            double chance = entry.getValue().getChance();

            com.sk89q.worldedit.world.block.BlockType blockType =
                    com.sk89q.worldedit.bukkit.BukkitAdapter.asBlockType(material);

            if (blockType != null) {
                randomPattern.add(blockType.getDefaultState(), chance);
            }
        }

        return randomPattern;
    }

    /**
     * Método de reset legado (fallback)
     */
    private void resetLegacy() {
        Main.getInstance().getLogger().warning("Usando método de reset legado para mina " + id);

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        World world = pos1.getWorld();
        Random random = new Random();
        List<Material> blockList = getWeightedBlockList();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!blockList.isEmpty()) {
                        Material material = blockList.get(random.nextInt(blockList.size()));
                        block.setType(material);
                    }
                }
            }
        }

        currentBlocks = totalBlocks;
        lastReset = System.currentTimeMillis();
    }

    /**
     * Teleporta jogadores para o topo da mina mantendo posição X/Z
     */
    private void teleportPlayersToTop() {
        List<Player> players = getPlayersInMine();

        if (players.isEmpty()) return;

        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());

        for (Player player : players) {
            Location currentLoc = player.getLocation();

            Location topLoc = new Location(
                    currentLoc.getWorld(),
                    currentLoc.getX(),
                    maxY + 1,
                    currentLoc.getZ(),
                    currentLoc.getYaw(),
                    currentLoc.getPitch()
            );

            player.teleport(topLoc);
            Main.getInstance().getLanguageManager().sendMessage(player, "mine.reset-teleport-top");
        }
    }

    /**
     * Cria uma lista ponderada de blocos baseada nas chances
     */
    private List<Material> getWeightedBlockList() {
        List<Material> weightedList = new ArrayList<>();

        for (Map.Entry<Material, MineBlock> entry : blocks.entrySet()) {
            int weight = (int) (entry.getValue().getChance() * 10);
            for (int i = 0; i < weight; i++) {
                weightedList.add(entry.getKey());
            }
        }

        return weightedList;
    }

    /**
     * Obtém jogadores dentro da mina
     */
    public List<Player> getPlayersInMine() {
        List<Player> players = new ArrayList<>();
        if (!isConfigured()) return players;

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        for (Player player : Bukkit.getOnlinePlayers()) {
            Location loc = player.getLocation();
            if (loc.getWorld().equals(pos1.getWorld())) {
                if (loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                        loc.getBlockY() >= minY && loc.getBlockY() <= maxY &&
                        loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ) {
                    players.add(player);
                }
            }
        }

        return players;
    }

    /**
     * Verifica se uma localização está dentro da mina
     */
    public boolean isInMine(Location loc) {
        if (!isConfigured() || loc == null || !loc.getWorld().equals(pos1.getWorld())) return false;

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        return loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                loc.getBlockY() >= minY && loc.getBlockY() <= maxY &&
                loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ;
    }

    /**
     * Calcula o total de blocos da mina
     */
    public void calculateTotalBlocks() {
        if (!isConfigured()) {
            totalBlocks = 0;
            return;
        }

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        totalBlocks = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        currentBlocks = totalBlocks;
    }

    /**
     * Chamado quando um bloco é quebrado na mina
     */
    public void onBlockBreak(int amount) {
        this.currentBlocks -= amount;
        checkReset();
    }

    private void checkReset() {
        double percentage = getPercentageRemaining();
        if (percentage <= resetPercentage) {
            reset();
        }
    }

    /**
     * Obtém a porcentagem de blocos restantes
     */
    public double getPercentageRemaining() {
        if (totalBlocks == 0) return 100.0;
        return (currentBlocks * 100.0) / totalBlocks;
    }

    /**
     * Verifica se a mina está totalmente configurada
     */
    public boolean isConfigured() {
        return pos1 != null && pos2 != null && pos1.getWorld() != null;
    }

    /**
     * Obtém o bloco de mina para um material específico
     */
    public MineBlock getMineBlock(Material material) {
        return blocks.get(material);
    }

    /**
     * Verifica se o material é uma cabeça base64
     */
    public boolean isBase64Head() {
        return materialData != null && materialData.length() > 50;
    }

    /**
     * Obtém o material como Material enum (se não for base64)
     */
    public Material getMaterial() {
        if (isBase64Head()) {
            return Material.PLAYER_HEAD;
        }

        try {
            return Material.valueOf(materialData.toUpperCase());
        } catch (IllegalArgumentException e) {
            Main.getInstance().getLogger().warning("Material inválido para mina " + id + ": " + materialData);
            return Material.DIAMOND_PICKAXE;
        }
    }

    /**
     * Obtém a textura base64 da cabeça (se aplicável)
     */
    public String getBase64Texture() {
        return isBase64Head() ? materialData : null;
    }

    // ============ UTILITÁRIOS DE LOCALIZAÇÃO ============

    private static Location loadLocation(YamlConfiguration config, String path) {
        String worldName = config.getString(path + ".world");
        if (worldName == null || worldName.isEmpty()) return null;

        double x = config.getDouble(path + ".x");
        double y = config.getDouble(path + ".y");
        double z = config.getDouble(path + ".z");
        float yaw = (float) config.getDouble(path + ".yaw", 0);
        float pitch = (float) config.getDouble(path + ".pitch", 0);

        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            Main.getInstance().getLogger().warning("Mundo '" + worldName + "' não encontrado para: " + path);
            Main.getInstance().getLogger().warning("A mina será carregada novamente quando o mundo estiver disponível.");

            final String finalPath = path;
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                World retryWorld = Bukkit.getWorld(worldName);
                if (retryWorld != null) {
                    Main.getInstance().getLogger().info("Mundo '" + worldName + "' carregado com sucesso para: " + finalPath);
                }
            }, 100L);

            return null;
        }

        return new Location(world, x, y, z, yaw, pitch);
    }

    private void saveLocation(YamlConfiguration config, String path, Location loc) {
        if (loc == null) {
            return;
        }

        if (loc.getWorld() == null) {
            Main.getInstance().getLogger().warning("Tentando salvar localização com mundo null: " + path);
            return;
        }

        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", loc.getYaw());
        config.set(path + ".pitch", loc.getPitch());
    }

    // ============ GETTERS E SETTERS ============

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMaterialData() {
        return materialData;
    }

    public void setMaterialData(String materialData) {
        this.materialData = materialData;
    }

    public Location getSpawn() {
        return spawn;
    }

    public void setSpawn(Location spawn) {
        this.spawn = spawn;
    }

    public Location getExit() {
        return exit;
    }

    public void setExit(Location exit) {
        this.exit = exit;
    }

    public Location getPos1() {
        return pos1;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
        calculateTotalBlocks();
    }

    public Location getPos2() {
        return pos2;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
        calculateTotalBlocks();
    }

    public int getResetTime() {
        return resetTime;
    }

    public void setResetTime(int resetTime) {
        this.resetTime = resetTime;
    }

    public double getResetPercentage() {
        return resetPercentage;
    }

    public void setResetPercentage(double resetPercentage) {
        this.resetPercentage = resetPercentage;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public void setPvpEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Map<Material, MineBlock> getBlocks() {
        return blocks;
    }

    public void addBlock(MineBlock block) {
        blocks.put(block.getMaterial(), block);
    }

    public void removeBlock(Material material) {
        blocks.remove(material);
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public int getCurrentBlocks() {
        return currentBlocks;
    }

    public long getLastReset() {
        return lastReset;
    }

    public boolean isResetting() {
        return resetting;
    }

    public long getTimeUntilReset() {
        long elapsed = (System.currentTimeMillis() - lastReset) / 1000;
        return Math.max(0, resetTime - elapsed);
    }
}