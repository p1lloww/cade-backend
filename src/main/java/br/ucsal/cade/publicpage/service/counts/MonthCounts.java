package br.ucsal.cade.publicpage.service.counts;

import java.time.YearMonth;
import java.util.List;

/**
 * Os objetos disponíveis achados num mês, por categoria.
 *
 * @param categories da categoria com mais objetos para a com menos; no empate, por nome
 */
public record MonthCounts(YearMonth month, long total, List<CategoryCount> categories) {

    public record CategoryCount(String category, long count) {
    }
}
