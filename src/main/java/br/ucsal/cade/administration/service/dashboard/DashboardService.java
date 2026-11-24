package br.ucsal.cade.administration.service.dashboard;

import br.ucsal.cade.administration.persistence.audit.AuditRecord;
import br.ucsal.cade.administration.persistence.audit.AuditRecordRepository;
import br.ucsal.cade.founditems.api.count.FoundItemOverview;
import br.ucsal.cade.founditems.api.count.StatusCategoryCount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Monta as contagens do painel (RF46) com Collections e Streams. As situações vêm do módulo de achados; as devoluções
 * e o tempo até elas vêm do histórico (RF43), que já guarda a hora de cada uma e é do próprio módulo: o objeto não
 * precisa de uma coluna só para isso.
 */
@Service
public class DashboardService {

    /** Devolução pela convocação e pelo atalho de balcão (as ações gravadas pelo módulo de achados). */
    static final Set<String> RETURN_ACTIONS = Set.of("FOUND_ITEM_HANDED_OVER", "FOUND_ITEM_HANDED_OVER_AT_COUNTER");
    static final String REGISTERED_ACTION = "FOUND_ITEM_REGISTERED";
    static final String UNCLAIMED_CODE = "UNCLAIMED";

    private static final double HOURS_PER_DAY = 24.0;

    private final FoundItemOverview items;
    private final AuditRecordRepository audit;
    private final Clock clock;

    DashboardService(FoundItemOverview items, AuditRecordRepository audit, Clock clock) {
        this.items = items;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Dashboard overview() {
        List<StatusCategoryCount> counts = items.countByStatusAndCategory();

        Map<String, List<StatusCategoryCount>> byStatusCode = counts.stream()
                .collect(Collectors.groupingBy(StatusCategoryCount::statusCode));
        List<Dashboard.StatusTotal> byStatus = byStatusCode.values().stream()
                .map(rows -> new Dashboard.StatusTotal(rows.getFirst().statusCode(), rows.getFirst().statusLabel(),
                        rows.stream().mapToLong(StatusCategoryCount::total).sum()))
                .sorted((a, b) -> a.statusLabel().compareTo(b.statusLabel()))
                .toList();
        long unclaimed = byStatusCode.getOrDefault(UNCLAIMED_CODE, List.of()).stream()
                .mapToLong(StatusCategoryCount::total).sum();

        List<AuditRecord> returns = audit.findByActionIn(RETURN_ACTIONS);
        YearMonth thisMonth = YearMonth.now(clock);
        long returnsThisMonth = returns.stream()
                .filter(record -> YearMonth.from(record.getOccurredAt().atZone(clock.getZone())).equals(thisMonth))
                .count();

        return new Dashboard(counts, byStatus, unclaimed, returnsThisMonth, averageDaysToReturn(returns),
                clock.instant());
    }

    /** Média, em dias, do registro do objeto até a devolução; só entram devoluções com o registro no histórico. */
    private Double averageDaysToReturn(List<AuditRecord> returns) {
        Set<UUID> returnedItems = returns.stream().map(AuditRecord::getEntityId).collect(Collectors.toSet());
        if (returnedItems.isEmpty()) {
            return null;
        }
        Map<UUID, AuditRecord> registrations = audit.findByEntityIdInAndAction(returnedItems, REGISTERED_ACTION)
                .stream().collect(Collectors.toMap(AuditRecord::getEntityId, Function.identity(), (a, b) -> a));
        OptionalDouble average = returns.stream()
                .filter(record -> registrations.containsKey(record.getEntityId()))
                .mapToDouble(record -> Duration.between(registrations.get(record.getEntityId()).getOccurredAt(),
                        record.getOccurredAt()).toHours() / HOURS_PER_DAY)
                .average();
        return average.isPresent() ? Math.round(average.getAsDouble() * 10) / 10.0 : null;
    }
}
