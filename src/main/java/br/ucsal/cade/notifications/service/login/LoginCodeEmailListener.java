package br.ucsal.cade.notifications.service.login;

import br.ucsal.cade.access.api.event.LoginCodeRequestedEvent;
import br.ucsal.cade.shared.mail.EmailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * E-mails the login code once it is issued (RF01).
 *
 * <p>{@code fallbackExecution} is required: {@code LoginService} publishes the event outside a transaction (D18), and
 * without it Spring would silently drop the event.
 */
@Component
public class LoginCodeEmailListener {

    private final EmailSender emailSender;

    LoginCodeEmailListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void on(LoginCodeRequestedEvent event) {
        emailSender.send(LoginCodeEmail.from(event));
    }
}
