package br.ucsal.cade.founditems.service.search;

import br.ucsal.cade.founditems.persistence.item.FoundItem;
import br.ucsal.cade.founditems.persistence.item.FoundItemRepository;
import br.ucsal.cade.founditems.persistence.item.PlaceInUse;
import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Busca de objetos pelo funcionário (RF10): texto em full-text (índice GIN, D27) combinado com categoria, mês, bloco,
 * sala e situação, em páginas.
 */
@Service
public class FoundItemSearchService {

    static final int MAX_PAGE_SIZE = 100;

    private static final Logger log = LoggerFactory.getLogger(FoundItemSearchService.class);

    private final FoundItemRepository items;

    FoundItemSearchService(FoundItemRepository items) {
        this.items = items;
    }

    /**
     * A consulta devolve só os ids da página; os objetos vêm depois com a categoria, numa consulta só, na mesma
     * ordem. A ordem pedida na página é ignorada: com texto, os mais relevantes primeiro; sem texto, os mais recentes.
     */
    @Transactional(readOnly = true)
    public Page<FoundItem> search(FoundItemSearch search, Pageable pageable) {
        log.debug("Searching found items: text={}, category={}, month={}, block={}, room={}, statuses={}, page={}",
                search.hasText(), search.categoryId(), search.month(), search.block() != null,
                search.room() != null, search.statuses().size(), pageable.getPageNumber());
        Pageable page = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        YearMonth month = search.month();
        Page<UUID> ids = items.searchIds(search.text(), asText(search.categoryId()),
                month == null ? null : month.atDay(1).toString(),
                month == null ? null : month.plusMonths(1).atDay(1).toString(),
                search.block(), search.room(), search.statuses().stream().map(FoundItemStatus::code).toList(), page);

        Map<UUID, FoundItem> byId = items.findWithCategoryByIdIn(ids.getContent()).stream()
                .collect(Collectors.toMap(FoundItem::getId, Function.identity()));
        log.info("Found item search returned {} of {} item(s)", ids.getNumberOfElements(), ids.getTotalElements());
        return ids.map(byId::get);
    }

    @Transactional(readOnly = true)
    public SearchOptions options() {
        Map<String, List<String>> roomsByBlock = items.findPlacesInUse().stream()
                .collect(Collectors.groupingBy(PlaceInUse::getBlock, LinkedHashMap::new,
                        Collectors.mapping(PlaceInUse::getRoom, Collectors.toList())));
        List<SearchOptions.BlockRooms> places = roomsByBlock.entrySet().stream()
                .map(entry -> new SearchOptions.BlockRooms(entry.getKey(), entry.getValue()))
                .toList();
        List<YearMonth> months = items.findMonthsInUse().stream().map(YearMonth::parse).toList();
        return new SearchOptions(items.findCategoriesInUse(), places, months, FoundItemStatus.ALL);
    }

    private static String asText(Object value) {
        return Objects.toString(value, null);
    }
}
