package br.ucsal.cade.lossreports.service.lifecycle;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LossReportProperties.class)
class LossReportConfiguration {
}
