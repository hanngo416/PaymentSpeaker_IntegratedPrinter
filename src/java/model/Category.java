package model;

public class Category {

    private int categoryId;
    private String categoryName;
    private int displayOrder;

    public Category() {
    }

    public Category(int categoryId, String categoryName, int displayOrder) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.displayOrder = displayOrder;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
