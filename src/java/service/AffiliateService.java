package service;

import dao.AffiliateDAO;
import dao.CampaignDAO;
import dao.CommissionDAO;
import dao.PayoutDAO;
import dao.SettingDAO;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import model.Affiliate;
import model.Attribution;
import model.Campaign;
import model.User;
import model.Wallet;

public class AffiliateService {

    public static final String KEY_REVIEW_DAYS = "affiliate_review_days";
    public static final String KEY_PAYABLE_DAYS = "affiliate_payable_days";
    public static final String KEY_PAYOUT_MIN = "affiliate_payout_min";
    public static final String KEY_WITHHOLD_PCT = "affiliate_withhold_pct";
    public static final String KEY_WITHHOLD_FROM = "affiliate_withhold_from";

    private final AffiliateDAO affiliateDAO = new AffiliateDAO();
    private final CampaignDAO campaignDAO = new CampaignDAO();
    private final CommissionDAO commissionDAO = new CommissionDAO();
    private final PayoutDAO payoutDAO = new PayoutDAO();
    private final SettingDAO settingDAO = new SettingDAO();

    public Attribution attribute(String rawCode, User seller, long eligibleAmount) throws SQLException {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase();
        if (code.isEmpty()) {
            return null;
        }
        if (!code.matches("[A-Z0-9_-]{3,30}")) {
            throw new IllegalArgumentException("The affiliate code is not valid.");
        }
        Affiliate a = affiliateDAO.findByCode(code);
        if (a == null) {
            throw new IllegalArgumentException("No affiliate uses this code.");
        }
        if (!Affiliate.ACTIVE.equals(a.getStatus())) {
            throw new IllegalArgumentException("This affiliate is " + a.getStatus().toLowerCase() + " and cannot be credited with new orders.");
        }
        List<Campaign> running = campaignDAO.findRunning();
        if (running.isEmpty()) {
            throw new IllegalArgumentException("No running campaign includes this affiliate.");
        }
        Campaign best = null;
        long bestAmount = -1;
        long lowestMin = Long.MAX_VALUE;
        for (Campaign c : running) {
            long min = c.getMinOrder().longValue();
            lowestMin = Math.min(lowestMin, min);
            if (eligibleAmount < min) {
                continue;
            }
            long amount = c.commissionFor(eligibleAmount);
            if (amount > bestAmount) {
                best = c;
                bestAmount = amount;
            }
        }
        if (best == null) {
            throw new IllegalArgumentException("The order is below the minimum of " + String.format("%,d", lowestMin) + " VND for the running campaigns.");
        }
        String rate = best.rateText();
        String snapshot = best.getName() + " (#" + best.getCampaignId() + ") | " + rate
                + " | eligible " + String.format("%,d", eligibleAmount) + " VND | " + LocalDate.now();
        return new Attribution(a, best, eligibleAmount, bestAmount, rate, snapshot);
    }

    public int reviewDays() throws SQLException {
        return intSetting(KEY_REVIEW_DAYS, 3);
    }

    public int payableDays() throws SQLException {
        return intSetting(KEY_PAYABLE_DAYS, 14);
    }

    public long payoutMin() throws SQLException {
        return intSetting(KEY_PAYOUT_MIN, 40000);
    }

    public BigDecimal withholdPct() throws SQLException {
        return BigDecimal.valueOf(intSetting(KEY_WITHHOLD_PCT, 10));
    }

    public BigDecimal withholdFrom() throws SQLException {
        return BigDecimal.valueOf(intSetting(KEY_WITHHOLD_FROM, 60000));
    }

    public void saveSettings(int reviewDays, int payableDays, int payoutMin, int withholdPct, int withholdFrom) throws SQLException {
        settingDAO.set(KEY_REVIEW_DAYS, String.valueOf(reviewDays));
        settingDAO.set(KEY_PAYABLE_DAYS, String.valueOf(payableDays));
        settingDAO.set(KEY_PAYOUT_MIN, String.valueOf(payoutMin));
        settingDAO.set(KEY_WITHHOLD_PCT, String.valueOf(withholdPct));
        settingDAO.set(KEY_WITHHOLD_FROM, String.valueOf(withholdFrom));
    }

    public void releaseMatured() throws SQLException {
        commissionDAO.releaseMatured(payableDays());
    }

    public BigDecimal deduction(Affiliate a, BigDecimal gross) throws SQLException {
        if (gross.compareTo(withholdFrom()) < 0) {
            return BigDecimal.ZERO;
        }
        return gross.multiply(withholdPct()).divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
    }

    public Wallet wallet(int affiliateId) throws SQLException {
        releaseMatured();
        return payoutDAO.wallet(affiliateId);
    }

    public void requestPayout(Affiliate a) throws SQLException {
        Wallet w = wallet(a.getAffiliateId());
        if (a.getBankAccount() == null || a.getBankName() == null) {
            throw new IllegalArgumentException("Add your bank details first.");
        }
        if (!Affiliate.ACTIVE.equals(a.getStatus())) {
            throw new IllegalArgumentException("Your account is " + a.getStatus().toLowerCase() + ".");
        }
        if (w.getAvailable().longValue() < payoutMin()) {
            throw new IllegalArgumentException("The available balance is below the minimum of " + String.format("%,d", payoutMin()) + " VND.");
        }
        if (payoutDAO.hasOpenRequest(a.getAffiliateId())) {
            throw new IllegalArgumentException("A payout request is already waiting.");
        }
        try {
            payoutDAO.request(a.getAffiliateId(), w.getAvailable(), deduction(a, w.getAvailable()), a.getBankName() + " " + a.getBankAccount());
        } catch (SQLException e) {
            if (util.SqlErrors.isDuplicateKey(e)) {
                throw new IllegalArgumentException("A payout request is already waiting.");
            }
            throw e;
        }
    }

    private int intSetting(String key, int fallback) throws SQLException {
        try {
            return Integer.parseInt(settingDAO.get(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
