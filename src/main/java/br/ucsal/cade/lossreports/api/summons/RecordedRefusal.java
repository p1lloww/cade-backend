package br.ucsal.cade.lossreports.api.summons;

import java.util.UUID;

/**
 * O estudante depois de mais uma recusa no balcão.
 *
 * @param refusals quantas recusas ele já tem
 * @param blocked  se ele ficou impedido de abrir novos registros (RN12)
 */
public record RecordedRefusal(UUID studentId, int refusals, boolean blocked) {
}
