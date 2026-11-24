package br.ucsal.cade.founditems.api.registration;

import java.util.UUID;

/**
 * Cadastro de objeto achado por código, para o modo demonstração (RF47). Faz o mesmo que a rota do funcionário: grava
 * o objeto, o histórico e publica o evento que dispara o cruzamento.
 */
public interface FoundItemRegistration {

    /**
     * @param staffId id da conta do funcionário que cadastra
     * @return o id do objeto cadastrado
     */
    UUID register(FoundItemData data, UUID staffId);
}
