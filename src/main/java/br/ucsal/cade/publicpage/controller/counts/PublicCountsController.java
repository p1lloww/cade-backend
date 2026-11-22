package br.ucsal.cade.publicpage.controller.counts;

import br.ucsal.cade.publicpage.controller.counts.dto.PublicCountsResponseDTO;
import br.ucsal.cade.publicpage.service.counts.PublicCountsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rota pública: está em {@code SecurityConfiguration.PUBLIC_ROUTES}, sem {@code @RequiresRole}. */
@RestController
class PublicCountsController implements PublicCountsApi {

    static final String PATH = "/public/found-item-counts";

    private final PublicCountsService service;

    PublicCountsController(PublicCountsService service) {
        this.service = service;
    }

    @Override
    @GetMapping(PATH)
    public PublicCountsResponseDTO counts() {
        return PublicCountsResponseDTO.from(service.byMonth());
    }
}
