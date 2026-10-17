package model;

import java.math.BigDecimal;
import java.sql.Date;

public class DiscountCode {

    public static final String PERCENT = "PERCENT";
    public static final String FIXED = "FIXED";

    private int codeId;
    private String code;
    private String kind;
    private BigDecimal value;
    private String holder;
    private Integer maxUses;
    private int usedCount;
    private Date validFrom;
    private Date validTo;
    private boolean active;
    private boolean available;

    public long discountFor(long amount) {
        if (PERCENT.equals(kind)) {
            return amount * value.longValue() / 100;
        }
        return Math.min(amount, value.longValue());
    }

    public int getCodeId() { return codeId; }
    public void setCodeId(int codeId) { this.codeId = codeId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }

    public String getHolder() { return holder; }
    public void setHolder(String holder) { this.holder = holder; }

    public Integer getMaxUses() { return maxUses; }
    public void setMaxUses(Integer maxUses) { this.maxUses = maxUses; }

    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int usedCount) { this.usedCount = usedCount; }

    public Date getValidFrom() { return validFrom; }
    public void setValidFrom(Date validFrom) { this.validFrom = validFrom; }

    public Date getValidTo() { return validTo; }
    public void setValidTo(Date validTo) { this.validTo = validTo; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
