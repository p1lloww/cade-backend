package br.ucsal.cade.shared.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.Objects;

/** O cliente precisa esperar antes de tentar de novo (HTTP 429), pelo tempo do cabeçalho {@code Retry-After}. */
public class TooManyRequestsException extends ApiException {

    private final Duration retryAfter;

    public TooManyRequestsException(String userMessage, Duration retryAfter) {
        super(userMessage);
        this.retryAfter = Objects.requireNonNull(retryAfter);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.TOO_MANY_REQUESTS;
    }

    /** {@code Retry-After} em segundos inteiros, arredondado para cima, para o cliente nunca tentar cedo demais. */
    @Override
    public HttpHeaders headers() {
        long seconds = retryAfter.plusNanos(999_999_999).toSeconds();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(seconds));
        return headers;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
