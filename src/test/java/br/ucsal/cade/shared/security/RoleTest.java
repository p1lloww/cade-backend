package br.ucsal.cade.shared.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @ParameterizedTest(name = "{0} atende a {1}: {2}")
    @CsvSource({
            "STUDENT, STUDENT, true",
            "STUDENT, STAFF,   false",
            "STUDENT, ADMIN,   false",
            "STAFF,   STUDENT, false",
            "STAFF,   STAFF,   true",
            "STAFF,   ADMIN,   false",
            "ADMIN,   STUDENT, false",
            "ADMIN,   STAFF,   true",
            "ADMIN,   ADMIN,   true"})
    void adminIncludesStaffAndNothingElseIsInherited(Role role, Role required, boolean expected) {
        assertThat(role.satisfies(required)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"STUDENT, ROLE_STUDENT", "STAFF, ROLE_STAFF", "ADMIN, ROLE_ADMIN"})
    void turnsIntoTheSpringSecurityAuthority(Role role, String authority) {
        assertThat(role.authority()).isEqualTo(authority);
    }
}
