package br.ucsal.cade.founditems.controller.search;

import br.ucsal.cade.founditems.controller.item.dto.FoundItemResponseDTO;
import br.ucsal.cade.founditems.controller.search.dto.SearchOptionsResponseDTO;
import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import br.ucsal.cade.founditems.service.search.FoundItemSearch;
import br.ucsal.cade.founditems.service.search.FoundItemSearchService;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.authorization.RequiresRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/found-items")
@RequiresRole(Role.STAFF)
class FoundItemSearchController implements FoundItemSearchApi {

    private final FoundItemSearchService service;

    FoundItemSearchController(FoundItemSearchService service) {
        this.service = service;
    }

    @Override
    @GetMapping
    public PagedModel<FoundItemResponseDTO> search(@RequestParam(required = false) String q,
                                                   @RequestParam(required = false) UUID categoryId,
                                                   @RequestParam(required = false)
                                                   @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                                                   @RequestParam(required = false) String block,
                                                   @RequestParam(required = false) String room,
                                                   @RequestParam(required = false) List<FoundItemStatus> status,
                                                   Pageable pageable) {
        FoundItemSearch search = new FoundItemSearch(q, categoryId, month, block, room, status);
        return new PagedModel<>(service.search(search, pageable).map(FoundItemResponseDTO::from));
    }

    @Override
    @GetMapping("/search-options")
    public SearchOptionsResponseDTO options() {
        return SearchOptionsResponseDTO.from(service.options());
    }
}
