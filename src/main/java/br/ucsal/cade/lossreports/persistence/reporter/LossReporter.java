package br.ucsal.cade.lossreports.persistence.reporter;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

/** Garante o limite de registros em andamento mesmo com registros simultâneos (RN05). */
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
