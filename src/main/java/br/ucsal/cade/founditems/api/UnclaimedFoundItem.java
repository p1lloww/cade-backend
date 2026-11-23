package br.ucsal.cade.founditems.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Um objeto não reclamado, como o módulo de prazos o vê: onde está guardado e há quanto tempo (RF39). */
public record UnclaimedFoundItem(UUID id, String categoryName, String block, String room, LocalDate foundOn,
                                 String storageBox, String internalDescription, Instant registeredAt) {
}
