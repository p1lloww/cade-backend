package br.ucsal.cade.founditems.controller.item.dto;

import br.ucsal.cade.founditems.persistence.item.FoundItem;
import br.ucsal.cade.founditems.persistence.item.StorageBox;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.category.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = """
        Correção dos dados de um objeto achado. Leva os mesmos campos do cadastro (a foto não muda), o motivo e a \
        `version` que o funcionário recebeu por último. O que não for enviado volta ao valor vazio do cadastro.""")
public record FoundItemUpdateRequestDTO(

        @Schema(description = "Id da categoria (GET /categories).")
        @NotNull(message = "Escolha a categoria.")
        UUID categoryId,

        @Schema(description = "Bloco onde o objeto foi achado.", example = "Bloco 400", maxLength = 30)
        @NotBlank(message = "Informe o bloco.")
        @Size(max = FoundItem.MAX_PLACE_LENGTH, message = "O bloco pode ter no máximo 30 caracteres.")
        String block,

        @Schema(description = "Sala ou lugar dentro do bloco.", example = "Sala 421", maxLength = 30)
        @NotBlank(message = "Informe a sala.")
        @Size(max = FoundItem.MAX_PLACE_LENGTH, message = "A sala pode ter no máximo 30 caracteres.")
        String room,

        @Schema(description = "Data em que o objeto foi achado (não pode ser no futuro).", example = "2026-11-04")
        @NotNull(message = "Informe a data em que o objeto foi achado.")
        LocalDate foundOn,

        @Schema(description = "Caixa onde o objeto fica. Vazio: a caixa do mês da data (RF05).",
                example = "Caixa Novembro/2026", maxLength = 40)
        @Size(max = StorageBox.MAX_LENGTH, message = "A caixa pode ter no máximo 40 caracteres.")
        String storageBox,

        @Schema(description = "Risco do objeto. Vazio: o risco padrão da categoria. Nunca abaixo dele (RN04).")
        RiskLevel risk,

        @Schema(description = "Descrição interna. Vazio apaga. Só funcionários veem (RF06).", maxLength = 500)
        @Size(max = FoundItem.MAX_TEXT_LENGTH, message = "A descrição pode ter no máximo 500 caracteres.")
        String internalDescription,

        @Schema(description = "Detalhes reservados. Vazio apaga. Só funcionários veem (RF06).", maxLength = 500)
        @Size(max = FoundItem.MAX_TEXT_LENGTH, message = "Os detalhes podem ter no máximo 500 caracteres.")
        String reservedDetails,

        @Schema(description = "Por que o dado foi corrigido. Vai para o histórico (RF43).",
                example = "Local anotado errado na etiqueta", maxLength = AuditEvent.MAX_REASON_LENGTH - 200)
        @NotBlank(message = "Informe o motivo da correção.")
        @Size(max = AuditEvent.MAX_REASON_LENGTH - 200,
                message = "O motivo pode ter no máximo " + (AuditEvent.MAX_REASON_LENGTH - 200) + " caracteres.")
        String reason,

        @Schema(description = "A `version` do objeto na última resposta que o funcionário recebeu. Se o objeto mudou "
                + "desde então (por exemplo, foi reservado), a correção é recusada com 409.", example = "0")
        @NotNull(message = "Informe a versão do objeto.")
        Long version) {
}
