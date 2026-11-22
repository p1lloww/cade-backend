package br.ucsal.cade.notifications.service.deadline;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Com quanta antecedência avisar dos prazos, de {@code cade.notifications.deadlines.*}.
 *
 * @param summonsNotice           antes de a convocação vencer (RF34: 1 dia)
 * @param lossReportNotice        antes de o registro de perda vencer (RF35)
 * @param lossReportNoticeEnabled se o lembrete do registro sai; desligado até o PER-06, que é quem faz o registro
 *                                vencer e ser renovado (D30)
 */
@Validated
@ConfigurationProperties("cade.notifications.deadlines")
public record DeadlineNoticeProperties(@NotNull Duration summonsNotice, @NotNull Duration lossReportNotice,
                                       boolean lossReportNoticeEnabled) {
}
