package br.ucsal.cade.founditems.api.reservation;

import br.ucsal.cade.shared.category.RiskLevel;

import java.util.Objects;
import java.util.UUID;

/**
 * O objeto como o balcão o vê no atalho sem convocação (RF32): o risco define o que conferir (RN09). Não leva foto,
 * descrição interna nem detalhes reservados (RN03); o funcionário os vê na rota do próprio objeto.
 *
 * @param statusLabel           nome da situação para o usuário, como na seção 6.1
 * @param handOverWithoutSummons se o objeto pode ser entregue pelo atalho (Disponível ou Não reclamado)
 */
public record WalkInFoundItem(UUID id, RiskLevel risk, String status, String statusLabel,
                              boolean handOverWithoutSummons) {

    public WalkInFoundItem {
        Objects.requireNonNull(id);
        Objects.requireNonNull(risk);
    }
}
