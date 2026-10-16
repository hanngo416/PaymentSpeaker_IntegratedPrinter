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
    public static final int HEIGHT = 210;
    public static final int TOP = 16;
    public static final int LEFT = 80;

    public static class Tick {
        private final int y;
        private final String label;

        public Tick(int y, String label) {
            this.y = y;
            this.label = label;
        }

        public int getY() {
            return y;
        }

        public String getLabel() {
            return label;
        }
    }

    private TxChart() {
    }

    public static BigDecimal ceiling(List<DayTotal> days) {
        BigDecimal peak = BigDecimal.ZERO;
        for (DayTotal d : days) {
            if (d.getLegitTotal() != null && d.getLegitTotal().compareTo(peak) > 0) {
                peak = d.getLegitTotal();
            }
            if (d.getFlaggedTotal() != null && d.getFlaggedTotal().compareTo(peak) > 0) {
                peak = d.getFlaggedTotal();
            }
        }
        if (peak.signum() == 0) {
            return BigDecimal.valueOf(100000);
        }
        double val = peak.doubleValue();
        double roughStep = val / 4.0;
        double power = Math.pow(10, Math.floor(Math.log10(roughStep)));
        double norm = roughStep / power;
        double niceNorm;
        if (norm <= 1.0) {
            niceNorm = 1.0;
        } else if (norm <= 2.0) {
            niceNorm = 2.0;
        } else if (norm <= 2.5) {
            niceNorm = 2.5;
        } else if (norm <= 5.0) {
            niceNorm = 5.0;
        } else {
            niceNorm = 10.0;
        }
        long step = Math.max(1000L, (long) (niceNorm * power));
        BigDecimal ceil = BigDecimal.valueOf(step * 4);
        if (ceil.compareTo(peak) < 0) {
            ceil = BigDecimal.valueOf((step + (long) power) * 4);
        }
        return ceil;
    }

    public static BigDecimal max(List<DayTotal> days) {
        return ceiling(days);
    }

    public static List<Tick> ticks(BigDecimal ceiling) {
        List<Tick> list = new ArrayList<>();
        if (ceiling == null || ceiling.signum() <= 0) {
            ceiling = BigDecimal.valueOf(100000);
        }
        long step = ceiling.longValue() / 4;
        for (int i = 0; i <= 4; i++) {
            double ratio = i / 4.0;
            int y = TOP + HEIGHT - (int) Math.round(ratio * HEIGHT);
            long val = step * i;
            list.add(new Tick(y, money(BigDecimal.valueOf(val))));
        }
        return list;
    }

    public static String money(BigDecimal value) {
        return value == null ? "0" : new DecimalFormat("#,##0").format(value);
    }

    public static List<ChartBar> bars(List<DayTotal> days, BigDecimal ceiling) {
        List<ChartBar> bars = new ArrayList<>();
        if (days.isEmpty()) {
            return bars;
        }
        if (ceiling == null || ceiling.signum() <= 0) {
            ceiling = BigDecimal.valueOf(100000);
        }
        int slot = (WIDTH - LEFT) / days.size();
        int groupWidth = Math.max(12, slot * 7 / 10);
        int innerGap = Math.max(2, groupWidth / 10);
        int barWidth = Math.max(4, (groupWidth - innerGap) / 2);
        int actualGroupWidth = barWidth * 2 + innerGap;
        int labelEvery = days.size() > 15 ? 3 : 1;
        SimpleDateFormat dayFormat = new SimpleDateFormat("MM-dd");

        for (int i = 0; i < days.size(); i++) {
            DayTotal d = days.get(i);
            int legitH = scale(d.getLegitTotal(), ceiling);
            int flaggedH = scale(d.getFlaggedTotal(), ceiling);

            int slotX = LEFT + i * slot;
            int groupX = slotX + (slot - actualGroupWidth) / 2;
            int legitX = groupX;
            int flaggedX = groupX + barWidth + innerGap;

            ChartBar b = new ChartBar();
            b.setHitX(slotX);
            b.setHitWidth(slot);
            b.setBarWidth(barWidth);
            b.setWidth(barWidth);
            b.setX(legitX);
            b.setLegitX(legitX);
            b.setLegitY(TOP + HEIGHT - legitH);
            b.setLegitHeight(legitH);
            b.setFlaggedX(flaggedX);
            b.setFlaggedY(TOP + HEIGHT - flaggedH);
            b.setFlaggedHeight(flaggedH);
            b.setLabelX(slotX + slot / 2);
            b.setLabel(i % labelEvery == 0 ? dayFormat.format(d.getDay()) : "");
            b.setTip(d.getDay() + ": " + d.getCount() + " transaction(s), valid "
                    + money(d.getLegitTotal()) + " VND, flagged "
                    + money(d.getFlaggedTotal()) + " VND");
            bars.add(b);
        }
        return bars;
    }

    public static List<ChartBar> bars(List<DayTotal> days) {
        return bars(days, ceiling(days));
    }

    private static int scale(BigDecimal value, BigDecimal max) {
        if (value == null || value.signum() <= 0) {
            return 0;
        }
        return value.multiply(BigDecimal.valueOf(HEIGHT)).divide(max, 0, BigDecimal.ROUND_HALF_UP).intValue();
    }
}
