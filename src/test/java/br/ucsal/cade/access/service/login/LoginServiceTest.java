package br.ucsal.cade.access.service.login;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.access.api.event.LoginCodeRequestedEvent;
import br.ucsal.cade.access.persistence.logincode.LoginCode;
import br.ucsal.cade.access.persistence.logincode.LoginCodeRepository;
import br.ucsal.cade.access.persistence.user.User;
import br.ucsal.cade.access.persistence.user.UserRepository;
import br.ucsal.cade.shared.exception.BadRequestException;
import br.ucsal.cade.shared.exception.TooManyRequestsException;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@RecordApplicationEvents
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class LoginServiceTest {

    private static final String INVALID_CODE = "Código inválido ou expirado. Confira o código ou solicite um novo.";

    @Autowired
    private LoginService service;

    @Autowired
    private LoginCodeRepository codes;

    @Autowired
    private UserRepository users;

    @Autowired
    private MutableClock clock;

    @Autowired
    private ApplicationEvents events;

    @Test
    void refusesAnEmailOutsideTheInstitutionalDomain() {
        assertThatThrownBy(() -> service.requestCode("aluno@gmail.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Use o seu e-mail institucional @ucsal.edu.br.");

        assertThat(codes.findFirstByEmailOrderByCreatedAtDesc("aluno@gmail.com")).isEmpty();
        assertThat(events.stream(LoginCodeRequestedEvent.class)).isEmpty();
    }

    @Test
    void storesOnlyTheHashAndPublishesTheRawCodeInTheEvent() {
        String code = requestCode(" Hash@UCSAL.edu.br ");

        assertThat(code).matches("\\d{6}");
        LoginCode stored = codes.findFirstByEmailOrderByCreatedAtDesc("hash@ucsal.edu.br").orElseThrow();
        assertThat(stored.getExpiresAt()).isEqualTo(clock.instant().plus(Duration.ofMinutes(10)));
        assertThat(lastEvent("hash@ucsal.edu.br").toString()).doesNotContain(code);
    }

    @Test
    void allowsOnlyOneCodePerMinutePerEmail() {
        requestCode("reenvio@ucsal.edu.br");
        clock.advance(Duration.ofSeconds(45));

        assertThatThrownBy(() -> service.requestCode("reenvio@ucsal.edu.br"))
                .isInstanceOfSatisfying(TooManyRequestsException.class,
                        e -> assertThat(e.retryAfter()).isEqualTo(Duration.ofSeconds(15)));

        clock.advance(Duration.ofSeconds(15));
        requestCode("reenvio@ucsal.edu.br");
    }

    @Test
    void firstLoginCreatesAStudent() {
        String code = requestCode("novo@ucsal.edu.br");

        User user = service.exchangeCode("novo@ucsal.edu.br", code);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void existingAccountKeepsItsRole() {
        users.save(User.newAdmin("chefe@ucsal.edu.br", clock.instant()));
        String code = requestCode("chefe@ucsal.edu.br");

        assertThat(service.exchangeCode("chefe@ucsal.edu.br", code).getRole()).isEqualTo(Role.ADMIN);
        assertThat(users.findAll()).filteredOn(u -> u.getEmail().equals("chefe@ucsal.edu.br")).hasSize(1);
    }

    @Test
    void refusesAReusedCode() {
        String code = requestCode("reuso@ucsal.edu.br");
        service.exchangeCode("reuso@ucsal.edu.br", code);

        assertThatThrownBy(() -> service.exchangeCode("reuso@ucsal.edu.br", code))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(INVALID_CODE);
    }

    @Test
    void refusesAnExpiredCode() {
        String code = requestCode("expirado@ucsal.edu.br");
        clock.advance(Duration.ofMinutes(10));

        assertThatThrownBy(() -> service.exchangeCode("expirado@ucsal.edu.br", code))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(INVALID_CODE);
    }

    @Test
    void keepsTheAttemptCountEvenThoughTheRequestFails() {
        String code = requestCode("tentativa@ucsal.edu.br");

        assertThatThrownBy(() -> service.exchangeCode("tentativa@ucsal.edu.br", wrong(code)))
                .isInstanceOf(BadRequestException.class);

        assertThat(codes.findFirstByEmailOrderByCreatedAtDesc("tentativa@ucsal.edu.br"))
                .get()
                .extracting(LoginCode::getAttempts)
                .isEqualTo(1);
    }

    @Test
    void locksTheCodeAfterFiveWrongAttempts() {
        String code = requestCode("bloqueio@ucsal.edu.br");
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.exchangeCode("bloqueio@ucsal.edu.br", wrong(code)))
                    .isInstanceOf(BadRequestException.class);
        }

        assertThatThrownBy(() -> service.exchangeCode("bloqueio@ucsal.edu.br", code))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(INVALID_CODE);
    }

    @Test
    void aNewCodeReplacesThePreviousOne() {
        String first = requestCode("troca@ucsal.edu.br");
        clock.advance(Duration.ofMinutes(1));
        String second = requestCode("troca@ucsal.edu.br");

        if (!first.equals(second)) {
            assertThatThrownBy(() -> service.exchangeCode("troca@ucsal.edu.br", first))
                    .isInstanceOf(BadRequestException.class);
        }
        assertThat(service.exchangeCode("troca@ucsal.edu.br", second).getEmail()).isEqualTo("troca@ucsal.edu.br");
    }

    @Test
    void refusesAnEmailThatNeverRequestedACode() {
        assertThatThrownBy(() -> service.exchangeCode("nunca@ucsal.edu.br", "123456"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(INVALID_CODE);
    }

    /** Requests a code and returns the raw value, read from the published event as the e-mail sender would. */
    private String requestCode(String email) {
        service.requestCode(email);
        return lastEvent(User.normalize(email)).code();
    }

    private LoginCodeRequestedEvent lastEvent(String email) {
        return events.stream(LoginCodeRequestedEvent.class)
                .filter(event -> event.email().equals(email))
                .reduce((first, second) -> second)
                .orElseThrow();
    }

    private static String wrong(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }
}
