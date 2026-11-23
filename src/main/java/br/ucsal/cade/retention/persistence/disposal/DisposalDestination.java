package br.ucsal.cade.retention.persistence.disposal;

/** Para onde vai um objeto não reclamado (RF38). O bazar é o destino padrão, validado com o administrador (A2). */
public enum DisposalDestination {

    BAZAAR("Bazar"),
    DISCARD("Descarte"),
    OTHER("Outro");

    private final String label;

    DisposalDestination(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
