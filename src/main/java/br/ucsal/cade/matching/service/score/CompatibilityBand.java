package br.ucsal.cade.matching.service.score;

/** Faixa da compatibilidade (RN07). Sugestões baixas não aparecem por padrão. */
public enum CompatibilityBand {

    HIGH("Alta"),
    MEDIUM("Média"),
    LOW("Baixa");

    private final String label;

    CompatibilityBand(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean isShownByDefault() {
        return this != LOW;
    }
}
