package br.ucsal.cade.lossreports.service.report;

import br.ucsal.cade.lossreports.api.LossReportOwners;
import br.ucsal.cade.lossreports.persistence.report.LossReport;
import br.ucsal.cade.lossreports.persistence.report.LossReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
class LossReportOwnersService implements LossReportOwners {

    private final LossReportRepository reports;

    LossReportOwnersService(LossReportRepository reports) {
        this.reports = reports;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, UUID> studentsOf(Collection<UUID> lossReportIds) {
        if (lossReportIds.isEmpty()) {
            return Map.of();
        }
        return reports.findAllById(lossReportIds).stream()
                .collect(Collectors.toMap(LossReport::getId, LossReport::getStudentId));
    }
}
