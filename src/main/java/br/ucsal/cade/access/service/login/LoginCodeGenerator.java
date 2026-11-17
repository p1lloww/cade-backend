package br.ucsal.cade.access.service.login;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Gera códigos de login de seis dígitos com {@link SecureRandom} (D03). O {@code java.util.Random} é previsível e não
 * pode ser usado em nada que dê acesso.
 */
@Component
class LoginCodeGenerator {

    private static final int CODE_RANGE = 1_000_000;

    private final SecureRandom random = new SecureRandom();

    /** Um código de {@code 000000} a {@code 999999}, mantendo os zeros à esquerda. */
    String generate() {
        return "%06d".formatted(random.nextInt(CODE_RANGE));
    }
}
