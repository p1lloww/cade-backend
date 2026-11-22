package br.ucsal.cade.founditems.service.search;

import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Os filtros da busca de objetos (RF10). Só o texto é digitado; os outros vêm das opções da busca, então não há o que
 * validar além do tipo. Filtro nulo não filtra, e sem situação valem todas.
 *
 * @param text  o que o funcionário digitou; vazio conta como sem texto
 * @param month o mês em que o objeto foi achado
 */
public record FoundItemSearch(String text, UUID categoryId, YearMonth month, String block, String room,
                              List<FoundItemStatus> statuses) {

    public FoundItemSearch {
        text = text == null || text.isBlank() ? null : text.strip();
        statuses = statuses == null || statuses.isEmpty() ? FoundItemStatus.ALL : List.copyOf(statuses);
    }

    public boolean hasText() {
        return text != null;
    }
}
