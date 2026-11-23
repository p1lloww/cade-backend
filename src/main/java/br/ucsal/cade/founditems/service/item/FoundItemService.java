package br.ucsal.cade.founditems.service.item;

import br.ucsal.cade.founditems.api.event.FoundItemRegisteredEvent;
import br.ucsal.cade.founditems.api.event.FoundItemUpdatedEvent;
import br.ucsal.cade.founditems.persistence.item.FoundItem;
import br.ucsal.cade.founditems.persistence.item.FoundItemRepository;
import br.ucsal.cade.founditems.persistence.item.NewFoundItem;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.event.EventPublisher;
import br.ucsal.cade.shared.exception.BadRequestException;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import br.ucsal.cade.shared.exception.NotFoundException;
import br.ucsal.cade.shared.storage.PhotoStorage;
import br.ucsal.cade.shared.storage.StoredPhoto;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Cadastro e consulta de objetos achados pelo funcionário (RF04 a RF06). */
@Service
public class FoundItemService {

    static final String REGISTERED_ACTION = "FOUND_ITEM_REGISTERED";
    static final String REGISTERED_REASON = "Objeto cadastrado pelo funcionário.";
    static final String UPDATED_ACTION = "FOUND_ITEM_UPDATED";
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
        Category category = categories.findById(request.categoryId())
                .orElseThrow(() -> new BadRequestException(UNKNOWN_CATEGORY_MESSAGE));
        String photoKey = photos.save(request.photo());
        deleteOnRollback(photoKey);

        NewFoundItem data = new NewFoundItem(category, request.block(), request.room(), request.foundOn(),
                request.storageBox(), request.risk(), request.internalDescription(), request.reservedDetails());
        FoundItem item = items.save(FoundItem.register(data, photoKey, registeredBy, clock.instant(),
                LocalDate.now(clock)));

        events.publish(new AuditEvent(registeredBy, REGISTERED_ACTION, FoundItem.class.getSimpleName(),
                item.getId(), REGISTERED_REASON));
        events.publish(new FoundItemRegisteredEvent(item.getId(), category.getId(), item.getFoundOn()));
        return item;
    }

    /**
     * Corrige os dados do objeto (ACH-07), com o motivo e o que mudou no histórico (RF43). Só Disponível e Não
     * reclamado: Reservado, Devolvido e Destinado respondem 409. A {@code version} que o funcionário viu precisa ser a
     * atual, senão 409, e a gravação é feita já aqui dentro: se uma convocação reservar o objeto ao mesmo tempo, quem
     * grava por último perde no {@code @Version} e nada é gravado. Se nada mudou, nada é gravado nem publicado.
     *
     * @param staffId id da conta do funcionário (claim {@code sub} do token)
     */
    @Transactional
    public FoundItem update(UUID id, UpdateFoundItem request, UUID staffId) {
        FoundItem item = items.findWithCategoryById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
        if (item.getVersion() != request.version()) {
            throw new ObjectOptimisticLockingFailureException(FoundItem.class, id);
        }
        Category category = categories.findById(request.categoryId())
                .orElseThrow(() -> new BadRequestException(UNKNOWN_CATEGORY_MESSAGE));

        List<String> changes = item.update(category, request.block(), request.room(), request.foundOn(),
                request.storageBox(), request.risk(), request.internalDescription(), request.reservedDetails(),
                LocalDate.now(clock));
        if (changes.isEmpty()) {
            return item;
        }
        items.saveAndFlush(item);
        events.publish(new AuditEvent(staffId, UPDATED_ACTION, FoundItem.class.getSimpleName(), id,
                reasonOf(request.reason(), changes)));
        events.publish(new FoundItemUpdatedEvent(id, category.getId(), item.getFoundOn()));
        return item;
    }

    /** O motivo do funcionário e as mudanças, cabendo no limite do histórico. */
    private static String reasonOf(String reason, List<String> changes) {
        String text = reason.strip() + " Alterado: " + String.join("; ", changes) + ".";
        return text.length() <= AuditEvent.MAX_REASON_LENGTH
                ? text : text.substring(0, AuditEvent.MAX_REASON_LENGTH - 1) + "…";
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
