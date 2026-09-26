package model;

import model.enums.Role;
import model.enums.UserStatus;

/** Một dòng của bảng Users. Chưa có logic, chỉ giữ dữ liệu. */
public class User {

    private int userId;
    private String username;
    private String passwordHash;
    private String salt;
    private String fullName;
    private Role role;
    private Integer storeId;      // NULL với ADMIN / AUDITOR nên dùng Integer, không dùng int
    private UserStatus status;

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Integer getStoreId() { return storeId; }
    public void setStoreId(Integer storeId) { this.storeId = storeId; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
}
