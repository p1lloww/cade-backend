package br.ucsal.cade.lossreports.persistence.reporter;

import br.ucsal.cade.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O lock otimista da RN05. Sem a transação do teste, para cada {@link TransactionTemplate} ser um commit de verdade, e
 * com as duas transações intercaladas na mesma thread, para o conflito acontecer sempre na mesma ordem.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LossReporterRepositoryTest {

    @Autowired
    private LossReporterRepository reporters;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transaction;
    private TransactionTemplate otherTransaction;
    private UUID student;

    @BeforeEach
    void setUp() {
        transaction = new TransactionTemplate(transactionManager);
        otherTransaction = new TransactionTemplate(transactionManager);
        otherTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        student = UUID.randomUUID();
        transaction.executeWithoutResult(_ -> reporters.createIfAbsent(student));
    }

    @Test
    void readingWithTheLockRaisesTheVersionEvenWithoutChanges() {
        long before = version();

        transaction.executeWithoutResult(_ -> reporters.findLockedByStudentId(student).orElseThrow());

        assertThat(version()).isEqualTo(before + 1);
    }

    @Test
    void aPlainReadDoesNotRaiseTheVersion() {
        long before = version();

        transaction.executeWithoutResult(_ -> reporters.findById(student).orElseThrow());

        assertThat(version()).isEqualTo(before);
    }

    @Test
    void theSecondOfTwoSimultaneousRegistrationsFails() {
        long before = version();

        assertThatThrownBy(() -> transaction.executeWithoutResult(_ -> {
            reporters.findLockedByStudentId(student).orElseThrow();
            otherTransaction.executeWithoutResult(_ -> reporters.findLockedByStudentId(student).orElseThrow());
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertThat(version()).isEqualTo(before + 1);
    }

    @Test
    void creatingAnExistingRowChangesNothing() {
        transaction.executeWithoutResult(_ -> reporters.findLockedByStudentId(student).orElseThrow());
        long before = version();

        transaction.executeWithoutResult(_ -> reporters.createIfAbsent(student));

        assertThat(version()).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM loss_reporters WHERE student_id = ?", Long.class,
                student)).isOne();
    }

    private long version() {
        return jdbc.queryForObject("SELECT version FROM loss_reporters WHERE student_id = ?", Long.class, student);
    }
}
