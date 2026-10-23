package service;

import dao.SettingDAO;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import model.Device;

public class PrinterStatusService {

    public static final String SETTING_KEY = "printer_name";
    private static final long CACHE_MILLIS = 10000;

    private static String cachedName;
    private static long cachedAt;
    private static String[] cachedResult;

    private final SettingDAO settingDAO = new SettingDAO();

    public static List<String> installedPrinters() {
        List<String> names = new ArrayList<>();
        for (PrintService s : PrintServiceLookup.lookupPrintServices(null, null)) {
            names.add(s.getName());
        }
        return names;
    }

    public void apply(List<Device> devices) throws SQLException {
        String name = settingDAO.get(SETTING_KEY, "");
        for (Device d : devices) {
            if (!"PRINTER".equals(d.getDeviceType()) || "MAINTENANCE".equals(d.getStatus())) {
                continue;
            }
            if (name.isEmpty()) {
                d.setDetail("No Windows printer is selected yet.");
                continue;
            }
            String[] result = check(name);
            d.setStatus(result[0]);
            d.setDetail(result[1]);
        }
    }

    private static synchronized String[] check(String name) {
        long now = System.currentTimeMillis();
        if (name.equals(cachedName) && now - cachedAt < CACHE_MILLIS) {
            return cachedResult;
        }
        String[] result = query(name);
        cachedName = name;
        cachedAt = now;
        cachedResult = result;
        return result;
    }

    private static String[] query(String name) {
        if (!installedPrinters().contains(name)) {
            return new String[]{"OFFLINE", "Printer \"" + name + "\" is not installed on the server PC."};
        }
        String script = "$p = Get-CimInstance Win32_Printer | Where-Object { $_.Name -eq '" + name.replace("'", "''") + "' }; "
                + "if ($p) { '' + $p.WorkOffline + '|' + $p.PrinterStatus + '|' + $p.DetectedErrorState } else { 'MISSING' }";
        try {
            Process process = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", script)
                    .redirectErrorStream(true).start();
            if (!process.waitFor(8, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return unknown();
            }
            String line;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                line = in.readLine();
            }
            return interpret(line);
        } catch (IOException e) {
            return unknown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return unknown();
        }
    }

    private static String[] interpret(String line) {
        if (line == null || line.trim().isEmpty()) {
            return unknown();
        }
        if ("MISSING".equals(line.trim())) {
            return new String[]{"OFFLINE", "Windows does not know this printer."};
        }
        String[] p = line.trim().split("\\|");
        if (p.length != 3) {
            return unknown();
        }
        String offline = p[0];
        String status = p[1];
        String error = p[2];
        if ("True".equalsIgnoreCase(offline) || "7".equals(status) || "9".equals(error)) {
            return new String[]{"OFFLINE", "Windows reports the printer as offline. Check the power and the USB cable."};
        }
        if ("4".equals(error)) {
            return new String[]{"OFFLINE", "The printer is out of paper."};
        }
        if ("7".equals(error)) {
            return new String[]{"OFFLINE", "The printer cover is open."};
        }
        if ("8".equals(error)) {
            return new String[]{"OFFLINE", "The printer has a paper jam."};
        }
        if ("10".equals(error) || "6".equals(status)) {
            return new String[]{"OFFLINE", "The printer needs attention (stopped or service requested)."};
        }
        return new String[]{"ONLINE", "Windows reports the printer as ready."};
    }

    private static String[] unknown() {
        return new String[]{"ONLINE", "The printer state could not be read on this server."};
    }
}
