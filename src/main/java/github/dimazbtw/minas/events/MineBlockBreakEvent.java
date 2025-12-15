package github.dimazbtw.minas.events;

import github.dimazbtw.minas.data.Mine;
import github.dimazbtw.minas.data.MineBlock;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Evento disparado quando um jogador quebra um bloco dentro de uma mina
 */
public class MineBlockBreakEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Mine mine;
    private final Block block;
    private final MineBlock mineBlock;
    private boolean cancelled;
    private int xp;
    private double money;

    public MineBlockBreakEvent(Player player, Mine mine, Block block, MineBlock mineBlock) {
        super(player);
        this.mine = mine;
        this.block = block;
        this.mineBlock = mineBlock;
        this.cancelled = false;

        // Valores padrão do MineBlock
        if (mineBlock != null) {
            this.xp = mineBlock.getExp();
            this.money = mineBlock.getMoney();
        } else {
            this.xp = 0;
            this.money = 0.0;
        }
    }

    /**
     * Obtém a mina onde o bloco foi quebrado
     */
    public Mine getMine() {
        return mine;
    }

    /**
     * Obtém o bloco que foi quebrado
     */
    public Block getBlock() {
        return block;
    }

    /**
     * Obtém a configuração do bloco da mina (pode ser null)
     */
    public MineBlock getMineBlock() {
        return mineBlock;
    }

    /**
     * Obtém a quantidade de XP que será dada
     */
    public int getXp() {
        return xp;
    }

    /**
     * Define a quantidade de XP que será dada
     */
    public void setXp(int xp) {
        this.xp = Math.max(0, xp);
    }

    /**
     * Obtém a quantidade de dinheiro que será dada
     */
    public double getMoney() {
        return money;
    }

    /**
     * Define a quantidade de dinheiro que será dada
     */
    public void setMoney(double money) {
        this.money = Math.max(0, money);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}