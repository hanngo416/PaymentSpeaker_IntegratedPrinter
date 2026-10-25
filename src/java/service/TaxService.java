package service;

import dao.SettingDAO;
import dao.SummaryDAO;
import dao.TaxRuleDAO;
import dao.UserDAO;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.TaxParams;
import model.TaxRule;
import model.enums.Role;
import model.view.TaxEstimate;

public class TaxService {

    private static final Logger LOG = Logger.getLogger(TaxService.class.getName());

    public static class TaxView {

        private int year;
        private String sector;
        private TaxParams params;
        private TaxEstimate estimate;
        private List<String> todo = new ArrayList<>();
        private List<TaxRule> sources = new ArrayList<>();

        public int getYear() {
            return year;
        }

        public String getSector() {
            return sector;
        }

        public TaxParams getParams() {
            return params;
        }

        public TaxEstimate getEstimate() {
            return estimate;
        }

        public List<String> getTodo() {
            return todo;
        }

        public List<TaxRule> getSources() {
            return sources;
        }
    }

    private final SummaryDAO summaryDAO = new SummaryDAO();
    private final TaxRuleDAO taxRuleDAO = new TaxRuleDAO();
    private final SettingDAO settingDAO = new SettingDAO();
    private final UserDAO userDAO = new UserDAO();

    public TaxView view(int year) throws SQLException {
        TaxView v = new TaxView();
        v.year = year;
        v.sector = settingDAO.get("tax_sector", "GOODS");
        LocalDate today = LocalDate.now();
        Date on = Date.valueOf(today);
        v.params = taxRuleDAO.loadParams(v.sector, on);
        v.sources = taxRuleDAO.inForce(v.sector, on);
        if (v.params == null) {
            return v;
        }
        BigDecimal revenue = summaryDAO.yearRevenue(year);
        v.estimate = TaxEstimator.estimate(revenue, v.params);
        fillTodo(v, today);
        return v;
    }

    private void fillTodo(TaxView v, LocalDate today) {
        String status = v.estimate.getStatus();
        if (TaxEstimate.UNDER.equals(status)) {
            v.todo.add("Send the annual revenue notice by 31 January " + (v.year + 1) + ".");
            return;
        }
        v.todo.add("File the quarterly VAT and PIT declaration. Next due date: " + nextQuarterDeadline(today) + ".");
        v.todo.add("Register for e-invoices within 30 days of passing the threshold.");
        if (TaxEstimate.OVER_CEILING.equals(status)) {
            v.todo.add("Income-based method required, ask an accountant.");
        }
    }

    static LocalDate nextQuarterDeadline(LocalDate today) {
        LocalDate[] due = {
            LocalDate.of(today.getYear(), 1, 31),
            LocalDate.of(today.getYear(), 4, 30),
            LocalDate.of(today.getYear(), 7, 31),
            LocalDate.of(today.getYear(), 10, 31),
            LocalDate.of(today.getYear() + 1, 1, 31)
        };
        for (LocalDate d : due) {
            if (!d.isBefore(today)) {
                return d;
            }
        }
        return due[4];
    }

    public void notifyIfLevelRose(int year) {
        try {
            TaxView v = view(year);
            if (v.estimate == null) {
                return;
            }
            String key = "tax_notified_" + year;
            int last = Integer.parseInt(settingDAO.get(key, "0"));
            int level = v.estimate.getLevel();
            if (level <= last) {
                return;
            }
            settingDAO.set(key, String.valueOf(level));
            if (level == 0) {
                return;
            }
            String subject = "Tinky tax warning: " + v.estimate.getPercent().toPlainString() + "% of the threshold";
            String body = "Revenue for " + year + " is " + v.estimate.getRevenue().toBigInteger()
                    + " VND, which is " + v.estimate.getPercent().toPlainString()
                    + "% of the tax-free threshold. Remaining: " + v.estimate.getRemaining().toBigInteger()
                    + " VND. Open the Tax page in Tinky for the estimate and what to do next. "
                    + "Estimate only, not the amount to pay.";
            MailService mail = new MailService();
            for (String to : userDAO.findActiveEmails(Role.ADMIN, Role.STORE_MANAGER)) {
                try {
                    mail.send(to, subject, body);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Cannot send the tax warning mail", e);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Cannot check the tax warning level", e);
        }
    }
}
