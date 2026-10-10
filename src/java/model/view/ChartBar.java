package model.view;

public class ChartBar {

    private String label;
    private String tip;
    private int x;
    private int width;
    private int legitY;
    private int legitHeight;
    private int flaggedY;
    private int flaggedHeight;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getTip() {
        return tip;
    }

    public void setTip(String tip) {
        this.tip = tip;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getLegitY() {
        return legitY;
    }

    public void setLegitY(int legitY) {
        this.legitY = legitY;
    }

    public int getLegitHeight() {
        return legitHeight;
    }

    public void setLegitHeight(int legitHeight) {
        this.legitHeight = legitHeight;
    }

    public int getFlaggedY() {
        return flaggedY;
    }

    public void setFlaggedY(int flaggedY) {
        this.flaggedY = flaggedY;
    }

    public int getFlaggedHeight() {
        return flaggedHeight;
    }

    public void setFlaggedHeight(int flaggedHeight) {
        this.flaggedHeight = flaggedHeight;
    }
}
