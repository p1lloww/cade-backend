package br.ucsal.cade.lossreports.service.port;

import br.ucsal.cade.lossreports.api.deadline.ExpiringLossReport;
import br.ucsal.cade.lossreports.api.deadline.LossReportDeadlines;
import br.ucsal.cade.lossreports.persistence.report.LossReportRepository;
import br.ucsal.cade.lossreports.persistence.status.LossReportStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
class LossReportDeadlinesService implements LossReportDeadlines {

    private final LossReportRepository reports;

    LossReportDeadlinesService(LossReportRepository reports) {
        this.reports = reports;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpiringLossReport> expiringBetween(Instant from, Instant to) {
        return reports.findByStatusInAndExpiresAtGreaterThanAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
                        LossReportStatus.MATCHABLE, from, to).stream()
                .map(report -> new ExpiringLossReport(report.getId(), report.getStudentId(), report.getDescription(),
                        report.getExpiresAt()))
                .toList();
    }
}
