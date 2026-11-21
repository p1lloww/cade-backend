package br.ucsal.cade.matching.service.matching;

import br.ucsal.cade.founditems.api.event.FoundItemRegisteredEvent;
import br.ucsal.cade.lossreports.api.event.LossReportRegisteredEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Uma falha no cruzamento fica no log e não sobe para quem publicou o evento (RNF08). */
class MatchingListenerTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 10);

    private final MatchingService matching = mock(MatchingService.class);
    private final MatchingListener listener = new MatchingListener(matching);

    @Test
    void keepsAFailureWhenMatchingAFoundItem() {
        UUID item = UUID.randomUUID();
        when(matching.matchFoundItem(item)).thenThrow(new IllegalStateException("falha simulada"));

        assertThatCode(() -> listener.on(new FoundItemRegisteredEvent(item, UUID.randomUUID(), DAY)))
                .doesNotThrowAnyException();
        verify(matching).matchFoundItem(item);
    }

    @Test
    void keepsAFailureWhenMatchingALossReport() {
        UUID report = UUID.randomUUID();
        when(matching.matchLossReport(report)).thenThrow(new IllegalStateException("falha simulada"));

        assertThatCode(() -> listener.on(new LossReportRegisteredEvent(report, UUID.randomUUID(), DAY, DAY)))
                .doesNotThrowAnyException();
        verify(matching).matchLossReport(report);
    }
}
