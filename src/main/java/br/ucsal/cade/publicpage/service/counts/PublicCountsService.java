package br.ucsal.cade.publicpage.service.counts;

import br.ucsal.cade.founditems.api.count.AvailableItemCounts;
import br.ucsal.cade.founditems.api.count.CategoryMonthCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Contagens da página pública (RF41): quantos objetos disponíveis há por mês e categoria, para quem perdeu algo saber
 * que vale ir à sala, sem ver nada que permita "pescar" um objeto (S5, RN03).
 */
@Service
public class PublicCountsService {

    private static final Comparator<MonthCounts.CategoryCount> MOST_FIRST =
            Comparator.comparingLong(MonthCounts.CategoryCount::count).reversed()
                    .thenComparing(MonthCounts.CategoryCount::category);

    private static final Logger log = LoggerFactory.getLogger(PublicCountsService.class);

    private final AvailableItemCounts counts;

    PublicCountsService(AvailableItemCounts counts) {
        this.counts = counts;
    }

    /** Do mês mais recente para o mais antigo. */
    public List<MonthCounts> byMonth() {
        Map<YearMonth, List<CategoryMonthCount>> byMonth = counts.byCategoryAndMonth().stream()
                .collect(Collectors.groupingBy(CategoryMonthCount::month,
                        () -> new TreeMap<>(Comparator.reverseOrder()), Collectors.toList()));
        List<MonthCounts> months = byMonth.entrySet().stream()
                .map(entry -> toMonth(entry.getKey(), entry.getValue()))
                .toList();
        log.debug("Public counts: {} month(s)", months.size());
        return months;
    }

    private static MonthCounts toMonth(YearMonth month, List<CategoryMonthCount> rows) {
        List<MonthCounts.CategoryCount> categories = rows.stream()
                .map(row -> new MonthCounts.CategoryCount(row.category(), row.count()))
                .sorted(MOST_FIRST)
                .toList();
        long total = categories.stream().mapToLong(MonthCounts.CategoryCount::count).sum();
        return new MonthCounts(month, total, categories);
    }
}
