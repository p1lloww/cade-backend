package br.ucsal.cade.shared.storage;

import br.ucsal.cade.shared.exception.BadRequestException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Guarda as fotos numa pasta do disco (no Docker, um volume; requisitos, seção 7). Cada foto recebe um nome novo,
 * {@code <uuid>.<extensão>}: o nome original do arquivo nunca é usado, e uma chave fora desse formato é ignorada, para
 * ninguém ler arquivos fora da pasta.
 */
@Component
@EnableConfigurationProperties(PhotoProperties.class)
class LocalPhotoStorage implements PhotoStorage {

    static final String EMPTY_MESSAGE = "A foto está vazia.";
    static final String INVALID_TYPE_MESSAGE = "A foto precisa ser JPEG, PNG ou WebP.";

    private static final Pattern KEY = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.([a-z]+)");

    private final Path directory;
    private final long maxBytes;

    LocalPhotoStorage(PhotoProperties properties) {
        this.directory = properties.directory().toAbsolutePath().normalize();
        this.maxBytes = properties.maxSize().toBytes();
    }

    @Override
    public String save(byte[] content) {
        if (content == null || content.length == 0) {
            throw new BadRequestException(EMPTY_MESSAGE);
        }
        if (content.length > maxBytes) {
            throw new BadRequestException(tooLargeMessage());
        }
        PhotoType type = PhotoType.detect(content).orElseThrow(() -> new BadRequestException(INVALID_TYPE_MESSAGE));
        String key = UUID.randomUUID() + "." + type.extension();
        try {
            Files.createDirectories(directory);
            Files.write(directory.resolve(key), content);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not save photo " + key, exception);
        }
        return key;
    }

    @Override
    public Optional<StoredPhoto> load(String key) {
        Matcher matcher = KEY.matcher(key == null ? "" : key);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        Optional<PhotoType> type = PhotoType.fromExtension(matcher.group(1));
        Path file = directory.resolve(key);
        if (type.isEmpty() || !Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new StoredPhoto(Files.readAllBytes(file), type.get()));
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read photo " + key, exception);
        }
    }

    @Override
    public void delete(String key) {
        if (key == null || !KEY.matcher(key).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(directory.resolve(key));
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not delete photo " + key, exception);
        }
    }

    String tooLargeMessage() {
        return "A foto pode ter no máximo " + maxBytes / (1024 * 1024) + " MB.";
    }
}
