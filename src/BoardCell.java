import java.awt.*;
public class BoardCell {
    private boolean isOn;
    // isHint is true when the cell is marked as a cell to click by the hint tool
    private boolean isHint;

    public BoardCell() {
        this.isOn = false;
        this.isHint = false;
    }

    public void setHint(boolean isHint) {
        this.isHint = isHint;
    }

    public boolean isHint() {
        return isHint;
    }

    public boolean isOn() {
        return isOn;
    }

    public void setState(boolean isOn) {
        this.isOn = isOn;
    }

    public void toggle() {
        isOn = !isOn;
    }

    public void draw(Graphics g, int x, int y, int size) {
        // Outline color
        g.setColor(Color.BLACK);
        if (isHint) {
            g.setColor(Color.RED);
        }
        // Outline
        g.fillRect(x,y,size,size);

        // Center color
        g.setColor(Color.DARK_GRAY);
        if (isOn) {
            g.setColor(Color.WHITE);
        }
        // Center
        final int OUTLINE_WIDTH = 2;
        g.fillRect(x+OUTLINE_WIDTH,y+OUTLINE_WIDTH,size-2*OUTLINE_WIDTH,size-2*OUTLINE_WIDTH);
    }
}