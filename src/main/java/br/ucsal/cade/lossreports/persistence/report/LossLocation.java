package br.ucsal.cade.lossreports.persistence.report;

import br.ucsal.cade.shared.exception.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Um lugar onde o estudante acha que perdeu o objeto: o bloco e, se ele lembrar, a sala (RF13).
 *
 * <p>A sala desconhecida fica nula, nunca com um texto como "Não informado": o cruzamento compara salas, e duas salas
 * "Não informado" contariam como a mesma sala (RN07). O texto para o usuário é montado na resposta da API.
 *
 * <p>Crie sempre por {@link #of}, que aplica as regras; o construtor do record existe para o Hibernate.
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

    static final String BLANK_BLOCK_MESSAGE = "Informe o bloco de cada local.";

    /** @throws BadRequestException se o bloco está em branco */
    public static LossLocation of(String block, String room) {
        if (block == null || block.isBlank()) {
            throw new BadRequestException(BLANK_BLOCK_MESSAGE);
        }
        return new LossLocation(block.strip(), room == null || room.isBlank() ? null : room.strip());
    }
}
