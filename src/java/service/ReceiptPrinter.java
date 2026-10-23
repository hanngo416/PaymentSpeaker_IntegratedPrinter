package service;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import model.OrderItem;
import model.view.ReceiptRow;

public class ReceiptPrinter {

    public static final int COLUMNS = 32;

    private static final int BOLD = 1;
    private static final int CENTER = 2;
    private static final int BIG = 4;
    private static final int RULE = 8;

    public static final class Line {
        final String left;
        final String right;
        final int style;

        Line(String left, String right, int style) {
            this.left = left;
            this.right = right;
            this.style = style;
        }
    }

    public synchronized void print(String printerName, List<Line> lines) throws PrinterException {
        if (printerName == null || printerName.isEmpty()) {
            throw new PrinterException("No printer is selected in Settings.");
        }
        PrintService service = null;
        for (PrintService s : PrintServiceLookup.lookupPrintServices(null, null)) {
            if (s.getName().equals(printerName)) {
                service = s;
            }
        }
        if (service == null) {
            throw new PrinterException("Printer \"" + printerName + "\" is not installed on the server PC.");
        }
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(service);
        job.setJobName("Tinky receipt");
        job.setPrintable(new Slip(lines), job.defaultPage());
        job.print();
    }

    public static List<Line> receipt(ReceiptRow r, List<OrderItem> items, boolean reprint) {
        List<Line> l = new ArrayList<>();
        l.add(new Line("TINKY POS", null, BOLD | CENTER | BIG));
        l.add(new Line("PAYMENT RECEIPT", null, BOLD | CENTER));
        if (reprint) {
            l.add(new Line("* REPRINT COPY *", null, CENTER));
        }
        l.add(rule());
        l.add(row("Order", r.getOrderCode()));
        l.add(row("Time", r.getTransTime() == null ? "" : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(r.getTransTime())));
        l.add(row("Cashier", r.getCashierCode()));
        if (r.getBankTransId() != null && !r.getBankTransId().isEmpty()) {
            l.add(row("Bank ref", r.getBankTransId()));
        }
        boolean cash = r.getCashAmount().signum() > 0;
        boolean transfer = r.getAmount() != null && r.getAmount().signum() > 0;
        l.add(row("Payment", cash && transfer ? "Cash + VietQR" : cash ? "Cash" : "VietQR"));
        l.add(rule());
        if (items != null && !items.isEmpty()) {
            for (OrderItem it : items) {
                for (String part : wrap(it.getProductName(), COLUMNS)) {
                    l.add(new Line(part, null, 0));
                }
                l.add(new Line("  " + it.getQuantity() + " x " + money(it.getUnitPrice()), money(it.getSubtotal()), 0));
            }
            l.add(rule());
        }
        l.add(row("Subtotal", money(r.getSubtotal()) + " VND"));
        if (r.getDiscountAmount().signum() > 0) {
            String code = r.getDiscountCode() == null ? "" : " " + r.getDiscountCode();
            l.add(row("Discount" + code, "-" + money(r.getDiscountAmount()) + " VND"));
        }
        if (r.getAffiliateCode() != null) {
            l.add(row("Referral code", r.getAffiliateCode()));
        }
        if (cash) {
            l.add(row("  Cash", money(r.getCashAmount()) + " VND"));
            if (transfer) {
                l.add(row("  VietQR", money(r.getAmount()) + " VND"));
            }
        }
        if (r.getVatAmount().signum() > 0) {
            l.add(row("  VAT " + r.getVatRate().stripTrailingZeros().toPlainString() + "%" + (r.isVatIncluded() ? " incl." : ""), money(r.getVatAmount()) + " VND"));
        }
        l.add(rule());
        l.add(new Line("TOTAL PAID", money(r.getOrderAmount()) + " VND", BOLD | BIG));
        l.add(rule());
        l.add(new Line("TRANSACTION COMPLETED", null, BOLD | CENTER));
        l.add(new Line("Thank you for your purchase!", null, CENTER));
        return l;
    }

