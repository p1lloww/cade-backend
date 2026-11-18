package br.ucsal.cade.shared.security.authorization;

import br.ucsal.cade.shared.security.Role;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Perfis que podem usar uma rota (RF02, RNF05). Vale para o controller inteiro ou para um método; no método, substitui
 * a da classe. A hierarquia de {@link Role#satisfies} se aplica: {@code @RequiresRole(Role.STAFF)} aceita também o
 * administrador.
 *
 * <p>Por baixo é um {@link PreAuthorize}: o Spring troca {@code {value}} pelos perfis, então
 * {@code @RequiresRole({Role.STUDENT, Role.STAFF})} vira {@code hasAnyRole('STUDENT','STAFF')} (D20). Quem liga a
 * troca e a hierarquia é a {@link AuthorizationConfiguration}. Toda rota fora das públicas precisa dela; um teste
 * quebra o build quando falta.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@PreAuthorize("hasAnyRole({value})")
public @interface RequiresRole {

    /** Os perfis aceitos; basta o da conta atender a um deles. */
    Role[] value();
}
