package br.ucsal.cade.shared.time;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Liga o {@code @Scheduled} para as tarefas periódicas, como o reenvio dos avisos (D30) e os vencimentos. Sem o
 * {@code @EnableScheduling}, o {@code @Scheduled} é ignorado em silêncio.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SchedulingConfiguration {
}
