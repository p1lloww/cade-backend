package br.ucsal.cade.matching.service.score;

import java.util.Objects;

/**
 * Bloco e sala, como foram digitados.
 *
 * @param room nula quando não foi informada
 */
public record Place(String block, String room) {

    public Place {
        Objects.requireNonNull(block);
    }
}
