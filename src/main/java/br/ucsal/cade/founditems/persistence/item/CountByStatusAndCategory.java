package br.ucsal.cade.founditems.persistence.item;

/** Uma linha da contagem por situação e categoria; a situação vem como o código gravado. */
public interface CountByStatusAndCategory {

    String getStatus();

    String getCategory();

    long getTotal();
}
