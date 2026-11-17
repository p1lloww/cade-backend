package br.ucsal.cade.founditems.persistence.status;

import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;

import java.util.List;

/**
 * Situação de um objeto achado (RF12), com as transições da seção 6.1 dos requisitos. Padrão State: cada situação é
 * um record que sabe para onde pode ir, e toda ação que ela não permite é recusada com
 * {@link InvalidStatusTransitionException} (RN01).
 *
 * <p>A interface é {@code sealed}: as cinco situações são as únicas possíveis. Por isso os {@code switch} sobre uma
 * situação não têm {@code default}, e uma situação nova faz o compilador apontar cada lugar que precisa tratá-la.
 *
 * <table>
 *   <caption>Transições permitidas</caption>
 *   <tr><th>Ação</th><th>De</th><th>Para</th></tr>
 *   <tr><td>{@link #reserve()} (convocar)</td><td>Disponível, Não reclamado</td><td>Reservado</td></tr>
 *   <tr><td>{@link #release()} (liberar)</td><td>Reservado</td><td>Disponível</td></tr>
 *   <tr><td>{@link #handOver()} (entregar)</td><td>Disponível, Reservado, Não reclamado</td><td>Devolvido</td></tr>
 *   <tr><td>{@link #markUnclaimed()} (90 dias)</td><td>Disponível</td><td>Não reclamado</td></tr>
 *   <tr><td>{@link #dispose()} (destinar)</td><td>Não reclamado</td><td>Destinado</td></tr>
 * </table>
 *
 * <p>Devolvido e Destinado são finais: não permitem nenhuma ação.
 *
 * <p>As situações não guardam dados, então cada record tem uma única instância útil, nas constantes abaixo; como são
 * records, duas instâncias da mesma situação são sempre iguais.
 */
public sealed interface FoundItemStatus {

    Available AVAILABLE = new Available();
    Reserved RESERVED = new Reserved();
    Returned RETURNED = new Returned();
    Unclaimed UNCLAIMED = new Unclaimed();
    Disposed DISPOSED = new Disposed();

    /** Todas as situações, na ordem do ciclo de vida. */
    List<FoundItemStatus> ALL = List.of(AVAILABLE, RESERVED, RETURNED, UNCLAIMED, DISPOSED);

    /** Situação de todo objeto recém-cadastrado pelo funcionário. */
    static FoundItemStatus initial() {
        return AVAILABLE;
    }

    /** A situação gravada no banco com este código (ver {@link #code()}). */
    static FoundItemStatus fromCode(String code) {
        return ALL.stream()
                .filter(status -> status.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Situação de objeto desconhecida: " + code));
    }

    /** Convocar um estudante para retirar o objeto. */
    default FoundItemStatus reserve() {
        throw refused("convocar");
    }

    /**
     * Devolver o objeto à lista de disponíveis: a convocação venceu (7 dias) ou a verificação falhou no balcão.
     *
     * <p>Um objeto que estava Não reclamado antes de ser convocado também volta para Disponível, porque a seção 6.1
     * não prevê Reservado → Não reclamado. A rotina dos 90 dias (PRA-01) o marca de novo como Não reclamado.
     */
    default FoundItemStatus release() {
        throw refused("liberar");
    }

    /** Registrar a retirada do objeto pelo dono, com ou sem registro de perda (RF32). */
    default FoundItemStatus handOver() {
        throw refused("entregar");
    }

    /** Marcar o objeto que completou 90 dias desde a entrada. */
    default FoundItemStatus markUnclaimed() {
        throw refused("marcar como não reclamado");
    }

    /** Registrar o destino dado pelo administrador a um objeto não reclamado. */
    default FoundItemStatus dispose() {
        throw refused("destinar");
    }

    /** Se a situação encerra o ciclo de vida do objeto. */
    default boolean isFinal() {
        return switch (this) {
            case Returned _, Disposed _ -> true;
            case Available _, Reserved _, Unclaimed _ -> false;
        };
    }

    /** Código gravado no banco; nunca muda, mesmo que o nome em português mude. */
    default String code() {
        return switch (this) {
            case Available _ -> "AVAILABLE";
            case Reserved _ -> "RESERVED";
            case Returned _ -> "RETURNED";
            case Unclaimed _ -> "UNCLAIMED";
            case Disposed _ -> "DISPOSED";
        };
    }

    /** Nome da situação para o usuário, como nos requisitos. */
    default String label() {
        return switch (this) {
            case Available _ -> "Disponível";
            case Reserved _ -> "Reservado";
            case Returned _ -> "Devolvido";
            case Unclaimed _ -> "Não reclamado";
            case Disposed _ -> "Destinado";
        };
    }

    private InvalidStatusTransitionException refused(String action) {
        return new InvalidStatusTransitionException(action, "um objeto", label());
    }

    /** No balcão ou na lista, esperando o dono. */
    record Available() implements FoundItemStatus {

        @Override
        public FoundItemStatus reserve() {
            return RESERVED;
        }

        @Override
        public FoundItemStatus handOver() {
            return RETURNED;
        }

        @Override
        public FoundItemStatus markUnclaimed() {
            return UNCLAIMED;
        }
    }

    /** Separado para um estudante convocado. */
    record Reserved() implements FoundItemStatus {

        @Override
        public FoundItemStatus release() {
            return AVAILABLE;
        }

        @Override
        public FoundItemStatus handOver() {
            return RETURNED;
        }
    }

    /** Entregue ao dono. Final. */
    record Returned() implements FoundItemStatus {
    }

    /** Passou de 90 dias sem dono; ainda pode ser entregue ou receber um destino. */
    record Unclaimed() implements FoundItemStatus {

        @Override
        public FoundItemStatus reserve() {
            return RESERVED;
        }

        @Override
        public FoundItemStatus handOver() {
            return RETURNED;
        }

        @Override
        public FoundItemStatus dispose() {
            return DISPOSED;
        }
    }

    /** Recebeu um destino do administrador (doação, descarte). Final. */
    record Disposed() implements FoundItemStatus {
    }
}
