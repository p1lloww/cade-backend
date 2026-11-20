package br.ucsal.cade.access.controller.user;

import br.ucsal.cade.access.dto.request.RoleChangeRequestDTO;
import br.ucsal.cade.access.dto.response.UserResponseDTO;
import br.ucsal.cade.access.service.user.UserRoleService;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.authorization.RequiresRole;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiresRole(Role.ADMIN)
class UserController implements UserApi {

    private final UserRoleService service;

    UserController(UserRoleService service) {
        this.service = service;
    }

    @Override
    @GetMapping
    public PagedModel<UserResponseDTO> search(@RequestParam(required = false) Role role,
                                              @RequestParam(required = false) String email,
                                              Pageable pageable) {
        return new PagedModel<>(service.search(role, email, pageable));
    }

    @Override
    @PutMapping(path = "/{id}/role", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserResponseDTO changeRole(@PathVariable UUID id, @Valid @RequestBody RoleChangeRequestDTO request,
                                      @AuthenticationPrincipal Jwt token) {
        return service.changeRole(id, request, UUID.fromString(token.getSubject()));
    }
}
