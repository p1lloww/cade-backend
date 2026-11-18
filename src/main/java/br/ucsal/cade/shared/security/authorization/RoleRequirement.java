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
 * O que a {@link RequiresRole} de uma rota exige, já com a hierarquia de perfis aplicada. Quem aplica a regra é o
 * Spring Security (D20); esta leitura por reflexão serve ao Swagger, para mostrar quem pode usar a rota, e ao teste
 * que exige a anotação em toda rota fora das públicas.
 */
public final class RoleRequirement {

    private RoleRequirement() {
    }

    /**
     * Lê a anotação do método e, se ele não tiver, a do controller, e devolve todos os perfis que podem usar a rota:
     * {@code STAFF} e {@code ADMIN} para uma rota marcada com {@code @RequiresRole(Role.STAFF)}. Vazio quando nenhum
     * dos dois tem a anotação, o que só é permitido nas rotas públicas.
     *
     * @throws IllegalStateException se a anotação não lista nenhum perfil, o que fecharia a rota para todos
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
