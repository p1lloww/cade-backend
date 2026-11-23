package br.ucsal.cade.matching.service.score.ai;

import java.util.regex.Pattern;

/**
 * Tira de um texto o que identifica uma pessoa antes de ele ir a um serviço de fora (RNF04): e-mails e números longos
 * (telefone, matrícula, documento). O que descreve o objeto ("fone preto, adesivo de estrela") passa.
 */
public final class PersonalDataRedactor {

    public static final String PLACEHOLDER = "[removido]";

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    /** Cinco dígitos ou mais, com os separadores comuns de telefone e documento no meio. */
    private static final Pattern LONG_NUMBER = Pattern.compile("\\+?\\d(?:[\\d .()/-]*\\d){4,}");

    private PersonalDataRedactor() {
    }

    public static String redact(String text) {
        if (text == null) {
            return "";
        }
        String withoutEmails = EMAIL.matcher(text).replaceAll(PLACEHOLDER);
        return LONG_NUMBER.matcher(withoutEmails).replaceAll(PLACEHOLDER).strip();
    }
}
