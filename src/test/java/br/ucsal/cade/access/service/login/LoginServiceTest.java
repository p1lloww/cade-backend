package br.ucsal.cade.access.service.login;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.access.api.event.LoginCodeRequestedEvent;
import br.ucsal.cade.access.persistence.logincode.LoginCode;
import br.ucsal.cade.access.persistence.logincode.LoginCodeStore;
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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

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
    private LoginCodeStore codes;

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

        assertThat(codes.find("aluno@gmail.com")).isEmpty();
        assertThat(events.stream(LoginCodeRequestedEvent.class)).isEmpty();
    }

    @Test
    void storesOnlyTheHashAndPublishesTheRawCodeInTheEvent() {
        String code = requestCode(" Hash@UCSAL.edu.br ");

        assertThat(code).matches("\\d{6}");
        LoginCode stored = codes.find("hash@ucsal.edu.br").orElseThrow();
        assertThat(stored.codeHash()).doesNotContain(code);
        assertThat(stored.expiresAt()).isEqualTo(clock.instant().plus(Duration.ofMinutes(10)));
        assertThat(lastEvent("hash@ucsal.edu.br").toString()).doesNotContain(code);
    }

    @Test
    void allowsOnlyOneCodePerMinutePerEmail() {
        requestCode("reenvio@ucsal.edu.br");

        assertThatThrownBy(() -> service.requestCode("reenvio@ucsal.edu.br"))
                .isInstanceOfSatisfying(TooManyRequestsException.class, e -> assertThat(e.retryAfter())
                        .isPositive()
                        .isLessThanOrEqualTo(Duration.ofMinutes(1)));

        expireCooldown("reenvio@ucsal.edu.br");
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
    void aSuccessfulLoginDeletesEverythingKeptForTheEmail() {
        String code = requestCode("limpeza@ucsal.edu.br");

        service.exchangeCode("limpeza@ucsal.edu.br", code);

        assertThat(codes.find("limpeza@ucsal.edu.br")).isEmpty();
        requestCode("limpeza@ucsal.edu.br");
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
    void keepsTheAttemptCountAfterAWrongCode() {
        String code = requestCode("tentativa@ucsal.edu.br");

        assertThatThrownBy(() -> service.exchangeCode("tentativa@ucsal.edu.br", wrong(code)))
                .isInstanceOf(BadRequestException.class);

        assertThat(codes.find("tentativa@ucsal.edu.br")).get()
                .extracting(LoginCode::attempts)
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
    void parallelGuessesCannotGoPastTheAttemptLimit() throws Exception {
        String code = requestCode("paralelo@ucsal.edu.br");

        List<Boolean> results = runInParallel(20, () -> succeeds("paralelo@ucsal.edu.br", wrong(code)));

        assertThat(results).containsOnly(false);
        assertThat(codes.find("paralelo@ucsal.edu.br")).get()
                .extracting(LoginCode::attempts)
                .isEqualTo(20);
        assertThat(succeeds("paralelo@ucsal.edu.br", code)).isFalse();
    }

    @Test
    void twoParallelRequestsWithTheRightCodeLogInOnlyOnce() throws Exception {
        String code = requestCode("duplo@ucsal.edu.br");

        List<Boolean> results = runInParallel(2, () -> succeeds("duplo@ucsal.edu.br", code));

        assertThat(results).containsExactlyInAnyOrder(true, false);
    }

    @Test
    void aNewCodeReplacesThePreviousOne() {
        String first = requestCode("troca@ucsal.edu.br");
        expireCooldown("troca@ucsal.edu.br");
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

    @Test
    void wrongAttemptsAddUpAcrossCodesAndBlockTheEmailAfterThreeLockedCodes() {
        String email = "forca-bruta@ucsal.edu.br";
        for (int codeNumber = 1; codeNumber <= 3; codeNumber++) {
            String code = requestCode(email);
            for (int i = 0; i < 5; i++) {
                assertThat(succeeds(email, wrong(code))).isFalse();
            }
            expireCooldown(email);
        }

        assertThatThrownBy(() -> service.requestCode(email))
                .isInstanceOfSatisfying(TooManyRequestsException.class, e -> {
                    assertThat(e).hasMessageContaining("Muitas tentativas com código errado");
                    assertThat(e.retryAfter()).isPositive().isLessThanOrEqualTo(Duration.ofMinutes(15));
                });
    }

    @Test
    void theBlockAlsoEndsTheCurrentCodeAndLastsFifteenMinutes() {
        String email = "bloqueado@ucsal.edu.br";
        for (int codeNumber = 1; codeNumber <= 3; codeNumber++) {
            String code = requestCode(email);
            for (int i = 0; i < 5; i++) {
                succeeds(email, wrong(code));
            }
            if (codeNumber < 3) {
                expireCooldown(email);
            }
        }
        String lastCode = lastEvent(email).code();
        assertThat(succeeds(email, lastCode)).isFalse();

        clock.advance(Duration.ofMinutes(15));

        String code = requestCode(email);
        assertThat(service.exchangeCode(email, code).getEmail()).isEqualTo(email);
    }

    @Test
    void aSuccessfulLoginClearsTheWrongAttempts() {
        String email = "recomeco@ucsal.edu.br";
        String code = requestCode(email);
        for (int i = 0; i < 4; i++) {
            succeeds(email, wrong(code));
        }

        service.exchangeCode(email, code);

        requestCode(email);
        assertThat(codes.find(email)).get().extracting(LoginCode::failures).isEqualTo(0);
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

    /** Lets the one-minute waiting time between two codes pass. */
    private void expireCooldown(String email) {
        clock.advance(Duration.ofMinutes(1));
    }

    private boolean succeeds(String email, String code) {
        try {
            service.exchangeCode(email, code);
            return true;
        } catch (BadRequestException e) {
            return false;
        }
    }

    private static <T> List<T> runInParallel(int times, Callable<T> task) throws Exception {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<T>> futures = executor.invokeAll(IntStream.range(0, times).mapToObj(i -> task).toList());
            return futures.stream().map(LoginServiceTest::await).toList();
        }
    }

    private static <T> T await(Future<T> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String wrong(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }
}
