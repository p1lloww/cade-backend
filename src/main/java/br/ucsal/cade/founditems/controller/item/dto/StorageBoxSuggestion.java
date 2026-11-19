package br.ucsal.cade.founditems.controller.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Caixa do mês sugerida para a data (RF05).")
public record StorageBoxSuggestion(@Schema(example = "Caixa Novembro/2026") String storageBox) {
}
