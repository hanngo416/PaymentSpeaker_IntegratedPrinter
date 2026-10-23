package model.view;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Report {

    private String title;
    private String period;
    private Summary summary;
    private ShiftRow shift;
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

    public BigDecimal getCashDiff() {
        BigDecimal counted = getCountedCash();
        return counted == null ? null : counted.subtract(summary.getCash());
    }
}
