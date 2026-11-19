package br.ucsal.cade.lossreports.persistence.report;

import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.persistence.period.LostPeriod;
import br.ucsal.cade.shared.category.Category;

import java.util.List;

public record NewLossReport(Category category, String description, String secretDetail,
                            List<LossLocation> locations, LostPeriod period, String photoKey) {
}
