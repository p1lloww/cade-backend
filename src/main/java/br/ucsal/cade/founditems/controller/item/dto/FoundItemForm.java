package br.ucsal.cade.founditems.controller.item.dto;

import br.ucsal.cade.founditems.persistence.item.FoundItem;
import br.ucsal.cade.founditems.persistence.item.StorageBox;
import br.ucsal.cade.shared.category.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Formulário de cadastro (multipart): a foto e os campos do objeto. Só foto, categoria, bloco, sala e data são
 * obrigatórios (RF04).
 */
@Schema(description = "Cadastro de um objeto achado. Só foto, categoria, bloco, sala e data são obrigatórios.")
public record FoundItemForm(

        @Schema(description = "Foto do objeto (JPEG, PNG ou WebP, até 5 MB).", type = "string", format = "binary")
        @NotNull(message = "Envie a foto do objeto.")
        MultipartFile photo,

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
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate foundOn,

        @Schema(description = "Caixa onde o objeto vai ficar. Vazio: a caixa do mês da data (RF05).",
                example = "Caixa Novembro/2026", maxLength = 40)
        @Size(max = StorageBox.MAX_LENGTH, message = "A caixa pode ter no máximo 40 caracteres.")
        String storageBox,

        @Schema(description = "Risco do objeto. Vazio: o risco padrão da categoria. Só pode aumentar (RN04).")
        RiskLevel risk,

        @Schema(description = "Descrição interna (cor, marca). Só funcionários veem (RF06).",
                example = "Fone JBL preto", maxLength = 500)
        @Size(max = FoundItem.MAX_TEXT_LENGTH, message = "A descrição pode ter no máximo 500 caracteres.")
        String internalDescription,

        @Schema(description = "Detalhes reservados, para conferir o dono no balcão. Só funcionários veem (RF06).",
                example = "Adesivo de estrela no lado esquerdo", maxLength = 500)
        @Size(max = FoundItem.MAX_TEXT_LENGTH, message = "Os detalhes podem ter no máximo 500 caracteres.")
        String reservedDetails) {
}
