package br.ucsal.cade.shared.category;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Categoria de objeto (fone, celular, documentos...), com o risco padrão (RN04). Fica no núcleo compartilhado porque
 * objetos achados, perdas e o cruzamento usam a mesma lista, e esses módulos não podem depender um do outro (D21).
 * O administrador gerencia as categorias (ADM-04, RF44): nome, risco padrão e perguntas de conferência.
 */
@Entity
@Table(name = "categories")
public class Category {

    public static final int MAX_NAME_LENGTH = 60;
    public static final int MAX_QUESTIONS = 10;
    public static final int MAX_QUESTION_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = MAX_NAME_LENGTH)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskLevel defaultRisk;

    /** Perguntas que o funcionário faz no balcão para conferir que o objeto é de quem pede (RF44). Na ordem dada. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "category_verification_questions", joinColumns = @JoinColumn(name = "category_id"))
    @OrderColumn(name = "position")
    @Column(name = "question", nullable = false, length = MAX_QUESTION_LENGTH)
    private List<String> verificationQuestions = new ArrayList<>();

    protected Category() {
    }

    public Category(String name, RiskLevel defaultRisk) {
        this.name = Objects.requireNonNull(name).strip();
        this.defaultRisk = Objects.requireNonNull(defaultRisk);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public RiskLevel getDefaultRisk() {
        return defaultRisk;
    }

    public List<String> getVerificationQuestions() {
        return List.copyOf(verificationQuestions);
    }

    /**
     * Muda o nome, o risco padrão e as perguntas de conferência. O risco novo vale para os próximos objetos: os que já
     * estão na sala guardam o risco que tinham (RN04).
     */
    public void change(String name, RiskLevel defaultRisk, List<String> verificationQuestions) {
        this.name = Objects.requireNonNull(name).strip();
        this.defaultRisk = Objects.requireNonNull(defaultRisk);
        this.verificationQuestions.clear();
        this.verificationQuestions.addAll(verificationQuestions);
    }
}
