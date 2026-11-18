package br.ucsal.cade.shared.security.authorization;

import br.ucsal.cade.shared.security.Role;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lê a {@link RequiresRole} de uma rota por reflexão, para o Swagger e para o teste de cobertura. Quem aplica a regra é
 * o Spring Security (D20).
 */
public final class RoleRequirement {

    private RoleRequirement() {
    }

    /**
     * Os perfis aceitos, já com a hierarquia: {@code @RequiresRole(Role.STAFF)} devolve {@code STAFF} e {@code ADMIN}.
     * Vazio quando nem o método nem o controller têm a anotação.
     *
     * @throws IllegalStateException se a anotação não lista nenhum perfil
     */
    public static Optional<Set<Role>> acceptedRoles(HandlerMethod handler) {
        return annotationOn(handler.getMethod())
                .or(() -> annotationOn(handler.getBeanType()))
                .map(annotation -> withHierarchy(annotation.value(), handler));
    }

    private static Optional<RequiresRole> annotationOn(AnnotatedElement element) {
        return Optional.ofNullable(element.getAnnotation(RequiresRole.class));
    }

    private static Set<Role> withHierarchy(Role[] required, HandlerMethod handler) {
        if (required.length == 0) {
            throw new IllegalStateException("@RequiresRole without roles on " + handler);
        }
        return Arrays.stream(Role.values())
                .filter(role -> Arrays.stream(required).anyMatch(role::satisfies))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Role.class)));
    }
}
