/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

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

    private final SepayClient client = new SepayClient();
    private final TransactionIngestService ingestService = new TransactionIngestService();

    @Override
    public void run() {
        try {
            List<Map<String, String>> items = client.fetchRecent();
            int created = 0;
            int bad = 0;
            for (Map<String, String> item : items) {
                try {
                    if (ingestService.ingest(item) == TransactionIngestService.Outcome.CREATED) {
                        created++;
                    }
                } catch (IllegalArgumentException e) {
                    bad++;
                    LOG.log(Level.WARNING, "POLL_BAD_TRANSACTION id={0}", String.valueOf(item.get("id")).replaceAll("[\\r\\n]", "_"));
                }
            }
            LOG.log(Level.INFO, "POLL_OK fetched={0} new={1} bad={2}", new Object[]{items.size(), created, bad});
        } catch (Exception e) {
            LOG.log(Level.WARNING, "POLL_FAILED " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
