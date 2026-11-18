package br.ucsal.cade.shared.security.authorization;

import br.ucsal.cade.shared.security.Role;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Perfis que podem usar uma rota (RF02, RNF05). No método, substitui a da classe; {@code @RequiresRole(Role.STAFF)}
 * aceita também o administrador. Por baixo é um {@link PreAuthorize}: {@code @RequiresRole({Role.STUDENT, Role.STAFF})}
 * vira {@code hasAnyRole('STUDENT','STAFF')} (D20).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@PreAuthorize("hasAnyRole({value})")
public @interface RequiresRole {

    Role[] value();
}
