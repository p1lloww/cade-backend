package br.ucsal.cade.pickup.persistence.summons;

import br.ucsal.cade.shared.category.RiskLevel;

import java.util.UUID;

/**
 * Os dados de uma convocação nova, já conferidos pelos módulos donos de cada um.
 *
 * @param summonedBy id do funcionário que convoca
 * @param risk       risco do objeto (RN09)
 */
public record NewSummons(UUID suggestionId, UUID foundItemId, UUID lossReportId, UUID studentId, UUID summonedBy,
                         RiskLevel risk) {
}
