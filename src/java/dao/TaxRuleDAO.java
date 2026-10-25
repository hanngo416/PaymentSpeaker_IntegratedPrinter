package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Page;
import model.TaxParams;
import model.TaxRule;
import model.TaxRuleFilter;
import util.DBUtil;

public class TaxRuleDAO {

    private static final String SELECT
            = "SELECT rule_id, kind, sector, value, effective_from, effective_to, source_ref, verified, active, updated_at FROM TaxRules ";

    private static final String IN_FORCE
            = "active = 1 AND effective_from <= ? AND (effective_to IS NULL OR effective_to >= ?) ";

    public Page<TaxRule> search(TaxRuleFilter f) throws SQLException {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (f.getKind() != null && !f.getKind().isEmpty()) {
            where.append(" AND kind = ?");
            params.add(f.getKind());
        }
        if (f.getQ() != null && !f.getQ().isEmpty()) {
            where.append(" AND (source_ref LIKE ? OR sector LIKE ?)");
            String like = "%" + f.getQ() + "%";
            params.add(like);
            params.add(like);
        }
        int total;
        List<TaxRule> items = new ArrayList<>();
        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM TaxRules" + where)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }
            String sql = SELECT + where
                    + " ORDER BY kind, sector, effective_from DESC, rule_id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                bind(ps, params);
                ps.setInt(params.size() + 1, f.getOffset());
                ps.setInt(params.size() + 2, f.getPageSize());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(map(rs));
                    }
                }
            }
        }
        return new Page<>(items, total, f.getPage(), f.getPageSize());
    }

    public TaxRule findById(int id) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "WHERE rule_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void insert(TaxRule r) throws SQLException {
        String sql = "INSERT INTO TaxRules (kind, sector, value, effective_from, effective_to, source_ref, verified, active) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, r.getKind());
            ps.setString(2, r.getSector());
            ps.setBigDecimal(3, r.getValue());
            ps.setDate(4, r.getEffectiveFrom());
            ps.setDate(5, r.getEffectiveTo());
            ps.setString(6, r.getSourceRef());
            ps.setBoolean(7, r.isVerified());
            ps.setBoolean(8, r.isActive());
            ps.executeUpdate();
        }
    }

    public boolean update(TaxRule r) throws SQLException {
        String sql = "UPDATE TaxRules SET kind = ?, sector = ?, value = ?, effective_from = ?, effective_to = ?, "
                + "source_ref = ?, verified = ?, updated_at = SYSDATETIME() WHERE rule_id = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, r.getKind());
            ps.setString(2, r.getSector());
            ps.setBigDecimal(3, r.getValue());
            ps.setDate(4, r.getEffectiveFrom());
            ps.setDate(5, r.getEffectiveTo());
            ps.setString(6, r.getSourceRef());
            ps.setBoolean(7, r.isVerified());
            ps.setInt(8, r.getRuleId());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean setActive(int id, boolean active) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE TaxRules SET active = ?, updated_at = SYSDATETIME() WHERE rule_id = ?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM TaxRules WHERE rule_id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    public TaxParams loadParams(String sector, Date on) throws SQLException {
        TaxParams p = new TaxParams();
        try (Connection con = DBUtil.getConnection()) {
            p.setThreshold(pick(con, "THRESHOLD", null, on));
            p.setPitCeiling(pick(con, "PIT_CEILING", null, on));
            p.setVatRatePct(pick(con, "VAT_RATE", sector, on));
            p.setPitRatePct(pick(con, "PIT_RATE", sector, on));
            BigDecimal reduction = pick(con, "VAT_REDUCTION", null, on);
            p.setVatReductionPct(reduction == null ? BigDecimal.ZERO : reduction);
            List<BigDecimal> warns = warnLevels(con, on);
            if (warns.size() >= 3) {
                p.setWarn1(warns.get(0));
                p.setWarn2(warns.get(1));
                p.setWarn3(warns.get(2));
            }
        }
        if (p.getThreshold() == null || p.getThreshold().signum() <= 0 || p.getPitCeiling() == null
                || p.getVatRatePct() == null || p.getPitRatePct() == null || p.getWarn3() == null) {
            return null;
        }
        return p;
    }

    public List<TaxRule> inForce(String sector, Date on) throws SQLException {
        List<TaxRule> list = new ArrayList<>();
        String sql = SELECT + "WHERE (sector IS NULL OR sector = ?) AND " + IN_FORCE + "ORDER BY kind, value";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, sector);
            ps.setDate(2, on);
            ps.setDate(3, on);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private BigDecimal pick(Connection con, String kind, String sector, Date on) throws SQLException {
        String sql = "SELECT TOP 1 value FROM TaxRules WHERE kind = ? AND "
                + (sector == null ? "sector IS NULL" : "sector = ?") + " AND " + IN_FORCE
                + "ORDER BY effective_from DESC, rule_id DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, kind);
            if (sector != null) {
                ps.setString(i++, sector);
            }
            ps.setDate(i++, on);
            ps.setDate(i, on);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : null;
            }
        }
    }

    private List<BigDecimal> warnLevels(Connection con, Date on) throws SQLException {
        List<BigDecimal> list = new ArrayList<>();
        String sql = "SELECT value FROM TaxRules WHERE kind = 'WARN_LEVEL' AND " + IN_FORCE + "ORDER BY value";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, on);
            ps.setDate(2, on);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getBigDecimal(1));
                }
            }
        }
        return list;
    }

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private static TaxRule map(ResultSet rs) throws SQLException {
        TaxRule r = new TaxRule();
        r.setRuleId(rs.getInt("rule_id"));
        r.setKind(rs.getString("kind"));
        r.setSector(rs.getString("sector"));
        r.setValue(rs.getBigDecimal("value"));
        r.setEffectiveFrom(rs.getDate("effective_from"));
        r.setEffectiveTo(rs.getDate("effective_to"));
        r.setSourceRef(rs.getString("source_ref"));
        r.setVerified(rs.getBoolean("verified"));
        r.setActive(rs.getBoolean("active"));
        r.setUpdatedAt(rs.getTimestamp("updated_at"));
        return r;
    }
}
