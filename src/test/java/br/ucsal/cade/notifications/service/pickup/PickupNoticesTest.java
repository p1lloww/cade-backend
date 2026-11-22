package br.ucsal.cade.notifications.service.pickup;

import br.ucsal.cade.notifications.service.outbox.enqueue.NotificationContent;
import br.ucsal.cade.pickup.api.PickupInstructions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PickupNoticesTest {

    @Test
    void showsTheDeadlineInTheLocalTimeOfSalvador() {
        NotificationContent content = SummonsCreatedNotice.from(new PickupInstructions("Sala de achados e perdidos",
                "Das 7h às 21h50", Instant.parse("2026-11-28T18:00:00Z"), "Um documento com foto."));

        assertThat(content.subject()).isEqualTo(SummonsCreatedNotice.SUBJECT);
        assertThat(content.body()).contains("- Onde: Sala de achados e perdidos", "- Horário: Das 7h às 21h50",
                "- Até: 28/11/2026 às 15:00", "- O que levar: Um documento com foto.");
    }

    @Test
    void theConfirmationShowsWhenThePickupWasRegistered() {
        NotificationContent content = PickupRegisteredNotice.at(Instant.parse("2026-11-21T13:30:00Z"));

        assertThat(content.subject()).isEqualTo(PickupRegisteredNotice.SUBJECT);
        assertThat(content.body()).contains("em 21/11/2026 às 10:30");
    }
}
