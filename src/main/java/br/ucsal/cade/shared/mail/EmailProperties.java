package br.ucsal.cade.shared.mail;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


/**
 * Configuração de e-mail de {@code cade.email.*}. O servidor SMTP e as credenciais ficam em {@code spring.mail.*}
 * (RNF06).
 *
 * @param from remetente mostrado ao destinatário; no Gmail, precisa ser a conta de MAIL_USERNAME
 */
@Validated
@ConfigurationProperties("cade.email")
public record EmailProperties(@NotBlank @Email String from) {}
