package br.ucsal.cade.notifications.service.deadline;

import br.ucsal.cade.lossreports.api.deadline.ExpiringLossReport;
import br.ucsal.cade.notifications.service.outbox.enqueue.NotificationContent;
import br.ucsal.cade.pickup.api.PickupInstructions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeadlineNoticesTest {

    @Test
    void theSummonsReminderRepeatsWhereAndUntilWhenInTheLocalTime() {
        NotificationContent content = SummonsExpiringNotice.from(new PickupInstructions("Sala de achados e perdidos",
                "Das 7h às 21h50", Instant.parse("2026-11-28T18:00:00Z"), "Um documento com foto."));

        assertThat(content.subject()).isEqualTo(SummonsExpiringNotice.SUBJECT);
        assertThat(content.body()).contains("reservado só até 28/11/2026 às 15:00", "- Onde: Sala de achados e perdidos",
                "- O que levar: Um documento com foto.");
    }

    @Test
    void theLossReportReminderNamesTheReportAndTheDate() {
        NotificationContent content = LossReportExpiringNotice.from(new ExpiringLossReport(UUID.randomUUID(),
                UUID.randomUUID(), "Fone JBL preto", Instant.parse("2026-12-01T02:00:00Z")));

        assertThat(content.subject()).isEqualTo(LossReportExpiringNotice.SUBJECT);
        assertThat(content.body()).contains("\"Fone JBL preto\" vence em 30/11/2026");
    }
}
