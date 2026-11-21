package br.ucsal.cade.shared.event;

/**
 * Publica um evento para quem quiser ouvir, sem conhecer os ouvintes (Observer, D05). Os módulos dependem desta
 * interface, e não do mecanismo: hoje são os eventos do Spring ({@link SpringEventPublisher}); o RabbitMQ, se entrar,
 * fica atrás dela sem mudar quem publica (seção 7.2 dos requisitos).
 *
 * <p>Publique dentro da transação da ação: os ouvintes com {@code @TransactionalEventListener(AFTER_COMMIT)} só
 * recebem o evento se ela for confirmada, e o histórico ({@code AuditEvent}) exige a transação aberta.
 */
@FunctionalInterface
public interface EventPublisher {

    void publish(Object event);
}
