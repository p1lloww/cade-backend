package br.ucsal.cade.shared.mail;

/**
 * Envia e-mails (D04). Os módulos dependem desta interface, nunca do SMTP: os testes podem substituí-la e o provedor
 * pode mudar sem mexer neles.
 */
public interface EmailSender {

    /**
     * Envia a mensagem. Nunca lança exceção se o envio falhar: a falha vai para o log e quem chamou segue (RNF08).
     *
     * @return se o servidor aceitou a mensagem; quem precisa tentar de novo usa este retorno
     */
    boolean send(EmailMessage message);
}
