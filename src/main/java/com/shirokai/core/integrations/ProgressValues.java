package com.shirokai.core.integrations;

import java.math.*;

/** Percentage of official implemented species obtained, not a wild-capture or shiny-capture statistic. */
public final class ProgressValues {
    private ProgressValues() { }
    public static String percentage(int owned, int total) {
        if (owned < 0 || total <= 0 || owned > total) throw new IllegalArgumentException("Progresso inválido");
        return BigDecimal.valueOf(owned).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
