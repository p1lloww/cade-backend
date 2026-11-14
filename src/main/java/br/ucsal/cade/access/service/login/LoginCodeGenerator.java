package br.ucsal.cade.access.service.login;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates six-digit login codes with {@link SecureRandom} (D03). {@code java.util.Random} is predictable and must
 * not be used for anything that grants access.
 */
@Component
class LoginCodeGenerator {

    private static final int CODE_RANGE = 1_000_000;

    private final SecureRandom random = new SecureRandom();

    /** A code from {@code 000000} to {@code 999999}, keeping leading zeros. */
    String generate() {
        return "%06d".formatted(random.nextInt(CODE_RANGE));
    }
}
