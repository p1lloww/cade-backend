package br.ucsal.cade.notifications.service.deadline;

import br.ucsal.cade.lossreports.api.ExpiringLossReport;
import br.ucsal.cade.lossreports.api.LossReportDeadlines;
import br.ucsal.cade.notifications.persistence.outbox.NotificationKind;
import br.ucsal.cade.notifications.service.outbox.enqueue.Outbox;
import br.ucsal.cade.pickup.api.ExpiringSummons;
import br.ucsal.cade.pickup.api.SummonsDeadlines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * De hora em hora, põe na caixa de saída os lembretes de prazo: a convocação que vence em menos de 1 dia (RF34) e o
 * registro de perda perto de vencer (RF35), que o estudante pode renovar (PER-06).
 *
 * <p>A tarefa pode achar o mesmo prazo em várias rodadas seguidas; quem garante um lembrete só por convocação e por
 * registro é a caixa de saída, que ignora o mesmo tipo para a mesma referência (D30). Assim a tarefa não precisa
 * lembrar o que já avisou, e uma rodada perdida (aplicação fora do ar) é coberta pela seguinte.
 */
@Component
class DeadlineNoticeJob {

    private static final Logger log = LoggerFactory.getLogger(DeadlineNoticeJob.class);

    private final SummonsDeadlines summonses;
    private final LossReportDeadlines lossReports;
    private final Outbox outbox;
    private final DeadlineNoticeProperties properties;
    private final Clock clock;

    DeadlineNoticeJob(SummonsDeadlines summonses, LossReportDeadlines lossReports, Outbox outbox,
                      DeadlineNoticeProperties properties, Clock clock) {
        this.summonses = summonses;
        this.lossReports = lossReports;
        this.outbox = outbox;
        this.properties = properties;
        this.clock = clock;
    }

    /** Uma transação para a rodada: os lembretes só saem depois que todos estão gravados. */
    @Scheduled(fixedDelayString = "${cade.notifications.deadlines.interval}",
            initialDelayString = "${cade.notifications.deadlines.interval}")
    @Transactional
    public void queueDeadlineNotices() {
        Instant now = clock.instant();

        List<ExpiringSummons> expiringSummons = summonses.expiringBetween(now, now.plus(properties.summonsNotice()));
        long summonsQueued = expiringSummons.stream()
                .filter(summons -> outbox.enqueue(NotificationKind.SUMMONS_EXPIRING, summons.summonsId(),
                        summons.studentId(), SummonsExpiringNotice.from(summons.instructions())))
                .count();

        long reportsQueued = properties.lossReportNoticeEnabled() ? queueLossReportNotices(now) : 0;

        if (summonsQueued + reportsQueued > 0) {
            log.info("Deadline notices queued: summons={}/{}, lossReports={}", summonsQueued, expiringSummons.size(),
                    reportsQueued);
        }
    }

    private long queueLossReportNotices(Instant now) {
        List<ExpiringLossReport> expiring = lossReports.expiringBetween(now, now.plus(properties.lossReportNotice()));
        return expiring.stream()
                .filter(report -> outbox.enqueue(NotificationKind.LOSS_REPORT_EXPIRING, report.lossReportId(),
                        report.studentId(), LossReportExpiringNotice.from(report)))
                .count();
    }
}
