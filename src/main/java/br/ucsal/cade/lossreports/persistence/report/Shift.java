package br.ucsal.cade.lossreports.persistence.report;

/** Turno em que o estudante acha que perdeu o objeto (RF13). */
public enum Shift {

    MORNING("Manhã"),
    AFTERNOON("Tarde"),
    EVENING("Noite");

    private final String label;

    Shift(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
