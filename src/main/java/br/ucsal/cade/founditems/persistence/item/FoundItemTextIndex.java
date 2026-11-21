package br.ucsal.cade.founditems.persistence.item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Prepara o full-text da busca de objetos (RF10, RNF02): a extensão {@code unaccent}, a configuração de busca
 * {@code cade_pt} (o {@code portuguese} sem acentos) e o índice GIN. Sem Flyway (D06), e como o {@code ddl-auto} não
 * cria nada disso, tudo é criado na subida, depois das tabelas, e só se ainda não existe (D27).
 */
@Component
class FoundItemTextIndex implements ApplicationRunner {

    static final String NAME = "found_items_text_idx";

    /** O {@code unaccent} tira os acentos antes de o {@code portuguese_stem} reduzir a palavra. */
    private static final String CREATE_CONFIG = """
            DO $$
            BEGIN
                IF NOT EXISTS (SELECT 1 FROM pg_ts_config WHERE cfgname = '%1$s') THEN
                    CREATE TEXT SEARCH CONFIGURATION %1$s (COPY = portuguese);
                    ALTER TEXT SEARCH CONFIGURATION %1$s
                        ALTER MAPPING FOR hword, hword_part, word WITH unaccent, portuguese_stem;
                END IF;
            END $$""".formatted(FoundItemRepository.TEXT_CONFIG);

    private static final Logger log = LoggerFactory.getLogger(FoundItemTextIndex.class);

    private final JdbcTemplate jdbc;

    FoundItemTextIndex(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS unaccent");
        jdbc.execute(CREATE_CONFIG);
        // O índice não aceita o apelido "f." da consulta; sem ele, a expressão é a mesma para o PostgreSQL.
        jdbc.execute("CREATE INDEX IF NOT EXISTS " + NAME + " ON found_items USING GIN ("
                + FoundItemRepository.TEXT_VECTOR.replace("f.", "") + ")");
        log.debug("Full-text search ready: config {}, index {}", FoundItemRepository.TEXT_CONFIG, NAME);
    }
}
