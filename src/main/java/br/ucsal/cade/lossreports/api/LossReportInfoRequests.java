package br.ucsal.cade.lossreports.api;

import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import br.ucsal.cade.shared.exception.NotFoundException;

import java.util.UUID;

/** O pedido de mais informações ao estudante (RF24), feito pelo funcionário a partir de uma sugestão. */
public interface LossReportInfoRequests {

    /**
     * Aberto → Aguardando informações, com o motivo no histórico (RF43). Daí o estudante tem 5 dias para responder
     * (RN13); sem resposta, o registro volta a Aberto e a sugestão é descartada. O estudante recebe o pedido por
     * e-mail (RF35). Só roda dentro da transação de quem pede.
     *
     * @param suggestionId a sugestão que originou o pedido, descartada se ninguém responder
     * @param reason       o que o funcionário precisa saber; vai ao estudante e ao histórico, então não deve citar o
     *                     objeto achado (RN03)
     * @param staffId      id do funcionário
     * @throws NotFoundException se o registro não existe
     * @throws InvalidStatusTransitionException se o registro não está Aberto
     */
    void request(UUID lossReportId, UUID suggestionId, String reason, UUID staffId);
}
