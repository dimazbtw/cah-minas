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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class Mine {

    private final String id;
    private String displayName;

    // Localizações
    private Location spawn;
    private Location exit;
    private Location pos1;
    private Location pos2;

    // Opções
    private int resetTime; // em segundos
    private double resetPercentage;
    private String permission;
    private boolean pvpEnabled;
    private int order; // Ordem de prioridade

    // Blocos
    private final Map<Material, MineBlock> blocks;

    // Estado
    private int totalBlocks;
    private int currentBlocks;
    private long lastReset;
    private boolean resetting;

    public Mine(String id) {
        this.id = id;
        this.displayName = "&7Mina " + id;
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

        // REMOVIDO: Não carregar defaults aqui, pois sobrescreve os valores do arquivo

        mine.displayName = config.getString("display", "&7Mina " + id);

        // Carregar localizações
        mine.spawn = loadLocation(config, "locs.spawn");
        mine.exit = loadLocation(config, "locs.exit");
        mine.pos1 = loadLocation(config, "locs.pos1");
        mine.pos2 = loadLocation(config, "locs.pos2");

        // Carregar opções
        mine.resetTime = config.getInt("options.reset-time", 300);
        mine.resetPercentage = config.getDouble("options.percentage", 50.0);
        mine.permission = config.getString("options.permission", "mina." + id);
        mine.pvpEnabled = config.getBoolean("options.pvp", false);
        mine.order = config.getInt("options.order", 0);

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

                        // Carregar rewards
                        List<String> rewards = blockSection.getStringList("rewards");
                        mineBlock.setRewards(rewards);

                        mine.blocks.put(material, mineBlock);
                    }
                } catch (IllegalArgumentException e) {
                    Main.getInstance().getLogger().warning("Material inválido na mina " + id + ": " + materialName);
                }
            }
        }

        // Calcular total de blocos
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
        YamlConfiguration config = new YamlConfiguration();

        config.set("display", displayName);

        // Salvar localizações
        saveLocation(config, "locs.spawn", spawn);
        saveLocation(config, "locs.exit", exit);
        saveLocation(config, "locs.pos1", pos1);
        saveLocation(config, "locs.pos2", pos2);

        // Salvar opções
        config.set("options.reset-time", resetTime);
        config.set("options.percentage", resetPercentage);
        config.set("options.permission", permission);
        config.set("options.pvp", pvpEnabled);
        config.set("options.order", order);

        // Salvar blocos
        for (Map.Entry<Material, MineBlock> entry : blocks.entrySet()) {
            String path = "blocks." + entry.getKey().name();
            MineBlock block = entry.getValue();

            config.set(path + ".chance", block.getChance());
            config.set(path + ".exp", block.getExp());
            config.set(path + ".money", block.getMoney());
            config.set(path + ".rewards", block.getRewards());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            Main.getInstance().getLogger().severe("Erro ao salvar mina " + id + ": " + e.getMessage());
        }
    }

    /**
     * Reseta a mina (preenche com blocos) usando FAWE para máxima performance
     */
    public void reset() {
        if (!isConfigured() || resetting) return;

        resetting = true;

        // Teleportar jogadores para o spawn antes de resetar
        if (spawn != null) {
            for (Player player : getPlayersInMine()) {
                player.teleport(spawn);
                Main.getInstance().getLanguageManager().sendMessage(player, "mine.reset-teleport");
            }
        }

        // Usar FAWE para performance
        // Usar FAWE para performance
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

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Erro ao resetar mina " + id + " com FAWE: " + e.getMessage());
            e.printStackTrace();
            resetting = false;
            resetLegacy();
        }
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

            // Converter Bukkit Material para WorldEdit BlockType
            com.sk89q.worldedit.world.block.BlockType blockType =
                    com.sk89q.worldedit.bukkit.BukkitAdapter.asBlockType(material);

            if (blockType != null) {
                // Adicionar ao pattern com peso baseado na chance
                randomPattern.add(blockType.getDefaultState(), chance);
            }
        }

        return randomPattern;
    }

    /**
     * Método de reset legado (fallback) caso FAWE não esteja disponível
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
    public void onBlockBreak() {
        if (currentBlocks > 0) {
            currentBlocks--;
        }

        // Verificar se deve resetar por porcentagem
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

    // ============ UTILITÁRIOS DE LOCALIZAÇÃO ============

    private static Location loadLocation(YamlConfiguration config, String path) {
        String worldName = config.getString(path + ".world");
        if (worldName == null || worldName.isEmpty()) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            Main.getInstance().getLogger().warning("Mundo '" + worldName + "' não encontrado para localização: " + path);
            return null;
        }

        double x = config.getDouble(path + ".x");
        double y = config.getDouble(path + ".y");
        double z = config.getDouble(path + ".z");
        float yaw = (float) config.getDouble(path + ".yaw", 0);
        float pitch = (float) config.getDouble(path + ".pitch", 0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    private void saveLocation(YamlConfiguration config, String path, Location loc) {
        if (loc == null) {
            config.set(path, null);
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