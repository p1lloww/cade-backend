package br.ucsal.cade.shared.security.authorization;

import br.ucsal.cade.shared.security.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aplica a {@link RequiresRole} antes de o controller rodar. Quando chega aqui, o Spring Security já conferiu o token;
 * falta conferir se o perfil dele abre a rota. Se não abre, a {@link AccessDeniedException} vira um 403 com mensagem
 * em português no {@code ApiExceptionHandler}.
 */
class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        Optional<RoleRequirement> requirement = RoleRequirement.of(method);
        if (requirement.isEmpty() || currentRoles().stream().anyMatch(requirement.get()::admits)) {
            return true;
        }
        throw new AccessDeniedException("Role not allowed on " + request.getRequestURI());
    }

    /** Os perfis vindos do token; vazio numa requisição sem autenticação. */
    private static Set<Role> currentRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Set.of();
        }
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return Arrays.stream(Role.values())
                .filter(role -> authorities.contains(role.authority()))
                .collect(Collectors.toSet());
    }
}
