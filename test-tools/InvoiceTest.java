import dao.InvoiceDAO;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import model.Invoice;
import service.InvoiceService;
import util.DBUtil;

public class InvoiceTest {

    private static int failures = 0;

    private static void check(String name, Object got, Object expected) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(got));
        if (!ok) {
            failures++;
        }
        System.out.println((ok ? "OK   " : "FAIL ") + name + " -> " + got + (ok ? "" : " (expected " + expected + ")"));
    }

    private static int scalar(String sql, Object... args) throws Exception {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private static void exec(String sql, Object... args) throws Exception {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            ps.executeUpdate();
        }
    }

    public static void main(String[] args) throws Exception {
        check("csv plain", InvoiceService.csvCell("abc"), "abc");
        check("csv comma", InvoiceService.csvCell("a,b"), "\"a,b\"");
        check("csv quote", InvoiceService.csvCell("a\"b"), "\"a\"\"b\"");
        check("csv newline", InvoiceService.csvCell("a\nb"), "\"a\nb\"");
        check("csv formula =", InvoiceService.csvCell("=1+1"), "'=1+1");
        check("csv formula +", InvoiceService.csvCell("+1"), "'+1");
        check("csv formula -", InvoiceService.csvCell("-1"), "'-1");
        check("csv formula @", InvoiceService.csvCell("@x"), "'@x");
        check("csv formula and comma", InvoiceService.csvCell("=a,b"), "\"'=a,b\"");
        check("csv null", InvoiceService.csvCell(null), "");

        check("vat 108000 at 8", InvoiceService.vatForChannel(new BigDecimal("108000"), new BigDecimal("8")), "8000");
        check("vat 1000 at 0", InvoiceService.vatForChannel(new BigDecimal("1000"), BigDecimal.ZERO), "0");
        check("vat 1000 at 8", InvoiceService.vatForChannel(new BigDecimal("1000"), new BigDecimal("8")), "74");
        check("vat 5 at 100 half up", InvoiceService.vatForChannel(new BigDecimal("5"), new BigDecimal("100")), "3");

        check("tax 10", InvoiceService.validTaxCode("0123456789"), true);
        check("tax 13", InvoiceService.validTaxCode("0123456789-001"), true);
        check("tax 9", InvoiceService.validTaxCode("012345678"), false);
        check("tax letters", InvoiceService.validTaxCode("01234567ab"), false);
        check("tax bad suffix", InvoiceService.validTaxCode("0123456789-01"), false);
        check("tax newline", InvoiceService.validTaxCode("0123456789\n"), false);

        check("provider ok", InvoiceService.validProviderNo("C26TAA/0001-2.x_y"), true);
        check("provider empty", InvoiceService.validProviderNo(""), false);
        check("provider space", InvoiceService.validProviderNo("A B"), false);
        check("provider 30", InvoiceService.validProviderNo("12345678901234567890123456789A"), true);
        check("provider 31", InvoiceService.validProviderNo("12345678901234567890123456789AB"), false);
        check("lookup 40", InvoiceService.validLookupCode("1234567890123456789012345678901234567890"), true);
        check("lookup 41", InvoiceService.validLookupCode("12345678901234567890123456789012345678901"), false);
        check("lookup bad char", InvoiceService.validLookupCode("ab;cd"), false);
        check("lookup null", InvoiceService.validLookupCode(null), false);

        check("invoice no", InvoiceService.invoiceNo(42), "INV000042");

        int saleId = scalar("SELECT TOP 1 sale_id FROM ChannelSales WHERE status = N'COMPLETED' AND sale_id NOT IN "
                + "(SELECT source_id FROM Invoices WHERE source_type = N'CHANNEL_SALE') ORDER BY sale_id");
        int actor = scalar("SELECT TOP 1 user_id FROM Users ORDER BY user_id");
        check("seed sale found", saleId > 0, true);
        int before = scalar("SELECT COUNT(*) FROM Invoices");
        InvoiceService svc = new InvoiceService();
        InvoiceDAO dao = new InvoiceDAO();
        Integer createdId = null;
        try {
            Invoice inv = svc.draftFromChannelSale(saleId, actor);
            createdId = inv.getInvoiceId();
            check("draft status", inv.getStatus(), "DRAFT");
            check("draft number", inv.getInvoiceNo(), InvoiceService.invoiceNo(createdId));
            check("draft buyer", inv.getBuyerName(), "Retail customer");
            check("draft lines", dao.find(createdId).getLines().size(), 1);
            boolean refused = false;
            try {
                svc.draftFromChannelSale(saleId, actor);
            } catch (InvoiceService.Refused e) {
                refused = true;
            }
            check("second draft refused", refused, true);
            check("invoice count +1", scalar("SELECT COUNT(*) FROM Invoices"), before + 1);
            check("edit draft", svc.updateBuyer(createdId, "Cong ty A, B", "0123456789", "1 Main St"), true);
            check("markSent 1", svc.markSent(createdId), true);
            check("markSent 2", svc.markSent(createdId), false);
            check("edit sent refused", svc.updateBuyer(createdId, "X", null, null), false);
            check("delete sent refused", svc.delete(createdId), false);
            check("issue bad value", svc.markIssued(createdId, "bad value", "LK1"), false);
            check("issue ok", svc.markIssued(createdId, "C26TAA-1", "LOOK123"), true);
            check("issue again", svc.markIssued(createdId, "C26TAA-2", "LOOK124"), false);
            check("edit issued refused", svc.updateBuyer(createdId, "Y", null, null), false);
            check("delete issued refused", svc.delete(createdId), false);
            String csv = svc.exportCsv(createdId);
            check("export has number", csv.contains(inv.getInvoiceNo()), true);
            check("export has quoted buyer", csv.contains("\"Cong ty A, B\""), true);
            check("export has header", csv.contains("line_no,name,quantity,unit_price,subtotal"), true);
        } finally {
            if (createdId != null) {
                exec("DELETE FROM InvoiceLines WHERE invoice_id = ?", createdId);
                exec("DELETE FROM Invoices WHERE invoice_id = ?", createdId);
            }
        }
        check("invoices restored", scalar("SELECT COUNT(*) FROM Invoices"), before);
        check("no stray lines", scalar("SELECT COUNT(*) FROM InvoiceLines WHERE invoice_id NOT IN (SELECT invoice_id FROM Invoices)"), 0);

        System.out.println(failures == 0 ? "ALL OK" : failures + " FAILED");
        if (failures != 0) {
            System.exit(1);
        }
    }
}
