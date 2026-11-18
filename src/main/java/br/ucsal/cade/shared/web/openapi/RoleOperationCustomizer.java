package br.ucsal.cade.shared.web.openapi;

import br.ucsal.cade.shared.security.authorization.RoleRequirement;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.stream.Collectors;

/**
 * Mostra no Swagger quem pode usar cada rota, lendo a mesma {@code @RequiresRole} que o servidor aplica: a lista de
 * perfis entra na descrição, e a resposta 403 é documentada. Assim a documentação nunca diverge da regra.
 */
@Component
class RoleOperationCustomizer implements OperationCustomizer {

    static final String FORBIDDEN = "403";

    @Override
    public Operation customize(Operation operation, HandlerMethod handler) {
        RoleRequirement.acceptedRoles(handler).ifPresent(accepted -> {
            String roles = accepted.stream()
                    .sorted()
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            String description = operation.getDescription() == null ? "" : operation.getDescription() + "\n\n";
            operation.setDescription(description + "**Perfis:** " + roles);
            ApiResponses responses = operation.getResponses() == null ? new ApiResponses() : operation.getResponses();
            responses.putIfAbsent(FORBIDDEN,
                    new ApiResponse().description("O perfil da conta não pode usar esta rota."));
            operation.setResponses(responses);
        });
        return operation;
    }
}
