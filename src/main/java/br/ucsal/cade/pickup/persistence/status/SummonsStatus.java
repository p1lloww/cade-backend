package br.ucsal.cade.pickup.persistence.status;

import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;

import java.util.List;

/**
 * Situação de uma convocação, no mesmo modelo de {@code SuggestionStatus}: transição fora da tabela é recusada (RN01).
 * Acompanha a sugestão convocada: Ativa enquanto ela está Convocada, e depois Concluída, Recusada ou Vencida como ela
 * (seção 6.3).
 *
 * <table>
 *   <caption>Transições permitidas</caption>
 *   <tr><th>Ação</th><th>De</th><th>Para</th></tr>
 *   <tr><td>{@link #complete()} (retirada registrada)</td><td>Ativa</td><td>Concluída</td></tr>
 *   <tr><td>{@link #refuse()} (verificação falhou no balcão)</td><td>Ativa</td><td>Recusada</td></tr>
 *   <tr><td>{@link #expire()} (7 dias sem retirada)</td><td>Ativa</td><td>Vencida</td></tr>
 * </table>
 *
 * <p>Concluída, Recusada e Vencida são finais. Um objeto tem no máximo uma convocação Ativa (RN18).
 */
public sealed interface SummonsStatus {

    Active ACTIVE = new Active();
    Completed COMPLETED = new Completed();
    Refused REFUSED = new Refused();
    Expired EXPIRED = new Expired();

    List<SummonsStatus> ALL = List.of(ACTIVE, COMPLETED, REFUSED, EXPIRED);

    /** Toda convocação nasce Ativa: o estudante tem o prazo da RN15 para retirar o objeto. */
    static SummonsStatus initial() {
        return ACTIVE;
    }

    static SummonsStatus fromCode(String code) {
        return ALL.stream()
                .filter(status -> status.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Situação de convocação desconhecida: " + code));
    }

    /** O estudante retirou o objeto no balcão. */
    default SummonsStatus complete() {
        throw refused("concluir");
    }

    /** A verificação falhou no balcão; conta para o limite de recusas (RN12). */
    default SummonsStatus refuse() {
        throw refused("recusar");
    }

    /** O prazo da RN15 passou sem retirada. */
    default SummonsStatus expire() {
        throw refused("vencer");
    }

    default boolean isFinal() {
        return switch (this) {
            case Completed _, Refused _, Expired _ -> true;
            case Active _ -> false;
        };
    }

    default String code() {
        return switch (this) {
            case Active _ -> "ACTIVE";
            case Completed _ -> "COMPLETED";
            case Refused _ -> "REFUSED";
            case Expired _ -> "EXPIRED";
        };
    }

    default String label() {
        return switch (this) {
            case Active _ -> "Ativa";
            case Completed _ -> "Concluída";
            case Refused _ -> "Recusada";
            case Expired _ -> "Vencida";
        };
    }

    private InvalidStatusTransitionException refused(String action) {
        return new InvalidStatusTransitionException(action, "uma convocação", label());
    }

    record Active() implements SummonsStatus {

        @Override
        public SummonsStatus complete() {
            return COMPLETED;
        }

        @Override
        public SummonsStatus refuse() {
            return REFUSED;
        }

        @Override
        public SummonsStatus expire() {
            return EXPIRED;
        }
    }

    record Completed() implements SummonsStatus {
    }

    record Refused() implements SummonsStatus {
    }

    record Expired() implements SummonsStatus {
    }
}
