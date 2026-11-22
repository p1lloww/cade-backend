package br.ucsal.cade.lossreports.api;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** De quem é cada registro de perda, para avisar o dono (RF33). Só o id da conta, nada do conteúdo do registro. */
public interface LossReportOwners {

    /** Registro → estudante dono. Os registros que não existem ficam de fora. */
    Map<UUID, UUID> studentsOf(Collection<UUID> lossReportIds);
}
