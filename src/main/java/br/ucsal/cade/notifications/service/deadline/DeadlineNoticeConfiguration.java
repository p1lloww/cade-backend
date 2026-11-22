package br.ucsal.cade.notifications.service.deadline;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DeadlineNoticeProperties.class)
class DeadlineNoticeConfiguration {
}
