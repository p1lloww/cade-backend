package br.ucsal.cade.matching.service.suggestion;

import br.ucsal.cade.shared.exception.NotFoundException;

public class SuggestionNotFoundException extends NotFoundException {

    public SuggestionNotFoundException() {
        super("Sugestão não encontrada.");
    }
}
