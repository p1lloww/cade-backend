package br.ucsal.cade.founditems.service.search;

import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import br.ucsal.cade.shared.category.Category;

import java.time.YearMonth;
import java.util.List;

/**
 * O que existe para escolher na busca: só categorias, lugares e meses que têm algum objeto, para todo filtro escolhido
 * poder dar resultado.
 *
 * @param months do mais recente para o mais antigo
 */
public record SearchOptions(List<Category> categories, List<BlockRooms> places, List<YearMonth> months,
                            List<FoundItemStatus> statuses) {

    /** Um bloco e as salas dele onde algum objeto foi achado. */
    public record BlockRooms(String block, List<String> rooms) {
    }
}
