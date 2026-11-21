package br.ucsal.cade.shared.category;

import br.ucsal.cade.shared.exception.BadRequestException;

import java.util.UUID;

public class UnknownCategoryException extends BadRequestException {

    private final UUID categoryId;

    public UnknownCategoryException(UUID categoryId) {
        super("Categoria não encontrada.");
        this.categoryId = categoryId;
    }

    public UUID categoryId() {
        return categoryId;
    }
}
