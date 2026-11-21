package br.ucsal.cade.shared.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Entrega o evento aos ouvintes do Spring ({@code @EventListener}, {@code @TransactionalEventListener}). */
@Component
class SpringEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher publisher;

    SpringEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(Object event) {
        publisher.publishEvent(event);
    }
}
