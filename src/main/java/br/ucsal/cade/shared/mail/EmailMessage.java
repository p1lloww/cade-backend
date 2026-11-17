package br.ucsal.cade.shared.mail;

import java.util.Objects;

/**
 * A plain-text e-mail ready to be sent.
 *
 * @param to      recipient address
 * @param subject subject line
 * @param body    plain-text body; may hold secrets such as a login code, so it is never logged
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
