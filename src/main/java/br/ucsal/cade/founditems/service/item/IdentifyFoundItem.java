package br.ucsal.cade.founditems.service.item;

/**
 * O que foi encontrado no objeto que o identifica (RF08), como o funcionário informou. Pelo menos um dos três; o
 * objeto confere isso.
 *
 * @param name     nome encontrado no objeto, ou nulo
 * @param email    e-mail encontrado no objeto, ou nulo
 * @param document documento encontrado no objeto, ou nulo
 */
public record IdentifyFoundItem(String name, String email, String document) {
}
