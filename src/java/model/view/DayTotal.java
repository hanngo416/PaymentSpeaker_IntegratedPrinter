package model.view;

import java.math.BigDecimal;
import java.sql.Date;

public class DayTotal {

    private Date day;
    private BigDecimal legitTotal;
    private BigDecimal flaggedTotal;
    private int count;

    public Date getDay() {
        return day;
    }

    public void setDay(Date day) {
        this.day = day;
    }

    public BigDecimal getLegitTotal() {
        return legitTotal;
    }

    public void setLegitTotal(BigDecimal legitTotal) {
        this.legitTotal = legitTotal;
    }

    public BigDecimal getFlaggedTotal() {
        return flaggedTotal;
    }

    public void setFlaggedTotal(BigDecimal flaggedTotal) {
        this.flaggedTotal = flaggedTotal;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
