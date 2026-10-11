package util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import model.view.ChartBar;
import model.view.DayTotal;

public final class TxChart {

    public static final int WIDTH = 700;
    public static final int HEIGHT = 240;
    public static final int LEFT = 70;

    private TxChart() {
    }

    public static BigDecimal max(List<DayTotal> days) {
        BigDecimal max = BigDecimal.ZERO;
        for (DayTotal d : days) {
            BigDecimal total = d.getLegitTotal().add(d.getFlaggedTotal());
            if (total.compareTo(max) > 0) {
                max = total;
            }
        }
        return max;
    }

    public static String money(BigDecimal value) {
        return new DecimalFormat("#,##0").format(value);
    }

    public static List<ChartBar> bars(List<DayTotal> days) {
        List<ChartBar> bars = new ArrayList<>();
        if (days.isEmpty()) {
            return bars;
        }
        BigDecimal max = max(days);
        if (max.signum() == 0) {
            max = BigDecimal.ONE;
        }
        int slot = (WIDTH - LEFT) / days.size();
        int barWidth = Math.max(4, slot * 7 / 10);
        int labelEvery = days.size() > 15 ? 3 : 1;
        SimpleDateFormat dayFormat = new SimpleDateFormat("MM-dd");
        for (int i = 0; i < days.size(); i++) {
            DayTotal d = days.get(i);
            int legitH = scale(d.getLegitTotal(), max);
            int flaggedH = scale(d.getFlaggedTotal(), max);
            ChartBar b = new ChartBar();
            b.setX(LEFT + i * slot + (slot - barWidth) / 2);
            b.setWidth(barWidth);
            b.setLegitHeight(legitH);
            b.setLegitY(HEIGHT - legitH);
            b.setFlaggedHeight(flaggedH);
            b.setFlaggedY(HEIGHT - legitH - flaggedH);
            b.setLabel(i % labelEvery == 0 ? dayFormat.format(d.getDay()) : "");
            b.setTip(d.getDay() + ": " + d.getCount() + " transaction(s), valid "
                    + money(d.getLegitTotal()) + " VND, flagged "
                    + money(d.getFlaggedTotal()) + " VND");
            bars.add(b);
        }
        return bars;
    }

    private static int scale(BigDecimal value, BigDecimal max) {
        return value.multiply(BigDecimal.valueOf(HEIGHT)).divide(max, 0, BigDecimal.ROUND_HALF_UP).intValue();
    }
}
