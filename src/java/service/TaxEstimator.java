package service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import model.TaxParams;
import model.view.TaxEstimate;

public final class TaxEstimator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private TaxEstimator() {
    }

    public static TaxEstimate estimate(BigDecimal revenue, TaxParams p) {
        BigDecimal threshold = p.getThreshold();
        BigDecimal percent = revenue.multiply(HUNDRED).divide(threshold, 2, RoundingMode.HALF_UP);
        BigDecimal scaled = revenue.multiply(HUNDRED);

        int level = 0;
        if (scaled.compareTo(p.getWarn3().multiply(threshold)) >= 0) {
            level = 3;
        } else if (scaled.compareTo(p.getWarn2().multiply(threshold)) >= 0) {
            level = 2;
        } else if (scaled.compareTo(p.getWarn1().multiply(threshold)) >= 0) {
            level = 1;
        }

        BigDecimal remaining = threshold.subtract(revenue).max(BigDecimal.ZERO);

        if (revenue.compareTo(threshold) <= 0) {
            return new TaxEstimate(revenue, percent, level, TaxEstimate.UNDER,
                    BigDecimal.ZERO, BigDecimal.ZERO, remaining);
        }

        BigDecimal keep = BigDecimal.ONE.subtract(p.getVatReductionPct().divide(HUNDRED));
        BigDecimal vat = revenue.multiply(p.getVatRatePct()).divide(HUNDRED)
                .multiply(keep).setScale(0, RoundingMode.HALF_UP);

        if (revenue.compareTo(p.getPitCeiling()) > 0) {
            return new TaxEstimate(revenue, percent, level, TaxEstimate.OVER_CEILING,
                    vat, null, remaining);
        }

        BigDecimal pit = revenue.subtract(threshold).multiply(p.getPitRatePct())
                .divide(HUNDRED).setScale(0, RoundingMode.HALF_UP);
        return new TaxEstimate(revenue, percent, level, TaxEstimate.OVER, vat, pit, remaining);
    }
}
