package br.ucsal.cade.shared.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.Objects;

/** The client must wait before trying again (HTTP 429), as told by the {@code Retry-After} header. */
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

    /** {@code Retry-After} in whole seconds, rounded up so the client never retries too early. */
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
