package github.dimazbtw.minas.events;

import github.dimazbtw.minas.data.Mine;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Evento disparado quando um jogador sai de uma mina
 */
public class PlayerQuitMineEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Mine mine;

    public PlayerQuitMineEvent(Player player, Mine mine) {
        super(player);
        this.mine = mine;
    }

    /**
     * Obtém a mina que o jogador saiu
     */
    public Mine getMine() {
        return mine;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}