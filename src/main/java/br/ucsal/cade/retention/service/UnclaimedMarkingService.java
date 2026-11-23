package br.ucsal.cade.retention.service;

import br.ucsal.cade.founditems.api.UnclaimedFoundItems;
import br.ucsal.cade.shared.settings.Deadlines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Marca como não reclamado o objeto que passou do prazo de guarda (RF37, RN16), o que o administrador configurou
 * (RF45). Cada objeto numa transação própria
 * (que fica no módulo de achados): uma falha num deles não desfaz nem impede os outros, e a próxima rodada tenta de
 * novo.
 */
@Service
public class UnclaimedMarkingService {

    private static final Logger log = LoggerFactory.getLogger(UnclaimedMarkingService.class);

    private final UnclaimedFoundItems unclaimed;
    private final Deadlines deadlines;
    private final Clock clock;

    UnclaimedMarkingService(UnclaimedFoundItems unclaimed, Deadlines deadlines, Clock clock) {
        this.unclaimed = unclaimed;
        this.deadlines = deadlines;
        this.clock = clock;
    }

    /** @return quantos objetos viraram não reclamados nesta rodada */
    public int markDueItems() {
        Instant cutoff = clock.instant().minus(deadlines.storagePeriod());
        List<UUID> due = unclaimed.idsDueForUnclaimed(cutoff);
        int marked = 0;
        for (UUID id : due) {
            try {
                if (unclaimed.markUnclaimed(id)) {
                    marked++;
                }
            } catch (RuntimeException e) {
                log.warn("Could not mark found item {} as unclaimed; the next run tries again", id, e);
            }
        }
        if (!due.isEmpty()) {
            log.info("Unclaimed run: {} due, {} marked", due.size(), marked);
        }
        return marked;
    }
}
