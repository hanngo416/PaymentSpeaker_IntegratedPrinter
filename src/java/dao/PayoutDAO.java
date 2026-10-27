package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Payout;
import model.Wallet;
import util.DBUtil;

public class PayoutDAO {

    private static final String SELECT = "SELECT p.payout_id, p.affiliate_id, a.name AS affiliate_name, "
            + "ISNULL(p.bank_info, ISNULL(a.bank_name + N' ' + a.bank_account, N'')) AS bank_info, "
            + "p.batch_code, p.gross, p.deduction, p.net, p.status, p.reference, p.requested_at, p.paid_at "
            + "FROM Payouts p JOIN Affiliates a ON a.affiliate_id = p.affiliate_id ";

    private static final String WALLET_SQL = "SELECT "
            + "ISNULL((SELECT SUM(amount) FROM Commissions WHERE affiliate_id = ? AND status IN (N'PENDING', N'APPROVED', N'ON_HOLD')), 0) AS pending, "
            + "ISNULL((SELECT SUM(amount) FROM Commissions WHERE affiliate_id = ? AND status = N'AVAILABLE'), 0) "
            + "+ ISNULL((SELECT SUM(amount) FROM CommissionAdjustments WHERE affiliate_id = ? AND payout_id IS NULL), 0) AS available, "
            + "ISNULL((SELECT SUM(net) FROM Payouts WHERE affiliate_id = ? AND status = N'PAID'), 0) AS paid, "
            + "ISNULL((SELECT SUM(amount - original_amount) FROM Commissions WHERE affiliate_id = ?), 0) "
            + "+ ISNULL((SELECT SUM(amount) FROM CommissionAdjustments WHERE affiliate_id = ?), 0) AS adjustments";

    public Wallet wallet(int affiliateId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(WALLET_SQL)) {
            for (int i = 1; i <= 6; i++) {
                ps.setInt(i, affiliateId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                Wallet w = new Wallet();
                if (rs.next()) {
                    w.setPending(rs.getBigDecimal("pending"));
                    w.setAvailable(rs.getBigDecimal("available"));
                    w.setPaid(rs.getBigDecimal("paid"));
                    w.setAdjustments(rs.getBigDecimal("adjustments"));
                }
                return w;
            }
        }
    }

