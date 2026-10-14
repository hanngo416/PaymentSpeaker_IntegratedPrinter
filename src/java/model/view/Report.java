package model.view;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything a report page or printout needs: the totals, the shifts inside the period
 * and (for a month or a year) one line per day or month.
 */
public class Report {

    private String title;
    private String period;
    private Summary summary;
    private ShiftRow shift;                              // set for a single-shift report
    private List<ShiftRow> shifts = new ArrayList<>();
    private List<Summary> rows = new ArrayList<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public Summary getSummary() { return summary; }
    public void setSummary(Summary summary) { this.summary = summary; }

    public ShiftRow getShift() { return shift; }
    public void setShift(ShiftRow shift) { this.shift = shift; }

    public List<ShiftRow> getShifts() { return shifts; }
    public void setShifts(List<ShiftRow> shifts) { this.shifts = shifts; }

    public List<Summary> getRows() { return rows; }
    public void setRows(List<Summary> rows) { this.rows = rows; }

    // Cash counted by the cashiers of the shifts in this report; null while a shift is still open or an admin closed one for them
    public BigDecimal getCountedCash() {
        List<ShiftRow> list = shift != null ? java.util.Collections.singletonList(shift) : shifts;
        if (list.isEmpty()) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (ShiftRow s : list) {
            if (s.isOpen() || s.getCountedCash() == null) {
                return null;
            }
            sum = sum.add(s.getCountedCash());
        }
        return sum;
    }

    // Counted cash minus the cash the paid orders should have left in the drawer
    public BigDecimal getCashDiff() {
        BigDecimal counted = getCountedCash();
        return counted == null ? null : counted.subtract(summary.getCash());
    }
}
