package br.ucsal.cade.shared.category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * Categoria de objeto (fone, celular, documentos...), com o risco padrão (RN04). Fica no núcleo compartilhado porque
 * objetos achados, perdas e o cruzamento usam a mesma lista, e esses módulos não podem depender um do outro (D21).
 * O administrador gerencia as categorias no ADM-04; até lá, valem as categorias iniciais.
 */
@Entity
@Table(name = "categories")
public class Category {

    public static final int MAX_NAME_LENGTH = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = MAX_NAME_LENGTH)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskLevel defaultRisk;

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
}
