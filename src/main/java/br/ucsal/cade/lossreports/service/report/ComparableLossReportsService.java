package br.ucsal.cade.lossreports.service.report;

import br.ucsal.cade.lossreports.api.ComparableLossReport;
import br.ucsal.cade.lossreports.api.ComparableLossReports;
import br.ucsal.cade.lossreports.api.MatchableLossReport;
import br.ucsal.cade.lossreports.persistence.report.LossReport;
import br.ucsal.cade.lossreports.persistence.report.LossReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
class ComparableLossReportsService implements ComparableLossReports {

    private final LossReportRepository reports;

    ComparableLossReportsService(LossReportRepository reports) {
        this.reports = reports;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, ComparableLossReport> findAll(Collection<UUID> lossReportIds) {
        if (lossReportIds.isEmpty()) {
            return Map.of();
        }
        return reports.findWithCategoryAndLocationsByIdIn(lossReportIds).stream()
                .map(ComparableLossReportsService::toComparable)
                .collect(Collectors.toMap(ComparableLossReport::id, Function.identity()));
    }

    private static ComparableLossReport toComparable(LossReport report) {
        List<MatchableLossReport.Location> locations = report.getLocations().stream()
                .map(location -> new MatchableLossReport.Location(location.block(), location.room()))
                .toList();
        String shift = report.getPeriod().shift() == null ? null : report.getPeriod().shift().label();
        return new ComparableLossReport(report.getId(), report.getCategory().getName(),
                report.getPeriod().lostFrom(), report.getPeriod().lostUntil(), shift, locations,
                report.getDescription(), report.getSecretDetail(), report.getStatus().label(),
                report.getRegisteredAt(), report.getProofSerialNumber(), report.hasProofPhoto());
    }
}
