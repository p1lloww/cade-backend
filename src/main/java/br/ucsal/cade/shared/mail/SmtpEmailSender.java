package br.ucsal.cade.shared.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

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
    public void send(EmailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();

        mail.setFrom(properties.from());
        mail.setTo(message.to());
        mail.setSubject(message.subject());
        mail.setText(message.body());

        try {
            mailSender.send(mail);
        } catch (MailException e) {
            log.warn("Could not send e-mail to {} ({}): {}", message.to(), message.subject(), e.getMessage());
        }
    }
}
