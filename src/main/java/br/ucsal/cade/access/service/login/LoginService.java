package br.ucsal.cade.access.service.login;

import br.ucsal.cade.access.api.event.LoginCodeRequestedEvent;
import br.ucsal.cade.access.persistence.logincode.LoginCode;
import br.ucsal.cade.access.persistence.logincode.LoginCodeStore;
import br.ucsal.cade.access.persistence.user.User;
import br.ucsal.cade.access.persistence.user.UserRepository;
import br.ucsal.cade.access.service.user.InstitutionalEmailPolicy;
import br.ucsal.cade.shared.exception.BadRequestException;
import br.ucsal.cade.shared.exception.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Passwordless login (RF01, D02, D03): sends a one-time code to an institutional e-mail and exchanges a valid code
 * for the account. Issuing the access token is not done here.
 */
@Service
public class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private static final String INVALID_CODE_MESSAGE =
            "Código inválido ou expirado. Confira o código ou solicite um novo.";

    private final LoginCodeStore codes;
    private final UserRepository users;
    private final InstitutionalEmailPolicy emailPolicy;
    private final LoginCodeGenerator generator;
    private final PasswordEncoder encoder;
    private final LoginCodeProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    LoginService(LoginCodeStore codes, UserRepository users, InstitutionalEmailPolicy emailPolicy,
                 LoginCodeGenerator generator, PasswordEncoder encoder, LoginCodeProperties properties,
                 ApplicationEventPublisher events, Clock clock) {
        this.codes = codes;
        this.users = users;
        this.emailPolicy = emailPolicy;
        this.generator = generator;
        this.encoder = encoder;
        this.properties = properties;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Issues a new code and publishes {@link LoginCodeRequestedEvent} so it is e-mailed. The new code replaces any
     * previous one of the same e-mail.
     *
     * @throws BadRequestException      if the e-mail is not institutional
     * @throws TooManyRequestsException if the last code was issued less than the resend interval ago
     */
    public void requestCode(String rawEmail) {
        String email = User.normalize(rawEmail);
        if (!emailPolicy.accepts(email)) {
            throw new BadRequestException("Use o seu e-mail institucional @" + emailPolicy.domain() + ".");
        }
        if (!codes.startCooldown(email, properties.resendInterval())) {
            throw new TooManyRequestsException(
                    "Um código acabou de ser enviado. Aguarde um minuto para pedir outro.",
                    codes.cooldownRemaining(email));
        }

        String code = generator.generate();
        LoginCode issued = LoginCode.issue(email, encoder.encode(code), clock.instant(), properties.ttl());
        codes.save(issued, properties.ttl());
        events.publishEvent(new LoginCodeRequestedEvent(email, code, issued.expiresAt()));
    }

    /**
     * Checks the code typed by the user and returns the account, creating it as a student on the first login. On
     * success, the code and the resend waiting time of the e-mail are deleted.
     *
     * <p>The attempt is counted before the comparison and the code is deleted by a single Redis command, so
     * parallel requests can neither go past the attempt limit nor use the same code twice.
     *
     * @throws BadRequestException with the same message for every kind of failure, so an attacker cannot tell a
     *                             wrong code from an expired or locked one
     */
    @Transactional
    public User exchangeCode(String rawEmail, String rawCode) {
        String email = User.normalize(rawEmail);
        LoginCode code = codes.find(email).orElseThrow(() -> refused("no code: never requested, expired or used"));

        long attempt = codes.registerAttempt(email);
        if (attempt < 0) {
            throw refused("code expired during the check");
        }
        if (attempt > properties.maxAttempts()) {
            throw refused("too many attempts");
        }
        if (!code.matches(rawCode, encoder)) {
            throw refused("wrong code");
        }
        if (!codes.consume(email)) {
            throw refused("code already used by a concurrent request");
        }
        return users.findByEmail(email)
                .orElseGet(() -> users.save(User.newStudent(email, clock.instant())));
    }

    private static BadRequestException refused(String reason) {
        log.info("Login code refused: {}", reason);
        return new BadRequestException(INVALID_CODE_MESSAGE);
    }
}
