package br.ucsal.cade.shared.security;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.shared.security.authorization.RoleRequirement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nenhuma rota fica aberta a qualquer perfil por esquecimento: toda rota dos nossos controllers ou é pública
 * ({@link SecurityConfiguration#PUBLIC_ROUTES}) ou declara a {@code @RequiresRole}.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RouteAuthorizationCoverageTest {

    private static final String OUR_PACKAGE = "br.ucsal.cade.";

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapping;

    @Test
    void everyNonPublicRouteDeclaresTheRolesItAccepts() {
        Set<String> publicRoutes = Set.of(SecurityConfiguration.PUBLIC_ROUTES);
        List<String> checked = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> route : mapping.getHandlerMethods().entrySet()) {
            HandlerMethod handler = route.getValue();
            if (!handler.getBeanType().getName().startsWith(OUR_PACKAGE)) {
                continue;
            }
            Set<String> patterns = route.getKey().getPatternValues();
            checked.addAll(patterns);
            if (!publicRoutes.containsAll(patterns) && RoleRequirement.acceptedRoles(handler).isEmpty()) {
                missing.add(patterns + " em " + handler);
            }
        }

        assertThat(checked).contains("/auth/token");
        assertThat(missing).as("rotas sem @RequiresRole e fora das públicas").isEmpty();
    }
}
