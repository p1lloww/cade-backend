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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.OptionalInt;

/**
 * Passwordless login (RF01, D02, D03): sends a one-time code to an institutional e-mail and exchanges a valid code
 * for the account. Issuing the access token is not done here.
 *
 * <p>No method here opens a transaction: each step is a single atomic statement of {@link LoginCodeStore}, and a
 * refused attempt must stay counted even though the method ends with an exception (D18).
 */
@Service
public class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private static final String INVALID_CODE_MESSAGE =
            "Código inválido ou expirado. Confira o código ou solicite um novo.";
    private static final String RESEND_MESSAGE =
            "Um código acabou de ser enviado. Aguarde um minuto para pedir outro.";
    private static final String BLOCKED_MESSAGE =
            "Muitas tentativas com código errado. Aguarde alguns minutos para pedir um novo código.";

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
     * @throws TooManyRequestsException if the last code was issued less than the resend interval ago, or the e-mail
     *                                  is blocked after too many wrong attempts
     */
    public void requestCode(String rawEmail) {
        String email = User.normalize(rawEmail);
        if (!emailPolicy.accepts(email)) {
            throw new BadRequestException("Use o seu e-mail institucional @" + emailPolicy.domain() + ".");
        }
        Instant now = clock.instant();
        // Cheap check first, so a refused request does not pay for the BCrypt hash. The save below is the one that
        // decides when two requests arrive together.
        codes.find(email).ifPresent(current -> refuseIfWaiting(current, now));

        String code = generator.generate();
        LoginCode issued = LoginCode.issue(email, encoder.encode(code), now, properties.ttl());
        if (!codes.save(issued, properties.resendInterval())) {
            codes.find(email).ifPresent(current -> refuseIfWaiting(current, now));
            throw new TooManyRequestsException(RESEND_MESSAGE, properties.resendInterval());
        }
        events.publishEvent(new LoginCodeRequestedEvent(email, code, issued.expiresAt()));
    }

    /**
     * Checks the code typed by the user and returns the account, creating it as a student on the first login. On
     * success, the code and the wrong attempts of the e-mail are deleted.
     *
     * <p>The attempt is counted before the comparison and the code is deleted by a single statement, so parallel
     * requests can neither go past the attempt limit nor use the same code twice. Wrong attempts add up across
     * codes, and too many of them block the e-mail for a while (D18).
     *
     * @throws BadRequestException with the same message for every kind of failure, so an attacker cannot tell a
     *                             wrong code from an expired or locked one
     */
    public User exchangeCode(String rawEmail, String rawCode) {
        String email = User.normalize(rawEmail);
        Instant now = clock.instant();
        LoginCode code = codes.find(email)
                .filter(current -> current.isValidAt(now))
                .orElseThrow(() -> refused("no valid code: never requested, expired, used or blocked"));

        OptionalInt attempt = codes.registerAttempt(email, now);
        if (attempt.isEmpty()) {
            throw refused("code expired or replaced during the check");
        }
        if (attempt.getAsInt() > properties.maxAttempts()) {
            throw refused("too many attempts");
        }
        if (!code.matches(rawCode, encoder)) {
            if (codes.registerFailure(email, now, properties.maxFailures(), properties.blockDuration())) {
                log.warn("E-mail blocked for {} after {} wrong login attempts.",
                        properties.blockDuration(), properties.maxFailures());
            }
            throw refused("wrong code");
        }
        if (!codes.consume(email, code.codeHash())) {
            throw refused("code already used by a concurrent request");
        }
        return users.findByEmail(email)
                .orElseGet(() -> users.save(User.newStudent(email, now)));
    }

    private void refuseIfWaiting(LoginCode current, Instant now) {
        if (current.isBlockedAt(now)) {
            throw new TooManyRequestsException(BLOCKED_MESSAGE, Duration.between(now, current.blockedUntil()));
        }
        Instant resendAllowedAt = current.createdAt().plus(properties.resendInterval());
        if (now.isBefore(resendAllowedAt)) {
            throw new TooManyRequestsException(RESEND_MESSAGE, Duration.between(now, resendAllowedAt));
        }
    }

    private static BadRequestException refused(String reason) {
        log.info("Login code refused: {}", reason);
        return new BadRequestException(INVALID_CODE_MESSAGE);
    }
}
