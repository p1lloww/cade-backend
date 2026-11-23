package br.ucsal.cade.lossreports.service.report;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LossReportProperties.class)
class LossReportConfiguration {
}
