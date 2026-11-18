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

/** Faz o Spring Security aplicar a {@link RequiresRole} como {@code @PreAuthorize}, com a hierarquia (D20). */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class AuthorizationConfiguration {

    /** Sem ele, o {@code {value}} da {@link RequiresRole} não seria trocado pelos perfis. */
    @Bean
    static AnnotationTemplateExpressionDefaults templateExpressionDefaults() {
        return new AnnotationTemplateExpressionDefaults();
    }

    /** Gerada a partir do {@link Role#satisfies} ({@code ROLE_ADMIN > ROLE_STAFF}), para a regra ficar num lugar só. */
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
