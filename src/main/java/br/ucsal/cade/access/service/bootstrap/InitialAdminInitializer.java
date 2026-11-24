package br.ucsal.cade.access.service.bootstrap;

import br.ucsal.cade.access.persistence.user.User;
import br.ucsal.cade.access.persistence.user.UserRepository;
import br.ucsal.cade.access.service.user.InstitutionalEmailPolicy;
import br.ucsal.cade.shared.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Cria o administrador na subida a partir da variável {@code ADMIN_EMAIL} (requisitos, seção 2). Pode rodar em toda
 * subida: se a conta já existe, só garante que ela seja administradora.
 */
@Component
class InitialAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminInitializer.class);

    private final UserRepository users;
    private final Clock clock;
    private final InstitutionalEmailPolicy emailPolicy;
    private final String adminEmail;

    InitialAdminInitializer(UserRepository users, Clock clock, InstitutionalEmailPolicy emailPolicy,
                            @Value("${cade.initial-admin.email:}") String adminEmail) {
        this.users = users;
        this.clock = clock;
        this.emailPolicy = emailPolicy;
        this.adminEmail = adminEmail;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("ADMIN_EMAIL is not set: no administrator was created.");
            return;
        }
        String email = User.normalize(adminEmail);
        if (!emailPolicy.accepts(email)) {
            throw new IllegalStateException("ADMIN_EMAIL must be an @" + emailPolicy.domain()
                    + " address, because login works through a code sent to that address.");
        }
        users.findByEmail(email).ifPresentOrElse(user -> {
            if (!user.hasRole(Role.ADMIN)) {
                user.promoteToAdmin();
                users.save(user);
                log.info("Existing account promoted to administrator.");
            }
        }, () -> {
            users.save(User.newAdmin(email, clock.instant()));
            log.info("Initial administrator created.");
        });
    }
}