    public Map<Integer, BigDecimal> availableByAffiliate() throws SQLException {
        String sql = "SELECT affiliate_id, SUM(v) AS available FROM ("
                + "SELECT affiliate_id, amount AS v FROM Commissions WHERE status = N'AVAILABLE' "
                + "UNION ALL SELECT affiliate_id, amount FROM CommissionAdjustments WHERE payout_id IS NULL) t GROUP BY affiliate_id";
        Map<Integer, BigDecimal> map = new LinkedHashMap<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getInt("affiliate_id"), rs.getBigDecimal("available"));
            }
        }
        return map;
    }

    public List<Payout> search(String q, String status, Integer affiliateId) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isEmpty()) {
            sql.append("AND (a.name LIKE ? OR p.reference LIKE ? OR p.batch_code LIKE ?) ");
            args.add("%" + q + "%");
            args.add("%" + q + "%");
            args.add("%" + q + "%");
        }
        if (status != null && !status.isEmpty()) {
            sql.append("AND p.status = ? ");
            args.add(status);
        }
        if (affiliateId != null) {
            sql.append("AND p.affiliate_id = ? ");
            args.add(affiliateId);
        }
        sql.append("ORDER BY CASE WHEN p.status = N'REQUESTED' THEN 0 ELSE 1 END, p.requested_at DESC, p.payout_id DESC");
        List<Payout> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < args.size(); i++) {
                ps.setObject(i + 1, args.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public boolean hasOpenRequest(int affiliateId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT 1 FROM Payouts WHERE affiliate_id = ? AND status = N'REQUESTED'")) {
            ps.setInt(1, affiliateId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void request(int affiliateId, BigDecimal gross, BigDecimal deduction, String bankInfo) throws SQLException {
        String sql = "INSERT INTO Payouts (affiliate_id, gross, deduction, net, status, bank_info) VALUES (?, ?, ?, ?, N'REQUESTED', ?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, affiliateId);
            ps.setBigDecimal(2, gross);
            ps.setBigDecimal(3, deduction);
            ps.setBigDecimal(4, gross.subtract(deduction));
            ps.setString(5, bankInfo);
            ps.executeUpdate();
        }
    }

    public int payBatch(List<Integer> affiliateIds, String reference, int userId, BigDecimal withholdPct, BigDecimal withholdFrom) throws SQLException {
        int paid = 0;
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                String batch;
                try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(DISTINCT batch_code) + 1 FROM Payouts WHERE batch_code IS NOT NULL");
                     ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    batch = String.format("BATCH-%02d", rs.getInt(1));
                }
                for (int affId : affiliateIds) {
                    BigDecimal gross = BigDecimal.ZERO;
                    String bank = "";
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT ISNULL(bank_name + N' ' + bank_account, N'') AS bank, "
                            + "ISNULL((SELECT SUM(amount) FROM Commissions WITH (UPDLOCK, HOLDLOCK) WHERE affiliate_id = a.affiliate_id AND status = N'AVAILABLE'), 0) "
                            + "+ ISNULL((SELECT SUM(amount) FROM CommissionAdjustments WITH (UPDLOCK, HOLDLOCK) WHERE affiliate_id = a.affiliate_id AND payout_id IS NULL), 0) AS gross "
                            + "FROM Affiliates a WHERE a.affiliate_id = ? AND a.status <> N'SUSPENDED' AND a.bank_account IS NOT NULL")) {
                        ps.setInt(1, affId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                continue;
                            }
                            gross = rs.getBigDecimal("gross");
                            bank = rs.getString("bank");
                        }
                    }
                    if (gross.signum() <= 0) {
                        continue;
                    }
                    BigDecimal ded = BigDecimal.ZERO;
                    if (gross.compareTo(withholdFrom) >= 0) {
                        ded = gross.multiply(withholdPct).divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
                    }
                    int payoutId;
                    try (PreparedStatement ps = con.prepareStatement("SELECT TOP 1 payout_id FROM Payouts WHERE affiliate_id = ? AND status = N'REQUESTED' ORDER BY payout_id")) {
                        ps.setInt(1, affId);
                        try (ResultSet rs = ps.executeQuery()) {
                            payoutId = rs.next() ? rs.getInt(1) : 0;
                        }
                    }
                    if (payoutId > 0) {
                        try (PreparedStatement ps = con.prepareStatement("UPDATE Payouts SET batch_code = ?, gross = ?, deduction = ?, net = ?, status = N'PAID', bank_info = ?, reference = ?, paid_at = SYSDATETIME(), paid_by = ? WHERE payout_id = ?")) {
                            ps.setString(1, batch);
                            ps.setBigDecimal(2, gross);
                            ps.setBigDecimal(3, ded);
                            ps.setBigDecimal(4, gross.subtract(ded));
                            ps.setString(5, bank);
                            ps.setString(6, reference);
                            ps.setInt(7, userId);
                            ps.setInt(8, payoutId);
                            ps.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement ps = con.prepareStatement("INSERT INTO Payouts (affiliate_id, batch_code, gross, deduction, net, status, bank_info, reference, paid_at, paid_by) VALUES (?, ?, ?, ?, ?, N'PAID', ?, ?, SYSDATETIME(), ?)", java.sql.Statement.RETURN_GENERATED_KEYS)) {
                            ps.setInt(1, affId);
                            ps.setString(2, batch);
                            ps.setBigDecimal(3, gross);
                            ps.setBigDecimal(4, ded);
                            ps.setBigDecimal(5, gross.subtract(ded));
                            ps.setString(6, bank);
                            ps.setString(7, reference);
                            ps.setInt(8, userId);
                            ps.executeUpdate();
                            try (ResultSet keys = ps.getGeneratedKeys()) {
                                keys.next();
                                payoutId = keys.getInt(1);
                            }
                        }
                    }
                    try (PreparedStatement ps = con.prepareStatement("UPDATE Commissions SET status = N'PAID', payout_id = ?, updated_at = SYSDATETIME() WHERE affiliate_id = ? AND status = N'AVAILABLE'")) {
                        ps.setInt(1, payoutId);
                        ps.setInt(2, affId);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = con.prepareStatement("UPDATE CommissionAdjustments SET payout_id = ? WHERE affiliate_id = ? AND payout_id IS NULL")) {
                        ps.setInt(1, payoutId);
                        ps.setInt(2, affId);
                        ps.executeUpdate();
                    }
                    paid++;
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
        return paid;
    }

    private Payout map(ResultSet rs) throws SQLException {
        Payout p = new Payout();
        p.setPayoutId(rs.getInt("payout_id"));
        p.setAffiliateId(rs.getInt("affiliate_id"));
        p.setAffiliateName(rs.getString("affiliate_name"));
        p.setBankInfo(rs.getString("bank_info"));
        p.setBatchCode(rs.getString("batch_code"));
        p.setGross(rs.getBigDecimal("gross"));
        p.setDeduction(rs.getBigDecimal("deduction"));
        p.setNet(rs.getBigDecimal("net"));
        p.setStatus(rs.getString("status"));
        p.setReference(rs.getString("reference"));
        p.setRequestedAt(rs.getTimestamp("requested_at"));
        p.setPaidAt(rs.getTimestamp("paid_at"));
        return p;
    }
}
