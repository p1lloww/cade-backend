package br.ucsal.cade.lossreports.persistence.location;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Um lugar onde o estudante acha que perdeu o objeto: o bloco e, se ele lembrar, a sala (RF13).
 *
 * <p>A sala desconhecida fica nula, nunca com um texto como "Não informado": o cruzamento compara salas, e duas salas
 * "Não informado" contariam como a mesma sala (RN07). O texto para o usuário é montado na resposta da API.
 *
 * <p>Crie sempre por {@link #of}, que normaliza os textos; o construtor do record existe para o Hibernate.
 *
 * @param room sala; nula quando o estudante lembra só do bloco
 */
@Embeddable
public record LossLocation(

        @Column(nullable = false, length = MAX_LENGTH)
        String block,

        @Column(length = MAX_LENGTH)
        String room) {

    public static final int MAX_LENGTH = 30;

    /** Tira os espaços das pontas; sala em branco vira nula. O bloco chega validado pelo {@code @NotBlank}. */
    public static LossLocation of(String block, String room) {
        return new LossLocation(block.strip(), room == null || room.isBlank() ? null : room.strip());
    }
}
