package br.ucsal.cade.pickup.service.summons;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PickupProperties.class)
class PickupConfiguration {
}
