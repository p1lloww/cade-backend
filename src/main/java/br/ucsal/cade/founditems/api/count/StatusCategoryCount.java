package br.ucsal.cade.founditems.api.count;

/**
 * Uma linha da contagem do painel.
 *
 * @param statusCode  código da situação (por exemplo {@code AVAILABLE}, {@code UNCLAIMED})
 * @param statusLabel nome da situação para mostrar ao usuário
 * @param category    nome da categoria
 */
public record StatusCategoryCount(String statusCode, String statusLabel, String category, long total) {
}
