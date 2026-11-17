package br.ucsal.cade.notifications.service.login;

import br.ucsal.cade.access.api.event.LoginCodeRequestedEvent;
import br.ucsal.cade.shared.mail.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envia por e-mail o código de login assim que ele é emitido (RF01), numa thread separada da requisição (D05).
 *
 * <p>O {@code fallbackExecution} é necessário: o {@code LoginService} publica o evento fora de transação (D18), e sem
 * ele o Spring descartaria o evento em silêncio.
 */
@Component
public class LoginCodeEmailListener {

    private static final Logger log = LoggerFactory.getLogger(LoginCodeEmailListener.class);

    private final EmailSender emailSender;

    LoginCodeEmailListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void on(LoginCodeRequestedEvent event) {
        // O toString do evento esconde o código.
        log.debug("Login code e-mail requested: {}", event);
        emailSender.send(LoginCodeEmail.from(event));
    }
}
