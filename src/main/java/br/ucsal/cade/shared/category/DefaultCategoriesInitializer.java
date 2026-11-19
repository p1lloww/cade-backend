package br.ucsal.cade.shared.category;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cria na subida as categorias mais comuns na sala (S8 dos requisitos), com o risco da RN09. Pode rodar em toda
 * subida: só cria as que faltam e nunca altera uma categoria que já existe, para não desfazer o que o administrador
 * mudar (ADM-04).
 */
@Component
class DefaultCategoriesInitializer implements ApplicationRunner {

    static final Map<String, RiskLevel> DEFAULTS = defaults();

    private final CategoryRepository categories;

    DefaultCategoriesInitializer(CategoryRepository categories) {
        this.categories = categories;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        DEFAULTS.forEach((name, risk) -> {
            if (categories.findByNameIgnoreCase(name).isEmpty()) {
                categories.save(new Category(name, risk));
            }
        });
    }

    private static Map<String, RiskLevel> defaults() {
        Map<String, RiskLevel> defaults = new LinkedHashMap<>();
        defaults.put("Celular", RiskLevel.HIGH);
        defaults.put("Notebook", RiskLevel.HIGH);
        defaults.put("Carteira", RiskLevel.HIGH);
        defaults.put("Chaves", RiskLevel.HIGH);
        defaults.put("Mochila", RiskLevel.MEDIUM);
        defaults.put("Casaco", RiskLevel.MEDIUM);
        defaults.put("Fone", RiskLevel.MEDIUM);
        defaults.put("Carregador", RiskLevel.MEDIUM);
        defaults.put("Óculos", RiskLevel.MEDIUM);
        defaults.put("Garrafa", RiskLevel.LOW);
        defaults.put("Guarda-chuva", RiskLevel.LOW);
        defaults.put("Caderno", RiskLevel.LOW);
        defaults.put("Outros", RiskLevel.LOW);
        defaults.put("Documentos", RiskLevel.DOCUMENT);
        return Collections.unmodifiableMap(defaults);
    }
}
