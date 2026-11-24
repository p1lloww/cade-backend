package br.ucsal.cade.founditems.api.count;

import java.time.YearMonth;

public record CategoryMonthCount(String category, YearMonth month, long count) {
}
