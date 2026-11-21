package br.ucsal.cade.pickup.persistence.pickup;

/** O documento com foto apresentado no balcão (RN10). */
public enum IdDocumentType {

    RG("RG"),

    CNH("CNH"),

    STUDENT_CARD("Carteirinha da UCSal"),

    PASSPORT("Passaporte"),

    OTHER("Outro documento com foto");

    private final String label;

    IdDocumentType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
