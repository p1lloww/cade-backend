package br.ucsal.cade.lossreports.persistence.status;

import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;

import java.util.List;

/**
 * Situação do registro de perda (seção 6.2). Transição fora da tabela é recusada (RN01).
 *
 * <table>
 *   <caption>Transições permitidas</caption>
 *   <tr><th>Ação</th><th>De</th><th>Para</th></tr>
 *   <tr><td>{@link #requestInfo()} (pedir informações)</td><td>Aberto</td><td>Aguardando informações</td></tr>
 *   <tr><td>{@link #resume()} (retomar)</td><td>Aguardando informações</td><td>Aberto</td></tr>
 *   <tr><td>{@link #summon()} (convocar)</td><td>Aberto</td><td>Convocado</td></tr>
 *   <tr><td>{@link #release()} (liberar)</td><td>Convocado</td><td>Aberto</td></tr>
 *   <tr><td>{@link #complete()} (concluir)</td><td>Convocado</td><td>Concluído</td></tr>
 *   <tr><td>{@link #expire()} (30 dias)</td><td>Aberto</td><td>Vencido</td></tr>
 *   <tr><td>{@link #cancel()} (cancelar)</td><td>Aberto, Aguardando informações</td><td>Cancelado</td></tr>
 * </table>
 *
 * <p>Concluído, Vencido e Cancelado são finais.
 */
public sealed interface LossReportStatus {

    Open OPEN = new Open();
    AwaitingInfo AWAITING_INFO = new AwaitingInfo();
    Summoned SUMMONED = new Summoned();
    Completed COMPLETED = new Completed();
    Expired EXPIRED = new Expired();
    Cancelled CANCELLED = new Cancelled();

    List<LossReportStatus> ALL = List.of(OPEN, AWAITING_INFO, SUMMONED, COMPLETED, EXPIRED, CANCELLED);

    List<LossReportStatus> ACTIVE = ALL.stream().filter(status -> !status.isFinal()).toList();

    static LossReportStatus initial() {
        return OPEN;
    }

    static LossReportStatus fromCode(String code) {
        return ALL.stream()
                .filter(status -> status.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Situação de registro desconhecida: " + code));
    }

    /** O funcionário pede mais informações ao estudante, com motivo. */
    default LossReportStatus requestInfo() {
        throw refused("pedir informações sobre");
    }

    /** Volta para Aberto: o estudante respondeu ao pedido, ou passaram 5 dias sem resposta (RN13). */
    default LossReportStatus resume() {
        throw refused("retomar");
    }

    /** O funcionário convoca o estudante a partir de uma sugestão. */
    default LossReportStatus summon() {
        throw refused("convocar");
    }

    /** Volta para Aberto: a convocação venceu (RN15) ou a verificação falhou no balcão. */
    default LossReportStatus release() {
        throw refused("liberar");
    }

    default LossReportStatus complete() {
        throw refused("concluir");
    }

    /** O registro completou 30 dias sem renovação (RN14). */
    default LossReportStatus expire() {
        throw refused("vencer");
    }

    default LossReportStatus cancel() {
        throw refused("cancelar");
    }

    default boolean isFinal() {
        return switch (this) {
            case Completed _, Expired _, Cancelled _ -> true;
            case Open _, AwaitingInfo _, Summoned _ -> false;
        };
    }

    /** Depois da convocação, descrição e detalhe não mudam mais (RN06). */
    default boolean isEditable() {
        return switch (this) {
            case Open _, AwaitingInfo _ -> true;
            case Summoned _, Completed _, Expired _, Cancelled _ -> false;
        };
    }

    default String code() {
        return switch (this) {
            case Open _ -> "OPEN";
            case AwaitingInfo _ -> "AWAITING_INFO";
            case Summoned _ -> "SUMMONED";
            case Completed _ -> "COMPLETED";
            case Expired _ -> "EXPIRED";
            case Cancelled _ -> "CANCELLED";
        };
    }

    default String label() {
        return switch (this) {
            case Open _ -> "Aberto";
            case AwaitingInfo _ -> "Aguardando informações";
            case Summoned _ -> "Convocado";
            case Completed _ -> "Concluído";
            case Expired _ -> "Vencido";
            case Cancelled _ -> "Cancelado";
        };
    }

    private InvalidStatusTransitionException refused(String action) {
        return new InvalidStatusTransitionException(action, "um registro de perda", label());
    }

    /** Esperando o cruzamento ou a decisão do funcionário. */
    record Open() implements LossReportStatus {

        @Override
        public LossReportStatus requestInfo() {
            return AWAITING_INFO;
        }

        @Override
        public LossReportStatus summon() {
            return SUMMONED;
        }

        @Override
        public LossReportStatus expire() {
            return EXPIRED;
        }

        @Override
        public LossReportStatus cancel() {
            return CANCELLED;
        }
    }

    /** O funcionário pediu mais informações e espera a resposta do estudante. */
    record AwaitingInfo() implements LossReportStatus {

        @Override
        public LossReportStatus resume() {
            return OPEN;
        }

        @Override
        public LossReportStatus cancel() {
            return CANCELLED;
        }
    }

    /** O estudante foi chamado para retirar um objeto. */
    record Summoned() implements LossReportStatus {

        @Override
        public LossReportStatus release() {
            return OPEN;
        }

        @Override
        public LossReportStatus complete() {
            return COMPLETED;
        }
    }

    /** O objeto foi retirado pelo dono. Final. */
    record Completed() implements LossReportStatus {
    }

    /** Passou de 30 dias sem renovação. Final. */
    record Expired() implements LossReportStatus {
    }

    /** O estudante cancelou. Final. */
    record Cancelled() implements LossReportStatus {
    }
}
