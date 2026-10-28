/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

import dao.SettingDAO;
import dao.TransactionDAO;
import dao.UserDAO;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.enums.Role;
import util.AppConfig;

/**
 *
 * @author PC
 */
public class PaymentPollJob implements Runnable {

    private static final Logger LOG = Logger.getLogger(PaymentPollJob.class.getName());
    private static final int OVERLAP_MINUTES = 10;
    private static final int MAX_LOOK_BACK_DAYS = 7;
    private static final int RETRY_LOOK_BACK_MINUTES = 120;
    private static final int MAX_CONSECUTIVE_BEFORE_ALERT = 5;

    private static final int BACKOFF_STEP_1_SECONDS = 15;
    private static final int BACKOFF_STEP_2_SECONDS = 30;
    private static final int BACKOFF_MAX_SECONDS = 60;

    private boolean retryWide;
    private int consecutiveFailures;
    private long nextAllowedTime;

    private final SepayClient client = new SepayClient();
    private final TransactionIngestService ingestService = new TransactionIngestService();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final SettingDAO settingDAO = new SettingDAO();

    private Date startOfRead() throws SQLException {
        long now = System.currentTimeMillis();
        Timestamp latest = transactionDAO.findLatestTransTime();
        long from = latest != null
                ? Math.min(latest.getTime(), now) - OVERLAP_MINUTES * 60000L
                : now - client.getWindowMinutes() * 60000L;
        if (retryWide) {
            from = Math.min(from, now - RETRY_LOOK_BACK_MINUTES * 60000L);
        }
        return new Date(Math.max(from, now - MAX_LOOK_BACK_DAYS * 86400000L));
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        if (now < nextAllowedTime) {
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        format.setTimeZone(AppConfig.BANK_ZONE);
        String timeStr = format.format(new Date(now));
        try {
            List<Map<String, String>> items = client.fetchSince(startOfRead());
            int created = 0;
            int bad = 0;
            int failed = 0;
            for (Map<String, String> item : items) {
                try {
                    if (ingestService.ingest(item) == TransactionIngestService.Outcome.CREATED) {
                        created++;
                    }
                } catch (IllegalArgumentException e) {
                    bad++;
                    LOG.log(Level.WARNING, "POLL_BAD_TRANSACTION id={0}", String.valueOf(item.get("id")).replaceAll("[\\r\\n]", "_"));
                } catch (SQLException | RuntimeException e) {
                    failed++;
                    LOG.log(Level.WARNING, "POLL_TRANSACTION_FAILED id=" + String.valueOf(item.get("id")).replaceAll("[\\r\\n]", "_")
                            + " " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
            retryWide = failed > 0;
            consecutiveFailures = 0;
            nextAllowedTime = 0L;
            saveSyncState(timeStr, "SUCCESS", created, "");
            LOG.log(Level.INFO, "POLL_OK fetched={0} new={1} bad={2} failed={3}", new Object[]{items.size(), created, bad, failed});
        } catch (Exception e) {
            consecutiveFailures++;
            int backoffSec = consecutiveFailures == 1 ? BACKOFF_STEP_1_SECONDS
                    : consecutiveFailures == 2 ? BACKOFF_STEP_2_SECONDS : BACKOFF_MAX_SECONDS;
            nextAllowedTime = now + backoffSec * 1000L;
            String errorMsg = e.getClass().getSimpleName() + ": " + e.getMessage();
            saveSyncState(timeStr, "ERROR", 0, errorMsg);
            LOG.log(Level.WARNING, "POLL_FAILED consecutive=" + consecutiveFailures + " backoff=" + backoffSec + "s " + errorMsg);
            if (consecutiveFailures == MAX_CONSECUTIVE_BEFORE_ALERT) {
                raiseGatewayAlert(errorMsg);
            }
        }
    }

    private void saveSyncState(String timeStr, String status, int count, String error) {
        try {
            settingDAO.set("sepay_last_poll_time", timeStr);
            settingDAO.set("sepay_last_status", status);
            settingDAO.set("sepay_last_count", String.valueOf(count));
            settingDAO.set("sepay_last_error", error == null ? "" : error);
            settingDAO.set("sepay_consecutive_failures", String.valueOf(consecutiveFailures));
        } catch (SQLException ex) {
            LOG.log(Level.WARNING, "Cannot save SePay sync state: " + ex.getMessage());
        }
    }

    private void raiseGatewayAlert(String errorMsg) {
        AuditService.log(null, "SEPAY_GATEWAY_ALERT", "PAYMENT_GATEWAY", null,
                "consecutive_failures", String.valueOf(consecutiveFailures), "error", errorMsg);
        try {
            List<String> emails = new UserDAO().findActiveEmails(Role.ADMIN, Role.STORE_MANAGER);
            if (!emails.isEmpty()) {
                MailService mail = new MailService();
                String subject = "Payment Gateway Warning: SePay Sync Failing";
                String body = "SePay payment gateway synchronization has failed 5 consecutive times.\n"
                        + "Last error: " + errorMsg + "\n"
                        + "System backoff is currently active. Please check the bank account and SePay API token.";
                for (String to : emails) {
                    try {
                        mail.send(to, subject, body);
                    } catch (Exception ex) {
                        LOG.log(Level.WARNING, "Failed to send gateway alert mail to " + to, ex);
                    }
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error preparing gateway alert mail", ex);
        }
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public long getNextAllowedTime() {
        return nextAllowedTime;
    }

    public void setConsecutiveFailures(int failures) {
        this.consecutiveFailures = failures;
    }
}
