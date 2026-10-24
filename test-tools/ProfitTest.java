import java.math.BigDecimal;
import model.TaxParams;
import service.ProfitService;

public class ProfitTest {

    private static int failures = 0;

    private static TaxParams params(String vat, String reduction) {
        TaxParams p = new TaxParams();
        p.setVatRatePct(new BigDecimal(vat));
        p.setVatReductionPct(new BigDecimal(reduction));
        return p;
    }

    private static BigDecimal bd(long v) {
        return BigDecimal.valueOf(v);
    }

    private static void check(String name, Object got, Object expected) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(got));
        if (!ok) {
            failures++;
        }
        System.out.println((ok ? "OK   " : "FAIL ") + name + " -> " + got + (ok ? "" : " (expected " + expected + ")"));
    }

    public static void main(String[] args) {
        check("profit basic", ProfitService.profitOf(bd(1000), bd(100), bd(50), bd(20), bd(400), bd(10)).toBigInteger(), "420");
        check("profit negative kept", ProfitService.profitOf(bd(100), bd(0), bd(0), bd(0), bd(150), bd(0)).toBigInteger(), "-50");
        check("reserve 1,000,000 vat 8 red 0", ProfitService.taxReserve(bd(1000000), params("8", "0")).toPlainString(), "80000");
        check("reserve vat 8 red 20", ProfitService.taxReserve(bd(1000000), params("8", "20")).toPlainString(), "64000");
        check("reserve vat 1 red 20 rounds", ProfitService.taxReserve(bd(1234567), params("1", "20")).toPlainString(), "9877");
        check("reserve zero revenue", ProfitService.taxReserve(BigDecimal.ZERO, params("8", "20")).toPlainString(), "0");
        check("reserve null params", ProfitService.taxReserve(bd(1000000), null).toPlainString(), "0");
        System.out.println(failures == 0 ? "ALL OK" : failures + " FAILED");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
