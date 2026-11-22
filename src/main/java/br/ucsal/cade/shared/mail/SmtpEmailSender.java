package br.ucsal.cade.shared.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Envia e-mails pelo servidor SMTP de {@code spring.mail.*}: Mailpit em desenvolvimento e Gmail em produção (D04). O
 * log registra só o destinatário e o assunto, nunca o corpo.
 */
@Component
@EnableConfigurationProperties(EmailProperties.class)
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;
    private final EmailProperties properties;

    SmtpEmailSender(JavaMailSender mailSender, EmailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public boolean send(EmailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();

        mail.setFrom(properties.from());
        mail.setTo(message.to());
        mail.setSubject(message.subject());
        mail.setText(message.body());

        try {
            log.debug("Sending e-mail to {}: {}", message.to(), message.subject());
            mailSender.send(mail);
            log.debug("E-mail sent to {}: {}", message.to(), message.subject());
            return true;
        } catch (MailException e) {
            log.warn("Could not send e-mail to {} ({}): {}", message.to(), message.subject(), e.getMessage());
            return false;
        }
    }
}
