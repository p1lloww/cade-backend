package br.ucsal.cade.shared.storage;

import br.ucsal.cade.shared.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalPhotoStorageTest {

    @TempDir
    private Path directory;

    private LocalPhotoStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalPhotoStorage(new PhotoProperties(directory, DataSize.ofBytes(64)));
    }

    @Test
    void savesAndLoadsEachAcceptedFormat() {
        for (byte[] content : new byte[][]{TestPhotos.jpeg(), TestPhotos.png(), TestPhotos.webp()}) {
            String key = storage.save(content);

            StoredPhoto loaded = storage.load(key).orElseThrow();
            assertThat(loaded.content()).isEqualTo(content);
            assertThat(key).endsWith("." + loaded.type().extension());
        }
        assertThat(storage.load(storage.save(TestPhotos.png())).orElseThrow().type().mediaType())
                .isEqualTo("image/png");
    }

    @Test
    void namesEveryPhotoWithANewKey() {
        assertThat(storage.save(TestPhotos.jpeg())).isNotEqualTo(storage.save(TestPhotos.jpeg()));
    }

    @Test
    void refusesAnEmptyFile() {
        assertThatThrownBy(() -> storage.save(new byte[0]))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(LocalPhotoStorage.EMPTY_MESSAGE);
    }

    @Test
    void refusesAFileThatIsNotAnAcceptedPhoto() {
        assertThatThrownBy(() -> storage.save(TestPhotos.pdf()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(LocalPhotoStorage.INVALID_TYPE_MESSAGE);
    }

    @Test
    void refusesAPhotoLargerThanTheLimit() throws Exception {
        byte[] large = new byte[65];
        System.arraycopy(TestPhotos.jpeg(), 0, large, 0, TestPhotos.jpeg().length);

        assertThatThrownBy(() -> storage.save(large)).isInstanceOf(BadRequestException.class);
        try (var files = Files.list(directory)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void ignoresKeysOutsideTheFormatSoNothingOutsideTheFolderIsRead() throws Exception {
        Files.writeString(directory.resolveSibling("secret.txt"), "segredo");

        assertThat(storage.load("../secret.txt")).isEmpty();
        assertThat(storage.load("not-a-key.jpg")).isEmpty();
        assertThat(storage.load(null)).isEmpty();
    }

    @Test
    void deletesAPhoto() {
        String key = storage.save(TestPhotos.jpeg());

        storage.delete(key);

        assertThat(storage.load(key)).isEmpty();
    }
}
