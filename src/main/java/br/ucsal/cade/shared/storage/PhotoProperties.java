package br.ucsal.cade.shared.storage;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

/**
 * Configuração das fotos, de {@code cade.photos.*}.
 *
 * @param directory pasta onde as fotos ficam; no Docker, um volume ({@code PHOTOS_DIR})
 * @param maxSize   tamanho máximo de uma foto
 */
@Validated
@ConfigurationProperties("cade.photos")
public record PhotoProperties(@NotNull Path directory, @NotNull DataSize maxSize) {
}
