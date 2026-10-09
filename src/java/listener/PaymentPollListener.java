/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package listener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import service.PaymentPollJob;

/**
 *
 * @author PC
 */
@WebListener
public class PaymentPollListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(PaymentPollListener.class.getName());
    private static final int INITIAL_DELAY_SECONDS = 10;
    private static final int PERIOD_SECONDS = 60;

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        PaymentPollJob job;
        try {
            job = new PaymentPollJob();
        } catch (IllegalStateException e) {
            LOG.log(Level.SEVERE, "Payment polling is OFF: {0}", e.getMessage());
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(job, INITIAL_DELAY_SECONDS, PERIOD_SECONDS, TimeUnit.SECONDS);
        LOG.log(Level.INFO, "Payment polling started: every {0} seconds", PERIOD_SECONDS);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
