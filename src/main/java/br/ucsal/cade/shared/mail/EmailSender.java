package br.ucsal.cade.shared.mail;

/**
 * Sends e-mails (D04). Modules depend on this interface, never on SMTP, so tests can replace it and the provider can
 * change without touching them.
 */
public interface EmailSender {

    /**
     * Sends the message. Never throws on delivery failure: the failure is logged and the caller goes on (RNF08).
     */
    void send(EmailMessage message);
}
