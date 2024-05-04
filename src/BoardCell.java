import java.awt.*;
public class BoardCell {
    private boolean isOn;
    private boolean isHint;

    public BoardCell(boolean isOn) {
        this.isOn = isOn;
        this.isHint = false;
    }

    public BoardCell(BoardCell inputCell) {
        this.isOn = inputCell.isOn;
        this.isHint = inputCell.isHint;
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
        g.fillRect(x+2,y+2,size-4,size-4);
    }
}