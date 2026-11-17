package br.ucsal.cade.notifications.service.login;

import br.ucsal.cade.access.api.event.LoginCodeRequestedEvent;
import br.ucsal.cade.shared.mail.EmailMessage;
import br.ucsal.cade.shared.time.TimeConfiguration;

import java.time.format.DateTimeFormatter;

/**
 * Texto do e-mail que leva o código de login (RF01).
 */
public class LoginCodeEmail {

    static final String SUBJECT = "Seu código de acesso ao Cadê?";
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm").withZone(TimeConfiguration.ZONE);

    private LoginCodeEmail() {}

    static EmailMessage from(LoginCodeRequestedEvent event) {
        String body = """
                Olá!

                Seu código de acesso ao Cadê? é: %s

                Ele vale até as %s e só pode ser usado uma vez.
                Se você não pediu este código, ignore este e-mail.

                Cadê? — Achados e Perdidos da UCSal
                """.formatted(event.code(), TIME.format(event.expiresAt()));
        return new EmailMessage(event.email(), SUBJECT, body);
    }
}