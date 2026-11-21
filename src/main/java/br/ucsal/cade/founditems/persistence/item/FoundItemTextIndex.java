package br.ucsal.cade.founditems.persistence.item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Cria o índice full-text da busca de objetos (RF10, RNF02). O {@code ddl-auto} não cria índice GIN (D06), então ele é
 * criado na subida, depois das tabelas; o {@code IF NOT EXISTS} deixa rodar a cada subida (D27).
 */
@Component
class FoundItemTextIndex implements ApplicationRunner {

    static final String NAME = "found_items_text_idx";

    private static final Logger log = LoggerFactory.getLogger(FoundItemTextIndex.class);

    private final JdbcTemplate jdbc;

    FoundItemTextIndex(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        // O índice não aceita o apelido "f." da consulta; sem ele, a expressão é a mesma para o PostgreSQL.
        jdbc.execute("CREATE INDEX IF NOT EXISTS " + NAME + " ON found_items USING GIN ("
                + FoundItemRepository.TEXT_VECTOR.replace("f.", "") + ")");
        log.debug("Full-text index {} ready", NAME);
    }
}
