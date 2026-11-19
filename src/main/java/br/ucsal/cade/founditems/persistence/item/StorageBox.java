package br.ucsal.cade.founditems.persistence.item;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Caixa do mês onde o objeto fica guardado na sala (RF05). A sala já organiza os objetos em caixas por mês (S1), então
 * o sistema sugere a caixa pela data e o funcionário pode trocar.
 */
public final class StorageBox {

    public static final int MAX_LENGTH = 40;

    private static final String[] MONTHS = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho",
            "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};

    private StorageBox() {
    }

    /** A caixa do mês da data, por exemplo {@code Caixa Novembro/2026}. */
    public static String suggestedFor(LocalDate date) {
        Objects.requireNonNull(date);
        return "Caixa " + MONTHS[date.getMonthValue() - 1] + "/" + date.getYear();
    }
}
