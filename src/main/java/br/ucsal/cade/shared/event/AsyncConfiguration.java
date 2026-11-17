package br.ucsal.cade.shared.event;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Liga o {@code @Async} para os efeitos colaterais, como os e-mails (D05). Com {@code spring.threads.virtual.enabled},
 * o executor padrão do Spring Boot roda cada tarefa numa Virtual Thread.
 *
 * <p>Sem o {@code @EnableAsync}, o {@code @Async} é ignorado em silêncio e o método roda na thread de quem chamou.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfiguration {
}
