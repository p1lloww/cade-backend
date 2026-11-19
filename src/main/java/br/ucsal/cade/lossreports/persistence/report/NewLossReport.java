package br.ucsal.cade.lossreports.persistence.report;

import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.persistence.period.LostPeriod;
import br.ucsal.cade.shared.category.Category;

import java.util.List;

/**
 * Dados de uma perda a registrar, já com a categoria encontrada, o período validado e a foto guardada.
 *
 * @param secretDetail detalhe que só o dono conhece; nulo se o estudante não informar
 * @param locations    locais onde acha que perdeu; vazia quando não sabe
 * @param photoKey     chave da foto na {@code PhotoStorage}; nula quando o estudante não enviou foto
 */
public record NewLossReport(Category category, String description, String secretDetail,
                            List<LossLocation> locations, LostPeriod period, String photoKey) {
}
