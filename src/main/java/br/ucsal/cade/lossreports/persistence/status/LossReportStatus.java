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
 *   <tr><td>{@link #refuse()} (recusar)</td><td>Convocado</td><td>Recusado</td></tr>
 *   <tr><td>{@link #complete()} (concluir)</td><td>Convocado</td><td>Concluído</td></tr>
 *   <tr><td>{@link #expire()} (30 dias)</td><td>Aberto</td><td>Vencido</td></tr>
 *   <tr><td>{@link #cancel()} (cancelar)</td><td>Aberto, Aguardando informações</td><td>Cancelado</td></tr>
 * </table>
 *
 * <p>Concluído, Vencido, Cancelado e Recusado são finais. Recusado não está na seção 6.2: a verificação falhou no
 * balcão, e o registro se encerra em vez de voltar a Aberto, para que a descrição nunca mais mude depois de uma
 * convocação (RN06, D26).
 */
public sealed interface LossReportStatus {

    Open OPEN = new Open();
    AwaitingInfo AWAITING_INFO = new AwaitingInfo();
    Summoned SUMMONED = new Summoned();
    Completed COMPLETED = new Completed();
    Expired EXPIRED = new Expired();
    Cancelled CANCELLED = new Cancelled();
    Refused REFUSED = new Refused();

    List<LossReportStatus> ALL = List.of(OPEN, AWAITING_INFO, SUMMONED, COMPLETED, EXPIRED, CANCELLED,
            REFUSED);

    List<LossReportStatus> ACTIVE = ALL.stream().filter(status -> !status.isFinal()).toList();

    /**
     * Situações em que o registro entra no cruzamento (RF20, D23): o estudante ainda procura o objeto e ninguém foi
     * convocado por ele.
     */
    List<LossReportStatus> MATCHABLE = List.of(OPEN, AWAITING_INFO);

    static LossReportStatus initial() {
        return OPEN;
    }

    static LossReportStatus fromCode(String code) {
        return ALL.stream()
                .filter(status -> status.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Situação de registro desconhecida: " + code));
    }

    default LossReportStatus requestInfo() {
        throw refused("pedir informações sobre");
    }

    /** Volta para Aberto: o estudante respondeu ao pedido, ou passaram 5 dias sem resposta (RN13). */
    default LossReportStatus resume() {
        throw refused("retomar");
    }

    default LossReportStatus summon() {
        throw refused("convocar");
    }

    /** Volta para Aberto: a convocação venceu (RN15). */
    default LossReportStatus release() {
        throw refused("liberar");
    }

    /** A verificação falhou no balcão (RF31): o registro se encerra (D26). */
    default LossReportStatus refuse() {
        throw refused("recusar");
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
            case Completed _, Expired _, Cancelled _, Refused _ -> true;
            case Open _, AwaitingInfo _, Summoned _ -> false;
        };
    }

    /** Depois da convocação, descrição e detalhe não mudam mais (RN06). */
    default boolean isEditable() {
        return switch (this) {
            case Open _, AwaitingInfo _ -> true;
            case Summoned _, Completed _, Expired _, Cancelled _, Refused _ -> false;
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
            case Refused _ -> "REFUSED";
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
            case Refused _ -> "Recusado";
        };
    }

    /**
     * A situação explicada ao estudante em linguagem simples, para a lista "meus registros" (RF16). Não fala de
     * nenhum objeto achado (RN03): no Convocado, onde e até quando retirar vêm da convocação, no módulo de retirada.
     */
    default String description() {
        return switch (this) {
            case Open _ -> "Estamos de olho: quando chegar à sala um objeto parecido, o funcionário confere e você é "
                    + "avisado.";
            case AwaitingInfo _ -> "O funcionário precisa de mais detalhes sobre a sua perda. Responda para o registro "
                    + "continuar.";
            case Summoned _ -> "Encontramos um objeto que pode ser seu. Veja onde e até quando retirar.";
            case Completed _ -> "Você retirou o objeto. Este registro está encerrado.";
            case Expired _ -> "O registro passou de 30 dias sem renovação e foi encerrado. Se ainda procura, registre "
                    + "de novo.";
            case Cancelled _ -> "Você cancelou este registro.";
            case Refused _ -> "A conferência no balcão não confirmou que o objeto era seu. O registro foi encerrado.";
        };
    }

    private InvalidStatusTransitionException refused(String action) {
        return new InvalidStatusTransitionException(action, "um registro de perda", label());
    }

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

    record Summoned() implements LossReportStatus {

        @Override
        public LossReportStatus release() {
            return OPEN;
        }

        @Override
        public LossReportStatus refuse() {
            return REFUSED;
        }

        @Override
        public LossReportStatus complete() {
            return COMPLETED;
        }
    }

    record Completed() implements LossReportStatus {
    }

    record Expired() implements LossReportStatus {
    }

    record Cancelled() implements LossReportStatus {
    }

    record Refused() implements LossReportStatus {
    }
}
