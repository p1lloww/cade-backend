package br.ucsal.cade.retention.service;

import br.ucsal.cade.founditems.api.UnclaimedFoundItem;
import br.ucsal.cade.founditems.api.UnclaimedFoundItems;
import br.ucsal.cade.retention.persistence.disposal.Disposal;
import br.ucsal.cade.retention.persistence.disposal.DisposalDestination;
import br.ucsal.cade.retention.persistence.disposal.DisposalRepository;
import br.ucsal.cade.shared.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A lista dos não reclamados por caixa (RF39) e o destino dado pelo administrador (RF38). */
@Service
public class DisposalService {

    private final UnclaimedFoundItems unclaimed;
    private final DisposalRepository disposals;
    private final Clock clock;

    DisposalService(UnclaimedFoundItems unclaimed, DisposalRepository disposals, Clock clock) {
        this.unclaimed = unclaimed;
        this.disposals = disposals;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<UnclaimedItemsByBox> listByBox() {
        Map<String, List<UnclaimedFoundItem>> byBox = new LinkedHashMap<>();
        for (UnclaimedFoundItem item : unclaimed.listUnclaimed()) {
            byBox.computeIfAbsent(item.storageBox(), box -> new ArrayList<>()).add(item);
        }
        return byBox.entrySet().stream().map(e -> new UnclaimedItemsByBox(e.getKey(), e.getValue())).toList();
    }

    /**
     * Destina um não reclamado: a situação muda e o destino é gravado na mesma transação, com a data e o
     * administrador. Sem destino informado, vale o bazar; "outro" exige dizer qual.
     */
    @Transactional
    public Disposal dispose(UUID foundItemId, DisposalDestination destination, String note, UUID adminId) {
        DisposalDestination chosen = destination == null ? DisposalDestination.BAZAAR : destination;
        String cleanNote = note == null || note.isBlank() ? null : note.strip();
        if (chosen == DisposalDestination.OTHER && cleanNote == null) {
            throw new BadRequestException("Para o destino \"Outro\", diga qual é.");
        }
        String reason = "Destino: " + chosen.label() + (cleanNote == null ? "." : " (" + cleanNote + ").");
        unclaimed.dispose(foundItemId, adminId, reason);
        return disposals.save(new Disposal(foundItemId, chosen, cleanNote, clock.instant(), adminId));
    }
}
