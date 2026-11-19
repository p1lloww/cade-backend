package br.ucsal.cade.shared.exception;

import org.springframework.http.HttpStatus;

/** O registro pedido não existe (HTTP 404). */
public class NotFoundException extends ApiException {

    public NotFoundException(String userMessage) {
        super(userMessage);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.NOT_FOUND;
    }
}
