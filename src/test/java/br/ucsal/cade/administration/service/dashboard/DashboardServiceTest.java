package br.ucsal.cade.administration.service.dashboard;

import br.ucsal.cade.administration.persistence.audit.AuditRecord;
import br.ucsal.cade.administration.persistence.audit.AuditRecordRepository;
import br.ucsal.cade.founditems.api.count.FoundItemOverview;
import br.ucsal.cade.founditems.api.count.StatusCategoryCount;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.time.TimeConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** As contagens do painel (RF46): situação e categoria, não reclamados, devoluções no mês e tempo médio. */
class DashboardServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T15:00:00Z");

    private final AuditRecordRepository audit = mock(AuditRecordRepository.class);

    @Test
    void countsObjectsByStatusAndCategoryAndTotalsEachStatus() {
        FoundItemOverview items = () -> List.of(
                new StatusCategoryCount("AVAILABLE", "Disponível", "Fone", 3),
                new StatusCategoryCount("AVAILABLE", "Disponível", "Garrafa", 2),
                new StatusCategoryCount("UNCLAIMED", "Não reclamado", "Fone", 4),
                new StatusCategoryCount("RETURNED", "Devolvido", "Fone", 1));

        Dashboard dashboard = service(items).overview();

        assertThat(dashboard.byStatusAndCategory()).hasSize(4);
        assertThat(dashboard.byStatus()).extracting(Dashboard.StatusTotal::statusCode, Dashboard.StatusTotal::total)
                .containsExactlyInAnyOrder(org.assertj.core.groups.Tuple.tuple("AVAILABLE", 5L),
                        org.assertj.core.groups.Tuple.tuple("UNCLAIMED", 4L),
                        org.assertj.core.groups.Tuple.tuple("RETURNED", 1L));
        assertThat(dashboard.unclaimed()).isEqualTo(4);
    }

    @Test
    void returnsOfTheCurrentMonthAreCountedAndTheAverageTimeComesFromTheHistory() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID lastMonth = UUID.randomUUID();
        List<AuditRecord> returns = List.of(
                record("FOUND_ITEM_HANDED_OVER", first, "2026-11-14T12:00:00Z"),
                record("FOUND_ITEM_HANDED_OVER_AT_COUNTER", second, "2026-11-12T12:00:00Z"),
                record("FOUND_ITEM_HANDED_OVER", lastMonth, "2026-10-30T12:00:00Z"));
        when(audit.findByActionIn(any())).thenReturn(returns);
        when(audit.findByEntityIdInAndAction(any(Collection.class), eq("FOUND_ITEM_REGISTERED"))).thenReturn(List.of(
                record("FOUND_ITEM_REGISTERED", first, "2026-11-10T12:00:00Z"),
                record("FOUND_ITEM_REGISTERED", second, "2026-11-10T12:00:00Z"),
                record("FOUND_ITEM_REGISTERED", lastMonth, "2026-10-28T12:00:00Z")));

        Dashboard dashboard = service(List::of).overview();

        assertThat(dashboard.returnsThisMonth()).isEqualTo(2);
        // 4 dias, 2 dias e 2 dias: média de 2,7.
        assertThat(dashboard.averageDaysToReturn()).isEqualTo(2.7);
    }

    @Test
    void withoutAnyReturnTheAverageIsNullAndNothingBreaks() {
        when(audit.findByActionIn(any())).thenReturn(List.of());

        Dashboard dashboard = service(List::of).overview();

        assertThat(dashboard.returnsThisMonth()).isZero();
        assertThat(dashboard.averageDaysToReturn()).isNull();
        assertThat(dashboard.unclaimed()).isZero();
        assertThat(dashboard.byStatus()).isEmpty();
    }

    private DashboardService service(FoundItemOverview items) {
        return new DashboardService(items, audit, Clock.fixed(NOW, TimeConfiguration.ZONE));
    }

    private static AuditRecord record(String action, UUID entity, String at) {
        return AuditRecord.of(new AuditEvent(UUID.randomUUID(), action, "FoundItem", entity, "teste"),
                Instant.parse(at));
    }
}
