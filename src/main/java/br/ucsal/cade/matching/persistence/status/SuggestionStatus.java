package br.ucsal.cade.matching.persistence.status;

import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;

import java.util.List;

/**
 * Situação de uma sugestão do cruzamento (seção 6.3), no mesmo modelo de {@code FoundItemStatus}: transição fora da
 * tabela é recusada (RN01).
 *
 * <table>
 *   <caption>Transições permitidas</caption>
 *   <tr><th>Ação</th><th>De</th><th>Para</th></tr>
 *   <tr><td>{@link #putOnHold()} (pôr em espera)</td><td>Nova</td><td>Em espera</td></tr>
 *   <tr><td>{@link #resume()} (retomar)</td><td>Em espera</td><td>Nova</td></tr>
 *   <tr><td>{@link #summon()} (convocar)</td><td>Nova</td><td>Convocada</td></tr>
 *   <tr><td>{@link #discard()} (descartar)</td><td>Nova</td><td>Descartada</td></tr>
 *   <tr><td>{@link #complete()} (concluir)</td><td>Convocada</td><td>Concluída</td></tr>
 *   <tr><td>{@link #refuse()} (recusar)</td><td>Convocada</td><td>Recusada</td></tr>
 *   <tr><td>{@link #expire()} (7 dias)</td><td>Convocada</td><td>Vencida</td></tr>
 *   <tr><td>{@link #close()} (encerrar)</td><td>Nova, Em espera</td><td>Encerrada</td></tr>
 * </table>
 *
 * <p>Concluída, Descartada, Recusada, Vencida e Encerrada são finais.
 */
public sealed interface SuggestionStatus {

    New NEW = new New();
    OnHold ON_HOLD = new OnHold();
    Summoned SUMMONED = new Summoned();
    Completed COMPLETED = new Completed();
    Discarded DISCARDED = new Discarded();
    Refused REFUSED = new Refused();
    Expired EXPIRED = new Expired();
    Closed CLOSED = new Closed();

    List<SuggestionStatus> ALL = List.of(NEW, ON_HOLD, SUMMONED, COMPLETED, DISCARDED, REFUSED, EXPIRED, CLOSED);

    /** As que ainda pedem atenção do funcionário; as finais saem das listas. */
    List<SuggestionStatus> ACTIVE = List.of(NEW, ON_HOLD, SUMMONED);

    /** Toda sugestão nasce Nova: o cruzamento encontrou compatibilidade média ou alta. */
    static SuggestionStatus initial() {
        return NEW;
    }

    static SuggestionStatus fromCode(String code) {
        return ALL.stream()
                .filter(status -> status.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Situação de sugestão desconhecida: " + code));
    }

    /** Outra sugestão do mesmo objeto foi convocada (RN18). */
    default SuggestionStatus putOnHold() {
        throw refused("pôr em espera");
    }

    /** A convocação da outra sugestão do objeto venceu ou foi recusada (RN18). */
    default SuggestionStatus resume() {
        throw refused("retomar");
    }

    default SuggestionStatus summon() {
        throw refused("convocar");
    }

    /** O funcionário discorda da sugestão; exige motivo (RN08). */
    default SuggestionStatus discard() {
        throw refused("descartar");
    }

    default SuggestionStatus complete() {
        throw refused("concluir");
    }

    /** A verificação falhou no balcão; conta para o limite de recusas (RN12). */
    default SuggestionStatus refuse() {
        throw refused("recusar");
    }

    /** A convocação completou 7 dias sem retirada (RN15). */
    default SuggestionStatus expire() {
        throw refused("vencer");
    }

    /** O objeto foi entregue a outra pessoa, ou a perda foi cancelada ou venceu. */
    default SuggestionStatus close() {
        throw refused("encerrar");
    }

    default boolean isFinal() {
        return switch (this) {
            case Completed _, Discarded _, Refused _, Expired _, Closed _ -> true;
            case New _, OnHold _, Summoned _ -> false;
        };
    }

    default String code() {
        return switch (this) {
            case New _ -> "NEW";
            case OnHold _ -> "ON_HOLD";
            case Summoned _ -> "SUMMONED";
            case Completed _ -> "COMPLETED";
            case Discarded _ -> "DISCARDED";
            case Refused _ -> "REFUSED";
            case Expired _ -> "EXPIRED";
            case Closed _ -> "CLOSED";
        };
    }

    default String label() {
        return switch (this) {
            case New _ -> "Nova";
            case OnHold _ -> "Em espera";
            case Summoned _ -> "Convocada";
            case Completed _ -> "Concluída";
            case Discarded _ -> "Descartada";
            case Refused _ -> "Recusada";
            case Expired _ -> "Vencida";
            case Closed _ -> "Encerrada";
        };
    }

    private InvalidStatusTransitionException refused(String action) {
        return new InvalidStatusTransitionException(action, "uma sugestão", label());
    }

    record New() implements SuggestionStatus {

        @Override
        public SuggestionStatus putOnHold() {
            return ON_HOLD;
        }

        @Override
        public SuggestionStatus summon() {
            return SUMMONED;
        }

        @Override
        public SuggestionStatus discard() {
            return DISCARDED;
        }

        @Override
        public SuggestionStatus close() {
            return CLOSED;
        }
    }

    record OnHold() implements SuggestionStatus {

        @Override
        public SuggestionStatus resume() {
            return NEW;
        }

        @Override
        public SuggestionStatus close() {
            return CLOSED;
        }
    }

    record Summoned() implements SuggestionStatus {

        @Override
        public SuggestionStatus complete() {
            return COMPLETED;
        }

        @Override
        public SuggestionStatus refuse() {
            return REFUSED;
        }

        @Override
        public SuggestionStatus expire() {
            return EXPIRED;
        }
    }

    record Completed() implements SuggestionStatus {
    }

    record Discarded() implements SuggestionStatus {
    }

    record Refused() implements SuggestionStatus {
    }

    record Expired() implements SuggestionStatus {
    }

    record Closed() implements SuggestionStatus {
    }
}
