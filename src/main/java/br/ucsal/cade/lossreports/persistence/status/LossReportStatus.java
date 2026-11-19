package br.ucsal.cade.lossreports.persistence.status;

import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;

import java.util.List;

/**
 * Situação de um registro de perda, com as transições da seção 6.2 dos requisitos. Padrão State: cada situação é um
 * record que sabe para onde pode ir, e toda ação que ela não permite é recusada com
 * {@link InvalidStatusTransitionException} (RN01).
 *
 * <p>A interface é {@code sealed}: as seis situações são as únicas possíveis. Por isso os {@code switch} sobre uma
 * situação não têm {@code default}, e uma situação nova faz o compilador apontar cada lugar que precisa tratá-la.
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
 * <p>Concluído, Vencido e Cancelado são finais: não permitem nenhuma ação. A renovação do prazo (RN14) não muda a
 * situação, por isso não aparece aqui.
 *
 * <p>As situações não guardam dados, então cada record tem uma única instância útil, nas constantes abaixo; como são
 * records, duas instâncias da mesma situação são sempre iguais.
 */
public sealed interface LossReportStatus {

    Open OPEN = new Open();
    AwaitingInfo AWAITING_INFO = new AwaitingInfo();
    Summoned SUMMONED = new Summoned();
    Completed COMPLETED = new Completed();
    Expired EXPIRED = new Expired();
    Cancelled CANCELLED = new Cancelled();

    /** Todas as situações, na ordem do ciclo de vida. */
    List<LossReportStatus> ALL = List.of(OPEN, AWAITING_INFO, SUMMONED, COMPLETED, EXPIRED, CANCELLED);

    /** As situações que ainda não terminaram; são as que contam para o limite de 5 registros (RN05). */
    List<LossReportStatus> ACTIVE = ALL.stream().filter(status -> !status.isFinal()).toList();

    /** Situação de todo registro recém-criado pelo estudante. */
    static LossReportStatus initial() {
        return OPEN;
    }

    /** A situação gravada no banco com este código (ver {@link #code()}). */
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

    /** A retirada do objeto foi registrada. */
    default LossReportStatus complete() {
        throw refused("concluir");
    }

    /** O registro completou 30 dias sem renovação (RN14). */
    default LossReportStatus expire() {
        throw refused("vencer");
    }

    /** O estudante cancela o registro ("já achei"). */
    default LossReportStatus cancel() {
        throw refused("cancelar");
    }

    /** Se a situação encerra o ciclo de vida do registro. */
    default boolean isFinal() {
        return switch (this) {
            case Completed _, Expired _, Cancelled _ -> true;
            case Open _, AwaitingInfo _, Summoned _ -> false;
        };
    }

    /**
     * Se o estudante ainda pode alterar a descrição e o detalhe. Depois da convocação, não pode mais, para que ninguém
     * ajuste o texto ao objeto (RN06).
     */
    default boolean isEditable() {
        return switch (this) {
            case Open _, AwaitingInfo _ -> true;
            case Summoned _, Completed _, Expired _, Cancelled _ -> false;
        };
    }

    /** Código gravado no banco; nunca muda, mesmo que o nome em português mude. */
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

    /** Nome da situação para o usuário, como nos requisitos. */
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
