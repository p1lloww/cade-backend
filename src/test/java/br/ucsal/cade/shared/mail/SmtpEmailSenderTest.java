package br.ucsal.cade.shared.mail;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpEmailSenderTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final SmtpEmailSender sender = new SmtpEmailSender(mailSender, new EmailProperties("cade@test.local"));

    @Test
    void sendsFromTheConfiguredAddress() {
        sender.send(new EmailMessage("aluno@ucsal.edu.br", "Assunto", "Corpo"));

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sent.capture());
        assertThat(sent.getValue().getFrom()).isEqualTo("cade@test.local");
        assertThat(sent.getValue().getTo()).containsExactly("aluno@ucsal.edu.br");
        assertThat(sent.getValue().getSubject()).isEqualTo("Assunto");
        assertThat(sent.getValue().getText()).isEqualTo("Corpo");
    }

    @Test
    void doesNotThrowWhenTheServerFails() {
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

                assertThatNoException().isThrownBy(
                        () -> sender.send(new EmailMessage("aluno@ucsal.edu.br", "Assunto", "Corpo")));
    }

    @Test
    void hidesTheBodyFromToString() {
        assertThat(new EmailMessage("a@ucsal.edu.br", "Assunto", "123456").toString()).doesNotContain("123456");
    }
}