package br.ucsal.cade.shared.storage;

import java.util.Objects;

/** Uma foto lida do armazenamento, com o formato para o {@code Content-Type} da resposta. */
public record StoredPhoto(byte[] content, PhotoType type) {

    public StoredPhoto {
        Objects.requireNonNull(content);
        Objects.requireNonNull(type);
    }
}
