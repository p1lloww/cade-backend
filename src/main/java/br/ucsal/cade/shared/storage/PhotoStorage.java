package br.ucsal.cade.shared.storage;

import java.util.Optional;

/**
 * Onde as fotos ficam guardadas (o {@code ArmazenamentoFotos} dos requisitos, seção 7). Quem usa só conhece esta
 * interface, então trocar o disco local por outro armazenamento não muda o resto do código (inversão de dependência).
 * Fica no núcleo porque objetos achados (ACH-02) e provas de perda (PER-05) guardam fotos.
 *
 * <p>As fotos nunca têm URL pública: são servidas pela API, só a quem o perfil permite (RN03, D09).
 */
public interface PhotoStorage {

    /**
     * Confere e guarda a foto.
     *
     * @return a chave que identifica a foto, a ser gravada no registro dono dela
     * @throws br.ucsal.cade.shared.exception.BadRequestException se o arquivo está vazio, é grande demais ou não é
     *                                                             JPEG, PNG ou WebP
     */
    String save(byte[] content);

    Optional<StoredPhoto> load(String key);

    void delete(String key);
}
