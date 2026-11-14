import dao.SummaryDAO;
import dao.TaxDocumentDAO;
import dao.TaxToolsDAO;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import util.DBUtil;

public class TaxReportsTest {

    private static int failures = 0;
    private static int passes = 0;
    private static int user;
    private static String userCode;

    private static void check(String name, Object got, Object expected) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(got));
        if (ok) {
            passes++;
        } else {
            failures++;
        }
        System.out.println((ok ? "OK   " : "FAIL ") + name + " -> " + got + (ok ? "" : " (expected " + expected + ")"));
    }

    private static String one(String q, Object... args) throws Exception {
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(q)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static void run(String q, Object... args) throws Exception {
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(q)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            ps.executeUpdate();
        }
    }

    private static int count(String q, Object... args) throws Exception {
        return Integer.parseInt(one(q, args));
    }

    private static int order(String code, String amount, String status, Timestamp at, String discount, String vat, boolean included) throws Exception {
        run("INSERT INTO Orders (order_code, created_by, created_by_code, amount, cash_amount, status, expires_at, created_at, discount_amount, vat_amount, vat_included) VALUES (?, ?, ?, ?, 0, ?, ?, ?, ?, ?, ?)",
                code, user, userCode, new BigDecimal(amount), status, new Timestamp(at.getTime() + 600000), at, new BigDecimal(discount), new BigDecimal(vat), included);
        return count("SELECT order_id FROM Orders WHERE order_code = ?", code);
    }

    private static int order(String code, String amount, String status, Timestamp at) throws Exception {
        return order(code, amount, status, at, "0", "0", true);
    }

    private static int invoice(String no, String type, int sourceId, String total, String status) throws Exception {
        run("INSERT INTO Invoices (invoice_no, source_type, source_id, buyer_name, total_amount, status, created_by) VALUES (?, ?, ?, N'Test buyer', ?, ?, ?)",
                no, type, sourceId, new BigDecimal(total), status, user);
        return count("SELECT invoice_id FROM Invoices WHERE invoice_no = ?", no);
    }

    private static int sale(String ref, String channel, String status, String soldAt, String goods) throws Exception {
        run("INSERT INTO ChannelSales (channel, external_ref, sold_at, status, goods_amount, created_by) VALUES (?, ?, ?, ?, ?, ?)",
                channel, ref, Timestamp.valueOf(soldAt), status, new BigDecimal(goods), user);
        return count("SELECT sale_id FROM ChannelSales WHERE external_ref = ?", ref);
    }

    private static Map<String, Object> month(List<Map<String, Object>> rows, String ym) {
        for (Map<String, Object> r : rows) {
            if (ym.equals(String.valueOf(r.get("ym")))) {
                return r;
            }
        }
        return null;
    }

    private static Map<String, Object> channel(List<Map<String, Object>> rows, String ym, String ch) {
        for (Map<String, Object> r : rows) {
            if (ym.equals(String.valueOf(r.get("ym"))) && ch.equals(r.get("channel"))) {
                return r;
            }
        }
        return null;
    }

    private static String delta(Map<String, Object> after, Map<String, Object> before, String key) {
        BigDecimal a = after == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(after.get(key)));
        BigDecimal b = before == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(before.get(key)));
        return a.subtract(b).setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String diff(BigDecimal after, BigDecimal before) {
        return after.subtract(before).setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static Map<String, Object> find(List<Map<String, Object>> rows, String ref) {
        for (Map<String, Object> r : rows) {
            if (ref.equals(String.valueOf(r.get("ref")))) {
                return r;
            }
        }
        return null;
    }

    private static String kind(List<Map<String, Object>> rows, String ref) {
        Map<String, Object> r = find(rows, ref);
        return r == null ? "ABSENT" : String.valueOf(r.get("kind"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> recon() throws Exception {
        Class<?> c = Class.forName("controller.tax.TaxToolsServlet");
        Object servlet = c.getDeclaredConstructor().newInstance();
        Method m = c.getDeclaredMethod("reconRows");
        m.setAccessible(true);
        return (List<Map<String, Object>>) m.invoke(servlet);
    }

    private static byte[] bytes(int n, int seed) {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++) {
            b[i] = (byte) (i * 31 + seed);
        }
        return b;
    }

    private static boolean has(List<Map<String, Object>> docs, int id) {
        for (Map<String, Object> d : docs) {
            if (((Number) d.get("doc_id")).intValue() == id) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        user = Integer.parseInt(one("SELECT TOP 1 user_id FROM Users WHERE status = N'ACTIVE' ORDER BY user_id"));
        userCode = one("SELECT user_code FROM Users WHERE user_id = ?", user);
        String tag = "TRT" + (System.currentTimeMillis() % 1000000000L);
        String like = tag + "%";
        TaxToolsDAO dao = new TaxToolsDAO();
        SummaryDAO summary = new SummaryDAO();
        TaxDocumentDAO docs = new TaxDocumentDAO();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp ago5 = new Timestamp(now.getTime() - 300000);
        Date y19from = Date.valueOf("2019-01-01");
        Date y19to = Date.valueOf("2019-12-31");
        List<Map<String, Object>> baseStore = dao.storeMonthly(y19from, y19to);
        List<Map<String, Object>> baseChannel = dao.channelMonthly(y19from, y19to);
        BigDecimal baseYear2019 = summary.yearRevenue(2019);
        BigDecimal baseYear2018 = summary.yearRevenue(2018);
        BigDecimal baseYear2020 = summary.yearRevenue(2020);
        BigDecimal baseBetween = summary.revenueBetween(y19from, y19to);
        BigDecimal baseDec31 = summary.revenueBetween(y19to, y19to);
        BigDecimal baseJan1 = summary.revenueBetween(y19from, y19from);
        Map<String, Object> baseCounts = docs.counts();
        int baseMissing = ((Number) baseCounts.get("missing")).intValue();
        int baseUploaded = ((Number) baseCounts.get("uploaded")).intValue();
        int baseReview = ((Number) baseCounts.get("review")).intValue();
        int baseTotal = ((Number) baseCounts.get("total")).intValue();
        try {
            int oMatched = order(tag + "M", "7100001", "COMPLETED", ago5);
            invoice(tag + "IM", "ORDER", oMatched, "7100001", "ISSUED");
            int oMissing = order(tag + "N", "7100002", "COMPLETED", ago5);
            int oDiff = order(tag + "D", "7100003", "COMPLETED", ago5);
            invoice(tag + "ID", "ORDER", oDiff, "7100000", "ISSUED");
            int oDraft = order(tag + "F", "7100004", "COMPLETED", ago5);
            invoice(tag + "IF", "ORDER", oDraft, "7100004", "DRAFT");
            int oSent = order(tag + "S", "7100005", "COMPLETED", ago5);
            invoice(tag + "IS", "ORDER", oSent, "7100005", "SENT_TO_PROVIDER");
            int oDraftDiff = order(tag + "G", "7100006", "COMPLETED", ago5);
            invoice(tag + "IG", "ORDER", oDraftDiff, "7100007", "DRAFT");
            order(tag + "T1", "7100008", "COMPLETED", ago5);
            order(tag + "T2", "7100008", "COMPLETED", new Timestamp(ago5.getTime() + 30000));
            order(tag + "U1", "7100009", "COMPLETED", ago5);
            order(tag + "U2", "7100009", "COMPLETED", new Timestamp(ago5.getTime() + 61000));
            order(tag + "X", "7100010", "CANCELLED", ago5);
            order(tag + "P", "7100011", "PENDING", ago5);
            order(tag + "O", "7100012", "COMPLETED", new Timestamp(now.getTime() - 100L * 86400000L));
            int oCancelledInv = order(tag + "Y", "7100013", "CANCELLED", ago5);
            int iOrphanCancelled = invoice(tag + "IY", "ORDER", oCancelledInv, "7100013", "ISSUED");
            int iOrphanGone = invoice(tag + "IZ", "ORDER", 2147483000, "7100014", "ISSUED");
            int sOk = sale(tag + "SOK", "SHOPEE", "COMPLETED", "2019-02-10 10:00:00", "1");
            int sBad = sale(tag + "SBD", "SHOPEE", "CANCELLED", "2019-02-10 10:00:00", "1");
            invoice(tag + "CO", "CHANNEL_SALE", sOk, "1", "ISSUED");
            invoice(tag + "CB", "CHANNEL_SALE", sBad, "1", "ISSUED");

            List<Map<String, Object>> rows = recon();
            check("matched order", kind(rows, tag + "M"), "MATCHED");
            check("missing invoice", kind(rows, tag + "N"), "MISSING_INVOICE");
            check("amount difference", kind(rows, tag + "D"), "AMOUNT_DIFFERENCE");
            check("invoice not issued (DRAFT)", kind(rows, tag + "F"), "INVOICE_NOT_ISSUED");
            check("invoice not issued (SENT_TO_PROVIDER)", kind(rows, tag + "S"), "INVOICE_NOT_ISSUED");
            check("amount difference beats not issued", kind(rows, tag + "G"), "AMOUNT_DIFFERENCE");
            check("possible duplicate first twin", kind(rows, tag + "T1"), "POSSIBLE_DUPLICATE");
            check("possible duplicate second twin", kind(rows, tag + "T2"), "POSSIBLE_DUPLICATE");
            check("61 s apart is not a duplicate (first)", kind(rows, tag + "U1"), "MISSING_INVOICE");
            check("61 s apart is not a duplicate (second)", kind(rows, tag + "U2"), "MISSING_INVOICE");
            check("cancelled order not reconciled", kind(rows, tag + "X"), "ABSENT");
            check("pending order not reconciled", kind(rows, tag + "P"), "ABSENT");
            check("order older than 90 days not reconciled", kind(rows, tag + "O"), "ABSENT");
            check("matched diff is zero", String.valueOf(find(rows, tag + "M").get("diff")), "0.00");
            check("difference amount", String.valueOf(find(rows, tag + "D").get("diff")), "3.00");
            check("missing invoice diff equals order amount", String.valueOf(find(rows, tag + "N").get("diff")), "7100002.00");
            check("matched row key", find(rows, tag + "M").get("key"), "ORDER:" + oMatched);
            check("invoice number shown", find(rows, tag + "M").get("invoiceNo"), tag + "IM");

            Map<String, Object> orphan1 = find(rows, tag + "IY");
            check("invoice of a cancelled order is unmatched", kind(rows, tag + "IY"), "UNMATCHED_INVOICE");
            check("invoice without order is unmatched", kind(rows, tag + "IZ"), "UNMATCHED_INVOICE");
            check("invoice of a cancelled channel sale is unmatched", kind(rows, tag + "CB"), "UNMATCHED_INVOICE");
            check("invoice of a completed channel sale is not unmatched", kind(rows, tag + "CO"), "ABSENT");
            check("orphan key", orphan1.get("key"), "INVOICE:" + iOrphanCancelled);
            check("orphan has no POS amount", orphan1.get("posAmount"), null);
            check("orphan invoice amount", String.valueOf(orphan1.get("invoiceAmount")), "7100013.00");
            int mine = 0;
            for (Map<String, Object> r : rows) {
                if (String.valueOf(r.get("ref")).startsWith(tag)) {
                    mine++;
                }
            }
            check("exactly the expected rows are reported", mine, 10 + 3);
            long orphans = 0;
            for (Map<String, Object> r : dao.orphanInvoices()) {
                if (String.valueOf(r.get("invoice_no")).startsWith(tag)) {
                    orphans++;
                }
            }
            check("raw orphanInvoices has the three", orphans, 3L);

            check("note default is empty", find(rows, tag + "N").get("note"), "");
            check("not resolved by default", find(rows, tag + "N").get("resolved"), false);
            dao.saveReconNote("ORDER:" + oMissing, "Invoice will be issued tomorrow", false, user);
            dao.saveReconNote("INVOICE:" + iOrphanGone, "Order was deleted by mistake", true, user);
            rows = recon();
            check("saved note shown", find(rows, tag + "N").get("note"), "Invoice will be issued tomorrow");
            check("saved note not resolved", find(rows, tag + "N").get("resolved"), false);
            check("orphan note resolved", find(rows, tag + "IZ").get("resolved"), true);
            check("kind unaffected by note", kind(rows, tag + "N"), "MISSING_INVOICE");
            check("other rows have no note", find(rows, tag + "D").get("note"), "");
            dao.saveReconNote("ORDER:" + oMissing, "Issued and sent", true, user);
            rows = recon();
            check("second save updates note", find(rows, tag + "N").get("note"), "Issued and sent");
            check("second save resolves", find(rows, tag + "N").get("resolved"), true);
            check("second save keeps one row", count("SELECT COUNT(*) FROM TaxReconNotes WHERE ref_key = ?", "ORDER:" + oMissing), 1);
            dao.saveReconNote("ORDER:" + oMissing, "Reopened", false, user);
            check("reopen clears resolved", dao.reconNotes().get("ORDER:" + oMissing).get("resolved"), false);
            check("reconNotes map has orphan key", dao.reconNotes().containsKey("INVOICE:" + iOrphanGone), true);
            check("scalar counts rows", dao.scalar("SELECT COUNT(*) FROM Orders WHERE order_code LIKE ?", like) >= 14, true);
            check("scalar of no row is 0", dao.scalar("SELECT 1 WHERE 1 = 0"), 0);

            order(tag + "A1", "1000", "COMPLETED", Timestamp.valueOf("2018-12-31 23:59:59.999"));
            order(tag + "A2", "2000", "COMPLETED", Timestamp.valueOf("2019-01-01 00:00:00"));
            order(tag + "A3", "4000", "COMPLETED", Timestamp.valueOf("2019-12-31 23:59:59.998"));
            order(tag + "A4", "8000", "COMPLETED", Timestamp.valueOf("2020-01-01 00:00:00"));
            order(tag + "A5", "10800", "COMPLETED", Timestamp.valueOf("2019-06-15 12:00:00"), "500", "800", false);
            order(tag + "A6", "5400", "COMPLETED", Timestamp.valueOf("2019-06-20 12:00:00"), "0", "400", true);
            order(tag + "A7", "90000", "CANCELLED", Timestamp.valueOf("2019-06-21 12:00:00"));
            order(tag + "A8", "70000", "PENDING", Timestamp.valueOf("2019-06-22 12:00:00"));
            sale(tag + "C1", "SHOPEE", "COMPLETED", "2019-06-10 09:00:00", "100000");
            sale(tag + "C2", "SHOPEE", "RETURNED", "2019-06-11 09:00:00", "700");
            sale(tag + "C3", "TIKTOK", "COMPLETED", "2019-06-12 09:00:00", "30000");
            sale(tag + "C4", "TIKTOK", "CANCELLED", "2019-06-13 09:00:00", "500");
            sale(tag + "C5", "SHOPEE", "COMPLETED", "2018-12-31 23:59:59", "9000000");
            sale(tag + "C6", "SHOPEE", "COMPLETED", "2020-01-01 00:00:00", "9000000");
            sale(tag + "C7", "SHOPEE", "COMPLETED", "2019-12-31 23:59:59", "300");

            List<Map<String, Object>> store = dao.storeMonthly(y19from, y19to);
            Map<String, Object> jun = month(store, "2019-06");
            Map<String, Object> junBase = month(baseStore, "2019-06");
            check("June store net: vat on top excluded, vat included kept, cancelled and pending out", delta(jun, junBase, "net"), String.valueOf(10000 + 5400));
            check("June discounts only from completed", delta(jun, junBase, "discounts"), "500");
            check("June cancelled total", delta(jun, junBase, "cancelled"), "90000");
            check("January store net", delta(month(store, "2019-01"), month(baseStore, "2019-01"), "net"), "2000");
            check("December store net", delta(month(store, "2019-12"), month(baseStore, "2019-12"), "net"), "4000");
            check("2018-12 order not in the 2019 range", month(store, "2018-12"), null);
            check("2020-01 order not in the 2019 range", month(store, "2020-01"), null);
            check("month label format", String.valueOf(jun.get("ym")), "2019-06");

            List<Map<String, Object>> ch = dao.channelMonthly(y19from, y19to);
            check("Shopee June net", delta(channel(ch, "2019-06", "SHOPEE"), channel(baseChannel, "2019-06", "SHOPEE"), "net"), "100000");
            check("Shopee June removed (returned)", delta(channel(ch, "2019-06", "SHOPEE"), channel(baseChannel, "2019-06", "SHOPEE"), "removed"), "700");
            check("TikTok June net", delta(channel(ch, "2019-06", "TIKTOK"), channel(baseChannel, "2019-06", "TIKTOK"), "net"), "30000");
            check("TikTok June removed (cancelled)", delta(channel(ch, "2019-06", "TIKTOK"), channel(baseChannel, "2019-06", "TIKTOK"), "removed"), "500");
            check("Shopee December net at end of year", delta(channel(ch, "2019-12", "SHOPEE"), channel(baseChannel, "2019-12", "SHOPEE"), "net"), "300");
            check("Shopee 2018-12-31 outside the range", channel(ch, "2018-12", "SHOPEE"), null);

            String expectedDelta = String.valueOf(2000 + 4000 + 10000 + 5400 + 100000 + 30000 + 300 + 1);
            check("yearRevenue 2019 delta", diff(summary.yearRevenue(2019), baseYear2019), expectedDelta);
            check("yearRevenue 2018 delta (last instant of the year)", diff(summary.yearRevenue(2018), baseYear2018), String.valueOf(1000 + 9000000));
            check("yearRevenue 2020 delta (first instant of the year)", diff(summary.yearRevenue(2020), baseYear2020), String.valueOf(8000 + 9000000));
            check("revenueBetween whole 2019 delta", diff(summary.revenueBetween(y19from, y19to), baseBetween), expectedDelta);
            check("revenueBetween 31 Dec only", diff(summary.revenueBetween(y19to, y19to), baseDec31), String.valueOf(4000 + 300));
            check("revenueBetween 1 Jan only", diff(summary.revenueBetween(y19from, y19from), baseJan1), "2000");
            BigDecimal reportSum = BigDecimal.ZERO;
            for (Map<String, Object> r : store) {
                reportSum = reportSum.add(new BigDecimal(String.valueOf(r.get("net"))));
            }
            for (Map<String, Object> r : ch) {
                reportSum = reportSum.add(new BigDecimal(String.valueOf(r.get("net"))));
            }
            check("report taxable total equals yearRevenue", reportSum.setScale(0).toPlainString(), summary.yearRevenue(2019).setScale(0).toPlainString());

            byte[] f1 = bytes(5000, 1);
            int d1 = docs.insert(tag + " bank statement", "BANK", "2026-Q1", tag + "a.pdf", "application/pdf", f1.length, new ByteArrayInputStream(f1), user);
            check("insert returns an id", d1 > 0, true);
            Map<String, Object> m1 = docs.find(d1);
            check("size stored", String.valueOf(m1.get("size_bytes")), "5000");
            check("status after insert", m1.get("status"), "UPLOADED");
            check("file name stored", m1.get("file_name"), tag + "a.pdf");
            check("period stored", m1.get("period_label"), "2026-Q1");
            check("uploader name resolved", m1.get("uploaded_name") != null, true);
            Object[] file1 = docs.file(d1);
            check("file returns same bytes", Arrays.equals((byte[]) file1[2], f1), true);
            check("file returns content type", file1[1], "application/pdf");
            check("file returns file name", file1[0], tag + "a.pdf");
            int d2 = docs.insert(tag + " invoice batch", "INVOICE", "", tag + "b.csv", "text/csv", 3, new ByteArrayInputStream(new byte[]{1, 2, 3}), user);
            check("empty period stored as null", docs.find(d2).get("period_label"), null);
            check("zero-size boundary insert", docs.insert(tag + " empty", "OTHER", "", tag + "e.csv", "text/csv", 0, new ByteArrayInputStream(new byte[0]), user) > 0, true);
            run("INSERT INTO TaxDocuments (doc_name, category, status) VALUES (?, N'FILING', N'MISSING')", tag + " missing filing");
            int d3 = count("SELECT doc_id FROM TaxDocuments WHERE doc_name = ?", tag + " missing filing");
            check("missing row has no file", docs.file(d3), null);
            check("missing row cannot be deleted", docs.delete(d3), false);
            check("missing row cannot be marked review", docs.setReview(d3, true), false);
            check("find of unknown id", docs.find(2147483000), null);
            check("file of unknown id", docs.file(2147483000), null);

            byte[] f3 = bytes(777, 9);
            check("attach to an uploaded row is refused", docs.attach(d1, tag + "z.pdf", "application/pdf", 3, new ByteArrayInputStream(new byte[]{9, 9, 9}), user), false);
            check("refused attach leaves the bytes", Arrays.equals((byte[]) docs.file(d1)[2], f1), true);
            check("attach to a missing row", docs.attach(d3, tag + "c.docx", "application/msword", f3.length, new ByteArrayInputStream(f3), user), true);
            check("attach sets UPLOADED", docs.find(d3).get("status"), "UPLOADED");
            check("attach stores the size", String.valueOf(docs.find(d3).get("size_bytes")), "777");
            check("attach stores the bytes", Arrays.equals((byte[]) docs.file(d3)[2], f3), true);
            check("attach stores the name", docs.file(d3)[0], tag + "c.docx");
            check("attach twice is refused", docs.attach(d3, "x.pdf", "application/pdf", 1, new ByteArrayInputStream(new byte[]{1}), user), false);

            check("review flag on", docs.setReview(d1, true), true);
            check("status REVIEW", docs.find(d1).get("status"), "REVIEW");
            check("review flag on again is refused", docs.setReview(d1, true), false);
            check("review flag off", docs.setReview(d1, false), true);
            check("status back to UPLOADED", docs.find(d1).get("status"), "UPLOADED");
            check("unreview on an uploaded row is refused", docs.setReview(d1, false), false);
            docs.setReview(d2, true);

            check("search by name text", docs.search(tag + " bank", "", "").size(), 1);
            check("search by file name", docs.search(tag + "b.csv", "", "").size(), 1);
            check("search by period", has(docs.search("2026-Q1", "", ""), d1), true);
            check("search by tag finds all four", docs.search(tag, "", "").size(), 4);
            check("search by category", docs.search(tag, "BANK", "").size(), 1);
            check("search by status REVIEW", docs.search(tag, "", "REVIEW").size(), 1);
            check("search by status UPLOADED", docs.search(tag, "", "UPLOADED").size(), 3);
            check("search by category and status", docs.search(tag, "INVOICE", "REVIEW").size(), 1);
            check("search no match", docs.search(tag + "-nothing", "", "").size(), 0);
            check("search sorts REVIEW before UPLOADED", ((Number) docs.search(tag, "", "").get(0).get("doc_id")).intValue(), d2);

            Map<String, Object> c = docs.counts();
            check("counts total", ((Number) c.get("total")).intValue(), baseTotal + 4);
            check("counts uploaded", ((Number) c.get("uploaded")).intValue(), baseUploaded + 3);
            check("counts review", ((Number) c.get("review")).intValue(), baseReview + 1);
            check("counts missing", ((Number) c.get("missing")).intValue(), baseMissing);

            boolean tooBig = false;
            try {
                run("INSERT INTO TaxDocuments (doc_name, category, status, size_bytes) VALUES (?, N'OTHER', N'UPLOADED', 10485761)", tag + " big");
            } catch (java.sql.SQLException e) {
                tooBig = true;
            }
            check("database refuses more than 10 MB", tooBig, true);
            run("INSERT INTO TaxDocuments (doc_name, category, status, size_bytes) VALUES (?, N'OTHER', N'UPLOADED', 10485760)", tag + " max");
            check("database accepts exactly 10 MB", count("SELECT COUNT(*) FROM TaxDocuments WHERE doc_name = ?", tag + " max"), 1);

            check("delete an uploaded row", docs.delete(d1), true);
            check("deleted row is gone", docs.find(d1), null);
            check("delete again is refused", docs.delete(d1), false);
            check("delete a reviewed row", docs.delete(d2), true);
        } finally {
            run("DELETE FROM TaxReconNotes WHERE ref_key IN (SELECT 'ORDER:' + CAST(order_id AS NVARCHAR(20)) FROM Orders WHERE order_code LIKE ?) OR ref_key IN (SELECT 'INVOICE:' + CAST(invoice_id AS NVARCHAR(20)) FROM Invoices WHERE invoice_no LIKE ?)", like, like);
            run("DELETE FROM InvoiceLines WHERE invoice_id IN (SELECT invoice_id FROM Invoices WHERE invoice_no LIKE ?)", like);
            run("DELETE FROM Invoices WHERE invoice_no LIKE ?", like);
            run("DELETE FROM OrderItems WHERE order_id IN (SELECT order_id FROM Orders WHERE order_code LIKE ?)", like);
            run("DELETE FROM Orders WHERE order_code LIKE ?", like);
            run("DELETE FROM ChannelSales WHERE external_ref LIKE ?", like);
            run("DELETE FROM TaxDocuments WHERE doc_name LIKE ?", like);
        }
        check("no orders left", count("SELECT COUNT(*) FROM Orders WHERE order_code LIKE ?", like), 0);
        check("no invoices left", count("SELECT COUNT(*) FROM Invoices WHERE invoice_no LIKE ?", like), 0);
        check("no channel sales left", count("SELECT COUNT(*) FROM ChannelSales WHERE external_ref LIKE ?", like), 0);
        check("no documents left", count("SELECT COUNT(*) FROM TaxDocuments WHERE doc_name LIKE ?", like), 0);
        check("no notes left", count("SELECT COUNT(*) FROM TaxReconNotes WHERE note IN (N'Invoice will be issued tomorrow', N'Order was deleted by mistake', N'Issued and sent', N'Reopened')"), 0);
        check("year revenue restored", summary.yearRevenue(2019).compareTo(baseYear2019), 0);
        check("document counts restored", ((Number) docs.counts().get("total")).intValue(), baseTotal);
        System.out.println(passes + " OK checks");
        System.out.println(failures == 0 ? "ALL OK" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
