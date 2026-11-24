package br.ucsal.cade.founditems.api.candidate;

import br.ucsal.cade.shared.category.RiskLevel;

import java.time.LocalDate;
import java.util.UUID;

/**
 * O objeto como o funcionário o compara com uma perda (RF23). Leva a descrição interna e os detalhes reservados, que
 * só o funcionário vê (RN03): nunca vai para estudantes. A foto fica na rota do próprio objeto.
 *
 * @param status nome da situação para o usuário, como na seção 6.1
 */
public record ComparableFoundItem(UUID id, String categoryName, LocalDate foundOn, String block, String room,
                                  String storageBox, RiskLevel risk, String status, String internalDescription,
                                  String reservedDetails) {
}
