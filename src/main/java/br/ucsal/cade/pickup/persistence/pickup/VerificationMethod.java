package br.ucsal.cade.pickup.persistence.pickup;

/** A verificação que o funcionário fez no balcão, além do documento com foto (RN09, RN10). */
public enum VerificationMethod {

    DESCRIPTION_MATCHED("Descrição coerente com o objeto"),

    DETAIL_CONFIRMED("Detalhe que não aparece na foto, ou o detalhe reservado"),

    DEVICE_UNLOCKED("Desbloqueio do aparelho na frente do funcionário"),

    PROOF_OF_OWNERSHIP("Prova de que o objeto é dele (nota fiscal ou número de série)"),

    HOLDER_PHOTO_MATCHED("Foto do documento achado confere com quem retira");

    private final String label;

    VerificationMethod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
