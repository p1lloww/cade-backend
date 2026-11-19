package br.ucsal.cade.shared.category;

import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.authorization.RequiresRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lista das categorias, para o cadastro de objetos (funcionário) e de perdas (estudante). */
@Tag(name = "Categorias")
@RestController
@RequiresRole({Role.STUDENT, Role.STAFF})
class CategoryController {

    private final CategoryRepository categories;

    CategoryController(CategoryRepository categories) {
        this.categories = categories;
    }

    @Operation(summary = "Listar as categorias", description = "Em ordem alfabética, com o risco padrão de cada uma.")
    @GetMapping("/categories")
    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categories.findAllByOrderByNameAsc().stream().map(CategoryResponse::from).toList();
    }
}
