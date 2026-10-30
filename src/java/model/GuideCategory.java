package model;

public class GuideCategory {

    private final String id;
    private final String title;
    private final String description;
    private final String icon;
    private final int order;

    public GuideCategory(String id, String title, String description, String icon, int order) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.order = order;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public int getOrder() {
        return order;
    }
}
