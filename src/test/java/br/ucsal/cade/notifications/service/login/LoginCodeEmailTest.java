package br.ucsal.cade.notifications.service.login;

import br.ucsal.cade.access.api.event.LoginCodeRequestedEvent;
import br.ucsal.cade.shared.mail.EmailMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class LoginCodeEmailTest {

    @Test
    void carriesTheCodeAndTheExpiryInSalvadorTime() {
        // 17:32 UTC são 14:32 em Salvador (UTC-3).
        var event = new LoginCodeRequestedEvent("aluno@ucsal.edu.br", "123456", Instant.parse("2026-11-16T17:32:00Z"));

        EmailMessage message = LoginCodeEmail.from(event);

        assertThat(message.to()).isEqualTo("aluno@ucsal.edu.br");
        assertThat(message.subject()).isEqualTo(LoginCodeEmail.SUBJECT);
        assertThat(message.body()).contains("123456").contains("14:32");
    }
}
