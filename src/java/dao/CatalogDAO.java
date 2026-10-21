package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import model.Category;
import model.Group;
import model.Page;
import model.Product;
import util.DBUtil;

public class CatalogDAO {

    public List<Category> findAllCategories() throws SQLException {
        String sql = "SELECT category_id, category_name, display_order FROM Categories ORDER BY display_order, category_id";
        List<Category> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Category(rs.getInt("category_id"), rs.getString("category_name"), rs.getInt("display_order")));
            }
        }
        return list;
    }

    public List<Group> findAllGroups() throws SQLException {
        String sql = "SELECT group_id, category_id, group_name, display_order FROM [Groups] ORDER BY category_id, display_order, group_id";
        List<Group> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Group(rs.getInt("group_id"), rs.getInt("category_id"), rs.getString("group_name"), rs.getInt("display_order")));
            }
        }
        return list;
    }

    public List<Product> findActiveProducts() throws SQLException {
        String sql = "SELECT p.product_id, p.group_id, p.product_code, p.product_name, p.price, p.active, p.display_order, "
                + "g.group_name, g.category_id, c.category_name "
                + "FROM Products p "
                + "JOIN [Groups] g ON p.group_id = g.group_id "
                + "JOIN Categories c ON g.category_id = c.category_id "
                + "WHERE p.active = 1 "
                + "ORDER BY c.display_order, g.display_order, p.display_order, p.product_name";
        List<Product> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Product p = new Product();
                p.setProductId(rs.getInt("product_id"));
                p.setGroupId(rs.getInt("group_id"));
                p.setProductCode(rs.getString("product_code"));
                p.setProductName(rs.getString("product_name"));
                p.setPrice(rs.getBigDecimal("price"));
                p.setActive(rs.getBoolean("active"));
                p.setDisplayOrder(rs.getInt("display_order"));
                p.setGroupName(rs.getString("group_name"));
                p.setCategoryId(rs.getInt("category_id"));
                p.setCategoryName(rs.getString("category_name"));
                list.add(p);
            }
        }
        return list;
    }

    public Product findProductById(int productId) throws SQLException {
        String sql = "SELECT p.product_id, p.group_id, p.product_code, p.product_name, p.price, p.active, p.display_order, "
                + "g.group_name, g.category_id, c.category_name "
                + "FROM Products p "
                + "JOIN [Groups] g ON p.group_id = g.group_id "
                + "JOIN Categories c ON g.category_id = c.category_id "
                + "WHERE p.product_id = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Product p = new Product();
                    p.setProductId(rs.getInt("product_id"));
                    p.setGroupId(rs.getInt("group_id"));
                    p.setProductCode(rs.getString("product_code"));
                    p.setProductName(rs.getString("product_name"));
                    p.setPrice(rs.getBigDecimal("price"));
                    p.setActive(rs.getBoolean("active"));
                    p.setDisplayOrder(rs.getInt("display_order"));
                    p.setGroupName(rs.getString("group_name"));
                    p.setCategoryId(rs.getInt("category_id"));
                    p.setCategoryName(rs.getString("category_name"));
                    return p;
                }
            }
        }
        return null;
    }

    public Page<Product> searchProducts(String q, Integer groupId, int page, int pageSize) throws SQLException {
        String from = "FROM Products p "
                + "JOIN [Groups] g ON p.group_id = g.group_id "
                + "JOIN Categories c ON g.category_id = c.category_id "
                + "WHERE (? = N'' OR p.product_name LIKE ? OR p.product_code LIKE ?) "
                + "AND (? IS NULL OR p.group_id = ?) ";
        String like = "%" + q + "%";
        int total;
        List<Product> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) " + from)) {
                bindSearch(ps, q, like, groupId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }
            String sql = "SELECT p.product_id, p.group_id, p.product_code, p.product_name, p.price, p.active, p.display_order, "
                    + "g.group_name, g.category_id, c.category_name " + from
                    + "ORDER BY c.display_order, g.display_order, p.display_order, p.product_name "
                    + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                bindSearch(ps, q, like, groupId);
                ps.setInt(6, (page - 1) * pageSize);
                ps.setInt(7, pageSize);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Product p = new Product();
                        p.setProductId(rs.getInt("product_id"));
                        p.setGroupId(rs.getInt("group_id"));
                        p.setProductCode(rs.getString("product_code"));
                        p.setProductName(rs.getString("product_name"));
                        p.setPrice(rs.getBigDecimal("price"));
                        p.setActive(rs.getBoolean("active"));
                        p.setDisplayOrder(rs.getInt("display_order"));
                        p.setGroupName(rs.getString("group_name"));
                        p.setCategoryId(rs.getInt("category_id"));
                        p.setCategoryName(rs.getString("category_name"));
                        list.add(p);
                    }
                }
            }
        }
        return new Page<>(list, total, page, pageSize);
    }

    private static void bindSearch(PreparedStatement ps, String q, String like, Integer groupId) throws SQLException {
        ps.setString(1, q);
        ps.setString(2, like);
        ps.setString(3, like);
        if (groupId == null) {
            ps.setNull(4, Types.INTEGER);
            ps.setNull(5, Types.INTEGER);
        } else {
            ps.setInt(4, groupId);
            ps.setInt(5, groupId);
        }
    }

    public void insertProduct(Product p) throws SQLException {
        String sql = "INSERT INTO Products (group_id, product_code, product_name, price, active, display_order) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, p.getGroupId());
            ps.setString(2, p.getProductCode());
            ps.setString(3, p.getProductName());
            ps.setBigDecimal(4, p.getPrice());
            ps.setBoolean(5, p.isActive());
            ps.setInt(6, p.getDisplayOrder());
            ps.executeUpdate();
        }
    }

    public void updateProduct(Product p) throws SQLException {
        String sql = "UPDATE Products SET group_id = ?, product_code = ?, product_name = ?, price = ?, active = ?, display_order = ? WHERE product_id = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, p.getGroupId());
            ps.setString(2, p.getProductCode());
            ps.setString(3, p.getProductName());
            ps.setBigDecimal(4, p.getPrice());
            ps.setBoolean(5, p.isActive());
            ps.setInt(6, p.getDisplayOrder());
            ps.setInt(7, p.getProductId());
            ps.executeUpdate();
        }
    }

    public boolean setProductActive(int productId, boolean active) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE Products SET active = ? WHERE product_id = ?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, productId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteUnusedProduct(int productId) throws SQLException {
        String sql = "DELETE FROM Products WHERE product_id = ? AND NOT EXISTS (SELECT 1 FROM OrderItems WHERE product_id = ?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, productId);
            return ps.executeUpdate() == 1;
        }
    }
}
