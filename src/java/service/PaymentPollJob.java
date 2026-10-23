/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

import dao.TransactionDAO;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author PC
 */
public class PaymentPollJob implements Runnable {

    private static final Logger LOG = Logger.getLogger(PaymentPollJob.class.getName());
    private static final int OVERLAP_MINUTES = 10;
    private static final int MAX_LOOK_BACK_DAYS = 7;

    private final SepayClient client = new SepayClient();
    private final TransactionIngestService ingestService = new TransactionIngestService();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    private Date startOfRead() throws SQLException {
        long now = System.currentTimeMillis();
        Timestamp latest = transactionDAO.findLatestTransTime();
        long from = latest != null
                ? Math.min(latest.getTime(), now) - OVERLAP_MINUTES * 60000L
                : now - client.getWindowMinutes() * 60000L;
        return new Date(Math.max(from, now - MAX_LOOK_BACK_DAYS * 86400000L));
    }

    @Override
    public void run() {
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
            LOG.log(Level.INFO, "POLL_OK fetched={0} new={1} bad={2} failed={3}", new Object[]{items.size(), created, bad, failed});
        } catch (Exception e) {
            LOG.log(Level.WARNING, "POLL_FAILED " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
