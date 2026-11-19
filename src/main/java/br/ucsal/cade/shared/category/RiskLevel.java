package br.ucsal.cade.shared.category;

/**
 * Risco de um objeto, que define o que o funcionário confere no balcão (RN09). A categoria dá o risco padrão, e o
 * funcionário pode aumentar o risco de um objeto específico, nunca reduzir (RN04).
 *
 * <p>Os três primeiros formam uma escala ({@code LOW < MEDIUM < HIGH}). {@code DOCUMENT} fica fora dela: documento é
 * entregue só ao titular, então o risco de um documento nunca muda e nenhum outro objeto vira documento.
 */
public enum RiskLevel {

    /** Guarda-chuva, garrafa, caderno: documento com foto e descrição coerente. */
    LOW("Baixo"),

    /** Mochila, casaco, fone, carregador, óculos: documento com foto e um detalhe que não aparece na foto. */
    MEDIUM("Médio"),

    /** Celular, notebook, carteira, chaves: documento com foto e desbloqueio na frente do funcionário, ou prova. */
    HIGH("Alto"),

    /** RG, carteirinha, cartão: entregue só ao titular, conferindo a foto. */
    DOCUMENT("Documento");

    private final String label;

    RiskLevel(String label) {
        this.label = label;
    }

    /** Nome do risco para o usuário, como na RN09. */
    public String label() {
        return label;
    }

    /** Se um objeto com este risco padrão pode passar a ter o risco {@code other} (RN04). */
    public boolean canBeRaisedTo(RiskLevel other) {
        if (this == DOCUMENT || other == DOCUMENT) {
            return this == other;
        }
        return other.ordinal() >= ordinal();
    }
}
