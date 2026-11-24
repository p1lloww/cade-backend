package br.ucsal.cade.administration.service.dashboard;

import br.ucsal.cade.founditems.api.count.StatusCategoryCount;

import java.time.Instant;
import java.util.List;

/**
 * As contagens do painel do administrador (RF46).
 *
 * @param byStatusAndCategory  objetos por situação e categoria
 * @param byStatus             objetos por situação, somando as categorias
 * @param unclaimed            quantos estão não reclamados agora
 * @param returnsThisMonth     devoluções feitas no mês corrente (fuso de Salvador)
 * @param averageDaysToReturn  tempo médio, em dias, entre a entrada na sala e a devolução, de todas as devoluções;
 *                             nulo enquanto não houve nenhuma
 */
public record Dashboard(List<StatusCategoryCount> byStatusAndCategory, List<StatusTotal> byStatus, long unclaimed,
                        long returnsThisMonth, Double averageDaysToReturn, Instant generatedAt) {

    /** Total de uma situação. */
    public record StatusTotal(String statusCode, String statusLabel, long total) {
    }
}
