package model.view;

import java.math.BigDecimal;

public class TaxEstimate {

    public static final String UNDER = "UNDER";
    public static final String OVER = "OVER";
    public static final String OVER_CEILING = "OVER_CEILING";

    private final BigDecimal revenue;
    private final BigDecimal percent;
    private final int level;
    private final String status;
    private final BigDecimal vat;
    private final BigDecimal pit;
    private final BigDecimal remaining;

    public TaxEstimate(BigDecimal revenue, BigDecimal percent, int level, String status,
            BigDecimal vat, BigDecimal pit, BigDecimal remaining) {
        this.revenue = revenue;
        this.percent = percent;
        this.level = level;
        this.status = status;
        this.vat = vat;
        this.pit = pit;
        this.remaining = remaining;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public BigDecimal getPercent() {
        return percent;
    }

    public int getLevel() {
        return level;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getVat() {
        return vat;
    }

    public BigDecimal getPit() {
        return pit;
    }

    public BigDecimal getRemaining() {
        return remaining;
    }
}
