package br.ucsal.cade.retention.service;

import br.ucsal.cade.founditems.api.UnclaimedFoundItem;

import java.util.List;

/** Os não reclamados de uma caixa, para o funcionário retirar da sala de uma vez (RF39). */
public record UnclaimedItemsByBox(String storageBox, List<UnclaimedFoundItem> items) {
}
