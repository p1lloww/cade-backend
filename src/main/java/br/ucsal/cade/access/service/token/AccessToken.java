package br.ucsal.cade.access.service.token;

import java.time.Duration;

/**
 * Um token de acesso assinado e por quanto tempo ele vale.
 *
 * @param value     o JWT codificado, enviado pelo cliente como {@code Authorization: Bearer <value>}
 * @param expiresIn tempo até o token vencer
 */
public record AccessToken(String value, Duration expiresIn) {

    @Override
    public String toString() {
        return "AccessToken[value=******, expiresIn=" + expiresIn + "]";
    }
}
