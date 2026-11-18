package br.ucsal.cade.shared.security.authorization;

import br.ucsal.cade.shared.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import java.util.Set;

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
        assertThat(acceptedRoles(new StaffController(), "inherited"))
                .containsExactlyInAnyOrder(Role.STAFF, Role.ADMIN);
    }

    @Test
    void theMethodAnnotationReplacesTheControllerOne() {
        assertThat(acceptedRoles(new StaffController(), "overridden")).containsExactly(Role.STUDENT);
    }

    @Test
    void acceptsAnyOfTheListedRoles() {
        assertThat(acceptedRoles(new StaffController(), "shared"))
                .containsExactlyInAnyOrder(Role.STUDENT, Role.STAFF, Role.ADMIN);
    }

    @Test
    void isEmptyWithoutAnnotation() throws Exception {
        HandlerMethod handler = new HandlerMethod(new UnannotatedController(), "open");

        assertThat(RoleRequirement.acceptedRoles(handler)).isEmpty();
    }

    @Test
    void refusesAnAnnotationWithoutRoles() throws Exception {
        HandlerMethod handler = new HandlerMethod(new StaffController(), "closed");

        assertThatIllegalStateException().isThrownBy(() -> RoleRequirement.acceptedRoles(handler));
    }

    private static Set<Role> acceptedRoles(Object controller, String method) {
        try {
            return RoleRequirement.acceptedRoles(new HandlerMethod(controller, method)).orElseThrow();
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(exception);
        }
    }
}
