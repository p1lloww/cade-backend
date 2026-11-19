package br.ucsal.cade.lossreports.persistence.reporter;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

/**
 * Uma linha por estudante que já registrou perda, só com a versão. Serve de ponto de disputa para o limite de 5
 * registros abertos (RN05): o cadastro lê esta linha com {@code OPTIMISTIC_FORCE_INCREMENT}, e dois cadastros
 * simultâneos do mesmo estudante não conseguem gravar a mesma versão; o segundo falha e é desfeito.
 *
 * <p>Não guarda contador: a contagem vem dos próprios registros, então não há o que dessincronizar quando um registro
 * é cancelado ou vence.
 *
 * <p>Não tem construtor público: a linha só nasce pelo {@link LossReporterRepository#createIfAbsent}.
 */
@Entity
@Table(name = "loss_reporters")
public class LossReporter {

    @Id
    private UUID studentId;

    @Version
    private long version;

    protected LossReporter() {
    }

    public UUID getStudentId() {
        return studentId;
    }
}
