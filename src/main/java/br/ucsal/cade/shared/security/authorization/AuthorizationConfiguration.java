package br.ucsal.cade.shared.security.authorization;

import br.ucsal.cade.shared.security.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Faz o Spring Security aplicar a {@link RequiresRole} como um {@code @PreAuthorize} (D20): liga a segurança por
 * método, a troca de {@code {value}} pelos perfis da anotação e a hierarquia de {@link Role}.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class AuthorizationConfiguration {

    /** Permite que meta-anotações como a {@link RequiresRole} usem os próprios atributos na expressão. */
    @Bean
    static AnnotationTemplateExpressionDefaults templateExpressionDefaults() {
        return new AnnotationTemplateExpressionDefaults();
    }

    /**
     * A hierarquia de {@link Role#satisfies} no formato do Spring ({@code ROLE_ADMIN > ROLE_STAFF}), gerada a partir do
     * enum para que a regra continue num lugar só.
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        String hierarchy = Arrays.stream(Role.values())
                .flatMap(role -> Arrays.stream(Role.values())
                        .filter(included -> included != role && role.satisfies(included))
                        .map(included -> role.authority() + " > " + included.authority()))
                .collect(Collectors.joining("\n"));
        return RoleHierarchyImpl.fromHierarchy(hierarchy);
    }
}
