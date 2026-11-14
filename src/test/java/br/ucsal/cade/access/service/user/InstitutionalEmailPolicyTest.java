package br.ucsal.cade.access.service.user;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class InstitutionalEmailPolicyTest {

    private final InstitutionalEmailPolicy policy = new InstitutionalEmailPolicy("ucsal.edu.br");

    @ParameterizedTest
    @ValueSource(strings = {"aluno@ucsal.edu.br", "nome.sobrenome@ucsal.edu.br"})
    void acceptsInstitutionalEmails(String email) {
        assertThat(policy.accepts(email)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "aluno@gmail.com",
            "aluno@fakeucsal.edu.br",
            "aluno@ucsal.edu.br.com",
            "aluno@sub.ucsal.edu.br",
            "a@b@ucsal.edu.br",
            "@ucsal.edu.br",
            "ucsal.edu.br"})
    void refusesAnythingElse(String email) {
        assertThat(policy.accepts(email)).isFalse();
    }
}
