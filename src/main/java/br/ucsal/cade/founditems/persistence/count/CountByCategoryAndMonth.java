package br.ucsal.cade.founditems.persistence.count;

/** Uma linha da contagem por categoria e mês; o mês vem como {@code yyyy-MM}. */
public interface CountByCategoryAndMonth {

    String getCategory();

    String getMonth();

    long getTotal();
}
