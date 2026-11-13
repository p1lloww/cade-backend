package br.ucsal.cade.access.service;

import br.ucsal.cade.access.persistence.User;
import br.ucsal.cade.access.persistence.UserRepository;
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
 * Creates the administrator on startup from the {@code ADMIN_EMAIL} variable (requirements, section 2).
 * Safe to run on every startup: if the account already exists, it only makes sure it is an administrator.
 */
@Component
class InitialAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminInitializer.class);

    private final UserRepository users;
    private final Clock clock;
    private final String adminEmail;
    private final String allowedDomain;

    InitialAdminInitializer(UserRepository users, Clock clock,
                            @Value("${cade.initial-admin.email:}") String adminEmail,
                            @Value("${cade.allowed-email-domain}") String allowedDomain) {
        this.users = users;
        this.clock = clock;
        this.adminEmail = adminEmail;
        this.allowedDomain = allowedDomain;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("ADMIN_EMAIL is not set: no administrator was created.");
            return;
        }
        String email = User.normalize(adminEmail);
        if (!email.endsWith("@" + allowedDomain)) {
            throw new IllegalStateException("ADMIN_EMAIL must be an @" + allowedDomain
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
