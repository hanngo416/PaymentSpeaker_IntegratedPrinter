import dao.ChannelSaleDAO;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import service.SalesImportService;
import util.DBUtil;

public class ImportTest {

    private static int failures = 0;

    private static void check(String name, Object got, Object expected) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(got));
        if (!ok) {
            failures++;
        }
        System.out.println((ok ? "OK   " : "FAIL ") + name + " -> " + got + (ok ? "" : " (expected " + expected + ")"));
    }

    private static InputStream in(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    private static final String HEAD = "order_ref,sold_at,status,goods_amount,platform_fee,shipping_fee,commission,cost\n";

    private static int count(String like) throws Exception {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM ChannelSales WHERE external_ref LIKE ?")) {
            ps.setString(1, like);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static void cleanup() throws Exception {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM ChannelSales WHERE external_ref LIKE 'TMP-IMPORT-%'")) {
            ps.executeUpdate();
        }
    }

    public static void main(String[] args) throws Exception {
        String good = "\uFEFF" + HEAD
                + "T-1,2026-10-01,completed,\"1,234,567\",1000,2000,0,500000\r\n"
                + "T-2,01/10/2026 10:30,,200000,,,,\r\n"
                + "T-3,2026-10-02 08:15:30,RETURNED,\"1.000.000\", 5000 ,0,0,0\r\n";
        SalesImportService.Parsed p = SalesImportService.parse(in(good));
        check("good errors", p.errors, "[]");
        check("good rows", p.rows.size(), 3);
        check("good goods 1", p.rows.get(0).getGoodsAmount().toPlainString(), "1234567");
        check("good status empty", p.rows.get(1).getStatus(), "COMPLETED");
        check("good status returned", p.rows.get(2).getStatus(), "RETURNED");
        check("good dot amount", p.rows.get(2).getGoodsAmount().toPlainString(), "1000000");
        check("good cost", p.rows.get(0).getCostAmount().toPlainString(), "500000");
        check("good datetime", p.rows.get(1).getSoldAt().toString(), "2026-10-01 10:30:00.0");
        check("good date only", p.rows.get(0).getSoldAt().toString(), "2026-10-01 00:00:00.0");

        p = SalesImportService.parse(in(HEAD + "A-1,2026-10-01,,1000,,,,\nA-2,2026-10-01,,abc,,,,\n"));
        check("bad amount error count", p.errors.size(), 1);
        check("bad amount line", p.errors.get(0).startsWith("Line 3:"), true);

        p = SalesImportService.parse(in("sold_at,goods_amount\n2026-10-01,1000\n"));
        check("missing column", p.errors, "[Missing column: order_ref]");

        p = SalesImportService.parse(in("Order ID,Order Date,Total Amount,Commission Fee\nX-1,2026-10-01,5000,100\n"));
        check("alias errors", p.errors, "[]");
        check("alias rows", p.rows.size(), 1);
        check("alias fee", p.rows.get(0).getPlatformFee().toPlainString(), "100");

        p = SalesImportService.parse(in(HEAD + "D-1,2026-10-01,,1000,,,,\nD-1,2026-10-01,,1000,,,,\nD-2,2026-10-01,,1000,,,,\n"));
        check("dup errors", p.errors, "[]");
        check("dup rows", p.rows.size(), 2);
        check("dup count", p.duplicates, 1);

        StringBuilder big = new StringBuilder(HEAD);
        for (int i = 0; i < 5001; i++) {
            big.append("B-").append(i).append(",2026-10-01,,1000,,,,\n");
        }
        p = SalesImportService.parse(in(big.toString()));
        check("5001 rows rejected", p.errors.size() > 0 && p.rows.isEmpty(), true);
        big = new StringBuilder(HEAD);
        for (int i = 0; i < 5000; i++) {
            big.append("B-").append(i).append(",2026-10-01,,1000,,,,\n");
        }
        p = SalesImportService.parse(in(big.toString()));
        check("5000 rows ok", p.rows.size(), 5000);

        p = SalesImportService.parse(in("order_ref,sold_at,goods_amount,note\r\n\"N-1\",2026-10-01,1000,\"two\r\nlines\"\r\n\r\nN-2,2026-10-01,zz,\"say \"\"hi\"\"\"\r\n"));
        check("newline lines", p.errors.size() + ":" + (p.errors.isEmpty() ? "" : p.errors.get(0).substring(0, 7)), "1:Line 5:");

        p = SalesImportService.parse(in(HEAD + "F-1,2999-01-01,,1000,,,,\nF-2,2026-10-01,paid,1000,,,,\nF-3,2026-10-01,,60000000,,,,\nF-4,2026-10-01,,0,,,,\nF-5,2026-10-01,,-5,,,,\n"));
        check("line errors", p.errors.size(), 5);

        p = SalesImportService.parse(in(HEAD + "G-1,2026-10-01,,\"1,000,000,000,000,000,000,000\",,,,\n"));
        check("huge grouped amount", p.errors.size() == 1 && p.errors.get(0).startsWith("Line 2:"), true);
        p = SalesImportService.parse(in(HEAD + "G-2,2026-10-01,,\"1,234.567\",,,,\n"));
        check("mixed separators", p.errors.size() == 1 && p.errors.get(0).startsWith("Line 2:"), true);
        p = SalesImportService.parse(in(HEAD + "G-3,2026-10-01,,\"1,234,567\",,,,\nG-4,2026-10-01,,\"1.234.567\",,,,\n"));
        check("single separators ok", p.errors.size() + ":" + p.rows.size(), "0:2");
        p = SalesImportService.parse(in(HEAD + "Q-1,2026-10-01,,\"1000,,,,\n"));
        check("unterminated quote", p.errors, "[Line 2: a quoted value is not closed.]");
        p = SalesImportService.parse(in("order_ref,sold_at,goods_amount\rL-1,2026-10-01,1000\rL-2,2026-10-01,zz\r"));
        check("lone CR", p.errors.size() == 1 && p.errors.get(0).startsWith("Line 3:") && p.rows.isEmpty(), true);
        p = SalesImportService.parse(in(HEAD));
        check("header only", p.errors, "[The file has no data rows.]");
        p = SalesImportService.parse(in("\uFEFF"));
        check("bom only", p.errors, "[The file is empty.]");
        p = SalesImportService.parse(in(""));
        check("empty", p.errors, "[The file is empty.]");

        String shopeeCsv = "Mã đơn hàng,Ngày đặt hàng,Trạng thái đơn hàng,Tổng số tiền người mua thanh toán,Phí dịch vụ,Phí vận chuyển người bán trả,Phí tiếp thị liên kết,Giá vốn\n261031SP001,2026-10-01 09:30:00,Hoàn thành,350000,28000,0,14000,180000\n";
        String tiktokCsv = "Order ID,Created Time,Order Status,SKU Subtotal After Discount,Payment Platform Fee,Seller Shipping Fee,Creator Commission,Cost of Goods\nTT261031TK01,2026-10-01 08:45:00,Delivered,420000,25200,0,21000,210000\n";

        SalesImportService.Parsed pShopee = SalesImportService.parse(in(shopeeCsv), "SHOPEE");
        check("shopee adapter parsed rows", pShopee.rows.size(), 1);
        check("shopee adapter no errors", pShopee.errors.size(), 0);

        String fullShopee = "Mã đơn hàng,Ngày đặt hàng,Trạng thái đơn hàng,Mã SKU,Tên sản phẩm,Tên phân loại hàng,Số lượng,Tổng giá bán sản phẩm,Mã giảm giá của Shop,Mã giảm giá của Shopee,Phí dịch vụ,Phí xử lý giao dịch,Phí vận chuyển người bán trả,Phí tiếp thị liên kết,Số tiền thực nhận,Kênh bán hàng,Giá vốn\n"
                + "261031SP001,2026-10-31 09:30:00,Hoàn thành,CF-DEN-01,Cà phê đen Tinky,Ly lớn,2,70000,5000,10000,5600,2100,0,3500,53800,LIVE,28000\n";
        SalesImportService.Parsed pFullShopee = SalesImportService.parse(in(fullShopee), "SHOPEE");
        check("full shopee parsed rows", pFullShopee.rows.size(), 1);
        check("full shopee items parsed", pFullShopee.rows.get(0).getItems().size(), 1);
        check("full shopee traffic parsed", pFullShopee.rows.get(0).getTrafficSource(), "LIVE");

        SalesImportService.Parsed pTikTok = SalesImportService.parse(in(tiktokCsv), "TIKTOK");
        check("tiktok adapter parsed rows", pTikTok.rows.size(), 1);
        check("tiktok adapter no errors", pTikTok.errors.size(), 0);

        String fullTikTok = "Order ID,Created Time,Order Status,Seller SKU,Product Name,Variation,Quantity,Subtotal after seller discounts,Seller discounts,TikTok discounts,TikTok Shop commission fee,Transaction fee,Seller Shipping Fee,Creator Commission,Total settlement amount,Traffic Type,Cost of Goods\n"
                + "TT261031TK01,2026-10-31 08:45:00,Completed,CF-DEN-01,Cà phê đen Tinky,Ly lớn,2,70000,-5000,-10000,-4200,-2100,0,-3500,55200,LIVE,28000\n";
        SalesImportService.Parsed pFullTikTok = SalesImportService.parse(in(fullTikTok), "TIKTOK");
        check("full tiktok parsed rows", pFullTikTok.rows.size(), 1);
        check("full tiktok items parsed", pFullTikTok.rows.get(0).getItems().size(), 1);
        check("full tiktok traffic parsed", pFullTikTok.rows.get(0).getTrafficSource(), "LIVE");
        check("full tiktok service fee parsed", pFullTikTok.rows.get(0).getServiceFee().toPlainString(), "4200");
        check("full tiktok transaction fee parsed", pFullTikTok.rows.get(0).getTransactionFee().toPlainString(), "2100");
        check("full tiktok seller discount parsed", pFullTikTok.rows.get(0).getSellerDiscount().toPlainString(), "5000");

        SalesImportService.Parsed pMismatch = SalesImportService.parse(in(tiktokCsv), "SHOPEE");
        check("tiktok file on shopee adapter rejected", pMismatch.errors.size() > 0 && pMismatch.rows.isEmpty(), true);

        String text = HEAD + "TMP-IMPORT-1,2026-10-01,,\"1,234,567\",0,0,0,0\nTMP-IMPORT-2,2026-10-01,,2000,0,0,0,0\nTMP-IMPORT-3,2026-10-01,,3000,0,0,0,0\n";
        int actor = Integer.parseInt(args.length > 0 ? args[0] : "1");
        SalesImportService svc = new SalesImportService();
        try {
            cleanup();
            SalesImportService.Result r = svc.importCsv(in(text), "SHOPEE", actor);
            check("db first imported", r.imported, 3);
            check("db first dup", r.duplicates, 0);
            check("db first rows", count("TMP-IMPORT-%"), 3);
            r = svc.importCsv(in(text), "SHOPEE", actor);
            check("db second imported", r.imported, 0);
            check("db second dup", r.duplicates, 3);
            r = svc.importCsv(in(HEAD + "TMP-IMPORT-4,2026-10-01,,1000,,,,\nTMP-IMPORT-5,2026-10-01,,abc,,,,\n"), "SHOPEE", actor);
            check("db bad file imported", r.imported, 0);
            check("db bad file errors", r.errors.size(), 1);
            check("db bad file inserted nothing", count("TMP-IMPORT-4"), 0);
        } finally {
            cleanup();
            check("db cleanup", count("TMP-IMPORT-%"), 0);
        }
        System.out.println(failures == 0 ? "ALL OK" : failures + " FAILED");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
