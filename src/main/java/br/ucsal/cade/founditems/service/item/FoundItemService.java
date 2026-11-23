package br.ucsal.cade.founditems.service.item;

import br.ucsal.cade.founditems.api.event.FoundItemRegisteredEvent;
import br.ucsal.cade.founditems.persistence.item.FoundItem;
import br.ucsal.cade.founditems.persistence.item.FoundItemRepository;
import br.ucsal.cade.founditems.persistence.item.NewFoundItem;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.event.EventPublisher;
import br.ucsal.cade.shared.exception.BadRequestException;
import br.ucsal.cade.shared.exception.NotFoundException;
import br.ucsal.cade.shared.storage.PhotoStorage;
import br.ucsal.cade.shared.storage.StoredPhoto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Cadastro e consulta de objetos achados pelo funcionário (RF04 a RF06). */
@Service
public class FoundItemService {

    static final String REGISTERED_ACTION = "FOUND_ITEM_REGISTERED";
    static final String REGISTERED_REASON = "Objeto cadastrado pelo funcionário.";
    static final String BROUGHT_BY_ACTION = "FOUND_ITEM_BROUGHT_BY_RECORDED";
    static final String BROUGHT_BY_REASON = "Quem trouxe o objeto foi registrado pelo funcionário.";
    static final String IDENTIFIED_ACTION = "FOUND_ITEM_IDENTIFIED";
    static final String IDENTIFICATION_CLEARED_ACTION = "FOUND_ITEM_IDENTIFICATION_CLEARED";
    static final String UNKNOWN_CATEGORY_MESSAGE = "Categoria não encontrada.";
    static final String NOT_FOUND_MESSAGE = "Objeto não encontrado.";

    private final FoundItemRepository items;
    private final CategoryRepository categories;
    private final PhotoStorage photos;
    private final EventPublisher events;
    private final Clock clock;

    FoundItemService(FoundItemRepository items, CategoryRepository categories, PhotoStorage photos,
                     EventPublisher events, Clock clock) {
        this.items = items;
        this.categories = categories;
        this.photos = photos;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Cadastra o objeto, grava o histórico e avisa que ele foi cadastrado, tudo na mesma transação (RF43, D10). A
     * foto é guardada antes; se a transação for desfeita, ela é apagada, para não sobrar foto sem objeto.
     *
     * @param registeredBy id da conta do funcionário (claim {@code sub} do token)
     */
    @Transactional
    public FoundItem register(RegisterFoundItem request, UUID registeredBy) {
        return register(request, null, registeredBy);
    }

    /** Como {@link #register(RegisterFoundItem, UUID)}, com quem trouxe o objeto (RF07), que é opcional. */
    @Transactional
    public FoundItem register(RegisterFoundItem request, String broughtBy, UUID registeredBy) {
        Category category = categories.findById(request.categoryId())
                .orElseThrow(() -> new BadRequestException(UNKNOWN_CATEGORY_MESSAGE));
        String photoKey = photos.save(request.photo());
        deleteOnRollback(photoKey);

        NewFoundItem data = new NewFoundItem(category, request.block(), request.room(), request.foundOn(),
                request.storageBox(), request.risk(), request.internalDescription(), request.reservedDetails());
        FoundItem registered = FoundItem.register(data, photoKey, registeredBy, clock.instant(),
                LocalDate.now(clock));
        registered.recordBroughtBy(broughtBy);
        FoundItem item = items.save(registered);

        events.publish(new AuditEvent(registeredBy, REGISTERED_ACTION, FoundItem.class.getSimpleName(),
                item.getId(), REGISTERED_REASON));
        events.publish(new FoundItemRegisteredEvent(item.getId(), category.getId(), item.getFoundOn()));
        return item;
    }

    /**
     * Registra quem trouxe o objeto (RF07): nome e matrícula, ou "equipe de limpeza". Em branco apaga. Vale em qualquer
     * situação. O histórico diz que foi registrado, não quem trouxe (dado pessoal).
     */
    @Transactional
    public FoundItem recordBroughtBy(UUID id, String broughtBy, UUID staffId) {
        FoundItem item = items.findWithCategoryById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
        item.recordBroughtBy(broughtBy);
        items.saveAndFlush(item);
        events.publish(new AuditEvent(staffId, BROUGHT_BY_ACTION, FoundItem.class.getSimpleName(), id,
                BROUGHT_BY_REASON));
        return item;
    }

    /**
     * Marca o objeto como identificado (RF08), com o nome, o e-mail ou o documento encontrado nele. O histórico diz
     * quais campos vieram, nunca o conteúdo: são dados pessoais de um terceiro.
     */
    @Transactional
    public FoundItem identify(UUID id, IdentifyFoundItem request, UUID staffId) {
        FoundItem item = items.findWithCategoryById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
        item.identify(request.name(), request.email(), request.document(), staffId, clock.instant());
        items.saveAndFlush(item);
        List<String> fields = new ArrayList<>();
        if (item.getIdentifiedName() != null) {
            fields.add("nome");
        }
        if (item.getIdentifiedEmail() != null) {
            fields.add("e-mail");
        }
        if (item.getIdentifiedDocument() != null) {
            fields.add("documento");
        }
        events.publish(new AuditEvent(staffId, IDENTIFIED_ACTION, FoundItem.class.getSimpleName(), id,
                "Objeto marcado como identificado. Dados informados: " + String.join(", ", fields) + "."));
        return item;
    }

    /** Desfaz a marcação de identificado (por exemplo, feita no objeto errado), com o registro no histórico. */
    @Transactional
    public FoundItem clearIdentification(UUID id, UUID staffId) {
        FoundItem item = items.findWithCategoryById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
        if (!item.isIdentified()) {
            return item;
        }
        item.clearIdentification();
        items.saveAndFlush(item);
        events.publish(new AuditEvent(staffId, IDENTIFICATION_CLEARED_ACTION, FoundItem.class.getSimpleName(), id,
                "A marcação de objeto identificado foi desfeita."));
        return item;
    }

    /** O objeto com todos os dados, para o funcionário. */
    @Transactional(readOnly = true)
    public FoundItem find(UUID id) {
        return items.findWithCategoryById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
    }

    /** A foto do objeto, servida só ao funcionário (RN03). */
    @Transactional(readOnly = true)
    public StoredPhoto photoOf(UUID id) {
        FoundItem item = items.findById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
        return photos.load(item.getPhotoKey()).orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
    }

    private void deleteOnRollback(String photoKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    photos.delete(photoKey);
                }
            }
        });
    }
}
