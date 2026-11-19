package br.ucsal.cade.founditems.service.item;

import br.ucsal.cade.founditems.api.event.FoundItemRegisteredEvent;
import br.ucsal.cade.founditems.persistence.item.FoundItem;
import br.ucsal.cade.founditems.persistence.item.FoundItemRepository;
import br.ucsal.cade.founditems.persistence.item.NewFoundItem;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.exception.BadRequestException;
import br.ucsal.cade.shared.exception.NotFoundException;
import br.ucsal.cade.shared.storage.PhotoStorage;
import br.ucsal.cade.shared.storage.StoredPhoto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/** Cadastro e consulta de objetos achados pelo funcionário (RF04 a RF06). */
@Service
public class FoundItemService {

    static final String REGISTERED_ACTION = "FOUND_ITEM_REGISTERED";
    static final String REGISTERED_REASON = "Objeto cadastrado pelo funcionário.";
    static final String UNKNOWN_CATEGORY_MESSAGE = "Categoria não encontrada.";
    static final String NOT_FOUND_MESSAGE = "Objeto não encontrado.";

    private final FoundItemRepository items;
    private final CategoryRepository categories;
    private final PhotoStorage photos;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    FoundItemService(FoundItemRepository items, CategoryRepository categories, PhotoStorage photos,
                     ApplicationEventPublisher events, Clock clock) {
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
        Category category = categories.findById(request.categoryId())
                .orElseThrow(() -> new BadRequestException(UNKNOWN_CATEGORY_MESSAGE));
        String photoKey = photos.save(request.photo());
        deleteOnRollback(photoKey);

        NewFoundItem data = new NewFoundItem(category, request.block(), request.room(), request.foundOn(),
                request.storageBox(), request.risk(), request.internalDescription(), request.reservedDetails());
        FoundItem item = items.save(FoundItem.register(data, photoKey, registeredBy, clock.instant(),
                LocalDate.now(clock)));

        events.publishEvent(new AuditEvent(registeredBy, REGISTERED_ACTION, FoundItem.class.getSimpleName(),
                item.getId(), REGISTERED_REASON));
        events.publishEvent(new FoundItemRegisteredEvent(item.getId(), category.getId(), item.getFoundOn()));
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
