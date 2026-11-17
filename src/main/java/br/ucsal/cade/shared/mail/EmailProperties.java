package br.ucsal.cade.shared.mail;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


/**
 * E-mail settings from {@code cade.email.*}. SMTP host and credentials stay in {@code spring.mail.*} (RNF06).
 *
 * @param from sender address shown to the recipient; with Gmail it must be the account in MAIL_USERNAME
 */
@Validated
@ConfigurationProperties("cade.email")
public record EmailProperties(@NotBlank @Email String from) {}
