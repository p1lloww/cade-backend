package br.ucsal.cade.matching.service.score;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Pesos do application.yml de verdade e pares objeto × perda para os testes. */
final class Candidates {

    static final UUID FONE = UUID.randomUUID();
    static final UUID CELULAR = UUID.randomUUID();
    static final LocalDate DAY = LocalDate.of(2026, 11, 4);

    private Candidates() {
    }

    static MatchingProperties properties() {
        try {
            List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                    .load("application", new ClassPathResource("application.yml"));
            return new Binder(ConfigurationPropertySources.from(sources))
                    .bindOrCreate("cade.matching", MatchingProperties.class);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    static FoundItemProfile found(LocalDate foundOn, String block, String room, String description) {
        return new FoundItemProfile(FONE, "Fone", foundOn, new Place(block, room), description);
    }

    static LossReportProfile lost(LocalDate from, LocalDate until, String description, Place... places) {
        return new LossReportProfile(FONE, from, until, Arrays.asList(places), description);
    }

    static MatchCandidate candidate(FoundItemProfile found, LossReportProfile lost) {
        return new MatchCandidate(found, lost);
    }
}
