package br.ucsal.cade.founditems.persistence.item;

import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.exception.BadRequestException;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Objeto achado e guardado na sala (RF04 a RF06). Só foto, categoria, bloco, sala e data são obrigatórios; o resto o
 * funcionário acrescenta se quiser.
 *
 * <p>Foto, descrição interna, detalhes reservados e o local exato nunca vão para estudantes nem visitantes (RN03):
 * só as rotas de funcionário devolvem esses campos.
 */
@Entity
@Table(name = "found_items")
public class FoundItem {

    public static final int MAX_PLACE_LENGTH = 30;
    public static final int MAX_TEXT_LENGTH = 500;
    public static final int MAX_BROUGHT_BY_LENGTH = 120;
    public static final int MAX_NAME_LENGTH = 120;
    public static final int MAX_EMAIL_LENGTH = 254;
    public static final int MAX_DOCUMENT_LENGTH = 60;

    static final String FUTURE_DATE_MESSAGE = "A data em que o objeto foi achado não pode ser no futuro.";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = MAX_PLACE_LENGTH)
    private String block;

    @Column(nullable = false, length = MAX_PLACE_LENGTH)
    private String room;

    @Column(nullable = false)
    private LocalDate foundOn;

    @Column(nullable = false, updatable = false)
    private String photoKey;

    @Column(nullable = false, length = StorageBox.MAX_LENGTH)
    private String storageBox;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskLevel risk;

    @Column(nullable = false, length = 20)
    private FoundItemStatus status;

    @Column(length = MAX_TEXT_LENGTH)
    private String internalDescription;

    @Column(length = MAX_TEXT_LENGTH)
    private String reservedDetails;

    /** Quem trouxe o objeto à sala: nome e matrícula, ou "equipe de limpeza" (RF07). Só funcionários veem. */
    @Column(length = MAX_BROUGHT_BY_LENGTH)
    private String broughtBy;

    /** O que foi encontrado no objeto que o identifica: nome, e-mail ou documento (RF08). Só funcionários veem. */
    @Column(length = MAX_NAME_LENGTH)
    private String identifiedName;

    @Column(length = MAX_EMAIL_LENGTH)
    private String identifiedEmail;

    @Column(length = MAX_DOCUMENT_LENGTH)
    private String identifiedDocument;

    private Instant identifiedAt;

    private UUID identifiedBy;

    @Column(nullable = false, updatable = false)
    private UUID registeredBy;

    @Column(nullable = false, updatable = false)
    private Instant registeredAt;

    /** Duas convocações ao mesmo tempo do mesmo objeto: só a primeira reserva o objeto (RN18). */
    @Version
    private long version;

    protected FoundItem() {
    }

    private FoundItem(NewFoundItem data, String photoKey, UUID registeredBy, Instant registeredAt, LocalDate today) {
        if (data.foundOn().isAfter(today)) {
            throw new BadRequestException(FUTURE_DATE_MESSAGE);
        }
        this.category = data.category();
        this.block = data.block().strip();
        this.room = data.room().strip();
        this.foundOn = data.foundOn();
        this.photoKey = Objects.requireNonNull(photoKey);
        this.storageBox = blankToNull(data.storageBox()) == null
                ? StorageBox.suggestedFor(data.foundOn()) : data.storageBox().strip();
        this.risk = riskFor(data.category(), data.risk());
        this.status = FoundItemStatus.initial();
        this.internalDescription = blankToNull(data.internalDescription());
        this.reservedDetails = blankToNull(data.reservedDetails());
        this.registeredBy = Objects.requireNonNull(registeredBy);
        this.registeredAt = Objects.requireNonNull(registeredAt);
    }

    /**
     * Cadastra um objeto na situação Disponível (seção 6.1). Sem caixa informada, usa a caixa do mês da data (RF05);
     * sem risco informado, usa o risco padrão da categoria (RN04).
     *
     * @param today a data de hoje, para recusar uma data no futuro
     * @throws BadRequestException se a data é futura ou o risco é menor que o da categoria
     */
    public static FoundItem register(NewFoundItem data, String photoKey, UUID registeredBy, Instant registeredAt,
                                     LocalDate today) {
        return new FoundItem(data, photoKey, registeredBy, registeredAt, today);
    }

    /** O funcionário pode aumentar o risco de um objeto, nunca reduzir abaixo do da categoria (RN04). */
    private static RiskLevel riskFor(Category category, RiskLevel requested) {
        RiskLevel defaultRisk = category.getDefaultRisk();
        if (requested == null) {
            return defaultRisk;
        }
        if (!defaultRisk.canBeRaisedTo(requested)) {
            throw new BadRequestException("O risco de um objeto da categoria " + category.getName()
                    + " não pode ser " + requested.label() + ": só é possível aumentar o risco "
                    + defaultRisk.label() + " da categoria.");
        }
        return requested;
    }

    /** Disponível ou Não reclamado → Reservado: um estudante foi convocado para retirar o objeto (RF26). */
    public void reserve() {
        status = status.reserve();
    }

    /** Reservado → Devolvido: o dono retirou o objeto no balcão (RF29). */
    public void handOver() {
        status = status.handOver();
    }

    /** Disponível ou Não reclamado → Devolvido pelo atalho de balcão, sem convocação (RF32). */
    public void handOverWithoutSummons() {
        status = status.handOverWithoutSummons();
    }

    /** Reservado → Disponível: a verificação falhou no balcão ou a convocação venceu. */
    public void release() {
        status = status.release();
    }

    /** Quem trouxe o objeto (RF07). Em branco apaga. */
    public void recordBroughtBy(String broughtBy) {
        this.broughtBy = blankToNull(broughtBy);
    }

    /**
     * Marca o objeto como identificado (RF08): nome, e-mail ou documento encontrado nele. Pelo menos um dos três. Vale
     * enquanto o objeto não saiu da sala (Disponível, Reservado ou Não reclamado); marcar de novo troca o que estava.
     *
     * @throws InvalidStatusTransitionException se o objeto já foi devolvido ou destinado
     * @throws BadRequestException              se não veio nenhum dos três
     */
    public void identify(String name, String email, String document, UUID staffId, Instant at) {
        if (status.isFinal()) {
            throw new InvalidStatusTransitionException("identificar", "um objeto", status.label());
        }
        String newName = blankToNull(name);
        String newEmail = blankToNull(email);
        String newDocument = blankToNull(document);
        if (newName == null && newEmail == null && newDocument == null) {
            throw new BadRequestException("Informe o nome, o e-mail ou o documento encontrado no objeto.");
        }
        this.identifiedName = newName;
        this.identifiedEmail = newEmail == null ? null : newEmail.toLowerCase(java.util.Locale.ROOT);
        this.identifiedDocument = newDocument;
        this.identifiedAt = Objects.requireNonNull(at);
        this.identifiedBy = Objects.requireNonNull(staffId);
    }

    /** Desfaz a marcação de identificado, por exemplo quando foi feita no objeto errado. */
    public void clearIdentification() {
        this.identifiedName = null;
        this.identifiedEmail = null;
        this.identifiedDocument = null;
        this.identifiedAt = null;
        this.identifiedBy = null;
    }

    public boolean isIdentified() {
        return identifiedAt != null;
    }

    public String getBroughtBy() {
        return broughtBy;
    }

    public String getIdentifiedName() {
        return identifiedName;
    }

    public String getIdentifiedEmail() {
        return identifiedEmail;
    }

    public String getIdentifiedDocument() {
        return identifiedDocument;
    }

    public Instant getIdentifiedAt() {
        return identifiedAt;
    }

    public UUID getIdentifiedBy() {
        return identifiedBy;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public UUID getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public String getBlock() {
        return block;
    }

    public String getRoom() {
        return room;
    }

    public LocalDate getFoundOn() {
        return foundOn;
    }

    public String getPhotoKey() {
        return photoKey;
    }

    public String getStorageBox() {
        return storageBox;
    }

    public RiskLevel getRisk() {
        return risk;
    }

    public FoundItemStatus getStatus() {
        return status;
    }

    public String getInternalDescription() {
        return internalDescription;
    }

    public String getReservedDetails() {
        return reservedDetails;
    }

    public UUID getRegisteredBy() {
        return registeredBy;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public long getVersion() {
        return version;
    }
}
