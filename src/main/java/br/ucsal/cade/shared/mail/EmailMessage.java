package br.ucsal.cade.shared.mail;

import java.util.Objects;

/**
 * Um e-mail em texto puro, pronto para envio.
 *
 * @param to      endereço do destinatário
 * @param subject assunto
 * @param body    corpo em texto puro; pode ter segredos, como um código de login, por isso nunca vai para o log
 */
public record EmailMessage(String to, String subject, String body) {

    public EmailMessage {
        Objects.requireNonNull(to);
        Objects.requireNonNull(subject);
        Objects.requireNonNull(body);
    }

    @Override
    public String toString() {
        return "EmailMessage[to=" + to + ", subject=" + subject + "]";
    }
}
