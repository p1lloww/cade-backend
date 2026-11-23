package br.ucsal.cade.lossreports.persistence.report;

import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.persistence.period.LostPeriod;
import br.ucsal.cade.lossreports.persistence.status.LossReportStatus;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** O detalhe secreto e a foto nunca vão para outros estudantes (RF14). */
@Entity
@Table(name = "loss_reports")
public class LossReport {

    public static final int MAX_TEXT_LENGTH = 500;
    public static final int MAX_LOCATIONS = 5;
    public static final int MAX_SERIAL_NUMBER_LENGTH = 60;

    /** Validade inicial do registro (RN14); o administrador pode mudá-la (RF45). */
    public static final Duration VALIDITY = Duration.ofDays(30);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID studentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = MAX_TEXT_LENGTH)
    private String description;

    @Column(length = MAX_TEXT_LENGTH)
    private String secretDetail;

    @ElementCollection
    @CollectionTable(name = "loss_report_locations", joinColumns = @JoinColumn(name = "loss_report_id"))
    @OrderColumn(name = "position")
    private List<LossLocation> locations = new ArrayList<>();

    @Embedded
    private LostPeriod period;

    @Column(updatable = false)
    private String photoKey;

    @Column(nullable = false, length = 20)
    private LossReportStatus status;

    @Column(nullable = false, updatable = false)
    private Instant registeredAt;

    @Column(nullable = false)
    private Instant expiresAt;

    /** Chave da foto da prova (foto antiga, nota fiscal) na {@code PhotoStorage}; nula quando não há (RF15). */
    private String proofKey;

    @Column(length = MAX_SERIAL_NUMBER_LENGTH)
    private String proofSerialNumber;

    @Version
    private long version;

    protected LossReport() {
    }

    private LossReport(NewLossReport data, UUID studentId, Instant registeredAt, Duration validity) {
        this.studentId = studentId;
        this.category = data.category();
        this.description = data.description().strip();
        this.secretDetail = data.secretDetail() == null || data.secretDetail().isBlank()
                ? null : data.secretDetail().strip();
        this.locations = new ArrayList<>(data.locations().stream().distinct().toList());
        this.period = data.period();
        this.photoKey = data.photoKey();
        this.status = LossReportStatus.initial();
        this.registeredAt = registeredAt;
        this.expiresAt = registeredAt.plus(validity);
    }

    /** Locais repetidos contam uma vez. */
    public static LossReport register(NewLossReport data, UUID studentId, Instant registeredAt) {
        return register(data, studentId, registeredAt, VALIDITY);
    }

    /** Com a validade que o administrador configurou (RF45); {@link #VALIDITY} é o valor inicial. */
    public static LossReport register(NewLossReport data, UUID studentId, Instant registeredAt, Duration validity) {
        return new LossReport(data, studentId, registeredAt, validity);
    }

    /**
     * O estudante anexa ou troca a prova: uma foto (foto antiga do objeto, nota fiscal) e/ou o número de série (RF15).
     * Vale em qualquer situação que não seja final: a prova é do próprio dono e é lida pelo funcionário na conferência.
     * Quem não envia um dos dois mantém o que já tinha.
     *
     * @param proofKey     chave da nova foto, ou nula para manter a atual
     * @param serialNumber número de série novo, ou nulo para manter o atual
     * @return a chave da foto que saiu do lugar, para quem chamou apagar do armazenamento; nula se não havia
     */
    public String attachProof(String proofKey, String serialNumber) {
        if (status.isFinal()) {
            throw new InvalidStatusTransitionException("anexar prova a", "um registro de perda", status.label());
        }
        String replaced = proofKey == null ? null : this.proofKey;
        if (proofKey != null) {
            this.proofKey = proofKey;
        }
        if (serialNumber != null) {
            this.proofSerialNumber = serialNumber.strip();
        }
        return replaced;
    }

    /**
     * Aberto → Convocado (RF26). Daí em diante a descrição e o detalhe secreto não mudam mais (RN06): toda edição
     * futura do registro precisa conferir {@link LossReportStatus#isEditable()}.
     */
    public void summon() {
        status = status.summon();
    }

    /** Convocado → Concluído: o estudante retirou o objeto (RF29). */
    public void complete() {
        status = status.complete();
    }

    /** Convocado → Aberto: a convocação venceu (RN15). */
    public void release() {
        status = status.release();
    }

    /** Convocado → Recusado: a verificação falhou no balcão (RF31), e o registro se encerra (D26). */
    public void refuse() {
        status = status.refuse();
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public Category getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getSecretDetail() {
        return secretDetail;
    }

    public List<LossLocation> getLocations() {
        return List.copyOf(locations);
    }

    public LostPeriod getPeriod() {
        return period;
    }

    public String getPhotoKey() {
        return photoKey;
    }

    public boolean hasPhoto() {
        return photoKey != null;
    }

    public LossReportStatus getStatus() {
        return status;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean hasProofPhoto() {
        return proofKey != null;
    }

    public String getProofKey() {
        return proofKey;
    }

    public String getProofSerialNumber() {
        return proofSerialNumber;
    }

    public long getVersion() {
        return version;
    }
}
