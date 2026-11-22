package br.ucsal.cade.pickup.persistence.walkin;

/** Quem retira pelo atalho de balcão (RF32): um estudante ou uma pessoa sem conta institucional (RN20). */
public enum HolderType {

    STUDENT("Estudante", true),

    VISITOR("Pessoa sem conta", false);

    private final String label;
    private final boolean requiresEnrollment;

    HolderType(String label, boolean requiresEnrollment) {
        this.label = label;
        this.requiresEnrollment = requiresEnrollment;
    }

    public String label() {
        return label;
    }

    /** Para estudantes, a matrícula também é registrada (RN10); quem não tem conta não tem matrícula. */
    public boolean requiresEnrollment() {
        return requiresEnrollment;
    }
}
