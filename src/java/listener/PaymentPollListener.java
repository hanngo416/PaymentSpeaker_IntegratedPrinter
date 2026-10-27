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
import util.ConfigUtil;

/**
 *
 * @author PC
 */
@WebListener
public class PaymentPollListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(PaymentPollListener.class.getName());
    private static final int INITIAL_DELAY_SECONDS = 10;
    private static final int DEFAULT_PERIOD_SECONDS = 5;

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
        int period = periodSeconds();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(job, INITIAL_DELAY_SECONDS, period, TimeUnit.SECONDS);
        LOG.log(Level.INFO, "Payment polling started: every {0} seconds", period);
    }

    private static int periodSeconds() {
        try {
            int value = Integer.parseInt(ConfigUtil.load("sepay.properties").getProperty("poll.seconds", "").trim());
            return value >= 5 ? value : DEFAULT_PERIOD_SECONDS;
        } catch (NumberFormatException e) {
            return DEFAULT_PERIOD_SECONDS;
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        java.util.Enumeration<java.sql.Driver> drivers = java.sql.DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            java.sql.Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == getClass().getClassLoader()) {
                try {
                    java.sql.DriverManager.deregisterDriver(driver);
                } catch (java.sql.SQLException ignored) {
                }
            }
        }
    }
}
