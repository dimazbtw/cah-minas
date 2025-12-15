package github.dimazbtw.minas.events;

import github.dimazbtw.minas.data.Mine;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Evento disparado quando um jogador entra em uma mina
 */
public class PlayerJoinMineEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Mine mine;
    private boolean cancelled;

    public PlayerJoinMineEvent(Player player, Mine mine) {
        super(player);
        this.mine = mine;
        this.cancelled = false;
    }

    /**
     * Obtém a mina que o jogador entrou
     */
    public Mine getMine() {
        return mine;
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