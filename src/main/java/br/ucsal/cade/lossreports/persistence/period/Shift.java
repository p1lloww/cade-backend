package br.ucsal.cade.lossreports.persistence.period;

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
