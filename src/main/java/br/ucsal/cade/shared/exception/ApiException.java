package br.ucsal.cade.shared.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * Erro de negócio que chega ao usuário. A mensagem aparece no front-end, por isso é escrita em português; cada
 * subclasse fixa o status HTTP. Um único tratador transforma qualquer subclasse numa resposta de erro, então um tipo
 * novo de erro não exige mudança no tratador.
 */
public abstract class ApiException extends RuntimeException {

    protected ApiException(String userMessage) {
        super(userMessage);
    }

    public abstract HttpStatus status();

    /** Cabeçalhos que a resposta precisa levar além do corpo do erro; nenhum por padrão. */
    public HttpHeaders headers() {
        return HttpHeaders.EMPTY;
    }
}
