package br.ucsal.cade.access.service.login;

import br.ucsal.cade.access.api.event.LoginCodeRequestedEvent;
import br.ucsal.cade.access.persistence.logincode.LoginCode;
import br.ucsal.cade.access.persistence.logincode.LoginCodeRepository;
import br.ucsal.cade.access.persistence.logincode.LoginCodeVerification;
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
import java.time.Duration;
import java.time.Instant;

/**
 * Passwordless login (RF01, D02, D03): sends a one-time code to an institutional e-mail and exchanges a valid code
 * for the account. Issuing the access token is not done here.
 */
@Service
public class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private static final String INVALID_CODE_MESSAGE =
            "Código inválido ou expirado. Confira o código ou solicite um novo.";

    private final LoginCodeRepository codes;
    private final UserRepository users;
    private final InstitutionalEmailPolicy emailPolicy;
    private final LoginCodeGenerator generator;
    private final PasswordEncoder encoder;
    private final LoginCodeProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    LoginService(LoginCodeRepository codes, UserRepository users, InstitutionalEmailPolicy emailPolicy,
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
     * @throws BadRequestException       if the e-mail is not institutional
     * @throws TooManyRequestsException  if the last code was issued less than the resend interval ago
     */
    @Transactional
    public void requestCode(String rawEmail) {
        String email = User.normalize(rawEmail);
        if (!emailPolicy.accepts(email)) {
            throw new BadRequestException("Use o seu e-mail institucional @" + emailPolicy.domain() + ".");
        }
        Instant now = clock.instant();
        codes.findFirstByEmailOrderByCreatedAtDesc(email)
                .filter(last -> last.wasIssuedWithin(properties.resendInterval(), now))
                .ifPresent(last -> {
                    Duration wait = Duration.between(now, last.getCreatedAt().plus(properties.resendInterval()));
                    throw new TooManyRequestsException(
                            "Um código acabou de ser enviado. Aguarde um minuto para pedir outro.", wait);
                });

        String code = generator.generate();
        LoginCode saved = codes.save(LoginCode.issue(email, encoder.encode(code), now, properties.ttl()));
        events.publishEvent(new LoginCodeRequestedEvent(email, code, saved.getExpiresAt()));
    }

    /**
     * Checks the code typed by the user and returns the account, creating it as a student on the first login.
     *
     * <p>{@code noRollbackFor}: a wrong code throws, but the attempt counted by {@link LoginCode#verify} must still
     * be committed; otherwise the rollback would undo it and the attempt limit would never be reached.
     *
     * @throws BadRequestException with the same message for every kind of failure, so an attacker cannot tell a
     *                             wrong code from an expired or locked one
     */
    @Transactional(noRollbackFor = BadRequestException.class)
    public User exchangeCode(String rawEmail, String rawCode) {
        String email = User.normalize(rawEmail);
        Instant now = clock.instant();
        LoginCode code = codes.findFirstByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BadRequestException(INVALID_CODE_MESSAGE));

        LoginCodeVerification result = code.verify(rawCode, encoder, now, properties.maxAttempts());
        if (result != LoginCodeVerification.VALID) {
            log.info("Login code refused: {}", result);
            throw new BadRequestException(INVALID_CODE_MESSAGE);
        }
        return users.findByEmail(email)
                .orElseGet(() -> users.save(User.newStudent(email, now)));
    }
}
