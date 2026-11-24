package br.ucsal.cade.lossreports.service.port;

import br.ucsal.cade.lossreports.api.registration.LossReportData;
import br.ucsal.cade.lossreports.api.registration.LossReportRegistration;
import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.service.report.LossReportService;
import br.ucsal.cade.lossreports.service.report.RegisterLossReport;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
class LossReportRegistrationService implements LossReportRegistration {

    private final LossReportService reports;

    LossReportRegistrationService(LossReportService reports) {
        this.reports = reports;
    }

    @Override
    public UUID register(LossReportData data, UUID studentId) {
        List<LossLocation> locations = data.block() == null ? List.of()
                : List.of(LossLocation.of(data.block(), data.room()));
        return reports.register(new RegisterLossReport(data.categoryId(), data.description(), data.secretDetail(),
                locations, data.lostOn(), null, null, null), studentId).getId();
    }

    @Override
    public boolean hasAnyOf(UUID studentId) {
        return !reports.listOf(studentId).isEmpty();
    }
}
