package br.ucsal.cade.matching.service.score.ai;

/**
 * A porta para uma IA que compara duas descrições em texto livre (RF25). Quem quiser ligar um serviço de IA fornece um
 * bean desta interface; o projeto não traz nenhum, nem chave de API, então por padrão nada sai do sistema.
 *
 * <p>Os textos chegam já sem dados pessoais ({@link PersonalDataRedactor}). Uma implementação pode demorar ou falhar:
 * quem a chama ({@link AiSimilarityCriterion}) espera só até o tempo limite e, se não vier, a nota fica sem a IA.
 */
@FunctionalInterface
public interface AiTextSimilarity {

    /** @return a semelhança entre as duas descrições, de 0 (nada a ver) a 1 (a mesma coisa) */
    double similarity(String foundDescription, String lossDescription);
}
