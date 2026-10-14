package service;

import dao.ShiftDAO;
import dao.SummaryDAO;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import model.view.Report;
import model.view.ShiftRow;
import model.view.Summary;

/**
 * Builds the shift, day, month and year reports. Every period is a time window [from, to).
 */
public class ReportService {

    private static final int SHIFTS_TO_SCAN = 200;

    private final ShiftDAO shiftDAO = new ShiftDAO();
    private final SummaryDAO summaryDAO = new SummaryDAO();

    // Null when there is no such shift. An open shift is reported up to now.
    public Report forShift(String id) throws SQLException {
        ShiftRow shift = shiftDAO.find(id);
        if (shift == null) {
            return null;
        }
        Timestamp to = shift.isOpen() ? new Timestamp(System.currentTimeMillis()) : shift.getClosedAt();
        Report r = new Report();
        r.setTitle("Shift " + shift.getId());
        r.setPeriod(shift.getOpenedAt().toString().substring(0, 16)
                + " - " + (shift.isOpen() ? "now" : shift.getClosedAt().toString().substring(0, 16)));
        r.setShift(shift);
        r.setSummary(summaryDAO.summarize(shift.getOpenedAt(), to));
        return r;
    }

    public Report forDay(LocalDate day) throws SQLException {
        Timestamp from = start(day);
        Timestamp to = start(day.plusDays(1));
        Report r = new Report();
        r.setTitle("Day report");
        r.setPeriod(day.toString());
        r.setSummary(summaryDAO.summarize(from, to));
        r.setShifts(shiftsBetween(from, to));
        return r;
    }

    public Report forMonth(YearMonth month) throws SQLException {
        Timestamp from = start(month.atDay(1));
        Timestamp to = start(month.plusMonths(1).atDay(1));
        Report r = new Report();
        r.setTitle("Month report");
        r.setPeriod(month.toString());
        r.setSummary(summaryDAO.summarize(from, to));
        r.setRows(summaryDAO.breakdown(from, to, 10));
        return r;
    }

    public Report forYear(int year) throws SQLException {
        Timestamp from = start(LocalDate.of(year, 1, 1));
        Timestamp to = start(LocalDate.of(year + 1, 1, 1));
        Report r = new Report();
        r.setTitle("Year report");
        r.setPeriod(String.valueOf(year));
        r.setSummary(summaryDAO.summarize(from, to));
        r.setRows(summaryDAO.breakdown(from, to, 7));
        return r;
    }

    public List<ShiftRow> recentShifts(int limit) throws SQLException {
        return shiftDAO.findRecent(limit);
    }

    // Shifts opened inside the window, oldest first
    private List<ShiftRow> shiftsBetween(Timestamp from, Timestamp to) throws SQLException {
        List<ShiftRow> inside = new ArrayList<>();
        for (ShiftRow s : shiftDAO.findRecent(SHIFTS_TO_SCAN)) {
            if (!s.getOpenedAt().before(from) && s.getOpenedAt().before(to)) {
                inside.add(0, s);
            }
        }
        return inside;
    }

    private static Timestamp start(LocalDate day) {
        return new Timestamp(day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}
