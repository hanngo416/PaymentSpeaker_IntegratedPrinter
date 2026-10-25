package model;

import java.math.BigDecimal;

public class TaxParams {

    private BigDecimal threshold;
    private BigDecimal pitCeiling;
    private BigDecimal vatRatePct;
    private BigDecimal pitRatePct;
    private BigDecimal vatReductionPct = BigDecimal.ZERO;
    private BigDecimal warn1;
    private BigDecimal warn2;
    private BigDecimal warn3;

    public BigDecimal getThreshold() {
        return threshold;
    }

    public void setThreshold(BigDecimal threshold) {
        this.threshold = threshold;
    }

    public BigDecimal getPitCeiling() {
        return pitCeiling;
    }

    public void setPitCeiling(BigDecimal pitCeiling) {
        this.pitCeiling = pitCeiling;
    }

    public BigDecimal getVatRatePct() {
        return vatRatePct;
    }

    public void setVatRatePct(BigDecimal vatRatePct) {
        this.vatRatePct = vatRatePct;
    }

    public BigDecimal getPitRatePct() {
        return pitRatePct;
    }

    public void setPitRatePct(BigDecimal pitRatePct) {
        this.pitRatePct = pitRatePct;
    }

    public BigDecimal getVatReductionPct() {
        return vatReductionPct;
    }

    public void setVatReductionPct(BigDecimal vatReductionPct) {
        this.vatReductionPct = vatReductionPct;
    }

    public BigDecimal getWarn1() {
        return warn1;
    }

    public void setWarn1(BigDecimal warn1) {
        this.warn1 = warn1;
    }

    public BigDecimal getWarn2() {
        return warn2;
    }

    public void setWarn2(BigDecimal warn2) {
        this.warn2 = warn2;
    }

    public BigDecimal getWarn3() {
        return warn3;
    }

    public void setWarn3(BigDecimal warn3) {
        this.warn3 = warn3;
    }
}
