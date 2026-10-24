package service;

import dao.ChannelSaleDAO;
import dao.InvoiceDAO;
import dao.OrderItemDAO;
import dao.SettingDAO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.regex.Pattern;
import model.ChannelSale;
import model.Invoice;
import model.InvoiceLine;
import model.Order;
import model.OrderItem;
import model.enums.OrderStatus;

public class InvoiceService {

    public static final String SELLER_NAME = "seller_name";
    public static final String SELLER_TAX_CODE = "seller_tax_code";
    public static final String SELLER_ADDRESS = "seller_address";
    public static final String DEFAULT_BUYER = "Retail customer";

    private static final Pattern TAX_CODE = Pattern.compile("\\d{10}(-\\d{3})?");
    private static final Pattern PROVIDER_CHARS = Pattern.compile("[A-Za-z0-9/_.-]+");
    private static final String EOL = "\r\n";

    public static class Refused extends Exception {

        private final String code;

        public Refused(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    private final InvoiceDAO dao = new InvoiceDAO();
    private final ChannelSaleDAO saleDAO = new ChannelSaleDAO();
    private final OrderItemDAO itemDAO = new OrderItemDAO();
    private final SettingDAO settingDAO = new SettingDAO();

    public static String invoiceNo(int id) {
        return String.format("INV%06d", id);
    }

    public static boolean validTaxCode(String s) {
        return s != null && TAX_CODE.matcher(s).matches();
    }

    public static boolean validProviderNo(String s) {
        return s != null && s.length() >= 1 && s.length() <= 30 && PROVIDER_CHARS.matcher(s).matches();
    }

    public static boolean validLookupCode(String s) {
        return s != null && s.length() >= 1 && s.length() <= 40 && PROVIDER_CHARS.matcher(s).matches();
    }

    public static BigDecimal vatForChannel(BigDecimal goods, BigDecimal ratePercent) {
        if (goods == null || ratePercent == null || ratePercent.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return goods.multiply(ratePercent).divide(ratePercent.add(BigDecimal.valueOf(100)), 0, RoundingMode.HALF_UP);
    }

    public static String csvCell(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        char first = s.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            s = "'" + s;
        }
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String money(BigDecimal v) {
        return v == null ? "0" : v.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    public static String buildCsv(Invoice inv, String sellerName, String sellerTax, String sellerAddress) {
        StringBuilder sb = new StringBuilder();
        sb.append("seller_name,").append(csvCell(sellerName)).append(EOL);
        sb.append("seller_tax_code,").append(csvCell(sellerTax)).append(EOL);
        sb.append("seller_address,").append(csvCell(sellerAddress)).append(EOL);
        sb.append("buyer_name,").append(csvCell(inv.getBuyerName())).append(EOL);
        sb.append("buyer_tax_code,").append(csvCell(inv.getBuyerTaxCode())).append(EOL);
        sb.append("buyer_address,").append(csvCell(inv.getBuyerAddress())).append(EOL);
        sb.append("invoice_no,").append(csvCell(inv.getInvoiceNo())).append(EOL);
        sb.append("created_date,").append(inv.getCreatedAt() == null ? ""
                : new SimpleDateFormat("yyyy-MM-dd").format(inv.getCreatedAt())).append(EOL);
        sb.append(EOL);
        sb.append("line_no,name,quantity,unit_price,subtotal").append(EOL);
        int n = 1;
        for (InvoiceLine l : inv.getLines()) {
            sb.append(n++).append(',').append(csvCell(l.getName())).append(',').append(l.getQuantity()).append(',')
                    .append(money(l.getUnitPrice())).append(',').append(money(l.getSubtotal())).append(EOL);
        }
        sb.append("total_amount,").append(money(inv.getTotalAmount())).append(EOL);
        sb.append("vat_amount,").append(money(inv.getVatAmount())).append(EOL);
        return sb.toString();
    }

    public Invoice draftFromOrder(int orderId, int actorId) throws SQLException, Refused {
        return draftFromOrder(orderId, actorId, DEFAULT_BUYER, null, null);
    }

    public Invoice draftFromOrder(int orderId, int actorId, String buyerName, String taxCode, String address)
            throws SQLException, Refused {
        Order order = dao.findOrder(orderId);
        if (order == null) {
            throw new Refused("notfound", "Order not found.");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new Refused("notcompleted", "Only a completed order can have an invoice.");
        }
        Invoice inv = base("ORDER", orderId, actorId, buyerName, taxCode, address);
        List<OrderItem> items = itemDAO.findByOrderId(orderId);
        BigDecimal discount = order.getDiscountAmount();
        BigDecimal vatOnTop = order.isVatIncluded() ? BigDecimal.ZERO : order.getVatAmount();
        if (items.isEmpty()) {
            BigDecimal goods = order.getAmount().subtract(vatOnTop).add(discount);
            inv.getLines().add(new InvoiceLine(cut("Counter sale " + order.getOrderCode()), 1, goods, goods));
        } else {
            for (OrderItem it : items) {
                inv.getLines().add(new InvoiceLine(cut(it.getProductName()), it.getQuantity(),
                        it.getUnitPrice(), it.getSubtotal()));
            }
        }
        if (discount.signum() > 0) {
            inv.getLines().add(new InvoiceLine("Discount", 1, discount.negate(), discount.negate()));
        }
        if (vatOnTop.signum() > 0) {
            inv.getLines().add(new InvoiceLine("VAT " + order.getVatRate().stripTrailingZeros().toPlainString() + "%",
                    1, vatOnTop, vatOnTop));
        }
        inv.setTotalAmount(order.getAmount());
        inv.setVatAmount(order.getVatAmount());
        return save(inv);
    }

    public Invoice draftFromChannelSale(int saleId, int actorId) throws SQLException, Refused {
        return draftFromChannelSale(saleId, actorId, DEFAULT_BUYER, null, null);
    }

    public Invoice draftFromChannelSale(int saleId, int actorId, String buyerName, String taxCode, String address)
            throws SQLException, Refused {
        ChannelSale sale = saleDAO.find(saleId);
        if (sale == null) {
            throw new Refused("notfound", "Sale not found.");
        }
        if (!"COMPLETED".equals(sale.getStatus())) {
            throw new Refused("notcompleted", "Only a completed sale can have an invoice.");
        }
        Invoice inv = base("CHANNEL_SALE", saleId, actorId, buyerName, taxCode, address);
        BigDecimal goods = sale.getGoodsAmount();
        inv.getLines().add(new InvoiceLine(cut("Online order " + sale.getExternalRef() + " (" + sale.getChannel() + ")"),
                1, goods, goods));
        inv.setTotalAmount(goods);
        inv.setVatAmount(vatForChannel(goods, settingDAO.getVatRate()));
        return save(inv);
    }

    public boolean updateBuyer(int id, String name, String taxCode, String address) throws SQLException {
        return dao.updateBuyer(id, name, taxCode, address);
    }

    public boolean markSent(int id) throws SQLException {
        return dao.markSent(id);
    }

    public boolean markIssued(int id, String providerNo, String lookupCode) throws SQLException {
        if (!validProviderNo(providerNo) || !validLookupCode(lookupCode)) {
            return false;
        }
        return dao.markIssued(id, providerNo, lookupCode);
    }

    public boolean delete(int id) throws SQLException {
        return dao.deleteDraft(id);
    }

    public String exportCsv(int id) throws SQLException {
        Invoice inv = dao.find(id);
        if (inv == null) {
            return null;
        }
        return buildCsv(inv, settingDAO.get(SELLER_NAME, "My Store"), settingDAO.get(SELLER_TAX_CODE, ""),
                settingDAO.get(SELLER_ADDRESS, ""));
    }

    private Invoice base(String type, int sourceId, int actorId, String name, String taxCode, String address)
            throws SQLException, Refused {
        if (dao.findIdBySource(type, sourceId) != null) {
            throw new Refused("dup", "An invoice already exists for this order or sale.");
        }
        Invoice inv = new Invoice();
        inv.setSourceType(type);
        inv.setSourceId(sourceId);
        inv.setCreatedBy(actorId);
        inv.setBuyerName(name);
        inv.setBuyerTaxCode(taxCode);
        inv.setBuyerAddress(address);
        return inv;
    }

    private Invoice save(Invoice inv) throws SQLException, Refused {
        if (!dao.createDraft(inv)) {
            throw new Refused("dup", "An invoice already exists for this order or sale.");
        }
        return inv;
    }

    private static String cut(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 100 ? s.substring(0, 100) : s;
    }
}
