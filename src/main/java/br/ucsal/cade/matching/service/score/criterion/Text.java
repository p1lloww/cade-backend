package br.ucsal.cade.matching.service.score.criterion;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Normalização de texto para comparar o que o funcionário e o estudante digitaram. */
final class Text {

    /** Palavras sem informação sobre o objeto. */
    static final Set<String> STOP_WORDS = Set.of("com", "sem", "para", "por", "pelo", "pela", "dos", "das", "nos",
            "nas", "num", "numa", "uma", "umas", "uns", "que", "mas", "meu", "minha", "meus", "minhas", "seu", "sua",
            "este", "esta", "esse", "essa", "isso", "ele", "ela", "tem", "foi", "era", "cor", "tipo", "parte", "lado");

    private Text() {
    }

    /** Minúsculas, sem acento e com os espaços reduzidos a um só. */
    static String normalize(String text) {
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).strip().replaceAll("\\s+", " ");
    }

    /** As palavras distintas do texto, sem as curtas, as vazias e as de {@code ignored}. */
    static Set<String> words(String text, int minLength, Set<String> ignored) {
        if (text == null) {
            return Set.of();
        }
        return Arrays.stream(normalize(text).split("[^\\p{Alnum}]+"))
                .filter(word -> word.length() >= minLength)
                .filter(word -> !STOP_WORDS.contains(word))
                .filter(word -> !ignored.contains(word))
                .collect(Collectors.toUnmodifiableSet());
    }
}
