package br.ucsal.cade.retention.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RetentionProperties.class)
class RetentionConfiguration {
}
