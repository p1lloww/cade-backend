package br.ucsal.cade.lossreports.api.candidate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A perda como o funcionário a compara com um objeto (RF23). Leva o detalhe secreto, que o funcionário usa para
 * conferir (RF14), mas não o estudante: quem perdeu só importa a partir da convocação.
 *
 * @param shift        nome do turno, ou nulo quando o estudante não informou
 * @param locations    vazia quando o estudante não sabe onde perdeu
 * @param secretDetail nulo quando o estudante não escreveu nenhum
 * @param status       nome da situação para o usuário, como na seção 6.2
 * @param proofSerialNumber número de série da prova (RF15); nulo se o estudante não enviou
 * @param hasProofPhoto     se o estudante anexou uma foto de prova, lida por {@code GET /loss-reports/{id}/proof}
 */
public record ComparableLossReport(UUID id, String categoryName, LocalDate lostFrom, LocalDate lostUntil,
                                   String shift, List<MatchableLossReport.Location> locations, String description,
                                   String secretDetail, String status, Instant registeredAt,
                                   String proofSerialNumber, boolean hasProofPhoto) {

    public ComparableLossReport {
        locations = locations == null ? List.of() : List.copyOf(locations);
    }
}
