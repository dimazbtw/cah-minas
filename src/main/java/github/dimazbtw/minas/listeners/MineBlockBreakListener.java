package github.dimazbtw.minas.listeners;

import github.dimazbtw.minas.Main;
import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.data.MineBlock;
import github.dimazbtw.minas.data.PickaxeData;
import github.dimazbtw.minas.events.MineBlockBreakEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MineBlockBreakListener implements Listener {

    private final Main plugin;

    // Pool de listas para evitar alocações
    private static final ThreadLocal<List<Block>> BLOCK_LIST_POOL =
            ThreadLocal.withInitial(() -> new ArrayList<>(512));

    public MineBlockBreakListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location blockLoc = block.getLocation();

        // Cache de mina
        Mine mine = plugin.getMineLocationCache().getCachedMine(blockLoc, plugin.getMineManager());
        if (mine == null) return;

        // Verificar permissão
        if (!player.hasPermission("minas.admin") && !player.hasPermission(mine.getPermission())) {
            event.setCancelled(true);
            plugin.getLanguageManager().sendMessage(player, "mine.no-permission");
            return;
        }

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (!plugin.getPickaxeManager().isMinePickaxe(handItem)) {
            event.setCancelled(true);
            plugin.getLanguageManager().sendMessage(player, "mine.need-pickaxe");
            return;
        }

        if (!plugin.getPickaxeManager().isOwner(player, handItem)) {
            event.setCancelled(true);
            player.sendMessage("§cEsta picareta não é sua!");
            return;
        }

        event.setDropItems(false);

        PickaxeData pickaxeData = plugin.getDatabaseManager().getPickaxeData(player.getUniqueId());

        // Calcular triggers uma vez
        boolean explosionTriggered = shouldTrigger(pickaxeData.getExplosion(), pickaxeData.getExplosionChance());
        boolean destroyerTriggered = !explosionTriggered && shouldTrigger(pickaxeData.getDestroyer(), pickaxeData.getDestroyerChance());

        List<Block> blocksToMine = getBlocksToMine(block, mine, destroyerTriggered, explosionTriggered, player);

        processBlocks(player, mine, blocksToMine, pickaxeData);
    }

    private boolean shouldTrigger(int level, double chance) {
        return level > 0 && ThreadLocalRandom.current().nextDouble() * 100 < chance;
    }

    private List<Block> getBlocksToMine(Block block, Mine mine, boolean destroyerTriggered,
                                        boolean explosionTriggered, Player player) {
        List<Block> blocksToMine = BLOCK_LIST_POOL.get();
        blocksToMine.clear();

        if (destroyerTriggered) {
            getLayerBlocksOptimized(block, mine, blocksToMine);
            player.sendMessage("§d§l✦ DESTRUIDOR! §fUma camada inteira foi destruída!");
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 0.8f, 0.5f);
            block.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, block.getLocation().add(0.5, 0.5, 0.5), 3);
        } else if (explosionTriggered) {
            getExplosionBlocksOptimized(block, mine, blocksToMine);
            player.sendMessage("§c§l✦ EXPLOSÃO! §fÁrea 7x7 destruída!");
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.2f);
            block.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, block.getLocation().add(0.5, 0.5, 0.5), 2);
        } else {
            blocksToMine.add(block);
        }

        return blocksToMine;
    }

    private void processBlocks(Player player, Mine mine, List<Block> blocksToMine, PickaxeData pickaxeData) {
        int totalBlocksMined = 0;
        double totalMoney = 0;
        int totalExp = 0;
        int totalPickaxeExp = 0;

        // Calcular multiplicadores uma vez
        double totalBonus = plugin.getBonusManager().getTotalMultiplier(player, pickaxeData.getSkinOrder());
        double fortuneMultiplier = pickaxeData.getFortuneMultiplier();
        double multiplierBonus = pickaxeData.getMultiplierBonus();
        double experiencedBonus = pickaxeData.getExperiencedBonus();

        final int blockCount = blocksToMine.size();

        for (int i = 0; i < blockCount; i++) {
            Block targetBlock = blocksToMine.get(i);
            Material blockType = targetBlock.getType();

            if (blockType == Material.AIR) continue;

            MineBlock targetMineBlock = mine.getMineBlock(blockType);

            MineBlockBreakEvent mineEvent = new MineBlockBreakEvent(player, mine, targetBlock, targetMineBlock);
            Bukkit.getPluginManager().callEvent(mineEvent);

            if (mineEvent.isCancelled()) continue;

            // Cálculos inline
            double money = mineEvent.getMoney() * fortuneMultiplier * totalBonus;
            int exp = (int) (mineEvent.getXp() * fortuneMultiplier * totalBonus * multiplierBonus);

            totalMoney += money;
            totalExp += exp;
            totalBlocksMined++;
            totalPickaxeExp += (int) Math.ceil(experiencedBonus);

            if (targetMineBlock != null) {
                targetMineBlock.executeRewards(player);
            }

            targetBlock.setType(Material.AIR);
        }

        // Atualizar mina uma vez
        mine.onBlockBreak(totalBlocksMined);

        // Processar rewards
        if (totalMoney > 0 && plugin.getVaultManager().isEconomyEnabled()) {
            plugin.getVaultManager().depositPlayer(player, totalMoney);
        }

        if (totalExp > 0) {
            plugin.getVaultManager().giveExp(player, totalExp);
        }

        plugin.getActionBarManager().addEarnings(player, totalMoney, totalExp);
        plugin.getPickaxeBossBarManager().showBossBar(player);

        pickaxeData.incrementBlocksMined(totalBlocksMined);
        pickaxeData.addExp(totalPickaxeExp);

        // Salvar async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().savePickaxeData(pickaxeData);
        });

        plugin.getPickaxeManager().updatePickaxe(player);
        plugin.getSessionManager().incrementBlocksMined(player, totalBlocksMined);
    }

    private void getLayerBlocksOptimized(Block centerBlock, Mine mine, List<Block> blocks) {
        Location pos1 = mine.getPos1();
        Location pos2 = mine.getPos2();

        if (pos1 == null || pos2 == null) {
            blocks.add(centerBlock);
            return;
        }

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        int y = centerBlock.getY();

        boolean hasBlockFilter = !mine.getBlocks().isEmpty();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Block block = centerBlock.getWorld().getBlockAt(x, y, z);
                Material type = block.getType();

                if (type == Material.AIR) continue;
                if (hasBlockFilter && mine.getMineBlock(type) == null) continue;

                blocks.add(block);
            }
        }
    }

    private void getExplosionBlocksOptimized(Block centerBlock, Mine mine, List<Block> blocks) {
        blocks.add(centerBlock);

        final int radius = 3;
        boolean hasBlockFilter = !mine.getBlocks().isEmpty();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    Block block = centerBlock.getRelative(x, y, z);

                    if (!mine.isInMine(block.getLocation())) continue;

                    Material type = block.getType();
                    if (type == Material.AIR) continue;
                    if (hasBlockFilter && mine.getMineBlock(type) == null) continue;

                    blocks.add(block);
                }
            }
        }
    }
}