    public static List<Line> testSlip(String printerName) {
        List<Line> l = new ArrayList<>();
        l.add(new Line("TINKY POS", null, BOLD | CENTER | BIG));
        l.add(new Line("PRINTER TEST", null, BOLD | CENTER));
        l.add(rule());
        l.add(row("Printer", printerName));
        l.add(row("Time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())));
        l.add(new Line("12345678901234567890123456789012", null, 0));
        l.add(new Line("If you can read this line fully,", null, 0));
        l.add(new Line("the printer is set up correctly.", null, 0));
        return l;
    }

    private static Line rule() {
        return new Line("", null, RULE);
    }

    private static Line row(String label, String value) {
        return new Line(label, ascii(value == null ? "" : value), 0);
    }

    private static String money(BigDecimal v) {
        return NumberFormat.getIntegerInstance(Locale.US).format(v == null ? BigDecimal.ZERO : v);
    }

    private static String ascii(String s) {
        String d = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D');
        StringBuilder sb = new StringBuilder();
        for (char c : d.toCharArray()) {
            sb.append(c >= 32 && c < 127 ? c : '?');
        }
        return sb.toString();
    }

    private static List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        String rest = ascii(text == null ? "" : text).trim();
        while (rest.length() > width) {
            int cut = rest.lastIndexOf(' ', width);
            if (cut <= 0) {
                cut = width;
            }
            out.add(rest.substring(0, cut).trim());
            rest = rest.substring(cut).trim();
        }
        out.add(rest);
        return out;
    }

    private static final class Slip implements Printable {

        private final List<Line> lines;

        Slip(List<Line> lines) {
            this.lines = lines;
        }

        @Override
        public int print(Graphics g, PageFormat pf, int pageIndex) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            double width = pf.getImageableWidth();
            double height = pf.getImageableHeight();

            float size = 12f;
            Font base = new Font(Font.MONOSPACED, Font.PLAIN, 1);
            while (size > 5f) {
                FontMetrics fm = g2.getFontMetrics(base.deriveFont(size));
                if (fm.charWidth('M') * COLUMNS <= width) {
                    break;
                }
                size -= 0.5f;
            }

            List<List<Line>> pages = new ArrayList<>();
            List<Line> current = new ArrayList<>();
            double used = 0;
            for (Line line : lines) {
                double h = lineHeight(g2.getFontMetrics(fontFor(base, size, line)), line);
                if (used + h > height && !current.isEmpty()) {
                    pages.add(current);
                    current = new ArrayList<>();
                    used = 0;
                }
                current.add(line);
                used += h;
            }
            pages.add(current);
            if (pageIndex >= pages.size()) {
                return NO_SUCH_PAGE;
            }

            g2.translate(pf.getImageableX(), pf.getImageableY());
            double y = 0;
            for (Line line : pages.get(pageIndex)) {
                Font f = fontFor(base, size, line);
                g2.setFont(f);
                FontMetrics fm = g2.getFontMetrics();
                if ((line.style & RULE) != 0) {
                    float[] dash = {3f, 3f};
                    g2.setStroke(new java.awt.BasicStroke(1f, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER, 10f, dash, 0f));
                    int ly = (int) (y + lineHeight(fm, line) / 2.0);
                    g2.drawLine(0, ly, (int) width, ly);
                    y += lineHeight(fm, line);
                    continue;
                }
                y += fm.getAscent();
                if (line.right != null) {
                    g2.drawString(line.left, 0, (float) y);
                    g2.drawString(line.right, (float) (width - fm.stringWidth(line.right)), (float) y);
                } else if ((line.style & CENTER) != 0) {
                    g2.drawString(line.left, (float) ((width - fm.stringWidth(line.left)) / 2), (float) y);
                } else {
                    g2.drawString(line.left, 0, (float) y);
                }
                y += fm.getDescent();
            }
            return PAGE_EXISTS;
        }

        private static double lineHeight(FontMetrics fm, Line line) {
            double h = fm.getAscent() + fm.getDescent();
            return (line.style & RULE) != 0 ? h * 0.6 : h;
        }

        private static Font fontFor(Font base, float size, Line line) {
            float s = (line.style & BIG) != 0 ? size * 1.4f : size;
            return base.deriveFont((line.style & BOLD) != 0 ? Font.BOLD : Font.PLAIN, s);
        }
    }
}
