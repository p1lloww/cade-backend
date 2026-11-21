package br.ucsal.cade.lossreports.persistence.reporter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.ColumnDefault;

import java.util.UUID;

/**
 * A linha de cada estudante que registra perdas. Garante o limite de registros em andamento mesmo com registros
 * simultâneos (RN05) e conta as recusas no balcão, que bloqueiam novos registros a partir da terceira (RN12).
 */
@Entity
@Table(name = "loss_reporters")
public class LossReporter {

    public static final int MAX_REFUSALS = 3;

    @Id
    private UUID studentId;

    /** O padrão no banco deixa o {@code ddl-auto} criar a coluna numa tabela que já tem linhas. */
    @ColumnDefault("0")
    @Column(nullable = false)
    private int refusals;

    @Version
    private long version;

    protected LossReporter() {
    }

    /** Mais uma recusa no balcão (RF31). */
    public void recordRefusal() {
        refusals++;
    }

    /** Com 3 recusas, o estudante não abre novos registros até o administrador liberar (RN12). */
    public boolean isBlocked() {
        return refusals >= MAX_REFUSALS;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public int getRefusals() {
        return refusals;
    }
}
