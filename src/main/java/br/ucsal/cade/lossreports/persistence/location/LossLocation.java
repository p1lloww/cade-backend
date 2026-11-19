package br.ucsal.cade.lossreports.persistence.location;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Local da perda: bloco e, opcionalmente, sala (RF13). Sala desconhecida fica nula, nunca como texto, para o
 * cruzamento não tratar duas salas "não informadas" como a mesma (RN07).
 */
@Embeddable
public record LossLocation(

        @Column(nullable = false, length = MAX_LENGTH)
        String block,

        @Column(length = MAX_LENGTH)
        String room) {

    public static final int MAX_LENGTH = 30;

    public static LossLocation of(String block, String room) {
        return new LossLocation(block.strip(), room == null || room.isBlank() ? null : room.strip());
    }
}
