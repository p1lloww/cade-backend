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
import java.util.ArrayList;
import java.util.List;
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

    /** Disponível → Não reclamado: o objeto completou o prazo de guarda sem dono (RN16). */
    public void markUnclaimed() {
        status = status.markUnclaimed();
    }

    /** Não reclamado → Destinado: o administrador deu um destino ao objeto (RF38). */
    public void dispose() {
        status = status.dispose();
    }

    /** Reservado → Disponível: a verificação falhou no balcão ou a convocação venceu. */
    public void release() {
        status = status.release();
    }

    /**
     * O funcionário corrige os dados do objeto (ACH-07). A foto não muda. Só Disponível e Não reclamado admitem a
     * correção ({@link FoundItemStatus#isEditable()}). O risco segue a RN04: nunca abaixo do da categoria; sem risco
     * informado, vale o padrão da categoria. A caixa em branco volta à sugerida pela data (RF05).
     *
     * @param today a data de hoje, para recusar uma data no futuro
     * @return as mudanças, uma por campo, no formato {@code "campo: antes → depois"}; vazio se nada mudou. Os dois
     * textos livres (descrição interna e detalhes reservados) aparecem só pelo nome, sem o conteúdo
     * @throws InvalidStatusTransitionException se o objeto não está numa situação editável
     * @throws BadRequestException              se a data é futura ou o risco é menor que o da categoria
     */
    public List<String> update(Category category, String block, String room, LocalDate foundOn, String storageBox,
                               RiskLevel risk, String internalDescription, String reservedDetails, LocalDate today) {
        if (!status.isEditable()) {
            throw new InvalidStatusTransitionException("corrigir", "um objeto", status.label());
        }
        if (Objects.requireNonNull(foundOn).isAfter(today)) {
            throw new BadRequestException(FUTURE_DATE_MESSAGE);
        }
        String newBlock = Objects.requireNonNull(block).strip();
        String newRoom = Objects.requireNonNull(room).strip();
        String newBox = blankToNull(storageBox) == null ? StorageBox.suggestedFor(foundOn) : storageBox.strip();
        RiskLevel newRisk = riskFor(Objects.requireNonNull(category), risk);
        String newDescription = blankToNull(internalDescription);
        String newDetails = blankToNull(reservedDetails);

        List<String> changes = new ArrayList<>();
        if (!isSameCategory(category)) {
            changes.add("categoria: " + this.category.getName() + " → " + category.getName());
            this.category = category;
        }
        if (!this.block.equals(newBlock)) {
            changes.add("bloco: " + this.block + " → " + newBlock);
            this.block = newBlock;
        }
        if (!this.room.equals(newRoom)) {
            changes.add("sala: " + this.room + " → " + newRoom);
            this.room = newRoom;
        }
        if (!this.foundOn.equals(foundOn)) {
            changes.add("data: " + this.foundOn + " → " + foundOn);
            this.foundOn = foundOn;
        }
        if (!this.storageBox.equals(newBox)) {
            changes.add("caixa: " + this.storageBox + " → " + newBox);
            this.storageBox = newBox;
        }
        if (this.risk != newRisk) {
            changes.add("risco: " + this.risk.label() + " → " + newRisk.label());
            this.risk = newRisk;
        }
        if (!Objects.equals(this.internalDescription, newDescription)) {
            changes.add("descrição interna alterada");
            this.internalDescription = newDescription;
        }
        if (!Objects.equals(this.reservedDetails, newDetails)) {
            changes.add("detalhes reservados alterados");
            this.reservedDetails = newDetails;
        }
        return changes;
    }

    private boolean isSameCategory(Category other) {
        return category == other || (category.getId() != null && category.getId().equals(other.getId()));
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
