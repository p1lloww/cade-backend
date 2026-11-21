package br.ucsal.cade.lossreports.api;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Registros de perda em qualquer situação, para a comparação lado a lado com os objetos (RF23). */
public interface ComparableLossReports {

    /** Numa consulta só; um id que não existe fica fora do mapa. */
    Map<UUID, ComparableLossReport> findAll(Collection<UUID> lossReportIds);
}
