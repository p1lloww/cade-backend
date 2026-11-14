package br.ucsal.cade.shared.exception;

import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.Objects;

/** The client must wait before trying again (HTTP 429); {@link #retryAfter()} feeds the Retry-After header. */
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

    public Duration retryAfter() {
        return retryAfter;
    }
}
