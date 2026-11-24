package br.ucsal.cade.retention.service.unclaimed;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Todo dia procura os objetos que passaram do prazo de guarda (RF37). O intervalo vem da configuração. */
@Component
class UnclaimedJob {

    private final UnclaimedMarkingService marking;

    UnclaimedJob(UnclaimedMarkingService marking) {
        this.marking = marking;
    }

    @Scheduled(fixedDelayString = "${cade.retention.interval}", initialDelayString = "${cade.retention.interval}")
    void markUnclaimedItems() {
        marking.markDueItems();
    }
}
