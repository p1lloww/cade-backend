package br.ucsal.cade.publicpage.service.counts;

import br.ucsal.cade.founditems.api.count.CategoryMonthCount;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PublicCountsServiceTest {

    private static final YearMonth OCTOBER = YearMonth.of(2026, 10);
    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    @Test
    void groupsByMonthFromTheMostRecentWithTheTotal() {
        PublicCountsService service = new PublicCountsService(() -> List.of(
                new CategoryMonthCount("Fone", OCTOBER, 5),
                new CategoryMonthCount("Garrafa", NOVEMBER, 12),
                new CategoryMonthCount("Garrafa", OCTOBER, 2),
                new CategoryMonthCount("Fone", NOVEMBER, 5)));

        List<MonthCounts> months = service.byMonth();

        assertThat(months).extracting(MonthCounts::month).containsExactly(NOVEMBER, OCTOBER);
        assertThat(months).extracting(MonthCounts::total).containsExactly(17L, 7L);
    }

    @Test
    void inEachMonthTheCategoryWithMoreItemsComesFirstAndTiesGoByName() {
        PublicCountsService service = new PublicCountsService(() -> List.of(
                new CategoryMonthCount("Guarda-chuva", NOVEMBER, 3),
                new CategoryMonthCount("Fone", NOVEMBER, 3),
                new CategoryMonthCount("Garrafa", NOVEMBER, 12)));

        assertThat(service.byMonth().getFirst().categories()).containsExactly(
                new MonthCounts.CategoryCount("Garrafa", 12),
                new MonthCounts.CategoryCount("Fone", 3),
                new MonthCounts.CategoryCount("Guarda-chuva", 3));
    }

    @Test
    void withoutItemsThereAreNoMonths() {
        assertThat(new PublicCountsService(List::of).byMonth()).isEmpty();
    }
}
