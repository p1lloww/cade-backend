package br.ucsal.cade.shared.storage;

import java.util.Arrays;
import java.util.Optional;

/**
 * Formatos de foto aceitos. O formato é reconhecido pelos primeiros bytes do arquivo, e não pelo nome nem pelo
 * {@code Content-Type} que o navegador manda, que qualquer um pode trocar.
 */
public enum PhotoType {

    JPEG("image/jpeg", "jpg") {
        @Override
        boolean matches(byte[] content) {
            return startsWith(content, 0, 0xFF, 0xD8, 0xFF);
        }
    },
    PNG("image/png", "png") {
        @Override
        boolean matches(byte[] content) {
            return startsWith(content, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
        }
    },
    WEBP("image/webp", "webp") {
        @Override
        boolean matches(byte[] content) {
            return startsWith(content, 0, 'R', 'I', 'F', 'F') && startsWith(content, 8, 'W', 'E', 'B', 'P');
        }
    };

    private final String mediaType;
    private final String extension;

    PhotoType(String mediaType, String extension) {
        this.mediaType = mediaType;
        this.extension = extension;
    }

    public String mediaType() {
        return mediaType;
    }

    public String extension() {
        return extension;
    }

    abstract boolean matches(byte[] content);

    /** O formato do conteúdo, ou vazio se não for uma foto aceita. */
    public static Optional<PhotoType> detect(byte[] content) {
        return Arrays.stream(values()).filter(type -> type.matches(content)).findFirst();
    }

    /** O formato pela extensão de uma chave gravada ({@code <uuid>.jpg}). */
    static Optional<PhotoType> fromExtension(String extension) {
        return Arrays.stream(values()).filter(type -> type.extension.equals(extension)).findFirst();
    }

    private static boolean startsWith(byte[] content, int offset, int... expected) {
        if (content.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((content[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
