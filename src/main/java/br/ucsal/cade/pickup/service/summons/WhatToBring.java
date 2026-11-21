package br.ucsal.cade.pickup.service.summons;

import br.ucsal.cade.shared.category.RiskLevel;

/**
 * O que o estudante convocado leva ao balcão, conforme o risco do objeto (RN09). Todo caso pede documento com foto
 * (RN10). O texto fala só do que o estudante precisa trazer, sem contar o que o funcionário vai conferir (RN03).
 */
final class WhatToBring {

    static final String PHOTO_ID = "Um documento com foto (RG, CNH ou carteirinha da UCSal).";

    private WhatToBring() {
    }

    static String forRisk(RiskLevel risk) {
        return switch (risk) {
            case LOW -> PHOTO_ID;
            case MEDIUM -> PHOTO_ID + " Esteja pronto para descrever um detalhe do objeto.";
            case HIGH -> PHOTO_ID + " Se for um aparelho, você vai desbloqueá-lo na frente do funcionário; se não"
                    + " puder, leve uma prova de que é seu, como a nota fiscal ou o número de série.";
            case DOCUMENT -> PHOTO_ID + " O documento achado é entregue só ao titular.";
        };
    }
}
