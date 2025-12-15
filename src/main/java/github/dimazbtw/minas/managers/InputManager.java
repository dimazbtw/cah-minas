package github.dimazbtw.minas.managers;

import github.dimazbtw.minas.Main;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Gerencia inputs de jogadores via chat
 */
public class InputManager {

    private final Main plugin;
    private final Map<UUID, InputRequest> pendingInputs;

    public InputManager(Main plugin) {
        this.plugin = plugin;
        this.pendingInputs = new HashMap<>();
    }

    /**
     * Solicita um input do jogador
     */
    public void requestInput(Player player, String context, Consumer<String> callback) {
        UUID uuid = player.getUniqueId();

        // Remover input anterior se existir
        pendingInputs.remove(uuid);

        // Criar novo request
        InputRequest request = new InputRequest(context, callback, System.currentTimeMillis());
        pendingInputs.put(uuid, request);
    }

    /**
     * Processa um input do jogador
     * @return true se o input foi processado, false se não havia input pendente
     */
    public boolean processInput(Player player, String message) {
        UUID uuid = player.getUniqueId();
        InputRequest request = pendingInputs.remove(uuid);

        if (request == null) {
            return false;
        }

        // Verificar se expirou (5 minutos)
        if (System.currentTimeMillis() - request.getTimestamp() > 300000) {
            return false;
        }

        // Verificar se é cancelamento
        if (message.equalsIgnoreCase("cancelar") || message.equalsIgnoreCase("cancel")) {
            player.sendMessage(plugin.getEnchantManager().getMessage("select-amount-cancelled"));
            return true;
        }

        // Executar callback
        request.getCallback().accept(message);
        return true;
    }

    /**
     * Verifica se o jogador tem input pendente
     */
    public boolean hasPendingInput(Player player) {
        return pendingInputs.containsKey(player.getUniqueId());
    }

    /**
     * Cancela o input pendente de um jogador
     */
    public void cancelInput(Player player) {
        pendingInputs.remove(player.getUniqueId());
    }

    /**
     * Limpa inputs expirados
     */
    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        pendingInputs.entrySet().removeIf(entry ->
                now - entry.getValue().getTimestamp() > 300000);
    }

    /**
     * Representa uma solicitação de input
     */
    private static class InputRequest {
        private final String context;
        private final Consumer<String> callback;
        private final long timestamp;

        public InputRequest(String context, Consumer<String> callback, long timestamp) {
            this.context = context;
            this.callback = callback;
            this.timestamp = timestamp;
        }

        public String getContext() {
            return context;
        }

        public Consumer<String> getCallback() {
            return callback;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}