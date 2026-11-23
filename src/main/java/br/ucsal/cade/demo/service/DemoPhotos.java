package br.ucsal.cade.demo.service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Fotos de mentira dos objetos de demonstração: um degradê de uma cor só, desenhado na hora. Assim o repositório não
 * guarda imagens e não há foto de pessoa nem de objeto real (RF47).
 */
final class DemoPhotos {

    private static final int WIDTH = 320;
    private static final int HEIGHT = 240;

    private DemoPhotos() {
    }

    /** Um PNG com um degradê da cor informada para um tom mais escuro. */
    static byte[] solid(Color color) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setPaint(new GradientPaint(0, 0, color, WIDTH, HEIGHT, color.darker().darker()));
            graphics.fillRect(0, 0, WIDTH, HEIGHT);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }
}
