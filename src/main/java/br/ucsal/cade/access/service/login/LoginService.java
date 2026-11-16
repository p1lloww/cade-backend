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
 * Login sem senha (RF01, D02, D03): envia um código de uso único a um e-mail institucional e troca um código válido
 * pela conta. A emissão do token de acesso não é feita aqui.
 *
 * <p>Nenhum método daqui abre transação: cada passo é um único comando atômico do {@link LoginCodeStore}, e uma
 * tentativa recusada precisa continuar contada mesmo que o método termine com exceção (D18).
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
     * Emite um código novo e publica o {@link LoginCodeRequestedEvent} para ele ser enviado por e-mail. O código novo
     * substitui qualquer código anterior do mesmo e-mail.
     *
     * @throws BadRequestException      se o e-mail não é institucional
     * @throws TooManyRequestsException se o último código foi emitido há menos que o intervalo de reenvio, ou se o
     *                                  e-mail está bloqueado por tentativas erradas demais
     */
    public void requestCode(String rawEmail) {
        String email = User.normalize(rawEmail);
        if (!emailPolicy.accepts(email)) {
            throw new BadRequestException("Use o seu e-mail institucional @" + emailPolicy.domain() + ".");
        }
        Instant now = clock.instant();
        // Conferência barata primeiro, para uma requisição recusada não pagar o hash do BCrypt. Quem decide quando duas
        // requisições chegam juntas é o save logo abaixo.
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
     * Confere o código digitado pelo usuário e devolve a conta, criando-a como estudante no primeiro login. Em caso de
     * sucesso, o código e as tentativas erradas do e-mail são apagados.
     *
     * <p>A tentativa é contada antes da comparação e o código é apagado por um único comando, então requisições em
     * paralelo não furam o limite de tentativas nem usam o mesmo código duas vezes. As tentativas erradas somam de
     * um código para o outro, e erros demais bloqueiam o e-mail por um tempo (D18).
     *
     * @throws BadRequestException com a mesma mensagem para todo tipo de falha, para quem ataca não distinguir um
     *                             código errado de um vencido ou travado
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
