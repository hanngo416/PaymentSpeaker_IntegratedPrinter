import java.math.BigDecimal;
import model.TaxParams;
import model.view.TaxEstimate;
import service.TaxEstimator;

public class TaxTest {

    private static int failures = 0;

    private static TaxParams params(String reduction) {
        TaxParams p = new TaxParams();
        p.setThreshold(new BigDecimal("1000000000"));
        p.setPitCeiling(new BigDecimal("3000000000"));
        p.setVatRatePct(new BigDecimal("1"));
        p.setPitRatePct(new BigDecimal("0.5"));
        p.setVatReductionPct(new BigDecimal(reduction));
        p.setWarn1(new BigDecimal("70"));
        p.setWarn2(new BigDecimal("90"));
        p.setWarn3(new BigDecimal("100"));
        return p;
    }

    private static void check(String name, Object got, Object expected) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(got));
        if (!ok) {
            failures++;
        }
        System.out.println((ok ? "OK   " : "FAIL ") + name + " -> " + got + (ok ? "" : " (expected " + expected + ")"));
    }

    private static String num(BigDecimal v) {
        return v == null ? "null" : v.toBigInteger().toString();
    }

    public static void main(String[] args) {
        TaxEstimate e = TaxEstimator.estimate(new BigDecimal("0"), params("20"));
        check("zero status", e.getStatus(), "UNDER");
        check("zero level", e.getLevel(), 0);
        check("zero vat", num(e.getVat()), "0");
        check("zero pit", num(e.getPit()), "0");

        check("699999999 level", TaxEstimator.estimate(new BigDecimal("699999999"), params("20")).getLevel(), 0);
        check("700000000 level", TaxEstimator.estimate(new BigDecimal("700000000"), params("20")).getLevel(), 1);
        check("900000000 level", TaxEstimator.estimate(new BigDecimal("900000000"), params("20")).getLevel(), 2);
        check("900000000 percent", TaxEstimator.estimate(new BigDecimal("900000000"), params("20")).getPercent().toPlainString(), "90.00");

        e = TaxEstimator.estimate(new BigDecimal("1000000000"), params("20"));
        check("threshold status", e.getStatus(), "UNDER");
        check("threshold level", e.getLevel(), 3);
        check("threshold vat", num(e.getVat()), "0");
        check("threshold pit", num(e.getPit()), "0");
        check("threshold remaining", num(e.getRemaining()), "0");

        e = TaxEstimator.estimate(new BigDecimal("1500000000"), params("20"));
        check("1.5bn status", e.getStatus(), "OVER");
        check("1.5bn vat with reduction", num(e.getVat()), "12000000");
        check("1.5bn pit", num(e.getPit()), "2500000");
        check("1.5bn remaining", num(e.getRemaining()), "0");

        e = TaxEstimator.estimate(new BigDecimal("1500000000"), params("0"));
        check("1.5bn vat no reduction", num(e.getVat()), "15000000");

        e = TaxEstimator.estimate(new BigDecimal("3000000000"), params("20"));
        check("ceiling status", e.getStatus(), "OVER");
        check("ceiling pit", num(e.getPit()), "10000000");

        e = TaxEstimator.estimate(new BigDecimal("3000000001"), params("20"));
        check("over ceiling status", e.getStatus(), "OVER_CEILING");
        check("over ceiling pit", num(e.getPit()), "null");
        check("over ceiling vat present", e.getVat() != null, true);

        e = TaxEstimator.estimate(new BigDecimal("250000000"), params("20"));
        check("remaining", num(e.getRemaining()), "750000000");

        System.out.println(failures == 0 ? "ALL OK" : failures + " FAILED");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
