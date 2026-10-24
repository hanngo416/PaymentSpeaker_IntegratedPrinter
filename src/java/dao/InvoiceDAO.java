package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import model.Invoice;
import model.InvoiceLine;
import model.Order;
import model.Page;
import model.enums.OrderStatus;
import service.InvoiceService;
import util.DBUtil;

public class InvoiceDAO {

    private static final String SELECT
            = "SELECT invoice_id, invoice_no, source_type, source_id, buyer_name, buyer_tax_code, buyer_address, "
            + "total_amount, vat_amount, status, provider_invoice_no, provider_lookup_code, created_by, created_at "
            + "FROM Invoices ";

    public boolean createDraft(Invoice inv) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT 1 FROM Invoices WITH (UPDLOCK, HOLDLOCK) WHERE source_type = ? AND source_id = ?")) {
                    ps.setString(1, inv.getSourceType());
                    ps.setInt(2, inv.getSourceId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            con.rollback();
                            return false;
                        }
                    }
                }
                String placeholder = "TMP" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                int id;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO Invoices (invoice_no, source_type, source_id, buyer_name, buyer_tax_code, "
                        + "buyer_address, total_amount, vat_amount, status, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, N'DRAFT', ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, placeholder);
                    ps.setString(2, inv.getSourceType());
                    ps.setInt(3, inv.getSourceId());
                    ps.setString(4, inv.getBuyerName());
                    ps.setString(5, inv.getBuyerTaxCode());
                    ps.setString(6, inv.getBuyerAddress());
                    ps.setBigDecimal(7, inv.getTotalAmount());
                    ps.setBigDecimal(8, inv.getVatAmount());
                    ps.setInt(9, inv.getCreatedBy());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        id = keys.getInt(1);
                    }
                }
                String number = InvoiceService.invoiceNo(id);
                try (PreparedStatement ps = con.prepareStatement("UPDATE Invoices SET invoice_no = ? WHERE invoice_id = ?")) {
                    ps.setString(1, number);
                    ps.setInt(2, id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO InvoiceLines (invoice_id, name, quantity, unit_price, subtotal) VALUES (?, ?, ?, ?, ?)")) {
                    for (InvoiceLine l : inv.getLines()) {
                        ps.setInt(1, id);
                        ps.setString(2, l.getName());
                        ps.setInt(3, l.getQuantity());
                        ps.setBigDecimal(4, l.getUnitPrice());
                        ps.setBigDecimal(5, l.getSubtotal());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                con.commit();
                inv.setInvoiceId(id);
                inv.setInvoiceNo(number);
                inv.setStatus("DRAFT");
                return true;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public Order findOrder(int orderId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT order_id, order_code, amount, ISNULL(discount_amount, 0) AS discount_amount, vat_rate, vat_amount, vat_included, status "
                     + "FROM Orders WHERE order_id = ?")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Order o = new Order();
                o.setOrderId(rs.getInt("order_id"));
                o.setOrderCode(rs.getString("order_code"));
                o.setAmount(rs.getBigDecimal("amount"));
                o.setDiscountAmount(rs.getBigDecimal("discount_amount"));
                o.setVatRate(rs.getBigDecimal("vat_rate"));
                o.setVatAmount(rs.getBigDecimal("vat_amount"));
                o.setVatIncluded(rs.getBoolean("vat_included"));
                o.setStatus(OrderStatus.valueOf(rs.getString("status")));
                return o;
            }
        }
    }

    public Invoice find(int id) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            Invoice inv;
            try (PreparedStatement ps = con.prepareStatement(SELECT + "WHERE invoice_id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    inv = map(rs);
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT name, quantity, unit_price, subtotal FROM InvoiceLines WHERE invoice_id = ? ORDER BY line_id")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        inv.getLines().add(new InvoiceLine(rs.getString(1), rs.getInt(2),
                                rs.getBigDecimal(3), rs.getBigDecimal(4)));
                    }
                }
            }
            return inv;
        }
    }

    public Integer findIdBySource(String sourceType, int sourceId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT invoice_id FROM Invoices WHERE source_type = ? AND source_id = ?")) {
            ps.setString(1, sourceType);
            ps.setInt(2, sourceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    public Page<Invoice> search(String statusOrNull, int page, int size) throws SQLException {
        int total;
        List<Invoice> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM Invoices WHERE (? IS NULL OR status = ?)")) {
                ps.setString(1, statusOrNull);
                ps.setString(2, statusOrNull);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = con.prepareStatement(SELECT + "WHERE (? IS NULL OR status = ?) "
                    + "ORDER BY created_at DESC, invoice_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY")) {
                ps.setString(1, statusOrNull);
                ps.setString(2, statusOrNull);
                ps.setInt(3, (page - 1) * size);
                ps.setInt(4, size);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(map(rs));
                    }
                }
            }
        }
        return new Page<>(list, total, page, size);
    }

    public boolean updateBuyer(int id, String name, String taxCode, String address) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE Invoices SET buyer_name = ?, buyer_tax_code = ?, buyer_address = ? "
                     + "WHERE invoice_id = ? AND status = N'DRAFT'")) {
            ps.setString(1, name);
            ps.setString(2, taxCode);
            ps.setString(3, address);
            ps.setInt(4, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean markSent(int id) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE Invoices SET status = N'SENT_TO_PROVIDER' WHERE invoice_id = ? AND status = N'DRAFT'")) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean markIssued(int id, String providerNo, String lookupCode) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE Invoices SET status = N'ISSUED', provider_invoice_no = ?, provider_lookup_code = ? "
                     + "WHERE invoice_id = ? AND status = N'SENT_TO_PROVIDER'")) {
            ps.setString(1, providerNo);
            ps.setString(2, lookupCode);
            ps.setInt(3, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteDraft(int id) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM Invoices WHERE invoice_id = ? AND status = N'DRAFT'")) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    private Invoice map(ResultSet rs) throws SQLException {
        Invoice i = new Invoice();
        i.setInvoiceId(rs.getInt("invoice_id"));
        i.setInvoiceNo(rs.getString("invoice_no"));
        i.setSourceType(rs.getString("source_type"));
        i.setSourceId(rs.getInt("source_id"));
        i.setBuyerName(rs.getString("buyer_name"));
        i.setBuyerTaxCode(rs.getString("buyer_tax_code"));
        i.setBuyerAddress(rs.getString("buyer_address"));
        i.setTotalAmount(rs.getBigDecimal("total_amount"));
        i.setVatAmount(rs.getBigDecimal("vat_amount"));
        i.setStatus(rs.getString("status"));
        i.setProviderInvoiceNo(rs.getString("provider_invoice_no"));
        i.setProviderLookupCode(rs.getString("provider_lookup_code"));
        i.setCreatedBy(rs.getInt("created_by"));
        i.setCreatedAt(rs.getTimestamp("created_at"));
        return i;
    }
}
