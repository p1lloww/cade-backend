package br.ucsal.cade.administration.controller.category;

import br.ucsal.cade.administration.controller.category.dto.CategoryRequestDTO;
import br.ucsal.cade.administration.service.category.CategoryAdminService;
import br.ucsal.cade.shared.category.dto.CategoryResponseDTO;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.authorization.RequiresRole;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/categories")
@RequiresRole(Role.ADMIN)
class CategoryAdminController implements CategoryAdminApi {

    private final CategoryAdminService service;

    CategoryAdminController(CategoryAdminService service) {
        this.service = service;
    }

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponseDTO create(@Valid @RequestBody CategoryRequestDTO request,
                                      @AuthenticationPrincipal Jwt token) {
        return CategoryResponseDTO.from(service.create(request.name(), request.defaultRisk(),
                request.verificationQuestions(), UUID.fromString(token.getSubject())));
    }

    @Override
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CategoryResponseDTO update(@PathVariable UUID id, @Valid @RequestBody CategoryRequestDTO request,
                                      @AuthenticationPrincipal Jwt token) {
        return CategoryResponseDTO.from(service.update(id, request.name(), request.defaultRisk(),
                request.verificationQuestions(), UUID.fromString(token.getSubject())));
    }
}
