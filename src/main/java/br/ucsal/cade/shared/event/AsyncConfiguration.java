package br.ucsal.cade.shared.event;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Turns on {@code @Async} for side effects such as e-mails (D05). With {@code spring.threads.virtual.enabled}, Spring
 * Boot's default task executor runs each task on a virtual thread.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfiguration {
}
