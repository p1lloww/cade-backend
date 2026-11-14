package br.ucsal.cade.access.service.token;

import java.time.Duration;

/**
 * A signed access token and how long it lasts.
 *
 * @param value     the encoded JWT, sent by the client as {@code Authorization: Bearer <value>}
 * @param expiresIn time until the token expires
 */
public record AccessToken(String value, Duration expiresIn) {

    @Override
    public String toString() {
        return "AccessToken[value=******, expiresIn=" + expiresIn + "]";
    }
}
