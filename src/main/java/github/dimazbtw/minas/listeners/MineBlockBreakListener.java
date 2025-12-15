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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MineBlockBreakListener implements Listener {

    private final Main plugin;
    private final Set<Location> processingBlocks;
    private final Random random;

    public MineBlockBreakListener(Main plugin) {
        this.plugin = plugin;
        this.processingBlocks = new HashSet<>();
        this.random = new Random();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location blockLoc = block.getLocation();

        if (processingBlocks.contains(blockLoc)) {
            return;
        }

        Mine mine = plugin.getMineManager().getMineAt(blockLoc);
        if (mine == null) return;

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

        boolean explosionTriggered = false;
        boolean destroyerTriggered = false;

        if (pickaxeData.getExplosion() > 0) {
            double explosionChance = pickaxeData.getExplosionChance();
            if (random.nextDouble() * 100 < explosionChance) {
                explosionTriggered = true;
            }
        }

        if (pickaxeData.getDestroyer() > 0) {
            double destroyerChance = pickaxeData.getDestroyerChance();
            if (random.nextDouble() * 100 < destroyerChance) {
                destroyerTriggered = true;
            }
        }

        List<Block> blocksToMine;

        if (destroyerTriggered) {
            blocksToMine = getLayerBlocks(block, mine);
            player.sendMessage("§d§l✦ DESTRUIDOR! §fUma camada inteira foi destruída!");
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 0.8f, 0.5f);
            block.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, block.getLocation().add(0.5, 0.5, 0.5), 3);
        } else if (explosionTriggered) {
            blocksToMine = getExplosionBlocks(block, mine, 3);
            player.sendMessage("§c§l✦ EXPLOSÃO! §fÁrea 7x7 destruída!");
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.2f);
            block.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, block.getLocation().add(0.5, 0.5, 0.5), 2);
        } else {
            blocksToMine = new ArrayList<>();
            blocksToMine.add(block);
        }

        int totalBlocksMined = 0;
        double totalMoney = 0;
        int totalExp = 0;
        int totalPickaxeExp = 0;

        // Obter bónus total (skin + permissões) usando skinOrder
        double totalBonus = plugin.getBonusManager().getTotalMultiplier(player, pickaxeData.getSkinOrder());

        for (Block targetBlock : blocksToMine) {
            processingBlocks.add(targetBlock.getLocation());

            MineBlock targetMineBlock = mine.getMineBlock(targetBlock.getType());

            MineBlockBreakEvent mineEvent =
                    new MineBlockBreakEvent(player, mine, targetBlock, targetMineBlock);
            Bukkit.getPluginManager().callEvent(mineEvent);

            if (mineEvent.isCancelled()) {
                processingBlocks.remove(targetBlock.getLocation());
                continue;
            }

            double fortuneMultiplier = pickaxeData.getFortuneMultiplier();

            // Aplicar bónus total (skin + permissões) aos ganhos
            double money = mineEvent.getMoney() * fortuneMultiplier * totalBonus;
            int exp = (int) (mineEvent.getXp() * fortuneMultiplier * totalBonus);

            double multiplierBonus = pickaxeData.getMultiplierBonus();
            exp = (int) (exp * multiplierBonus);

            totalMoney += money;
            totalExp += exp;
            totalBlocksMined++;

            double experiencedBonus = pickaxeData.getExperiencedBonus();
            totalPickaxeExp += (int) Math.ceil(experiencedBonus);

            if (targetMineBlock != null) {
                targetMineBlock.executeRewards(player);
            }

            targetBlock.setType(Material.AIR);
            mine.onBlockBreak();

            processingBlocks.remove(targetBlock.getLocation());
        }

        if (totalMoney > 0 && plugin.getVaultManager().isEconomyEnabled()) {
            plugin.getVaultManager().depositPlayer(player, totalMoney);
        }

        if (totalExp > 0) {
            plugin.getVaultManager().giveExp(player, totalExp);
        }

        // ACTIONBAR – acumula ganhos (mostra a cada 5s)
        plugin.getActionBarManager().addEarnings(player, totalMoney, totalExp);
        plugin.getPickaxeBossBarManager().showBossBar(player);

        pickaxeData.incrementBlocksMined(totalBlocksMined);

        // Guardar nível antigo para verificar level up
        int oldLevel = pickaxeData.getLevel();
        pickaxeData.addExp(totalPickaxeExp);
        int newLevel = pickaxeData.getLevel();

        plugin.getDatabaseManager().savePickaxeData(pickaxeData);

        plugin.getPickaxeManager().updatePickaxe(player);

        for (int i = 0; i < totalBlocksMined; i++) {
            plugin.getSessionManager().incrementBlocksMined(player);
        }
    }


    private List<Block> getLayerBlocks(Block centerBlock, Mine mine) {
        List<Block> blocks = new ArrayList<>();

        Location pos1 = mine.getPos1();
        Location pos2 = mine.getPos2();

        if (pos1 == null || pos2 == null) {
            blocks.add(centerBlock);
            return blocks;
        }

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        int y = centerBlock.getY();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Block block = centerBlock.getWorld().getBlockAt(x, y, z);

                if (block.getType() == Material.AIR) continue;

                if (mine.getMineBlock(block.getType()) != null || mine.getBlocks().isEmpty()) {
                    blocks.add(block);
                }
            }
        }

        return blocks;
    }

    private List<Block> getExplosionBlocks(Block centerBlock, Mine mine, int radius) {
        List<Block> blocks = new ArrayList<>();
        blocks.add(centerBlock);

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    Block block = centerBlock.getRelative(x, y, z);
                    Location blockLoc = block.getLocation();

                    if (!mine.isInMine(blockLoc)) continue;

                    if (block.getType() == Material.AIR) continue;

                    if (mine.getMineBlock(block.getType()) != null || mine.getBlocks().isEmpty()) {
                        blocks.add(block);
                    }
                }
            }
        }

        return blocks;
    }
}