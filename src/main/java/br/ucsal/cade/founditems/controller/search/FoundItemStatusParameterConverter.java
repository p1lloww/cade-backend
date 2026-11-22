package br.ucsal.cade.founditems.controller.search;

import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/** Lê o parâmetro {@code status} pelo código; um código desconhecido vira 400, como qualquer valor mal formatado. */
@Component
class FoundItemStatusParameterConverter implements Converter<String, FoundItemStatus> {

    @Override
    public FoundItemStatus convert(String code) {
        return FoundItemStatus.fromCode(code);
    }
}
