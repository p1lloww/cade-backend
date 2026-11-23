package br.ucsal.cade.lossreports.persistence.report;

import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.persistence.period.LostPeriod;
import br.ucsal.cade.lossreports.persistence.status.LossReportStatus;
import br.ucsal.cade.shared.audit.AuditEvent;
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
import java.util.Objects;
import java.util.UUID;

/** O detalhe secreto e a foto nunca vão para outros estudantes (RF14). */
@Entity
@Table(name = "loss_reports")
public class LossReport {

    public static final int MAX_TEXT_LENGTH = 500;
    public static final int MAX_LOCATIONS = 5;
    public static final int MAX_ANSWER_LENGTH = 1000;

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

    /** Por que o funcionário pediu mais informações (RF24); fica depois da resposta, para o estudante reler. */
    @Column(length = AuditEvent.MAX_REASON_LENGTH)
    private String infoRequestReason;

    /** Quando o pedido foi feito; nulo fora de Aguardando informações. Daí contam os 5 dias da RN13. */
    private Instant infoRequestedAt;

    /** A sugestão que originou o pedido, para descartá-la se ninguém responder (RN13); nulo fora do pedido. */
    private UUID infoSuggestionId;

    @Column(length = MAX_ANSWER_LENGTH)
    private String infoAnswer;

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

    /** Aberto ou Aguardando informações → Cancelado: o estudante já achou o objeto (RF17). */
    public void cancel() {
        status = status.cancel();
    }

    /** Aberto → Vencido: o registro completou 30 dias sem renovação (RN14). */
    public void expire() {
        status = status.expire();
    }

    /**
     * Aberto → Aguardando informações: o funcionário pediu mais informações a partir de uma sugestão (RF24). O pedido
     * guarda a sugestão, que é descartada se passarem 5 dias sem resposta (RN13).
     */
    public void requestInfo(UUID suggestionId, String reason, Instant at) {
        status = status.requestInfo();
        this.infoSuggestionId = Objects.requireNonNull(suggestionId);
        this.infoRequestReason = Objects.requireNonNull(reason);
        this.infoRequestedAt = Objects.requireNonNull(at);
        this.infoAnswer = null;
    }

    /** Aguardando informações → Aberto: o estudante respondeu ao pedido (RF18). */
    public void respond(String answer) {
        status = status.resume();
        this.infoAnswer = Objects.requireNonNull(answer).strip();
        closeInfoRequest();
    }

    /** Aguardando informações → Aberto: passaram 5 dias sem resposta (RN13). */
    public void resumeWithoutAnswer() {
        status = status.resume();
        closeInfoRequest();
    }

    /**
     * Estende a validade por mais 30 dias a partir de {@code now} (RN14). Só enquanto o registro está Aberto ou
     * Aguardando informações: um registro Convocado não vence enquanto a convocação corre, e os finais não voltam.
     */
    public void renew(Instant now) {
        if (!status.isEditable()) {
            throw new InvalidStatusTransitionException("renovar", "um registro de perda", status.label());
        }
        this.expiresAt = Objects.requireNonNull(now).plus(VALIDITY);
    }

    private void closeInfoRequest() {
        this.infoRequestedAt = null;
        this.infoSuggestionId = null;
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

    public String getInfoRequestReason() {
        return infoRequestReason;
    }

    public Instant getInfoRequestedAt() {
        return infoRequestedAt;
    }

    public UUID getInfoSuggestionId() {
        return infoSuggestionId;
    }

    public String getInfoAnswer() {
        return infoAnswer;
    }

    public long getVersion() {
        return version;
    }
}
