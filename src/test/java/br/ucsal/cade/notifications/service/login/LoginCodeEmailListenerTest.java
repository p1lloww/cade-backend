package br.ucsal.cade.notifications.service.login;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.access.service.login.LoginService;
import br.ucsal.cade.shared.mail.EmailMessage;
import br.ucsal.cade.shared.mail.EmailSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class LoginCodeEmailListenerTest {

    @Autowired
    private LoginService loginService;

    @MockitoBean
    private EmailSender emailSender;

    @Test
    void emailsTheCodeAfterIsRequested() {
        loginService.requestCode("listener@ucsal.edu.br");

        // @Async: o e-mail sai em outra thread, então é preciso esperar por ele.
        ArgumentCaptor<EmailMessage> sent = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender, timeout(5_000)).send(sent.capture());
        assertThat(sent.getValue().to()).isEqualTo("listener@ucsal.edu.br");
        assertThat(sent.getValue().body()).containsPattern("\\d{6}");
    }
}
