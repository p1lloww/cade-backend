package br.ucsal.cade.administration.service.category;

import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.event.EventPublisher;
import br.ucsal.cade.shared.exception.BadRequestException;
import br.ucsal.cade.shared.exception.ConflictException;
import br.ucsal.cade.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * O administrador gerencia as categorias: nome, risco padrão e perguntas de conferência (RF44). Não há exclusão: os
 * objetos e as perdas já registrados apontam para a categoria. O risco novo vale para os próximos objetos (RN04).
 */
@Service
public class CategoryAdminService {

    static final String CREATED_ACTION = "CATEGORY_CREATED";
    static final String UPDATED_ACTION = "CATEGORY_UPDATED";
    static final String DUPLICATE_NAME_MESSAGE = "Já existe uma categoria com esse nome.";

    private static final Logger log = LoggerFactory.getLogger(CategoryAdminService.class);

    private final CategoryRepository categories;
    private final EventPublisher events;

    CategoryAdminService(CategoryRepository categories, EventPublisher events) {
        this.categories = categories;
        this.events = events;
    }

    @Transactional
    public Category create(String name, RiskLevel defaultRisk, List<String> questions, UUID adminId) {
        String cleanName = cleanName(name);
        requireFreeName(cleanName, null);
        Category category = new Category(cleanName, Objects.requireNonNull(defaultRisk));
        category.change(cleanName, defaultRisk, cleanQuestions(questions));
        Category saved = save(category);
        events.publish(new AuditEvent(adminId, CREATED_ACTION, Category.class.getSimpleName(), saved.getId(),
                "Categoria criada: " + cleanName + ", risco " + defaultRisk.label() + "."));
        log.info("Category {} created by {}", saved.getId(), adminId);
        return saved;
    }

    @Transactional
    public Category update(UUID id, String name, RiskLevel defaultRisk, List<String> questions, UUID adminId) {
        Category category = categories.findById(id).orElseThrow(() -> new NotFoundException("Categoria não encontrada."));
        String cleanName = cleanName(name);
        requireFreeName(cleanName, id);
        List<String> cleanQuestions = cleanQuestions(questions);

        List<String> changes = new ArrayList<>();
        if (!category.getName().equals(cleanName)) {
            changes.add("nome " + category.getName() + " → " + cleanName);
        }
        if (category.getDefaultRisk() != defaultRisk) {
            changes.add("risco " + category.getDefaultRisk().label() + " → " + defaultRisk.label());
        }
        if (!category.getVerificationQuestions().equals(cleanQuestions)) {
            changes.add("perguntas de conferência (" + cleanQuestions.size() + ")");
        }
        if (changes.isEmpty()) {
            return category;
        }
        category.change(cleanName, defaultRisk, cleanQuestions);
        Category saved = save(category);
        events.publish(new AuditEvent(adminId, UPDATED_ACTION, Category.class.getSimpleName(), id,
                "Categoria alterada: " + String.join("; ", changes) + "."));
        log.info("Category {} changed by {}: {}", id, adminId, changes);
        return saved;
    }

    private Category save(Category category) {
        try {
            return categories.saveAndFlush(category);
        } catch (DataIntegrityViolationException e) {
            // Dois administradores criando o mesmo nome ao mesmo tempo: a restrição de unicidade do banco decide.
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }
    }

    private void requireFreeName(String name, UUID currentId) {
        categories.findByNameIgnoreCase(name).filter(other -> !other.getId().equals(currentId)).ifPresent(other -> {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        });
    }

    private static String cleanName(String name) {
        String clean = name == null ? "" : name.strip();
        if (clean.isEmpty()) {
            throw new BadRequestException("Informe o nome da categoria.");
        }
        return clean;
    }

    private static List<String> cleanQuestions(List<String> questions) {
        if (questions == null) {
            return List.of();
        }
        if (questions.size() > Category.MAX_QUESTIONS) {
            throw new BadRequestException("Uma categoria pode ter no máximo " + Category.MAX_QUESTIONS
                    + " perguntas de conferência.");
        }
        List<String> clean = new ArrayList<>();
        for (String question : questions) {
            String text = question == null ? "" : question.strip();
            if (text.isEmpty()) {
                throw new BadRequestException("Há uma pergunta de conferência em branco.");
            }
            clean.add(text);
        }
        return clean;
    }
}
