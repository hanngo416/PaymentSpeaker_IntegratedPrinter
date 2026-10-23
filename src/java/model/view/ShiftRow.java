package model.view;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class ShiftRow {

    private String id;
    private String openedBy;
    private Timestamp openedAt;
    private String closedBy;
    private Timestamp closedAt;
    private BigDecimal countedCash;
    private boolean forced;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOpenedBy() { return openedBy; }
    public void setOpenedBy(String openedBy) { this.openedBy = openedBy; }

    public Timestamp getOpenedAt() { return openedAt; }
    public void setOpenedAt(Timestamp openedAt) { this.openedAt = openedAt; }

    public String getClosedBy() { return closedBy; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }

    public Timestamp getClosedAt() { return closedAt; }
    public void setClosedAt(Timestamp closedAt) { this.closedAt = closedAt; }

    public BigDecimal getCountedCash() { return countedCash; }
    public void setCountedCash(BigDecimal countedCash) { this.countedCash = countedCash; }

    public boolean isForced() { return forced; }
    public void setForced(boolean forced) { this.forced = forced; }

    public boolean isOpen() { return closedAt == null; }
}
