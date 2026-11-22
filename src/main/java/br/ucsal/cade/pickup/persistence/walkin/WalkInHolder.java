package br.ucsal.cade.pickup.persistence.walkin;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Quem retirou pelo atalho, identificado por nome além do documento (RF32). Não é ligado a uma conta: o módulo de
 * retirada não depende do de acesso (D10), e quem não tem conta só existe aqui.
 */
@Embeddable
public record WalkInHolder(
        @Enumerated(EnumType.STRING) @Column(name = "holder_type", nullable = false, length = 20) HolderType type,
        @Column(name = "holder_name", nullable = false, length = 120) String name) {
}
