package br.ucsal.cade.shared.audit;

import java.util.Objects;
import java.util.UUID;

/**
 * Contrato do evento de histórico (RF43, RNF07, RN19, D10). Qualquer módulo que realiza uma ação relevante publica
 * este evento com o {@code EventPublisher}, dentro da mesma transação da ação. O módulo de administração ouve o
 * evento de forma síncrona e grava o registro, então, se a gravação falhar, a ação inteira é desfeita.
 * <p>
 * O autor vem da claim {@code sub} do JWT, que guarda o id do usuário. Para ações do sistema (tarefas agendadas,
 * cruzamento automático), use {@link #SYSTEM_ACTOR}. O autor nunca é nulo, para que quem esquecer de informá-lo
 * falhe cedo.
 *
 * @param actorId    id do usuário autor da ação, ou {@link #SYSTEM_ACTOR} para ações do sistema
 * @param action     nome da ação em MAIÚSCULAS_COM_UNDERSCORE, por exemplo {@code FOUND_ITEM_REGISTERED}
 * @param entityType nome simples da entidade afetada, por exemplo {@code FoundItem}
 * @param entityId   id da entidade afetada
 * @param reason     motivo da ação; nunca vazio
 */
public record AuditEvent(UUID actorId, String action, String entityType, UUID entityId, String reason) {

    /** Autor usado quando a ação é feita pelo sistema e não por uma pessoa. */
    public static final UUID SYSTEM_ACTOR = new UUID(0L, 0L);

    /** Tamanho máximo de {@code action} e de {@code entityType}. */
    public static final int MAX_NAME_LENGTH = 60;

    /** Tamanho máximo de {@code reason}. */
    public static final int MAX_REASON_LENGTH = 1000;

    public AuditEvent {
        Objects.requireNonNull(actorId);
        Objects.requireNonNull(action);
        Objects.requireNonNull(entityType);
        Objects.requireNonNull(entityId);
        Objects.requireNonNull(reason);

        requireFilled(action, "action");
        requireFilled(entityType, "entityType");
        requireFilled(reason, "reason");

        requireMaxLength(action, MAX_NAME_LENGTH, "action");
        requireMaxLength(entityType, MAX_NAME_LENGTH, "entityType");
        requireMaxLength(reason, MAX_REASON_LENGTH, "reason");
    }

    private static void requireFilled(String value, String field) {
        if (value.isBlank()) {
            throw new IllegalArgumentException("O campo " + field + " não pode ser vazio.");
        }
    }

    private static void requireMaxLength(String value, int max, String field) {
        if (value.length() > max) {
            throw new IllegalArgumentException("O campo " + field + " pode ter no máximo " + max + " caracteres.");
        }
    }
}
