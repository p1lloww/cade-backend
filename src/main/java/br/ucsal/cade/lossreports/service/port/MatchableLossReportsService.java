package br.ucsal.cade.lossreports.service.port;

import br.ucsal.cade.lossreports.api.candidate.MatchableLossReport;
import br.ucsal.cade.lossreports.api.candidate.MatchableLossReports;
import br.ucsal.cade.lossreports.persistence.report.LossReport;
import br.ucsal.cade.lossreports.persistence.report.LossReportRepository;
import br.ucsal.cade.lossreports.persistence.status.LossReportStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class MatchableLossReportsService implements MatchableLossReports {

    private final LossReportRepository reports;

    MatchableLossReportsService(LossReportRepository reports) {
        this.reports = reports;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MatchableLossReport> find(UUID lossReportId) {
        return reports.findWithLocationsByIdAndStatusIn(lossReportId, LossReportStatus.MATCHABLE)
                .map(MatchableLossReportsService::toMatchable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchableLossReport> findByCategory(UUID categoryId) {
        return reports.findWithLocationsByCategoryIdAndStatusIn(categoryId, LossReportStatus.MATCHABLE).stream()
                .map(MatchableLossReportsService::toMatchable)
                .toList();
    }

    private static MatchableLossReport toMatchable(LossReport report) {
        List<MatchableLossReport.Location> locations = report.getLocations().stream()
                .map(location -> new MatchableLossReport.Location(location.block(), location.room()))
                .toList();
        return new MatchableLossReport(report.getId(), report.getCategory().getId(), report.getPeriod().lostFrom(),
                report.getPeriod().lostUntil(), locations, report.getDescription());
    }
}
