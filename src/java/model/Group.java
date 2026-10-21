package model;

public class Group {

    private int groupId;
    private int categoryId;
    private String groupName;
    private int displayOrder;

    public Group() {
    }

    public Group(int groupId, int categoryId, String groupName, int displayOrder) {
        this.groupId = groupId;
        this.categoryId = categoryId;
        this.groupName = groupName;
        this.displayOrder = displayOrder;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
