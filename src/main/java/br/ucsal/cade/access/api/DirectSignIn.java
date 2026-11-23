package br.ucsal.cade.access.api;

import br.ucsal.cade.shared.security.Role;

/**
 * Entrada direta, sem código de e-mail, só para o modo demonstração (RF47, D04). A implementação só existe no profile
 * {@code demo}: fora dele, não há bean, então nenhum código consegue emitir um token sem o código de login (D02).
 */
public interface DirectSignIn {

    /**
     * Devolve o token da conta com o e-mail informado, criando a conta com o perfil pedido se ela ainda não existe.
     * Se a conta já existia com outro perfil, vale o perfil que ela tem.
     */
    SignedIn signIn(String email, Role role);
}
