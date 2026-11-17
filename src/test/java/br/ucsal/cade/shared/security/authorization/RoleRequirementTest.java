package br.ucsal.cade.shared.security.authorization;

import br.ucsal.cade.shared.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class RoleRequirementTest {

    @RequiresRole(Role.STAFF)
    static class StaffController {

        public void inherited() {
        }

        @RequiresRole(Role.STUDENT)
        public void overridden() {
        }

        @RequiresRole({Role.STUDENT, Role.STAFF})
        public void shared() {
        }

        @RequiresRole({})
        public void closed() {
        }
    }

    static class UnannotatedController {

        public void open() {
        }
    }

    @Test
    void readsTheControllerAnnotationAndAddsTheAdmin() {
        assertThat(requirement(new StaffController(), "inherited").acceptedRoles())
                .containsExactlyInAnyOrder(Role.STAFF, Role.ADMIN);
    }

    @Test
    void theMethodAnnotationReplacesTheControllerOne() {
        RoleRequirement requirement = requirement(new StaffController(), "overridden");

        assertThat(requirement.acceptedRoles()).containsExactly(Role.STUDENT);
        assertThat(requirement.admits(Role.ADMIN)).isFalse();
    }

    @Test
    void acceptsAnyOfTheListedRoles() {
        assertThat(requirement(new StaffController(), "shared").acceptedRoles())
                .containsExactlyInAnyOrder(Role.STUDENT, Role.STAFF, Role.ADMIN);
    }

    @Test
    void isEmptyWithoutAnnotation() throws Exception {
        HandlerMethod handler = new HandlerMethod(new UnannotatedController(), "open");

        assertThat(RoleRequirement.of(handler)).isEmpty();
    }

    @Test
    void refusesAnAnnotationWithoutRoles() throws Exception {
        HandlerMethod handler = new HandlerMethod(new StaffController(), "closed");

        assertThatIllegalStateException().isThrownBy(() -> RoleRequirement.of(handler));
    }

    private static RoleRequirement requirement(Object controller, String method) {
        try {
            return RoleRequirement.of(new HandlerMethod(controller, method)).orElseThrow();
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(exception);
        }
    }
}
