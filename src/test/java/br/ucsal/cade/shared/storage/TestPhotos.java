package br.ucsal.cade.shared.storage;

/** Conteúdos mínimos com a assinatura de cada formato, para os testes não dependerem de arquivos de imagem. */
public final class TestPhotos {

    private TestPhotos() {
    }

    public static byte[] jpeg() {
        return bytes(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00);
    }

    public static byte[] png() {
        return bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00);
    }

    public static byte[] webp() {
        return bytes('R', 'I', 'F', 'F', 0x10, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' ');
    }

    public static byte[] pdf() {
        return bytes('%', 'P', 'D', 'F', '-', '1', '.', '7');
    }

    private static byte[] bytes(int... values) {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            bytes[i] = (byte) values[i];
        }
        return bytes;
    }
}
